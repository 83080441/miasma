package dev.sdfg.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.sdfg.mod.element.ElementDiscovery;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /element unlock <level>} grants that particle catalog.
 * Level 1 is every element that exists today.
 */
@EventBusSubscriber(modid = ExampleMod.MODID)
public final class ElementCommands {
    private ElementCommands() {
    }

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("element")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> unlock(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "level")
                                        ))))
        );
    }

    private static int unlock(CommandSourceStack source, int level) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ElementDiscovery.UnlockResult result = ElementDiscovery.unlockLevel(player, level);
        if (result.matched() == 0) {
            source.sendFailure(Component.translatable("sdfg.command.element.unlock.empty", level));
            return 0;
        }
        if (result.newly() == 0) {
            source.sendSuccess(() -> Component.translatable("sdfg.command.element.unlock.already", level), true);
            return result.matched();
        }
        int gained = result.newly();
        source.sendSuccess(() -> Component.translatable("sdfg.command.element.unlock.done", level, gained), true);
        return gained;
    }
}
