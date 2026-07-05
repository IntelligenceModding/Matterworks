package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.slot.CrystalSlot;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public class MatterNetworkControllerMenu extends AbstractContainerMenu implements NamedBlockMenu {
    private static final int FILTER_SLOT_IMPORT_WHITELIST_X = 195;
    private static final int FILTER_SLOT_IMPORT_BLACKLIST_X = 217;
    private static final int FILTER_SLOT_EXPORT_WHITELIST_X = 195;
    private static final int FILTER_SLOT_EXPORT_BLACKLIST_X = 217;
    private static final int FILTER_SLOT_IMPORT_Y = 75;
    private static final int FILTER_SLOT_EXPORT_Y = 96;
    private static final int[] CRYSTAL_SLOT_XS = {260, 280, 300};
    private static final int CRYSTAL_SLOT_Y = 96;
    private final MatterNetworkControllerBlockEntity blockEntity;
    private final boolean remoteAccess;
    private final SelectedNodeFilterHandler selectedNodeFilterHandler = new SelectedNodeFilterHandler(this);
    private final SelectedNodeCrystalHandler selectedNodeCrystalHandler = new SelectedNodeCrystalHandler(this);
    @Nullable
    private BlockPos selectedTargetPos;
    private int activeFilterChannel = MatterPylonBlockEntity.CHANNEL_ITEMS;

    public MatterNetworkControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), extraData.readableBytes() > 0 && extraData.readBoolean());
    }

    public MatterNetworkControllerMenu(int containerId, Inventory playerInventory, MatterNetworkControllerBlockEntity blockEntity, boolean remoteAccess) {
        super(ModMenuTypes.MATTER_NETWORK_CONTROLLER.get(), containerId);
        this.blockEntity = blockEntity;
        this.remoteAccess = remoteAccess;
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_WHITELIST, FILTER_SLOT_IMPORT_WHITELIST_X, FILTER_SLOT_IMPORT_Y, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_BLACKLIST, FILTER_SLOT_IMPORT_BLACKLIST_X, FILTER_SLOT_IMPORT_Y, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_WHITELIST, FILTER_SLOT_EXPORT_WHITELIST_X, FILTER_SLOT_EXPORT_Y, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_BLACKLIST, FILTER_SLOT_EXPORT_BLACKLIST_X, FILTER_SLOT_EXPORT_Y, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_WHITELIST, FILTER_SLOT_IMPORT_WHITELIST_X, FILTER_SLOT_IMPORT_Y, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_BLACKLIST, FILTER_SLOT_IMPORT_BLACKLIST_X, FILTER_SLOT_IMPORT_Y, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_WHITELIST, FILTER_SLOT_EXPORT_WHITELIST_X, FILTER_SLOT_EXPORT_Y, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addSlot(new SelectedNodeFilterSlot(this, selectedNodeFilterHandler, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_BLACKLIST, FILTER_SLOT_EXPORT_BLACKLIST_X, FILTER_SLOT_EXPORT_Y, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        for (int slot = 0; slot < MatterPylonBlockEntity.CRYSTAL_SLOT_COUNT; slot++) {
            addSlot(new SelectedNodeCrystalSlot(this, selectedNodeCrystalHandler, slot, CRYSTAL_SLOT_XS[slot], CRYSTAL_SLOT_Y));
        }
    }

    public MatterNetworkControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Nullable
    public BlockPos getSelectedTargetPos() {
        return selectedTargetPos;
    }

    public void setSelectedTargetPos(@Nullable BlockPos selectedTargetPos) {
        this.selectedTargetPos = selectedTargetPos;
    }

    public void setActiveFilterChannel(int channel) {
        this.activeFilterChannel = channel;
    }

    public int getActiveFilterChannel() {
        return activeFilterChannel;
    }

    @Nullable
    public MatterPylonBlockEntity getSelectedTarget() {
        if (selectedTargetPos == null || blockEntity.getLevel() == null || !blockEntity.isConnectedTargetPosition(selectedTargetPos)) {
            return null;
        }
        return blockEntity.getLevel().getBlockEntity(selectedTargetPos) instanceof MatterPylonBlockEntity target ? target : null;
    }

    public boolean hasActiveCrystalTarget() {
        MatterPylonBlockEntity target = getSelectedTarget();
        return target != null && target.supportsUpgradeCrystals();
    }

    public boolean supportsFilterChannel(int channel) {
        MatterPylonBlockEntity target = getSelectedTarget();
        return target != null && target.supportsFilterChannel(channel);
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isControllerMenuStillValid(player, remoteAccess);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (handleFilterSlotClick(slotId, clickType)) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private boolean handleFilterSlotClick(int slotId, ClickType clickType) {
        if (slotId < 0 || slotId >= 8) {
            return false;
        }
        Slot slot = slots.get(slotId);
        if (!(slot instanceof SelectedNodeFilterSlot filterSlot) || !filterSlot.isActive()) {
            return true;
        }
        if (clickType != ClickType.PICKUP) {
            return true;
        }

        MatterPylonBlockEntity target = getSelectedTarget();
        if (target == null) {
            return true;
        }

        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            target.getFilterHandler().setStackInSlot(filterSlot.getContainerSlot(), ItemStack.EMPTY);
        } else if (filterSlot.acceptsFilterCard(carried)) {
            target.getFilterHandler().setStackInSlot(filterSlot.getContainerSlot(), carried.copyWithCount(1));
        }
        broadcastChanges();
        return true;
    }

    private static MatterNetworkControllerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, MatterNetworkControllerBlockEntity.class, "Matter Network Controller");
    }

    private static final class SelectedNodeFilterHandler implements IItemHandlerModifiable {
        private final MatterNetworkControllerMenu menu;

        private SelectedNodeFilterHandler(MatterNetworkControllerMenu menu) {
            this.menu = menu;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            if (target != null) {
                target.getFilterHandler().setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getSlots() {
            return MatterPylonBlockEntity.FILTER_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? ItemStack.EMPTY : target.getFilterHandler().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? stack : target.getFilterHandler().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? ItemStack.EMPTY : target.getFilterHandler().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? 1 : target.getFilterHandler().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target != null && target.getFilterHandler().isItemValid(slot, stack);
        }
    }

    private static final class SelectedNodeCrystalHandler implements IItemHandlerModifiable {
        private final MatterNetworkControllerMenu menu;

        private SelectedNodeCrystalHandler(MatterNetworkControllerMenu menu) {
            this.menu = menu;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            if (target != null) {
                target.getCrystalHandler().setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getSlots() {
            return MatterPylonBlockEntity.CRYSTAL_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? ItemStack.EMPTY : target.getCrystalHandler().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? stack : target.getCrystalHandler().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? ItemStack.EMPTY : target.getCrystalHandler().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target == null ? 1 : target.getCrystalHandler().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            MatterPylonBlockEntity target = menu.getSelectedTarget();
            return target != null && target.getCrystalHandler().isItemValid(slot, stack);
        }
    }

    private static final class SelectedNodeFilterSlot extends SlotItemHandler {
        private final MatterNetworkControllerMenu menu;
        private final int channel;

        private SelectedNodeFilterSlot(MatterNetworkControllerMenu menu, IItemHandlerModifiable itemHandler, int index, int xPosition, int yPosition, int channel) {
            super(itemHandler, index, xPosition, yPosition);
            this.menu = menu;
            this.channel = channel;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return menu.supportsFilterChannel(channel) && menu.getActiveFilterChannel() == channel;
        }

        private boolean acceptsFilterCard(ItemStack stack) {
            if (!menu.supportsFilterChannel(channel)) {
                return false;
            }
            if (channel == MatterPylonBlockEntity.CHANNEL_ITEMS) {
                return stack.getItem() == ModItems.MATTER_ITEM_FILTER.get();
            }
            if (channel == MatterPylonBlockEntity.CHANNEL_FLUIDS) {
                return stack.getItem() == ModItems.MATTER_FLUID_FILTER.get();
            }
            return false;
        }
    }

    private static final class SelectedNodeCrystalSlot extends CrystalSlot {
        private final MatterNetworkControllerMenu menu;

        private SelectedNodeCrystalSlot(MatterNetworkControllerMenu menu, IItemHandlerModifiable itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            this.menu = menu;
        }

        @Override
        public boolean isActive() {
            return menu.hasActiveCrystalTarget();
        }
    }
}
