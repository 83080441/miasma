package dev.sdfg.mod;

import dev.sdfg.mod.entity.WarpEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Dev/helper commands for Warp entities.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class WarpCommands {
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

        // Alias: clear every Warp in loaded worlds.
        dispatcher.register(
                Commands.literal("killwarps")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> killAllWarps(ctx.getSource()))
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
}
