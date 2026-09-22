package dev.sdfg.mod.entity;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Floating warp that distorts the view behind it.
 * Sized 0.5×0.5, snapped to block center. Distortion 1–100. Force 1–100 (gravity well).
 * Non-static subtypes pull items, players, and mobs (12 blocks); items are destroyed on contact;
 * touching living entities take Force-scaled damage once per second.
 */
public class WarpEntity extends Entity {
    public static final int MIN_DISTORTION = 1;
    public static final int MAX_DISTORTION = 100;
    public static final int DEFAULT_DISTORTION = 50;

    public static final int MIN_FORCE = 1;
    public static final int MAX_FORCE = 100;
    /** Default: slow drift. 100 = original aggressive pull strength. */
    public static final int DEFAULT_FORCE = 25;

    /** Gravity well radius for items, players, and mobs (blocks). */
    public static final double PULL_RANGE = 12.0;
    /** Distance at which an item is considered touching the warp. */
    private static final double ITEM_CONSUME_DISTANCE = 0.55;
    /** Touch radius for living damage (player/mob center to warp). */
    private static final double LIVING_TOUCH_DISTANCE = 0.9;
    /** Damage tick interval (20 ticks = 1 second). */
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    /** Half-heart (1 HP) per 10 Force → damage = Force / 10. */
    private static final float DAMAGE_PER_TEN_FORCE = 1.0F;
    /** Acceleration scale at Force 100 (legacy “full” pull). */
    private static final double PULL_ACCEL_AT_FULL_FORCE = 1.0;
    /** Living entities use a gentler multiplier than items at the same Force. */
    private static final double LIVING_PULL_MULTIPLIER = 0.55;
    /** Non-player mobs: much stronger well so they cannot casually walk out. */
    private static final double MOB_PULL_MULTIPLIER = LIVING_PULL_MULTIPLIER * 4.0;
    /**
     * Max inward acceleration added to players per tick (blocks/tick).
     * Vanilla walk ≈ 0.216; keep well below so walking always escapes.
     */
    private static final double MAX_LIVING_PULL_ACCEL = 0.10;
    /**
     * Cap for non-player mobs — above walk/sprint AI so the well wins.
     * (Used only if living-safe path is taken; mobs use the aggressive pull path.)
     */
    private static final double MAX_MOB_PULL_ACCEL = 0.50;

    private static final EntityDataAccessor<Integer> DATA_DISTORTION =
            SynchedEntityData.defineId(WarpEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SUBTYPE =
            SynchedEntityData.defineId(WarpEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FORCE =
            SynchedEntityData.defineId(WarpEntity.class, EntityDataSerializers.INT);

    private boolean snappedToBlock;
    /** Last game time (tick) when each living entity was damaged by this warp. */
    private final Map<UUID, Long> lastTouchDamageTime = new HashMap<>();

    public WarpEntity(EntityType<? extends WarpEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        if (!this.snappedToBlock) {
            this.snapToBlockCenter();
            this.snappedToBlock = true;
        }

        if (!this.level().isClientSide() && this.getSubtype() != WarpSubtype.STATIC) {
            this.applyGravityWell();
            if (this.tickCount % 100 == 0) {
                this.pruneDamageCooldowns();
            }
        }
    }

    private void applyGravityWell() {
        Vec3 center = this.position();
        AABB search = this.getBoundingBox().inflate(PULL_RANGE);
        double forceScale = this.getForceFactor() * PULL_ACCEL_AT_FULL_FORCE;
        this.pullAndConsumeItems(center, search, forceScale);
        this.pullLivingEntities(center, search, forceScale);
    }

    /**
     * Attracts item entities within {@link #PULL_RANGE}; stronger when closer.
     * Pull strength scales with {@link #getForce()} (1–100; 100 = full legacy strength).
     * On contact, discards the item and logs {@code item destroy: <id>} for future mechanics.
     */
    private void pullAndConsumeItems(Vec3 center, AABB search, double forceScale) {
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, search)) {
            if (!item.isAlive() || item.getItem().isEmpty()) {
                continue;
            }

            double dist = center.distanceTo(item.position());
            if (dist > PULL_RANGE) {
                continue;
            }

            if (dist <= ITEM_CONSUME_DISTANCE || item.getBoundingBox().intersects(this.getBoundingBox())) {
                this.consumeItem(item);
                continue;
            }

            this.applyPull(item, center, forceScale, 1.0, true, false, MAX_LIVING_PULL_ACCEL);
        }
    }

