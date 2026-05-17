package de.artemis.matterworks.common.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class UnstableMatterBucketData {
    public static final int MAX_CONTAINMENT_TICKS = 100;
    private static final String CONTAINMENT_TICKS_KEY = "ContainmentTicks";

    private UnstableMatterBucketData() {
    }

    public static int getContainmentTicks(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int stored = tag.contains(CONTAINMENT_TICKS_KEY) ? tag.getInt(CONTAINMENT_TICKS_KEY) : MAX_CONTAINMENT_TICKS;
        return Mth.clamp(stored, 0, MAX_CONTAINMENT_TICKS);
    }

    public static void setContainmentTicks(ItemStack stack, int ticks) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(CONTAINMENT_TICKS_KEY, Mth.clamp(ticks, 0, MAX_CONTAINMENT_TICKS));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}
