package de.artemis.matterworks.common.template;

import de.artemis.matterworks.common.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EncodedTemplateData {
    private static final String ENCODED_ITEM_KEY = "EncodedItem";
    private static final String ANALYSIS_PROGRESS_KEY = "AnalysisProgress";
    private static final String ANALYSIS_REQUIRED_KEY = "AnalysisRequired";

    private EncodedTemplateData() {
    }

    public static ItemStack createEncodedTemplate(ItemStack encodedSource, int requiredCount) {
        ItemStack stack = new ItemStack(ModItems.ENCODED_TEMPLATE.get());
        setEncodedItem(stack, encodedSource.getItem());
        setRequiredCount(stack, requiredCount);
        setAnalysisProgress(stack, 0);
        return stack;
    }

    public static ItemStack createEncodedTemplate(Item item, int requiredCount) {
        ItemStack stack = new ItemStack(ModItems.ENCODED_TEMPLATE.get());
        setEncodedItem(stack, item);
        setRequiredCount(stack, requiredCount);
        setAnalysisProgress(stack, 0);
        return stack;
    }

    public static void setEncodedItem(ItemStack stack, Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(ENCODED_ITEM_KEY, key.toString());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static boolean hasEncodedItem(ItemStack stack) {
        return getEncodedItem(stack) != null;
    }

    public static Item getEncodedItem(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(ENCODED_ITEM_KEY)) {
            return null;
        }

        ResourceLocation key = ResourceLocation.tryParse(tag.getString(ENCODED_ITEM_KEY));
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(key);
        return item == null ? null : item;
    }

    public static Component getEncodedItemName(ItemStack stack) {
        Item item = getEncodedItem(stack);
        if (item == null) {
            return null;
        }

        return Component.translatable(item.getDescriptionId());
    }

    public static int getAnalysisProgress(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, tag.getInt(ANALYSIS_PROGRESS_KEY));
    }

    public static void setAnalysisProgress(ItemStack stack, int progress) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(ANALYSIS_PROGRESS_KEY, Math.max(0, progress));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static int addAnalysisProgress(ItemStack stack, int amount) {
        int newProgress = Math.min(getRequiredCount(stack), getAnalysisProgress(stack) + Math.max(0, amount));
        setAnalysisProgress(stack, newProgress);
        return newProgress;
    }

    public static int getRequiredCount(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(1, tag.getInt(ANALYSIS_REQUIRED_KEY));
    }

    public static void setRequiredCount(ItemStack stack, int requiredCount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(ANALYSIS_REQUIRED_KEY, Math.max(1, requiredCount));
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static boolean isComplete(ItemStack stack) {
        return hasEncodedItem(stack) && getAnalysisProgress(stack) >= getRequiredCount(stack);
    }

    public static float getProgressRatio(ItemStack stack) {
        int requiredCount = getRequiredCount(stack);
        if (requiredCount <= 0) {
            return 0.0F;
        }
        return Mth.clamp((float) getAnalysisProgress(stack) / (float) requiredCount, 0.0F, 1.0F);
    }

    public static int getProgressPercent(ItemStack stack) {
        return Mth.clamp(Math.round(getProgressRatio(stack) * 100.0F), 0, 100);
    }
}
