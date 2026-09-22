package dev.sdfg.mod.worldgen;

import com.mojang.serialization.Codec;
import dev.sdfg.mod.Config;
import dev.sdfg.mod.entity.ModEntities;
import dev.sdfg.mod.entity.WarpEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Places sparse surface Warps once during chunk generation.
 * Positions come from {@link WarpPlacement}; destroyed Warps are never respawned.
 */
public class WarpSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    /** Cells that already received a Warp this JVM session (dedupes multi-biome feature calls). */
    private static final Set<Long> SPAWNED_CELLS = ConcurrentHashMap.newKeySet();

    public WarpSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!Config.WARP_GEN_ENABLED.getAsBoolean()) {
            return false;
        }

        WorldGenLevel level = context.level();
        ServerLevel server = level.getLevel();
        if (!server.dimension().equals(ServerLevel.OVERWORLD)) {
            return false;
        }

        int regionSize = Config.WARP_GEN_REGION_SIZE.getAsInt();
        int chancePermille = Config.WARP_GEN_CHANCE_PERMILLE.getAsInt();
        long seed = level.getSeed();

        ChunkPos chunkPos = ChunkPos.containing(context.origin());
        int minBlockX = chunkPos.getMinBlockX();
        int maxBlockX = chunkPos.getMaxBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();
        int maxBlockZ = chunkPos.getMaxBlockZ();

        int minCellX = WarpPlacement.cellCoord(minBlockX, regionSize);
        int maxCellX = WarpPlacement.cellCoord(maxBlockX, regionSize);
        int minCellZ = WarpPlacement.cellCoord(minBlockZ, regionSize);
        int maxCellZ = WarpPlacement.cellCoord(maxBlockZ, regionSize);

        boolean placed = false;
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                long cellKey = packCell(cellX, cellZ);
                if (SPAWNED_CELLS.contains(cellKey)) {
                    continue;
                }

                var siteOpt = WarpPlacement.findInCell(seed, cellX, cellZ, regionSize, chancePermille);
                if (siteOpt.isEmpty()) {
                    continue;
                }

                WarpPlacement.WarpSite site = siteOpt.get();
                if (site.x() < minBlockX || site.x() > maxBlockX || site.z() < minBlockZ || site.z() > maxBlockZ) {
                    continue;
                }

                if (!trySpawn(server, level, site)) {
                    continue;
                }

                SPAWNED_CELLS.add(cellKey);
                placed = true;
            }
        }
        return placed;
    }

    private static boolean trySpawn(ServerLevel server, WorldGenLevel level, WarpPlacement.WarpSite site) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, site.x(), site.z());
        if (surfaceY <= level.getMinY() || surfaceY >= level.getMaxY()) {
            return false;
        }

        // Float 2–3 blocks above the floor (heightmap Y = first air above ground).
        // Prefer +3 when clear, else +2; skip if neither column is free.
        Integer spawnY = pickHoverY(level, site.x(), surfaceY, site.z());
        if (spawnY == null) {
            return false;
        }

        BlockPos pos = site.atY(spawnY);
        AABB near = new AABB(pos).inflate(1.0);
        if (!server.getEntitiesOfClass(WarpEntity.class, near).isEmpty()) {
            return false;
        }

        WarpEntity warp = ModEntities.ENTITY1.get().create(server, EntitySpawnReason.CHUNK_GENERATION);
        if (warp == null) {
            return false;
        }

        warp.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        warp.setSubtype(site.subtype());
        warp.setDistortion(site.distortion());
        warp.setForce(site.force());
        warp.snapToBlockCenter();

        return level.addFreshEntity(warp);
    }

    /**
     * Heightmap Y is the first air above the floor. +2 ≈ three blocks up, +1 ≈ two blocks up.
     */
    private static Integer pickHoverY(WorldGenLevel level, int x, int surfaceY, int z) {
        int y3 = surfaceY + 2;
        if (isHoverSpotFree(level, x, y3, z)) {
            return y3;
        }
        int y2 = surfaceY + 1;
        if (isHoverSpotFree(level, x, y2, z)) {
            return y2;
        }
        return null;
    }

    private static boolean isHoverSpotFree(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY()) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isAir();
    }

    private static long packCell(int cellX, int cellZ) {
        return ((long) cellX << 32) ^ (cellZ & 0xFFFF_FFFFL);
    }
}
