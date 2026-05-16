package de.artemis.matterworks.common.energy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PowerBankData {
    private static final String ENERGY_KEY = "EnergyStored";
    private static final String AUTO_CHARGE_ENABLED_KEY = "AutoChargeEnabled";

    private PowerBankData() {
    }

    public static int getEnergyStored(ItemStack stack, int capacity) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Mth.clamp(tag.getInt(ENERGY_KEY), 0, Math.max(0, capacity));
    }

    public static void setEnergyStored(ItemStack stack, int energyStored, int capacity) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(ENERGY_KEY, Mth.clamp(energyStored, 0, Math.max(0, capacity)));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static boolean isAutoChargeEnabled(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains(AUTO_CHARGE_ENABLED_KEY) || tag.getBoolean(AUTO_CHARGE_ENABLED_KEY);
    }

    public static void setAutoChargeEnabled(ItemStack stack, boolean enabled) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(AUTO_CHARGE_ENABLED_KEY, enabled);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}
