package dev.sdfg.mod.entity;

import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A plain arrow whose only extra is the element from the scroll.
 * It cannot be picked up. While it flies it leaves motes of that element.
 */
public class ElementArrow extends AbstractArrow {
    private Element element = Element.EARTH;

    public ElementArrow(EntityType<? extends ElementArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public ElementArrow(Level level, LivingEntity owner, Element element) {
        super(ModEntities.ELEMENT_ARROW.get(), owner, level, new ItemStack(Items.ARROW), null);
        this.element = element == null ? Element.EARTH : element;
        this.pickup = Pickup.DISALLOWED;
    }

    public Element element() {
        return this.element;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server) || this.isInGround()) {
            return;
        }
        server.sendParticles(
                ColorParticleOption.create(ModParticles.FLIGHT_MOTE.get(), 0xFF000000 | this.element.color()),
                this.getX(),
                this.getY(),
                this.getZ(),
                0,
                0.0,
                0.0,
                0.0,
                1.0
        );
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Element", this.element.id());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Element read = Element.byId(input.getStringOr("Element", Element.EARTH.id()));
        this.element = read == null ? Element.EARTH : read;
        this.pickup = Pickup.DISALLOWED;
    }
}
