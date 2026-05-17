package de.artemis.matterworks.common.filter;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MatterFilterData {
    public static final int SLOT_COUNT = 27;
    private static final String ITEM_ENTRIES_KEY = "ItemEntries";
    private static final String FLUID_ENTRIES_KEY = "FluidEntries";

    private MatterFilterData() {
    }

    public static boolean hasItemEntries(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getList(ITEM_ENTRIES_KEY, Tag.TAG_COMPOUND).isEmpty();
    }

    public static boolean hasFluidEntries(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getList(FLUID_ENTRIES_KEY, Tag.TAG_COMPOUND).isEmpty();
    }

    public static void clearItemEntries(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(ITEM_ENTRIES_KEY);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static void clearFluidEntries(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(FLUID_ENTRIES_KEY);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static void loadItemsIntoContainer(ItemStack stack, HolderLookup.Provider registries, SimpleContainer container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, ItemStack.EMPTY);
        }

        ListTag entries = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getList(ITEM_ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (Tag entry : entries) {
            if (!(entry instanceof CompoundTag entryTag)) {
                continue;
            }
            int slot = entryTag.getInt("slot");
            if (slot < 0 || slot >= container.getContainerSize() || !entryTag.contains("stack", Tag.TAG_COMPOUND)) {
                continue;
            }
            container.setItem(slot, ItemStack.parseOptional(registries, entryTag.getCompound("stack")));
        }
    }

    public static void saveItemsFromContainer(ItemStack stack, HolderLookup.Provider registries, SimpleContainer container) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag entries = new ListTag();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack entryStack = sanitizeItemEntry(container.getItem(slot));
            if (entryStack.isEmpty()) {
                continue;
            }

            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("slot", slot);
            entryTag.put("stack", entryStack.save(registries, new CompoundTag()));
            entries.add(entryTag);
        }

        if (entries.isEmpty()) {
            tag.remove(ITEM_ENTRIES_KEY);
        } else {
            tag.put(ITEM_ENTRIES_KEY, entries);
        }
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static FluidStack[] loadFluidEntries(ItemStack stack) {
        FluidStack[] entries = new FluidStack[SLOT_COUNT];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = FluidStack.EMPTY;
        }

        ListTag fluids = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getList(FLUID_ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (Tag entry : fluids) {
            if (!(entry instanceof CompoundTag entryTag)) {
                continue;
            }
            int slot = entryTag.getInt("slot");
            if (slot < 0 || slot >= SLOT_COUNT) {
                continue;
            }
            String fluidId = entryTag.getString("fluid");
            ResourceLocation key = ResourceLocation.tryParse(fluidId);
            if (key == null || !BuiltInRegistries.FLUID.containsKey(key)) {
                continue;
            }
            entries[slot] = new FluidStack(BuiltInRegistries.FLUID.get(key), 1);
        }
        return entries;
    }

    public static void saveFluidEntries(ItemStack stack, FluidStack[] entries) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ListTag fluids = new ListTag();
        for (int slot = 0; slot < entries.length; slot++) {
            FluidStack entryFluid = sanitizeFluidEntry(entries[slot]);
            if (entryFluid.isEmpty()) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(entryFluid.getFluid());
            if (key == null) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("slot", slot);
            entryTag.putString("fluid", key.toString());
            fluids.add(entryTag);
        }

        if (fluids.isEmpty()) {
            tag.remove(FLUID_ENTRIES_KEY);
        } else {
            tag.put(FLUID_ENTRIES_KEY, fluids);
        }
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static ItemStack sanitizeItemEntry(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    public static FluidStack sanitizeFluidEntry(FluidStack stack) {
        return stack.isEmpty() ? FluidStack.EMPTY : stack.copyWithAmount(1);
    }

    public static boolean containsItem(ItemStack filterStack, HolderLookup.Provider registries, ItemStack candidate) {
        if (filterStack.isEmpty() || candidate.isEmpty() || !hasItemEntries(filterStack)) {
            return false;
        }

        SimpleContainer container = new SimpleContainer(SLOT_COUNT);
        loadItemsIntoContainer(filterStack, registries, container);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack entry = container.getItem(slot);
            if (!entry.isEmpty() && ItemStack.isSameItemSameComponents(entry, candidate)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsFluid(ItemStack filterStack, FluidStack candidate) {
        if (filterStack.isEmpty() || candidate.isEmpty() || !hasFluidEntries(filterStack)) {
            return false;
        }

        FluidStack[] entries = loadFluidEntries(filterStack);
        for (FluidStack entry : entries) {
            if (!entry.isEmpty() && FluidStack.isSameFluidSameComponents(entry, candidate)) {
                return true;
            }
        }
        return false;
    }
}
