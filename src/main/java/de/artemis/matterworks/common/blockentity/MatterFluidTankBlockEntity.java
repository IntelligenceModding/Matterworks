package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.fluid.FluidItemHelper;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MatterFluidTankBlockEntity extends MatterPylonBlockEntity {
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
        return fluidTank;
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
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.MATTER_FLUID_TANK.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterFluidTankMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_FLUIDS;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
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
}
