package de.artemis.matterworks.common.upgrade;

import de.artemis.matterworks.common.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class PowerCrystalEffects {
    public static final int CRYSTAL_CHARGE_COST = 1;
    public static final int BASE_FAILURE_CHANCE_PERCENT = 10;
    public static final float CRIMSON_SPEED_MULTIPLIER = 0.6F;
    public static final int ANALYZER_BASE_PROCESS_TIME = 100;
    public static final int ANALYZER_CRIMSON_PROCESS_TIME = 60;
    public static final int ANALYZER_BASE_ENERGY_PER_TICK = 25;
    public static final int ANALYZER_CRIMSON_ENERGY_PER_TICK = 45;
    public static final int ANALYZER_VERDANT_ENERGY_PER_TICK = 15;
    public static final int ANALYZER_BASE_ENERGY_CAPACITY = 10000;
    public static final int ANALYZER_VERDANT_ENERGY_CAPACITY = 20000;
    public static final int ANALYZER_AZURE_SAMPLE_PRESERVE_CHANCE = 4;
    public static final int CONSTRUCTOR_BASE_ENERGY_CAPACITY = 10000;
    public static final int CONSTRUCTOR_VERDANT_ENERGY_CAPACITY = 20000;

    private PowerCrystalEffects() {
    }

    public static boolean isPowerCrystal(ItemStack stack) {
        return stack.is(ModItems.CRIMSON_POWER_CRYSTAL.get())
                || stack.is(ModItems.AZURE_POWER_CRYSTAL.get())
                || stack.is(ModItems.VERDANT_POWER_CRYSTAL.get());
    }

    public static int getBarColor(ItemStack stack) {
        if (stack.is(ModItems.CRIMSON_POWER_CRYSTAL.get())) {
            return 0xFF5A5A;
        }
        if (stack.is(ModItems.AZURE_POWER_CRYSTAL.get())) {
            return 0x5AAEFF;
        }
        return 0x63D66C;
    }

    public static int getAnalyzerProcessTime(ItemStack crystalStack) {
        return getModifiedProcessTime(ANALYZER_BASE_PROCESS_TIME, crystalStack);
    }

    public static int getAnalyzerEnergyPerTick(ItemStack crystalStack) {
        return getModifiedEnergyPerTick(ANALYZER_BASE_ENERGY_PER_TICK, ANALYZER_CRIMSON_ENERGY_PER_TICK, ANALYZER_VERDANT_ENERGY_PER_TICK, crystalStack);
    }

    public static int getAnalyzerEnergyCapacity(ItemStack crystalStack) {
        return getModifiedEnergyCapacity(ANALYZER_BASE_ENERGY_CAPACITY, ANALYZER_VERDANT_ENERGY_CAPACITY, crystalStack);
    }

    public static int getAnalyzerProgressPerProcess(ItemStack crystalStack) {
        return 1;
    }

    public static int getFailureChancePercent(ItemStack crystalStack) {
        if (isActive(crystalStack) && crystalStack.is(ModItems.AZURE_POWER_CRYSTAL.get())) {
            return 0;
        }
        return BASE_FAILURE_CHANCE_PERCENT;
    }

    public static boolean shouldConsumeAnalyzerSample(ItemStack crystalStack, RandomSource random) {
        return true;
    }

    public static int getConstructorEnergyPerTick(int baseEnergyPerTick, ItemStack crystalStack) {
        return getModifiedEnergyPerTick(baseEnergyPerTick, Math.max(1, Math.round(baseEnergyPerTick * 1.8F)), Math.max(1, Math.round(baseEnergyPerTick * 0.6F)), crystalStack);
    }

    public static int getConstructorEnergyCapacity(ItemStack crystalStack) {
        return getModifiedEnergyCapacity(CONSTRUCTOR_BASE_ENERGY_CAPACITY, CONSTRUCTOR_VERDANT_ENERGY_CAPACITY, crystalStack);
    }

    public static boolean isActive(ItemStack crystalStack) {
        return isPowerCrystal(crystalStack) && PowerCrystalData.hasCharge(crystalStack);
    }

    public static int getModifiedProcessTime(int baseProcessTime, ItemStack crystalStack) {
        if (isActive(crystalStack) && crystalStack.is(ModItems.CRIMSON_POWER_CRYSTAL.get())) {
            return Math.max(1, Math.round(baseProcessTime * CRIMSON_SPEED_MULTIPLIER));
        }
        return baseProcessTime;
    }

    public static ChatFormatting getTooltipColor(ItemStack crystalStack) {
        if (crystalStack.is(ModItems.CRIMSON_POWER_CRYSTAL.get())) {
            return ChatFormatting.RED;
        }
        if (crystalStack.is(ModItems.AZURE_POWER_CRYSTAL.get())) {
            return ChatFormatting.AQUA;
        }
        return ChatFormatting.GREEN;
    }

    private static int getModifiedEnergyPerTick(int baseValue, int crimsonValue, int verdantValue, ItemStack crystalStack) {
        if (isActive(crystalStack) && crystalStack.is(ModItems.CRIMSON_POWER_CRYSTAL.get())) {
            return crimsonValue;
        }
        if (isActive(crystalStack) && crystalStack.is(ModItems.VERDANT_POWER_CRYSTAL.get())) {
            return verdantValue;
        }
        return baseValue;
    }

    private static int getModifiedEnergyCapacity(int baseValue, int verdantValue, ItemStack crystalStack) {
        if (isActive(crystalStack) && crystalStack.is(ModItems.VERDANT_POWER_CRYSTAL.get())) {
            return verdantValue;
        }
        return baseValue;
    }
}
