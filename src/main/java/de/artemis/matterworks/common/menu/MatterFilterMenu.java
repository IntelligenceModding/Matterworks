package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.filter.MatterFilterData;
import de.artemis.matterworks.common.fluid.FluidItemHelper;
import de.artemis.matterworks.common.item.MatterFilterItem;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MatterFilterMenu extends AbstractContainerMenu {
    public static final int GHOST_SLOT_COUNT = MatterFilterData.SLOT_COUNT;
    public static final int BUTTON_CLEAR = 0;
    public static final int BUTTON_SORT = 1;
    public static final int BUTTON_CLEAN = 2;
    private static final int PLAYER_INVENTORY_START = GHOST_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;
    private static final int PLAYER_INVENTORY_Y = 107;
    private static final int PLAYER_HOTBAR_Y = 165;
    private static final String NEOFORGE_MILK_ID = "neoforge:milk";
    private static final String FORGE_MILK_ID = "forge:milk";

    private final Player owner;
    private final InteractionHand hand;
    private final boolean fluidFilter;
    private final SimpleContainer ghostInventory = new SimpleContainer(GHOST_SLOT_COUNT);
    private final FluidStack[] ghostFluids = new FluidStack[GHOST_SLOT_COUNT];
    private final int lockedHotbarSlot;

    public MatterFilterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, InteractionHand.MAIN_HAND, extraData.readBoolean());
    }

    public MatterFilterMenu(int containerId, Inventory playerInventory, InteractionHand hand, boolean fluidFilter) {
        super(ModMenuTypes.MATTER_FILTER.get(), containerId);
        this.owner = playerInventory.player;
        this.hand = hand;
        this.fluidFilter = fluidFilter;
        this.lockedHotbarSlot = hand == InteractionHand.MAIN_HAND ? playerInventory.selected : -1;
        for (int i = 0; i < ghostFluids.length; i++) {
            ghostFluids[i] = FluidStack.EMPTY;
        }

        if (fluidFilter) {
            loadFluidEntries();
        } else {
            MatterFilterData.loadItemsIntoContainer(getFilterStack(), owner.level().registryAccess(), ghostInventory);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new GhostSlot(ghostInventory, column + row * 9, 8 + column * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = column + row * 9 + 9;
                addSlot(new Slot(playerInventory, inventorySlot, 8 + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }

        for (int slot = 0; slot < 9; slot++) {
            if (slot == lockedHotbarSlot) {
                addSlot(new LockedSlot(playerInventory, slot, 8 + slot * 18, PLAYER_HOTBAR_Y));
            } else {
                addSlot(new Slot(playerInventory, slot, 8 + slot * 18, PLAYER_HOTBAR_Y));
            }
        }
    }

    public boolean isFluidFilter() {
        return fluidFilter;
    }

    public ItemStack getGhostItem(int slot) {
        return slot >= 0 && slot < GHOST_SLOT_COUNT ? ghostInventory.getItem(slot) : ItemStack.EMPTY;
    }

    public FluidStack getGhostFluid(int slot) {
        return slot >= 0 && slot < GHOST_SLOT_COUNT ? ghostFluids[slot] : FluidStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CLEAR) {
            clearGhostEntries();
            saveGhostEntries();
            broadcastChanges();
            return true;
        }
        if (id == BUTTON_SORT) {
            sortGhostEntries();
            saveGhostEntries();
            broadcastChanges();
            return true;
        }
        if (id == BUTTON_CLEAN) {
            removeDuplicateGhostEntries();
            saveGhostEntries();
            broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GHOST_SLOT_COUNT) {
            if (clickType != ClickType.PICKUP) {
                return;
            }

            if (fluidFilter) {
                setFluidEntry(slotId, getFluidFromStack(getCarried()));
            } else {
                ItemStack entry = MatterFilterData.sanitizeItemEntry(getCarried());
                if (entry.getItem() == ModItems.MATTER_ITEM_FILTER.get() || entry.getItem() == ModItems.MATTER_FLUID_FILTER.get()) {
                    entry = ItemStack.EMPTY;
                }
                ghostInventory.setItem(slotId, entry);
            }
            saveGhostEntries();
            broadcastChanges();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack filterStack = getFilterStack();
        return !filterStack.isEmpty()
                && filterStack.getItem() instanceof MatterFilterItem item
                && item.isFluidFilter() == fluidFilter;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < GHOST_SLOT_COUNT || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = slots.get(index).getItem();
        if (sourceStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        boolean changed = fluidFilter ? addFluidEntry(getFluidFromStack(sourceStack)) : addItemEntry(sourceStack);
        if (changed) {
            saveGhostEntries();
            broadcastChanges();
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private boolean addItemEntry(ItemStack stack) {
        ItemStack entry = MatterFilterData.sanitizeItemEntry(stack);
        if (entry.isEmpty() || entry.getItem() == ModItems.MATTER_ITEM_FILTER.get() || entry.getItem() == ModItems.MATTER_FLUID_FILTER.get()) {
            return false;
        }

        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            if (ItemStack.isSameItemSameComponents(ghostInventory.getItem(slot), entry)) {
                return true;
            }
        }
        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            if (ghostInventory.getItem(slot).isEmpty()) {
                ghostInventory.setItem(slot, entry);
                return true;
            }
        }
        return false;
    }

    private boolean addFluidEntry(FluidStack fluid) {
        FluidStack entry = MatterFilterData.sanitizeFluidEntry(fluid);
        if (entry.isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            if (!ghostFluids[slot].isEmpty() && FluidStack.isSameFluidSameComponents(ghostFluids[slot], entry)) {
                return true;
            }
        }
        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            if (ghostFluids[slot].isEmpty()) {
                setFluidEntry(slot, entry);
                return true;
            }
        }
        return false;
    }

    private void setFluidEntry(int slot, FluidStack fluid) {
        ghostFluids[slot] = MatterFilterData.sanitizeFluidEntry(fluid);
        ghostInventory.setItem(slot, ItemStack.EMPTY);
    }

    private void clearGhostEntries() {
        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            ghostInventory.setItem(slot, ItemStack.EMPTY);
            ghostFluids[slot] = FluidStack.EMPTY;
        }
    }

    private void sortGhostEntries() {
        if (fluidFilter) {
            List<FluidStack> entries = new ArrayList<>();
            for (FluidStack ghostFluid : ghostFluids) {
                FluidStack entry = MatterFilterData.sanitizeFluidEntry(ghostFluid);
                if (!entry.isEmpty()) {
                    entries.add(entry);
                }
            }
            entries.sort(Comparator
                    .comparing((FluidStack fluid) -> fluid.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()));
            clearGhostEntries();
            for (int slot = 0; slot < entries.size() && slot < GHOST_SLOT_COUNT; slot++) {
                setFluidEntry(slot, entries.get(slot));
            }
            return;
        }

        List<ItemStack> entries = new ArrayList<>();
        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            ItemStack entry = MatterFilterData.sanitizeItemEntry(ghostInventory.getItem(slot));
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator
                .comparing((ItemStack stack) -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        clearGhostEntries();
        for (int slot = 0; slot < entries.size() && slot < GHOST_SLOT_COUNT; slot++) {
            ghostInventory.setItem(slot, entries.get(slot));
        }
    }

    private void removeDuplicateGhostEntries() {
        if (fluidFilter) {
            List<FluidStack> uniqueEntries = new ArrayList<>();
            for (FluidStack ghostFluid : ghostFluids) {
                FluidStack entry = MatterFilterData.sanitizeFluidEntry(ghostFluid);
                if (entry.isEmpty() || containsFluid(uniqueEntries, entry)) {
                    continue;
                }
                uniqueEntries.add(entry);
            }
            clearGhostEntries();
            for (int slot = 0; slot < uniqueEntries.size() && slot < GHOST_SLOT_COUNT; slot++) {
                setFluidEntry(slot, uniqueEntries.get(slot));
            }
            return;
        }

        List<ItemStack> uniqueEntries = new ArrayList<>();
        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            ItemStack entry = MatterFilterData.sanitizeItemEntry(ghostInventory.getItem(slot));
            if (entry.isEmpty() || containsItem(uniqueEntries, entry)) {
                continue;
            }
            uniqueEntries.add(entry);
        }
        clearGhostEntries();
        for (int slot = 0; slot < uniqueEntries.size() && slot < GHOST_SLOT_COUNT; slot++) {
            ghostInventory.setItem(slot, uniqueEntries.get(slot));
        }
    }

    private void saveGhostEntries() {
        if (fluidFilter) {
            MatterFilterData.saveFluidEntries(getFilterStack(), ghostFluids);
        } else {
            MatterFilterData.saveItemsFromContainer(getFilterStack(), owner.level().registryAccess(), ghostInventory);
        }
        owner.getInventory().setChanged();
    }

    private void loadFluidEntries() {
        FluidStack[] loaded = MatterFilterData.loadFluidEntries(getFilterStack());
        for (int slot = 0; slot < GHOST_SLOT_COUNT; slot++) {
            setFluidEntry(slot, loaded[slot]);
        }
    }

    private ItemStack getFilterStack() {
        return owner.getItemInHand(hand);
    }

    private static FluidStack getFluidFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (stack.is(Items.MILK_BUCKET)) {
            FluidStack milk = getRegisteredFluidSample(NEOFORGE_MILK_ID);
            if (!milk.isEmpty()) {
                return milk;
            }
            return getRegisteredFluidSample(FORGE_MILK_ID);
        }
        var handler = FluidItemHelper.getFluidHandler(stack);
        if (handler == null) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluidInTank = handler.getFluidInTank(tank);
            if (!fluidInTank.isEmpty()) {
                return fluidInTank.copyWithAmount(1);
            }
        }
        FluidStack drained = handler.drain(1, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty() || drained.getFluid() == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return drained.copyWithAmount(1);
    }

    private static FluidStack getRegisteredFluidSample(String fluidId) {
        ResourceLocation key = ResourceLocation.tryParse(fluidId);
        if (key == null || !BuiltInRegistries.FLUID.containsKey(key)) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(BuiltInRegistries.FLUID.get(key), 1);
    }

    private static boolean containsItem(List<ItemStack> entries, ItemStack candidate) {
        for (ItemStack entry : entries) {
            if (ItemStack.isSameItemSameComponents(entry, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsFluid(List<FluidStack> entries, FluidStack candidate) {
        for (FluidStack entry : entries) {
            if (FluidStack.isSameFluidSameComponents(entry, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static final class GhostSlot extends Slot {
        private GhostSlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(Inventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
