package de.artemis.matterworks.common.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MolecularDisplacementEffect extends MobEffect {
    private static final int EFFECT_INTERVAL = 12;

    public MolecularDisplacementEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A5A43);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % EFFECT_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!(livingEntity.level() instanceof ServerLevel level)) {
            return true;
        }

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, livingEntity.getX(), livingEntity.getY(0.6D), livingEntity.getZ(), 2 + amplifier, 0.2D, 0.25D, 0.2D, 0.005D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, livingEntity.getX(), livingEntity.getY(0.8D), livingEntity.getZ(), 1, 0.15D, 0.15D, 0.15D, 0.0D);
        level.playSound(null, livingEntity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.08F, 1.7F + level.random.nextFloat() * 0.2F);

        livingEntity.hurt(level.damageSources().magic(), 1.0F + amplifier * 0.5F);

        if (livingEntity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            RandomSource random = level.random;
            if (random.nextFloat() < getDisplacementChance(amplifier)) {
                displaceRandomItem(player, random, amplifier);
            }
        }

        return true;
    }

    private float getDisplacementChance(int amplifier) {
        return switch (Math.min(amplifier, 2)) {
            case 0 -> 0.12F;
            case 1 -> 0.20F;
            default -> 0.30F;
        };
    }

    private void displaceRandomItem(Player player, RandomSource random, int amplifier) {
        List<ItemSlotRef> candidates = new ArrayList<>();
        collectCandidates(player.getInventory().items, candidates, InventoryGroup.MAIN, 9, player.getInventory().items.size());
        if (amplifier >= 1) {
            collectCandidates(player.getInventory().armor, candidates, InventoryGroup.ARMOR, 0, player.getInventory().armor.size());
        }
        if (amplifier >= 2) {
            collectCandidates(player.getInventory().items, candidates, InventoryGroup.HOTBAR, 0, 9);
        }
        if (candidates.isEmpty()) {
            return;
        }

        ItemSlotRef selected = candidates.get(random.nextInt(candidates.size()));
        ItemStack stack = selected.get(player);
        if (stack.isEmpty()) {
            return;
        }

        ItemStack dropped = stack.split(1);
        selected.set(player, stack);
        player.drop(dropped, true, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 0.2F, 0.8F + random.nextFloat() * 0.4F);
    }

    private void collectCandidates(net.minecraft.core.NonNullList<ItemStack> stacks, List<ItemSlotRef> candidates, InventoryGroup group, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                candidates.add(new ItemSlotRef(group, i));
            }
        }
    }

    private enum InventoryGroup {
        MAIN,
        ARMOR,
        HOTBAR
    }

    private record ItemSlotRef(InventoryGroup group, int slot) {
        ItemStack get(Player player) {
            return switch (group) {
                case MAIN -> player.getInventory().items.get(slot);
                case ARMOR -> player.getInventory().armor.get(slot);
                case HOTBAR -> player.getInventory().items.get(slot);
            };
        }

        void set(Player player, ItemStack stack) {
            switch (group) {
                case MAIN -> player.getInventory().items.set(slot, stack);
                case ARMOR -> player.getInventory().armor.set(slot, stack);
                case HOTBAR -> player.getInventory().items.set(slot, stack);
            }
        }
    }
}
