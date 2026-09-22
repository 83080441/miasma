package dev.sdfg.mod.worldgen;

import dev.sdfg.mod.entity.WarpSubtype;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Deterministic Warp sites from world seed + region cell coordinates.
 * XZ and attributes are fully predictable; Y is resolved later from the surface heightmap.
 */
public final class WarpPlacement {
    /** Salt for {@link WorldgenRandom#setLargeFeatureWithSalt}. */
    public static final int FEATURE_SALT = 0x57DF_601D;

    public static final int DEFAULT_REGION_SIZE = 384;
    /** 180 / 10000 = 18%. */
    public static final int DEFAULT_CHANCE_PERMILLE = 180;

    private static final int EDGE_MARGIN = 16;

    private WarpPlacement() {
    }

    /**
     * Predicted Warp site for one region cell, if the cell rolls a hit.
     *
     * @param seed            world seed
     * @param cellX           {@code floorDiv(blockX, regionSize)}
     * @param cellZ           {@code floorDiv(blockZ, regionSize)}
     * @param regionSize      cell size in blocks (min 32)
     * @param chancePermille  chance out of 10000 (0–10000)
     */
    public static Optional<WarpSite> findInCell(
            long seed,
            int cellX,
            int cellZ,
            int regionSize,
            int chancePermille
    ) {
        int size = Math.max(32, regionSize);
        int chance = Mth.clamp(chancePermille, 0, 10000);

        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, cellX, cellZ, FEATURE_SALT);

        if (random.nextInt(10000) >= chance) {
            return Optional.empty();
        }

        int margin = Math.min(EDGE_MARGIN, size / 4);
        int span = Math.max(1, size - 2 * margin);
        int x = cellX * size + margin + random.nextInt(span);
        int z = cellZ * size + margin + random.nextInt(span);

        WarpSubtype subtype = WarpSubtype.byOrdinalSafe(random.nextInt(WarpSubtype.values().length));
        int distortion = 1 + random.nextInt(100);
        int force = 1 + random.nextInt(100);

        return Optional.of(new WarpSite(x, z, subtype, distortion, force));
    }

    public static int cellCoord(int blockCoord, int regionSize) {
        return Math.floorDiv(blockCoord, Math.max(32, regionSize));
    }

    /**
     * All predicted sites whose XZ lie within {@code radius} of {@code (centerX, centerZ)}.
     */
    public static List<WarpSite> findInRadius(
            long seed,
            int centerX,
            int centerZ,
            int radius,
            int regionSize,
            int chancePermille
    ) {
        int size = Math.max(32, regionSize);
        int r = Math.max(0, radius);
        int minX = centerX - r;
        int maxX = centerX + r;
        int minZ = centerZ - r;
        int maxZ = centerZ + r;

        int minCellX = cellCoord(minX, size);
        int maxCellX = cellCoord(maxX, size);
        int minCellZ = cellCoord(minZ, size);
        int maxCellZ = cellCoord(maxZ, size);

        List<WarpSite> sites = new ArrayList<>();
        long radiusSq = (long) r * (long) r;
        for (int cx = minCellX; cx <= maxCellX; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                findInCell(seed, cx, cz, size, chancePermille).ifPresent(site -> {
                    long dx = (long) site.x() - centerX;
                    long dz = (long) site.z() - centerZ;
                    if (dx * dx + dz * dz <= radiusSq) {
                        sites.add(site);
                    }
                });
            }
        }
        return sites;
    }

    /**
     * XZ + attributes for a Warp. Y is filled at spawn from the surface heightmap.
     */
    public record WarpSite(int x, int z, WarpSubtype subtype, int distortion, int force) {
        public BlockPos atY(int y) {
            return new BlockPos(this.x, y, this.z);
        }
    }
}