    /**
     * Attracts players and mobs within {@link #PULL_RANGE}.
     * On touch, deals Force-scaled magic damage once per second
     * ({@code Force/10} HP = half a heart per 10 Force).
     * Spectators are ignored. Passengers: the root vehicle is pulled; damage hits the living.
     */
    private void pullLivingEntities(Vec3 center, AABB search, double forceScale) {
        AABB touchBox = this.getBoundingBox().inflate(0.15);

        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, search)) {
            if (!living.isAlive() || living.isSpectator()) {
                continue;
            }

            Entity target = living.getRootVehicle();
            if (target == this) {
                continue;
            }

            double dist = center.distanceTo(target.position());
            if (dist > PULL_RANGE || dist < 1.0E-4) {
                continue;
            }

            boolean touching = dist <= LIVING_TOUCH_DISTANCE || living.getBoundingBox().intersects(touchBox);
            if (touching) {
                this.tryTouchDamage(living);
            }

            boolean isPlayer = living instanceof Player;
            double strengthMul = isPlayer ? LIVING_PULL_MULTIPLIER : MOB_PULL_MULTIPLIER;
            // Players: soft capped pull (escapable). Mobs: damped aggressive pull (cannot walk out).
            boolean livingSafe = isPlayer;
            double maxAccel = isPlayer ? MAX_LIVING_PULL_ACCEL : MAX_MOB_PULL_ACCEL;
            this.applyPull(target, center, forceScale, strengthMul, false, livingSafe, maxAccel);
        }
    }

    /**
     * Damage = {@code Force / 10} HP (½ heart per 10 Force), at most once per second per victim.
     */
    private void tryTouchDamage(LivingEntity living) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }

        float damage = (this.getForce() / 10.0F) * DAMAGE_PER_TEN_FORCE;
        if (damage <= 0.0F) {
            return;
        }

        long now = server.getGameTime();
        UUID id = living.getUUID();
        Long last = this.lastTouchDamageTime.get(id);
        if (last != null && now - last < DAMAGE_INTERVAL_TICKS) {
            return;
        }

        if (living.hurtServer(server, this.damageSources().magic(), damage)) {
            this.lastTouchDamageTime.put(id, now);
            // Dark surface ripple when this Warp finishes a non-player mob.
            if (!(living instanceof Player) && (living.isDeadOrDying() || !living.isAlive())) {
                WarpDeathRipple.spawn(server, this.position());
            }
        }
    }

    private void pruneDamageCooldowns() {
        if (this.lastTouchDamageTime.isEmpty() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = this.lastTouchDamageTime.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (now - entry.getValue() > 200) {
                it.remove();
            }
        }
    }

    /**
     * @param softenGravity if true, disable entity gravity when the well grips (items only)
     * @param livingSafe if true, never damp existing motion and cap pull with {@code maxLivingAccel}
     * @param maxLivingAccel max inward accel when {@code livingSafe} (players vs mobs may differ)
     */
    private void applyPull(
            Entity entity,
            Vec3 center,
            double forceScale,
            double strengthMul,
            boolean softenGravity,
            boolean livingSafe,
            double maxLivingAccel
    ) {
        Vec3 delta = center.subtract(entity.position());
        double dist = delta.length();
        if (dist < 1.0E-4 || dist > PULL_RANGE) {
            return;
        }

        double proximity = 1.0 - dist / PULL_RANGE;
        double accel;
        if (livingSafe) {
            // Soft well: Force 100 near center stays under the given living cap.
            accel = (0.012 + 0.10 * proximity * proximity) * forceScale * strengthMul;
            accel = Math.min(accel, maxLivingAccel);
        } else {
            accel = (0.025 + 0.55 * proximity * proximity) * forceScale * strengthMul;
        }

        Vec3 pull = delta.scale(accel / dist);

        // Mobs directly under the node: strong vertical lift so they rise into the well.
        if (!livingSafe) {
            double horiz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            boolean below = delta.y > 0.25;
            if (below && horiz < 2.25) {
                double lift = (0.14 + 0.28 * proximity) * forceScale * Math.max(1.0, strengthMul * 0.35);
                pull = new Vec3(pull.x * 0.55, Math.max(pull.y, lift), pull.z * 0.55);
                entity.setNoGravity(true);
            }
        }

        if (livingSafe) {
            // Do not damp walk/sprint input — only add a capped inward nudge.
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
        } else {
            double damp = Mth.lerp((float) Mth.clamp(forceScale, 0.0F, 1.0F), 0.92F, 0.78F);
            entity.setDeltaMovement(entity.getDeltaMovement().scale(damp).add(pull));
        }
        entity.hurtMarked = true;

        if (softenGravity && proximity * forceScale > 0.2) {
            entity.setNoGravity(true);
        }
    }

    private void consumeItem(ItemEntity item) {
        ItemStack stack = item.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name = id != null ? id.getPath() : stack.getItem().toString();
        ExampleMod.LOGGER.info("item destroy: {}", name);
        item.discard();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        this.snapToBlockCenter();
        this.snappedToBlock = true;
    }

    public void snapToBlockCenter() {
        BlockPos block = this.blockPosition();
        this.setPos(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_DISTORTION, DEFAULT_DISTORTION);
        entityData.define(DATA_SUBTYPE, WarpSubtype.STATIC.ordinal());
        entityData.define(DATA_FORCE, DEFAULT_FORCE);
    }

    public int getDistortion() {
        return this.entityData.get(DATA_DISTORTION);
    }

    public void setDistortion(int distortion) {
        this.entityData.set(DATA_DISTORTION, Mth.clamp(distortion, MIN_DISTORTION, MAX_DISTORTION));
    }

    public float getDistortionFactor() {
        return getDistortion() / (float) MAX_DISTORTION;
    }

    public int getForce() {
        return this.entityData.get(DATA_FORCE);
    }

    public void setForce(int force) {
        this.entityData.set(DATA_FORCE, Mth.clamp(force, MIN_FORCE, MAX_FORCE));
    }

    public float getForceFactor() {
        return getForce() / (float) MAX_FORCE;
    }

    public WarpSubtype getSubtype() {
        return WarpSubtype.byOrdinalSafe(this.entityData.get(DATA_SUBTYPE));
    }

    public void setSubtype(WarpSubtype subtype) {
        this.entityData.set(DATA_SUBTYPE, subtype.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setDistortion(input.getIntOr("Distortion", DEFAULT_DISTORTION));
        this.setSubtype(WarpSubtype.byId(input.getStringOr("Subtype", WarpSubtype.STATIC.id())));
        this.setForce(input.getIntOr("Force", DEFAULT_FORCE));
        this.snappedToBlock = false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Distortion", this.getDistortion());
        output.putString("Subtype", this.getSubtype().id());
        output.putInt("Force", this.getForce());
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return false;
    }
}
