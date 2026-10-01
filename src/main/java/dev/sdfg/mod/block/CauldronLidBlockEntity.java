package dev.sdfg.mod.block;

import java.util.ArrayList;
import java.util.List;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Once a second, if pipes reach a container, pulls one random element out of the cauldron below.
 * Other elements stay in the bowl.
 */
public class CauldronLidBlockEntity extends BlockEntity {
    public static final int EXTRACT_INTERVAL = 20;

    public CauldronLidBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAULDRON_LID.get(), pos, state);
    }

    public void serverTick() {
        Level level = this.level;
        if (level == null || level.isClientSide() || level.getGameTime() % EXTRACT_INTERVAL != 0) {
            return;
        }
        BlockPos cauldronPos = this.worldPosition.below();
        if (!(level.getBlockEntity(cauldronPos) instanceof HeatedCauldronBlockEntity cauldron)) {
            return;
        }
        ElementAmounts contents = cauldron.contents();
        if (contents.isEmpty()) {
            return;
        }
        BlockPos target = PipeNetwork.findContainer(level, this.worldPosition);
        if (target == null || !(level.getBlockEntity(target) instanceof ElementContainerBlockEntity container)) {
            return;
        }
        List<Element> present = new ArrayList<>(contents.present());
        Element element = present.get(level.getRandom().nextInt(present.size()));
        int moved = container.insert(element, contents.get(element));
        if (moved <= 0) {
            return;
        }
        cauldron.drain(element, moved);
        level.playSound(null, this.worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.35F, 1.4F);
        if (level instanceof ServerLevel server) {
            int color = 0xFF000000 | element.color();
            server.sendParticles(
                    ColorParticleOption.create(ModParticles.ELEMENT_MOTE.get(), color),
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.4,
                    this.worldPosition.getZ() + 0.5,
                    4, 0.12, 0.12, 0.12, 0.01
            );
            server.sendParticles(
                    ColorParticleOption.create(ModParticles.ELEMENT_MOTE.get(), color),
                    target.getX() + 0.5,
                    target.getY() + 0.5,
                    target.getZ() + 0.5,
                    6, 0.2, 0.2, 0.2, 0.01
            );
        }
    }
}
