package dev.sdfg.mod.client;

import java.lang.reflect.Field;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/**
 * Zeroes walking after the keyboard has already written the move vector.
 * That field is protected, so it is reached by reflection instead of a class
 * in Minecraft's own package, which the module system will not split.
 */
public final class ChannelInputLock {
    private static final Field MOVE_VECTOR = moveVectorField();

    private ChannelInputLock() {
    }

    public static void stop(ClientInput input) {
        input.keyPresses = Input.EMPTY;
        if (MOVE_VECTOR == null) {
            return;
        }
        try {
            MOVE_VECTOR.set(input, Vec2.ZERO);
        } catch (IllegalAccessException ignored) {
            // The post-tick snap still holds the player if this field stays closed.
        }
    }

    private static Field moveVectorField() {
        try {
            Field field = ClientInput.class.getDeclaredField("moveVector");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
