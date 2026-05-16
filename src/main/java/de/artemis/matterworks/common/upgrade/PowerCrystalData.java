package de.artemis.matterworks.common.upgrade;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PowerCrystalData {
    public static final int MIN_CHARGE = 10;
    public static final int MAX_CHARGE = 100;
    private static final String CHARGE_KEY = "ChargePercent";

    private PowerCrystalData() {
    }

    public static int getChargePercent(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(CHARGE_KEY)) {
            return MAX_CHARGE;
        }
        return Mth.clamp(tag.getInt(CHARGE_KEY), 0, MAX_CHARGE);
    }

    public static void setChargePercent(ItemStack stack, int chargePercent) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(CHARGE_KEY, Mth.clamp(chargePercent, 0, MAX_CHARGE));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static int drainCharge(ItemStack stack, int amount) {
        int newCharge = Math.max(0, getChargePercent(stack) - Math.max(0, amount));
        setChargePercent(stack, newCharge);
        return newCharge;
    }

    public static boolean hasCharge(ItemStack stack) {
        return getChargePercent(stack) > 0;
    }

    public static float getChargeRatio(ItemStack stack) {
        return (float) getChargePercent(stack) / (float) MAX_CHARGE;
    }
}
