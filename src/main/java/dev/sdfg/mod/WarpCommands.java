package dev.sdfg.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.sdfg.mod.entity.WarpEntity;
import dev.sdfg.mod.worldgen.WarpPlacement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Dev/helper commands for Warp entities.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class WarpCommands {
    private static final int DEFAULT_LOCATE_RADIUS = 768;
    private static final int MAX_LOCATE_RADIUS = 4096;
    /** Fallback search when no WarpEntity is loaded yet. */
    private static final int DEFAULT_TP_SEARCH_RADIUS = 4096;

    private WarpCommands() {
    }

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("warpkill")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> killAllWarps(ctx.getSource()))
        );

        dispatcher.register(
                Commands.literal("killwarps")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> killAllWarps(ctx.getSource()))
        );

        dispatcher.register(
                Commands.literal("warplocate")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> locateWarps(ctx.getSource(), DEFAULT_LOCATE_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_LOCATE_RADIUS))
                                .executes(ctx -> locateWarps(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                )))
        );

        dispatcher.register(
                Commands.literal("warptp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> teleportToNearestWarp(ctx.getSource(), DEFAULT_TP_SEARCH_RADIUS))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_LOCATE_RADIUS))
                                .executes(ctx -> teleportToNearestWarp(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                )))
        );
    }

    private static int killAllWarps(CommandSourceStack source) {
        int removed = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof WarpEntity) {
                    entity.kill(level);
                    removed++;
                }
            }
        }
        int count = removed;
        source.sendSuccess(() -> Component.literal("Removed " + count + " Warp(s)"), true);
        return removed;
    }

    /**
     * Teleports the player to the nearest Warp: loaded entity first, else predicted site.
     */
    private static int teleportToNearestWarp(CommandSourceStack source, int radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        Vec3 from = player.position();

        WarpEntity nearestEntity = null;
        double bestEntityDistSq = Double.MAX_VALUE;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof WarpEntity warp) || !warp.isAlive()) {
                continue;
            }
            double d = warp.distanceToSqr(from);
            if (d < bestEntityDistSq) {
                bestEntityDistSq = d;
                nearestEntity = warp;
            }
        }

        if (nearestEntity != null) {
            final WarpEntity warp = nearestEntity;
            double x = warp.getX();
            double y = from.y; // keep caller's height — avoid burying under terrain
            double z = warp.getZ();
            player.teleportTo(x, y, z);
            double dist = Math.sqrt(bestEntityDistSq);
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "Teleported to Warp at [%.1f, %.1f, %.1f] (%s, dist=%.0f)",
                            x, y, z, warp.getSubtype().id(), dist
                    )),
                    true
            );
            return 1;
        }

        int centerX = BlockPos.containing(from).getX();
        int centerZ = BlockPos.containing(from).getZ();
        int regionSize = Config.WARP_GEN_REGION_SIZE.getAsInt();
        int chancePermille = Config.WARP_GEN_CHANCE_PERMILLE.getAsInt();

        List<WarpPlacement.WarpSite> sites = WarpPlacement.findInRadius(
                level.getSeed(),
                centerX,
                centerZ,
                radius,
                regionSize,
                chancePermille
        );
        if (sites.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No Warps loaded and no predicted sites within " + radius + " blocks."
            ));
            return 0;
        }

        sites.sort(Comparator.comparingLong(s -> distSq(centerX, centerZ, s.x(), s.z())));
        WarpPlacement.WarpSite site = sites.get(0);
        double y = from.y;
        player.teleportTo(site.x() + 0.5, y, site.z() + 0.5);
        long dist = Math.round(Math.sqrt(distSq(centerX, centerZ, site.x(), site.z())));
        source.sendSuccess(
                () -> Component.literal(String.format(
                        "Teleported to predicted Warp [%.1f, %.1f, %.1f] %s (dist=%d; chunk may still generate)",
                        site.x() + 0.5, y, site.z() + 0.5, site.subtype().id(), dist
                )),
                true
        );
        return 1;
    }

    /**
     * Lists predicted Warp XZ sites from the seed formula (does not spawn).
     */
    private static int locateWarps(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        int centerX = BlockPos.containing(pos).getX();
        int centerZ = BlockPos.containing(pos).getZ();

        int regionSize = Config.WARP_GEN_REGION_SIZE.getAsInt();
        int chancePermille = Config.WARP_GEN_CHANCE_PERMILLE.getAsInt();

        List<WarpPlacement.WarpSite> sites = WarpPlacement.findInRadius(
                level.getSeed(),
                centerX,
                centerZ,
                radius,
                regionSize,
                chancePermille
        );
        sites.sort(Comparator
                .comparingLong((WarpPlacement.WarpSite s) -> distSq(centerX, centerZ, s.x(), s.z()))
                .thenComparingInt(WarpPlacement.WarpSite::x)
                .thenComparingInt(WarpPlacement.WarpSite::z));

        if (sites.isEmpty()) {
            source.sendSuccess(
                    () -> Component.literal("No predicted Warps within " + radius + " blocks (seed formula)."),
                    false
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Predicted Warps within " + radius + " (XZ; Y = surface at gen):"),
                false
        );
        for (WarpPlacement.WarpSite site : sites) {
            long dist = Math.round(Math.sqrt(distSq(centerX, centerZ, site.x(), site.z())));
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "  [%d, ?, %d] %s dist=%d distortion=%d force=%d",
                            site.x(),
                            site.z(),
                            site.subtype().id(),
                            dist,
                            site.distortion(),
                            site.force()
                    )),
                    false
            );
        }
        return sites.size();
    }

    private static long distSq(int x0, int z0, int x1, int z1) {
        long dx = (long) x1 - x0;
        long dz = (long) z1 - z0;
        return dx * dx + dz * dz;
    }
}
