package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.ConfiguredEnergyStorage;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.menu.MatterEnergyCellMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MatterEnergyCellBlockEntity extends MatterPylonBlockEntity implements SideConfigurableBlockEntity {
    public static final int DISCHARGE_SLOT = 0;
    public static final int CHARGE_SLOT = 1;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_COUNT = 2;
    public static final int ENERGY_CAPACITY = 200_000;
    public static final int MAX_TRANSFER_PER_TICK = 2_000;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isEnergyItem(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final CellEnergyStorage energyStorage = new CellEnergyStorage(ENERGY_CAPACITY, MAX_TRANSFER_PER_TICK, MAX_TRANSFER_PER_TICK);
    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.BOTH, SideAccessMode.DISABLED, SideAccessMode.BOTH);
    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(DISCHARGE_SLOT);
                case 1 -> itemHandler.getStackInSlot(CHARGE_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(DISCHARGE_SLOT, stack, simulate);
                case 1 -> itemHandler.insertItem(CHARGE_SLOT, stack, simulate);
                default -> stack;
            };
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 || slot == 1 ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 || slot == 1;
        }
    };
    private final IItemHandler outputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(DISCHARGE_SLOT);
                case 1 -> itemHandler.getStackInSlot(CHARGE_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.extractItem(DISCHARGE_SLOT, amount, simulate);
                case 1 -> itemHandler.extractItem(CHARGE_SLOT, amount, simulate);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 || slot == 1 ? 1 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final IItemHandler[] configuredItemHandlers = createConfiguredItemHandlers();
    private final IEnergyStorage[] configuredEnergyHandlers = createConfiguredEnergyHandlers();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MatterEnergyCellBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_ENERGY_CELL.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterEnergyCellBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return side == null ? energyStorage : configuredEnergyHandlers[side.ordinal()];
    }

    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return side == null ? itemHandler : configuredItemHandlers[side.ordinal()];
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    protected void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        transferEnergyFromInputItem();
        transferEnergyToOutputItem();
        pushEnergyToNeighbors();
        super.serverTick();
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot).copy());
        }
        return inventory;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_ENERGY_CELL.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterEnergyCellMenu(containerId, playerInventory, this, data);
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new MatterEnergyCellMenu(containerId, inventory, this, data, remoteAccess), getDisplayName()),
                worldPosition
        );
        return true;
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_ENERGY;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
                || other.getType() == ModBlockEntities.MATTER_NETWORK_CONTROLLER.get()
                || other.getType() == ModBlockEntities.MATTER_ENERGY_CELL.get();
    }

    @Override
    protected @Nullable IEnergyStorage getAttachedEnergyStorage(boolean importer) {
        if (importer && !energyStorage.canReceive()) {
            return null;
        }
        if (!importer && !energyStorage.canExtract()) {
            return null;
        }
        return energyStorage;
    }

    @Override
    protected Component getMatterNetworkMenuTitle() {
        return Component.translatable("screen.matterworks.matter_network.energy_cell");
    }

    @Override
    protected void refreshChunkLoadingTickets(ServerLevel serverLevel) {
        PylonChunkLoading.forceNodeTickets(serverLevel, worldPosition);
    }

    @Override
    protected void releaseChunkLoadingTickets(ServerLevel serverLevel) {
        PylonChunkLoading.releaseNodeTickets(serverLevel, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        sideConfiguration.writeToTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        energyStorage.setStoredEnergy(tag.getInt("energy"));
        sideConfiguration.readFromTag(tag, this::sanitizeSideAccessMode);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        sideConfiguration.writeToTag(tag);
        return tag;
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return type == SideConfigType.ITEMS || type == SideConfigType.ENERGY;
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return supportsSideConfigType(type);
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return supportsSideConfigType(type);
    }

    @Override
    public SideAccessMode getSideAccessMode(SideConfigType type, Direction side) {
        return sideConfiguration.get(type, side);
    }

    @Override
    public void setSideAccessMode(SideConfigType type, Direction side, SideAccessMode mode) {
        if (!supportsSideConfigType(type)) {
            return;
        }
        if (sideConfiguration.set(type, side, sanitizeSideAccessMode(type, side, mode))) {
            setChanged();
            syncVisualState();
        }
    }

    private void transferEnergyFromInputItem() {
        ItemStack stack = itemHandler.getStackInSlot(DISCHARGE_SLOT);
        IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(stack);
        if (itemEnergy == null) {
            return;
        }

        int moved = EnergyItemHelper.transferEnergy(itemEnergy, energyStorage, MAX_TRANSFER_PER_TICK);
        if (moved > 0) {
            setChanged();
        }
    }

    private void transferEnergyToOutputItem() {
        ItemStack stack = itemHandler.getStackInSlot(CHARGE_SLOT);
        IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(stack);
        if (itemEnergy == null) {
            return;
        }

        int moved = EnergyItemHelper.transferEnergy(energyStorage, itemEnergy, MAX_TRANSFER_PER_TICK);
        if (moved > 0) {
            setChanged();
        }
    }

    private void pushEnergyToNeighbors() {
        for (Direction direction : Direction.values()) {
            int storedEnergy = energyStorage.getEnergyStored();
            if (storedEnergy <= 0) {
                return;
            }
            if (!getSideAccessMode(SideConfigType.ENERGY, direction).allowsOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(direction);
            IEnergyStorage targetStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (targetStorage == null || !targetStorage.canReceive()) {
                continue;
            }

            if (targetStorage.getEnergyStored() >= targetStorage.getMaxEnergyStored()) {
                continue;
            }

            if (targetStorage.getEnergyStored() >= storedEnergy) {
                continue;
            }

            int offered = Math.min(MAX_TRANSFER_PER_TICK, storedEnergy);
            int accepted = targetStorage.receiveEnergy(offered, false);
            if (accepted > 0) {
                energyStorage.extractEnergy(accepted, false);
                setChanged();
            }
        }
    }

    private static boolean isEnergyItem(ItemStack stack) {
        return EnergyItemHelper.getEnergyStorage(stack) != null;
    }

    private SideAccessMode sanitizeSideAccessMode(SideConfigType type, Direction side, SideAccessMode requestedMode) {
        return supportsSideConfigType(type) ? requestedMode : SideAccessMode.DISABLED;
    }

    private IItemHandler[] createConfiguredItemHandlers() {
        IItemHandler[] handlers = new IItemHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredItemHandler(
                    () -> getSideAccessMode(SideConfigType.ITEMS, side),
                    () -> inputAutomationHandler,
                    () -> outputAutomationHandler
            );
        }
        return handlers;
    }

    private IEnergyStorage[] createConfiguredEnergyHandlers() {
        IEnergyStorage[] handlers = new IEnergyStorage[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredEnergyStorage(
                    () -> getSideAccessMode(SideConfigType.ENERGY, side),
                    () -> energyStorage
            );
        }
        return handlers;
    }

    private final class CellEnergyStorage extends EnergyStorage {
        private CellEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }

        private void setStoredEnergy(int amount) {
            this.energy = Mth.clamp(amount, 0, this.capacity);
            setChanged();
        }
    }
}
