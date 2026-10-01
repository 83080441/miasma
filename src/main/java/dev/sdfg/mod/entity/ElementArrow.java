package dev.sdfg.mod.entity;

import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.Element;
import dev.sdfg.mod.particle.ModParticles;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bolt shot. Earth arcs like an arrow, water is thrown in a steeper arc and bursts,
 * fire flies straight and slows down. Damage ignores armor.
 */
public class ElementArrow extends AbstractArrow {
    /** Blocks of travel before the shot disappears. */
    public static final double MAX_RANGE = 15.0;
    /** Same red as the earth spike in {@link dev.sdfg.mod.client.ElementArrowRenderer}. */
    private static final int EARTH_TRAIL = 0xEB1F24;
    /** Two hearts. Earth. */
    private static final float EARTH_DAMAGE = 4.0F;
    /** Half a heart, plus the burn. Fire. */
    private static final float FIRE_DAMAGE = 1.0F;
    /** Direct hit is one heart, and the burst adds half a heart, for one and a half. */
    private static final float WATER_DIRECT = 3.0F;
    /** Half a heart across the 3×3 around a water burst. */
    private static final float WATER_SPLASH = 1.0F;
    private static final float BURN_SECONDS = 4.0F;
    /** How far the water burst reaches from the impact, half of a 3-block side. */
    private static final double SPLASH_RADIUS = 1.5;
    /** Ignores armor and Protection. See {@code data/sdfg/damage_type/bolt.json}. */
    private static final ResourceKey<DamageType> BOLT_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(ExampleMod.MODID, "bolt")
    );

    private static final EntityDataAccessor<Byte> DATA_ELEMENT =
            SynchedEntityData.defineId(ElementArrow.class, EntityDataSerializers.BYTE);

    private Element element = Element.EARTH;
    private Vec3 origin;

    public ElementArrow(EntityType<? extends ElementArrow> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
    }

    public ElementArrow(Level level, LivingEntity owner, Element element) {
        super(ModEntities.ELEMENT_ARROW.get(), owner, level, new ItemStack(Items.ARROW), null);
        this.setElement(element);
        this.pickup = Pickup.DISALLOWED;
        this.applyFlight();
    }

    /** Aimed along {@code look}. Water lofts into a throw; earth and fire follow the look. */
    public void launch(Vec3 look) {
        Element flying = this.element();
        double y = look.y;
        float power = 1.75F;
        if (flying == Element.WATER) {
            y += 0.45;
            power = 0.9F;
        } else if (flying == Element.FIRE) {
            power = 1.55F;
        }
        this.shoot(look.x, y, look.z, power, 0.0F);
        this.pickup = Pickup.DISALLOWED;
    }

    public Element element() {
        Element synced = Element.byNumber(this.entityData.get(DATA_ELEMENT) & 255);
        return synced == null ? this.element : synced;
    }

    private void setElement(Element element) {
        this.element = element == null ? Element.EARTH : element;
        this.entityData.set(DATA_ELEMENT, (byte) this.element.number());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, (byte) Element.EARTH.number());
    }

    @Override
    protected double getDefaultGravity() {
        return this.element() == Element.FIRE ? 0.0 : 0.05;
    }

    @Override
    protected float getAirDrag() {
        return this.element() == Element.FIRE ? 0.86F : super.getAirDrag();
    }

    private void applyFlight() {
        this.setNoGravity(this.element() == Element.FIRE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_ELEMENT.equals(accessor)) {
            this.applyFlight();
        }
    }

    @Override
    public void tick() {
        this.applyFlight();
        if (this.origin == null) {
            this.origin = this.position();
        }
        if (this.isInGround()) {
            this.discard();
            return;
        }
        super.tick();
        if (!this.isAlive() || this.isInGround()) {
            return;
        }
        if (this.element() == Element.FIRE && this.getDeltaMovement().lengthSqr() < 0.02) {
            this.discard();
            return;
        }
        if (this.position().distanceTo(this.origin) >= MAX_RANGE) {
            if (this.element() == Element.WATER && this.level() instanceof ServerLevel) {
                this.burst(this.position(), null);
            }
            this.discard();
            return;
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Element flying = this.element();
        if (flying == Element.FIRE) {
            server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 3, 0.04, 0.04, 0.04, 0.01);
        } else if (flying == Element.WATER) {
            server.sendParticles(
                    ColorParticleOption.create(ModParticles.CAULDRON_BUBBLE.get(), 0xFF000000 | flying.color()),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    2,
                    0.08,
                    0.08,
                    0.08,
                    0.0
            );
        } else {
            int rgb = flying == Element.EARTH ? EARTH_TRAIL : flying.color();
            server.sendParticles(
                    ColorParticleOption.create(ModParticles.FLIGHT_MOTE.get(), 0xFF000000 | rgb),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    3,
                    0.04,
                    0.04,
                    0.04,
                    0.0
            );
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        Entity target = hit.getEntity();
        Element flying = this.element();
        if (flying == Element.WATER) {
            this.burst(hit.getLocation(), target);
            this.hurt(target, WATER_DIRECT);
            this.playSound(SoundEvents.PLAYER_SPLASH, 1.0F, 1.0F);
        } else if (flying == Element.FIRE) {
            if (this.hurt(target, FIRE_DAMAGE)) {
                target.igniteForSeconds(BURN_SECONDS);
            }
            this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F);
        } else {
            this.hurt(target, EARTH_DAMAGE);
            this.playSound(SoundEvents.ARROW_HIT, 1.0F, 0.9F);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (this.element() == Element.WATER) {
            if (this.level() instanceof ServerLevel) {
                this.burst(hit.getLocation(), null);
                this.playSound(SoundEvents.PLAYER_SPLASH, 1.0F, 1.0F);
                this.discard();
            }
            return;
        }
        super.onHitBlock(hit);
    }

    /** Half a heart to living things in the 3×3 around the burst, except the direct target and the caster. */
    private void burst(Vec3 at, Entity direct) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Entity owner = this.getOwner();
        AABB area = new AABB(
                at.x - SPLASH_RADIUS, at.y - 0.5, at.z - SPLASH_RADIUS,
                at.x + SPLASH_RADIUS, at.y + 2.0, at.z + SPLASH_RADIUS
        );
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity != owner && entity != direct)) {
            living.hurtServer(level, this.boltDamage(owner), WATER_SPLASH);
        }
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 24, 0.7, 0.3, 0.7, 0.0);
        level.sendParticles(
                ColorParticleOption.create(ModParticles.CAULDRON_BUBBLE.get(), 0xFF000000 | Element.WATER.color()),
                at.x,
                at.y,
                at.z,
                12,
                0.6,
                0.2,
                0.6,
                0.0
        );
    }

    /** Armor and Protection do not reduce it. */
    private boolean hurt(Entity target, float damage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return false;
        }
        Entity owner = this.getOwner();
        if (target == owner) {
            return false;
        }
        if (owner instanceof LivingEntity living) {
            living.setLastHurtMob(target);
        }
        return target.hurtServer(level, this.boltDamage(owner), damage);
    }

    private DamageSource boltDamage(Entity owner) {
        return this.damageSources().source(BOLT_DAMAGE, this, owner != null ? owner : this);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Element", this.element().id());
        Vec3 from = this.origin == null ? this.position() : this.origin;
        output.putDouble("OriginX", from.x);
        output.putDouble("OriginY", from.y);
        output.putDouble("OriginZ", from.z);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setElement(Element.byId(input.getStringOr("Element", Element.EARTH.id())));
        this.origin = new Vec3(
                input.getDoubleOr("OriginX", this.getX()),
                input.getDoubleOr("OriginY", this.getY()),
                input.getDoubleOr("OriginZ", this.getZ())
        );
        this.pickup = Pickup.DISALLOWED;
        this.applyFlight();
    }
}
