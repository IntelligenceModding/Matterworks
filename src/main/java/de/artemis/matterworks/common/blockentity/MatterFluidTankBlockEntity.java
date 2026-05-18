package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.fluid.FluidItemHelper;
import de.artemis.matterworks.common.io.ConfiguredFluidHandler;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.menu.MatterFluidTankMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MatterFluidTankBlockEntity extends MatterPylonBlockEntity implements SideConfigurableBlockEntity {
    public static final int DRAIN_SLOT = 0;
    public static final int FILL_SLOT = 1;
    public static final int DATA_FLUID_AMOUNT = 0;
    public static final int DATA_FLUID_CAPACITY = 1;
    public static final int DATA_COUNT = 2;
    public static final int FLUID_CAPACITY = 32 * FluidType.BUCKET_VOLUME;
    public static final int MAX_TRANSFER_PER_TICK = FluidType.BUCKET_VOLUME;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return FluidItemHelper.isFluidItem(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.BOTH, SideAccessMode.BOTH, SideAccessMode.DISABLED);
    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(DRAIN_SLOT);
                case 1 -> itemHandler.getStackInSlot(FILL_SLOT);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(DRAIN_SLOT, stack, simulate);
                case 1 -> itemHandler.insertItem(FILL_SLOT, stack, simulate);
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
                case 0 -> itemHandler.getStackInSlot(DRAIN_SLOT);
                case 1 -> itemHandler.getStackInSlot(FILL_SLOT);
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
                case 0 -> itemHandler.extractItem(DRAIN_SLOT, amount, simulate);
                case 1 -> itemHandler.extractItem(FILL_SLOT, amount, simulate);
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
    private final IFluidHandler[] configuredFluidHandlers = createConfiguredFluidHandlers();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FLUID_AMOUNT -> fluidTank.getFluidAmount();
                case DATA_FLUID_CAPACITY -> fluidTank.getCapacity();
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

    public MatterFluidTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_FLUID_TANK.get(), pos, blockState);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterFluidTankBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidStorage(@Nullable Direction side) {
        return side == null ? fluidTank : configuredFluidHandlers[side.ordinal()];
    }

    public IItemHandler getAutomationHandler(@Nullable Direction side) {
        return side == null ? itemHandler : configuredItemHandlers[side.ordinal()];
    }

    public ContainerData getData() {
        return data;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return fluidTank.getCapacity();
    }

    public FluidStack getFluidStack() {
        return fluidTank.getFluid().copy();
    }

    @Override
    protected void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        transferFluidFromInputItem();
        transferFluidToOutputItem();
        pushFluidToNeighbors();
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
        return Component.translatable(ModBlocks.MATTER_FLUID_TANK.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterFluidTankMenu(containerId, playerInventory, this, data);
    }

    @Override
    public boolean openPrimaryMenu(Player player, boolean remoteAccess) {
        player.openMenu(
                new SimpleMenuProvider((containerId, inventory, menuPlayer) -> new MatterFluidTankMenu(containerId, inventory, this, data, remoteAccess), getDisplayName()),
                worldPosition
        );
        return true;
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_FLUIDS;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
                || other.getType() == ModBlockEntities.MATTER_NETWORK_CONTROLLER.get()
                || other.getType() == ModBlockEntities.MATTER_FLUID_TANK.get();
    }

    @Override
    protected @Nullable IFluidHandler getAttachedFluidHandler() {
        return fluidTank;
    }

    @Override
    protected Component getMatterNetworkMenuTitle() {
        return Component.translatable("screen.matterworks.matter_network.fluid_tank");
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
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        sideConfiguration.writeToTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        if (tag.contains("fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluid"));
        }
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
        return type == SideConfigType.ITEMS || type == SideConfigType.FLUIDS;
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

    private void transferFluidFromInputItem() {
        ItemStack stack = itemHandler.getStackInSlot(DRAIN_SLOT);
        IFluidHandlerItem itemFluid = FluidItemHelper.getFluidHandler(stack);
        if (itemFluid == null) {
            return;
        }

        FluidStack simulatedDrain = itemFluid.drain(MAX_TRANSFER_PER_TICK, IFluidHandler.FluidAction.SIMULATE);
        if (simulatedDrain.isEmpty()) {
            return;
        }

        int moved = FluidItemHelper.transferFluid(itemFluid, fluidTank, simulatedDrain, MAX_TRANSFER_PER_TICK);
        if (moved > 0) {
            itemHandler.setStackInSlot(DRAIN_SLOT, itemFluid.getContainer());
            setChanged();
        }
    }

    private void transferFluidToOutputItem() {
        ItemStack stack = itemHandler.getStackInSlot(FILL_SLOT);
        IFluidHandlerItem itemFluid = FluidItemHelper.getFluidHandler(stack);
        if (itemFluid == null || fluidTank.getFluidAmount() <= 0) {
            return;
        }

        FluidStack available = fluidTank.getFluid().copyWithAmount(Math.min(fluidTank.getFluidAmount(), MAX_TRANSFER_PER_TICK));
        int moved = FluidItemHelper.transferFluid(fluidTank, itemFluid, available, MAX_TRANSFER_PER_TICK);
        if (moved > 0) {
            itemHandler.setStackInSlot(FILL_SLOT, itemFluid.getContainer());
            setChanged();
        }
    }

    private void pushFluidToNeighbors() {
        for (Direction direction : Direction.values()) {
            if (fluidTank.getFluidAmount() <= 0) {
                return;
            }
            if (!getSideAccessMode(SideConfigType.FLUIDS, direction).allowsOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(direction);
            IFluidHandler targetHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, direction.getOpposite());
            if (targetHandler == null) {
                continue;
            }

            FluidStack available = fluidTank.getFluid().copyWithAmount(Math.min(fluidTank.getFluidAmount(), MAX_TRANSFER_PER_TICK));
            int moved = FluidItemHelper.transferFluid(fluidTank, targetHandler, available, MAX_TRANSFER_PER_TICK);
            if (moved > 0) {
                setChanged();
            }
        }
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

    private IFluidHandler[] createConfiguredFluidHandlers() {
        IFluidHandler[] handlers = new IFluidHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredFluidHandler(
                    () -> getSideAccessMode(SideConfigType.FLUIDS, side),
                    () -> fluidTank
            );
        }
        return handlers;
    }
}
