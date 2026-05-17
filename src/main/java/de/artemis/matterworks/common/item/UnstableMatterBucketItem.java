package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public class UnstableMatterBucketItem extends BucketItem {
    private static final float RUPTURE_DAMAGE = 6.0F;
    private static final int TOOLTIP_BAR_WIDTH = 40;
    private static final int BAR_COLOR = 0xFFD08E;

    public UnstableMatterBucketItem(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (livingEntity instanceof Player player && player.isCreative()) {
            if (UnstableMatterBucketData.getContainmentTicks(stack) != UnstableMatterBucketData.MAX_CONTAINMENT_TICKS) {
                UnstableMatterBucketData.setContainmentTicks(stack, UnstableMatterBucketData.MAX_CONTAINMENT_TICKS);
            }
            return;
        }

        int containmentTicks = UnstableMatterBucketData.getContainmentTicks(stack);
        if (containmentTicks <= 1) {
            rupture(level, livingEntity, stack);
            return;
        }

        UnstableMatterBucketData.setContainmentTicks(stack, containmentTicks - 1);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.round((UnstableMatterBucketData.getContainmentTicks(stack) / (float) UnstableMatterBucketData.MAX_CONTAINMENT_TICKS) * 13.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        int ticks = UnstableMatterBucketData.getContainmentTicks(stack);
        float seconds = ticks / 20.0F;
        float ratio = getContainmentRatio(stack);
        tooltipComponents.add(Component.translatable("tooltip.matterworks.unstable_matter_bucket.rupture", String.format(Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(buildContainmentBar(ratio));
    }

    private void rupture(Level level, LivingEntity livingEntity, ItemStack stack) {
        BlockPos spillPos = findSpillPos(level, livingEntity.blockPosition());
        if (spillPos != null) {
            level.setBlockAndUpdate(spillPos, ModBlocks.UNSTABLE_MATTER_BLOCK.get().defaultBlockState());
        }

        level.playSound(null, livingEntity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.45F, 1.3F);
        level.playSound(null, livingEntity.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 0.35F, 0.85F);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 center = livingEntity.position().add(0.0D, livingEntity.getBbHeight() * 0.5D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 14, 0.28D, 0.18D, 0.28D, 0.01D);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 10, 0.26D, 0.16D, 0.26D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 10, 0.24D, 0.12D, 0.24D, 0.01D);
        }

        livingEntity.hurt(level.damageSources().magic(), RUPTURE_DAMAGE);
        stack.shrink(1);
    }

    private BlockPos findSpillPos(Level level, BlockPos origin) {
        BlockPos[] candidates = new BlockPos[] {
                origin,
                origin.below(),
                origin.relative(Direction.NORTH),
                origin.relative(Direction.SOUTH),
                origin.relative(Direction.WEST),
                origin.relative(Direction.EAST)
        };

        for (BlockPos candidate : candidates) {
            BlockState state = level.getBlockState(candidate);
            if (state.canBeReplaced()) {
                return candidate;
            }
        }

        return null;
    }

    private float getContainmentRatio(ItemStack stack) {
        return UnstableMatterBucketData.getContainmentTicks(stack) / (float) UnstableMatterBucketData.MAX_CONTAINMENT_TICKS;
    }

    private Component buildContainmentBar(float ratio) {
        int filledSegments = Math.round(Mth.clamp(ratio, 0.0F, 1.0F) * TOOLTIP_BAR_WIDTH);
        String segment = "|";
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(segment.repeat(filledSegments)).withStyle(style -> style.withColor(BAR_COLOR)))
                .append(Component.literal(segment.repeat(Math.max(0, TOOLTIP_BAR_WIDTH - filledSegments))).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(Math.round(ratio * 100.0F) + "%").withStyle(ChatFormatting.GRAY));
    }
}
