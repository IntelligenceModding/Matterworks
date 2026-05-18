package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.debug.SideConfigDebugTracker;
import de.artemis.matterworks.common.io.ConfiguredEnergyStorage;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.menu.MatterGeneratorMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MatterGeneratorBlockEntity extends BlockEntity implements MenuProvider, CustomNamedBlockEntity, SideConfigurableBlockEntity {
    public static final int FUEL_SLOT = 0;
    public static final int DATA_BURN_REMAINING = 0;
    public static final int DATA_BURN_TOTAL = 1;
    public static final int DATA_ENERGY = 2;
    public static final int DATA_ENERGY_CAPACITY = 3;
    public static final int DATA_COUNT = 4;
    public static final int ENERGY_CAPACITY = 40000;
    public static final int FE_PER_TICK = 40;
    public static final int MAX_TRANSFER_PER_TICK = 200;
    public static final int COAL_BURN_TIME = 1600;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == FUEL_SLOT && isFuel(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(FUEL_SLOT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? itemHandler.insertItem(FUEL_SLOT, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? itemHandler.getSlotLimit(FUEL_SLOT) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && itemHandler.isItemValid(FUEL_SLOT, stack);
        }
    };

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(maxExtract, energyStored);
            if (extracted > 0 && !simulate) {
                energyStored -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return ENERGY_CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_BURN_REMAINING -> burnTimeRemaining;
                case DATA_BURN_TOTAL -> burnTimeTotal;
                case DATA_ENERGY -> energyStored;
                case DATA_ENERGY_CAPACITY -> ENERGY_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_BURN_REMAINING -> burnTimeRemaining = value;
                case DATA_BURN_TOTAL -> burnTimeTotal = value;
                case DATA_ENERGY -> energyStored = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.INPUT, SideAccessMode.DISABLED, SideAccessMode.OUTPUT);
    private final IItemHandler[] configuredItemHandlers = createConfiguredItemHandlers();
    private final IEnergyStorage[] configuredEnergyHandlers = createConfiguredEnergyHandlers();

    private int burnTimeRemaining;
    private int burnTimeTotal;
    private int energyStored;
    private String customName = "";

    public MatterGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_GENERATOR.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            SideConfigDebugTracker.onClientLoad(this);
        }
    }

    @Override
    public void setRemoved() {
        SideConfigDebugTracker.onClientUnload(this);
        super.setRemoved();
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterGeneratorBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public @Nullable IItemHandler getAutomationHandler(@Nullable Direction side) {
        return side == null ? itemHandler : configuredItemHandlers[side.ordinal()];
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return side == null ? energyStorage : configuredEnergyHandlers[side.ordinal()];
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean changed = false;

        if (burnTimeRemaining > 0) {
            burnTimeRemaining--;
            int generated = Math.min(FE_PER_TICK, ENERGY_CAPACITY - energyStored);
            if (generated > 0) {
                energyStored += generated;
            }
            changed = true;
        } else if (energyStored < ENERGY_CAPACITY) {
            ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
            int burnTime = getFuelBurnTime(fuelStack);
            if (burnTime > 0) {
                burnTimeRemaining = burnTime;
                burnTimeTotal = burnTime;
                fuelStack.shrink(1);
                changed = true;
            }
        }

        if (energyStored > 0) {
            changed |= pushEnergyToNeighbors();
        }

        if (changed) {
            setChanged();
        }
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i).copy());
        }
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return customName.isEmpty() ? getDefaultName() : Component.literal(customName);
    }

    @Override
    public String getCustomNameText() {
        return customName;
    }

    @Override
    public void setCustomNameText(String customName) {
        String normalized = normalizeCustomName(customName);
        if (this.customName.equals(normalized)) {
            return;
        }
        this.customName = normalized;
        setChanged();
        syncCustomName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterGeneratorMenu(containerId, playerInventory, this, data);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        sideConfiguration.writeToTag(tag);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("burn_time_remaining", burnTimeRemaining);
        tag.putInt("burn_time_total", burnTimeTotal);
        tag.putInt("energy", energyStored);
        sideConfiguration.writeToTag(tag);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        sideConfiguration.readFromTag(tag, this::sanitizeSideAccessMode);
        burnTimeRemaining = tag.getInt("burn_time_remaining");
        burnTimeTotal = tag.getInt("burn_time_total");
        energyStored = tag.getInt("energy");
        customName = normalizeCustomName(tag.getString("custom_name"));
    }

    private boolean pushEnergyToNeighbors() {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (energyStored <= 0) {
                break;
            }
            if (!getSideAccessMode(SideConfigType.ENERGY, direction).allowsOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(direction);
            IEnergyStorage targetStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (targetStorage == null || !targetStorage.canReceive()) {
                continue;
            }

            int offered = Math.min(MAX_TRANSFER_PER_TICK, energyStored);
            int accepted = targetStorage.receiveEnergy(offered, false);
            if (accepted > 0) {
                energyStored -= accepted;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    private static int getFuelBurnTime(ItemStack stack) {
        return isFuel(stack) ? COAL_BURN_TIME : 0;
    }

    private Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_GENERATOR.get().getDescriptionId());
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return type != SideConfigType.FLUIDS;
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return type == SideConfigType.ITEMS;
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return type == SideConfigType.ENERGY;
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
            syncCustomName();
        }
    }

    private static String normalizeCustomName(String customName) {
        String normalized = customName.strip();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private void syncCustomName() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private SideAccessMode sanitizeSideAccessMode(SideConfigType type, Direction side, SideAccessMode requestedMode) {
        boolean canInput = supportsSideConfigInput(type);
        boolean canOutput = supportsSideConfigOutput(type);
        if (requestedMode == SideAccessMode.BOTH && !(canInput && canOutput)) {
            return canInput ? SideAccessMode.INPUT : canOutput ? SideAccessMode.OUTPUT : SideAccessMode.DISABLED;
        }
        if (requestedMode == SideAccessMode.INPUT && !canInput) {
            return canOutput ? SideAccessMode.OUTPUT : SideAccessMode.DISABLED;
        }
        if (requestedMode == SideAccessMode.OUTPUT && !canOutput) {
            return canInput ? SideAccessMode.INPUT : SideAccessMode.DISABLED;
        }
        return requestedMode;
    }

    private IItemHandler[] createConfiguredItemHandlers() {
        IItemHandler[] handlers = new IItemHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredItemHandler(
                    () -> getSideAccessMode(SideConfigType.ITEMS, side),
                    () -> inputAutomationHandler,
                    () -> inputAutomationHandler
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
}
