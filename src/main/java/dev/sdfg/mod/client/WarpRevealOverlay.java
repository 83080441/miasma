package dev.sdfg.mod.client;

import dev.sdfg.mod.block.CauldronLidBlock;
import dev.sdfg.mod.block.ElementContainerBlock;
import dev.sdfg.mod.block.HeatedCauldronBlock;
import dev.sdfg.mod.ExampleMod;
import dev.sdfg.mod.element.BlockResidue;
import dev.sdfg.mod.element.ElementAmounts;
import dev.sdfg.mod.element.ElementLookup;
import dev.sdfg.mod.entity.WarpEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Revealing Lens HUD: helmet + sneak only.
 * Shows elements for the crosshair target (warp, mob, item entity, block, or fluid).
 * A bucket in hand is shown when the crosshair has nothing elemental.
 */
@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class WarpRevealOverlay {
    private static final double REVEAL_RANGE = 48.0;
    private static final int TEXT_COLOR = 0xFFE8E8F0;
    private static final int LABEL_COLOR = 0xFF9A9AB0;
    private static final int DEBUG_X_OFFSET = 14;

    private WarpRevealOverlay() {
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ExampleMod.REVEALING_HELMET.get())) {
            return;
        }
        if (!player.isCrouching()) {
            return;
        }

        RevealTarget target = resolveTarget(player);
        if (target == null) {
            target = fromHeldBucket(player);
        }
        if (target == null) {
            return;
        }

        GuiGraphicsExtractor gui = event.getGuiGraphics();
        Font font = minecraft.font;
        int centerX = gui.guiWidth() / 2;
        int centerY = gui.guiHeight() / 2;
        int debugX = centerX + DEBUG_X_OFFSET;

        if (target.warp() != null) {
            drawWarpDebug(gui, font, debugX, centerY - 40, target.warp(), player);
        } else {
            drawSimpleDebug(gui, font, debugX, centerY - 24, target);
        }
        ElementStripHud.draw(gui, font, centerX, centerY + ElementStripHud.BELOW_CROSSHAIR, target.amounts(), player);
    }

    /** Entity wins if closer than the block hit; otherwise the looked-at block. */
    private static RevealTarget resolveTarget(LocalPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);

        Entity entity = findLookedEntity(player);
        double entityDist = entity != null ? eye.distanceTo(entity.position()) : Double.POSITIVE_INFINITY;

        HitResult pick = player.pick(REVEAL_RANGE, 1.0F, true);
        BlockHitResult blockHit = null;
        double blockDist = Double.POSITIVE_INFINITY;
        if (pick instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            blockHit = bhr;
            blockDist = eye.distanceTo(bhr.getLocation());
        }

        if (entity != null && entityDist <= blockDist) {
            RevealTarget fromEntity = fromEntity(entity, entityDist);
            if (fromEntity != null) {
                return fromEntity;
            }
        }
        return blockHit != null ? fromBlock(player, blockHit) : null;
    }

    private static RevealTarget fromEntity(Entity entity, double dist) {
        if (entity instanceof WarpEntity warp) {
            return RevealTarget.warp(warp, dist);
        }
        if (entity instanceof LivingEntity living && !(living instanceof LocalPlayer) && !living.isSpectator()) {
            return RevealTarget.of("Entity Reveal", idOf(living.getType()), ElementLookup.of(living), dist);
        }
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            return RevealTarget.of("Item Reveal", idOf(stack.getItem()), ElementLookup.of(stack), dist);
        }
        return null;
    }

    private static RevealTarget fromBlock(LocalPlayer player, BlockHitResult blockHit) {
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        String title = "Block Reveal";
        if (state.getBlock() instanceof CauldronLidBlock) {
            pos = pos.below();
            state = player.level().getBlockState(pos);
            title = "Cauldron";
        } else if (state.getBlock() instanceof HeatedCauldronBlock) {
            title = "Cauldron";
        } else if (state.getBlock() instanceof ElementContainerBlock) {
            title = "Container";
        }
        ElementAmounts amounts = BlockResidue.amounts(player.level(), pos);
        if (amounts.isEmpty()) {
            return null;
        }
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            item = bucketOf(state.getFluidState());
        }
        double dist = player.getEyePosition(1.0F).distanceTo(blockHit.getLocation());
        return RevealTarget.of(title, idOf(item), amounts, dist);
    }

    /** Main-hand bucket, when the crosshair is not already on something with elements. */
    private static RevealTarget fromHeldBucket(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BucketItem)) {
            return null;
        }
        ElementAmounts amounts = ElementLookup.of(stack);
        if (amounts.isEmpty()) {
            return null;
        }
        return RevealTarget.of("Bucket", idOf(stack.getItem()), amounts, 0.0);
    }

    private static Item bucketOf(FluidState fluid) {
        if (fluid.isEmpty()) {
            return Items.AIR;
        }
        Item bucket = fluid.getType().getBucket();
        return bucket == null ? Items.AIR : bucket;
    }

    private static void drawWarpDebug(
            GuiGraphicsExtractor gui,
            Font font,
            int x,
            int y,
            WarpEntity warp,
            LocalPlayer player
    ) {
        double dist = player.getEyePosition().distanceTo(warp.position());
        float damageHp = warp.getForce() / 10.0F;

        gui.textWithBackdrop(font, Component.literal("Warp Debug"), x, y, 120, LABEL_COLOR);
        y += 12;
        y = drawLine(gui, font, x, y, "Subtype", warp.getSubtype().id());
        y = drawLine(gui, font, x, y, "Distortion", String.valueOf(warp.getDistortion()));
        y = drawLine(gui, font, x, y, "Force", String.valueOf(warp.getForce()));
        y = drawLine(gui, font, x, y, "Damage", String.format("%.1f HP / %.1f hearts", damageHp, damageHp / 2.0F));
        y = drawLine(gui, font, x, y, "Pos", String.format("%.1f %.1f %.1f", warp.getX(), warp.getY(), warp.getZ()));
        drawLine(gui, font, x, y, "Distance", String.format("%.1f", dist));
    }

    private static void drawSimpleDebug(GuiGraphicsExtractor gui, Font font, int x, int y, RevealTarget target) {
        gui.textWithBackdrop(font, Component.literal(target.title()), x, y, 140, LABEL_COLOR);
        y += 12;
        y = drawLine(gui, font, x, y, "Id", target.debugId());
        drawLine(gui, font, x, y, "Distance", String.format("%.1f", target.distance()));
    }

    private static int drawLine(GuiGraphicsExtractor gui, Font font, int x, int y, String label, String value) {
        gui.text(font, label + ": " + value, x, y, TEXT_COLOR, true);
        return y + 10;
    }

    private static Entity findLookedEntity(LocalPlayer player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(REVEAL_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(REVEAL_RANGE)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                box,
                entity -> isRevealable(entity) && entity.isAlive(),
                0.0F
        );
        return hit == null ? null : hit.getEntity();
    }

    private static boolean isRevealable(Entity entity) {
        if (entity instanceof WarpEntity || entity instanceof ItemEntity) {
            return true;
        }
        return entity instanceof LivingEntity living
                && !(living instanceof LocalPlayer)
                && !living.isSpectator();
    }

    private static String idOf(net.minecraft.world.entity.EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null ? id.toString() : type.toString();
    }

    private static String idOf(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id != null ? id.toString() : item.toString();
    }

    private record RevealTarget(
            WarpEntity warp,
            ElementAmounts amounts,
            String title,
            String debugId,
            double distance
    ) {
        static RevealTarget warp(WarpEntity warp, double dist) {
            return new RevealTarget(warp, warp.getElementAmounts(), "Warp Debug", warp.getSubtype().id(), dist);
        }

        static RevealTarget of(String title, String debugId, ElementAmounts amounts, double dist) {
            return new RevealTarget(null, amounts, title, debugId, dist);
        }
    }
}
