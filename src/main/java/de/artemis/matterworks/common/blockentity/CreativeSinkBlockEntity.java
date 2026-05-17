package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.block.CreativeSinkBlock;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class CreativeSinkBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 20000;
    public static final int ITEM_CAPACITY = 512;
    public static final int FLUID_CAPACITY = 8000;
    private static final int PASSIVE_DRAIN_PER_TICK = 40;
    private static final int MAX_TRANSFER = 120;
    private static final int PASSIVE_ITEM_DRAIN_PER_TICK = 8;
    private static final int PASSIVE_FLUID_DRAIN_PER_TICK = 250;

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.min(Math.min(maxReceive, MAX_TRANSFER), ENERGY_CAPACITY - energyStored);
            if (accepted > 0 && !simulate) {
                energyStored += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
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
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return displayedItem.isEmpty() ? ItemStack.EMPTY : displayedItem.copyWithCount(Math.min(displayedItem.getMaxStackSize(), itemStored));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            int accepted = Math.min(stack.getCount(), ITEM_CAPACITY - itemStored);
            if (accepted <= 0) {
                return stack;
            }
            if (!simulate) {
                itemStored += accepted;
                displayedItem = stack.copyWithCount(1);
                setChanged();
            }
            if (accepted == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            ItemStack remainder = stack.copy();
            remainder.shrink(accepted);
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return ITEM_CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    };
    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? storedFluid.copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? FLUID_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            int accepted = Math.min(resource.getAmount(), FLUID_CAPACITY - storedFluidAmount);
            if (accepted <= 0) {
                return 0;
            }
            if (!action.simulate()) {
                storedFluid = new FluidStack(resource.getFluidHolder(), accepted);
                storedFluidAmount += accepted;
                storedFluid.setAmount(storedFluidAmount);
                setChanged();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    private int energyStored;
    private int itemStored;
    private ItemStack displayedItem = ItemStack.EMPTY;
    private FluidStack storedFluid = FluidStack.EMPTY;
    private int storedFluidAmount;

    public CreativeSinkBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_SINK.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeSinkBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    public int getAnalogOutput() {
        float energyFill = energyStored / (float) ENERGY_CAPACITY;
        float itemFill = itemStored / (float) ITEM_CAPACITY;
        float fluidFill = storedFluidAmount / (float) FLUID_CAPACITY;
        return Mth.floor(Math.max(energyFill, Math.max(itemFill, fluidFill)) * 15.0F);
    }

    public void reportStatus(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.matterworks.creative_sink.status",
                energyStored,
                ENERGY_CAPACITY,
                itemStored,
                storedFluidAmount,
                FLUID_CAPACITY
        ), true);
    }

    private void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean changed = false;
        if (energyStored > 0) {
            energyStored = Math.max(0, energyStored - PASSIVE_DRAIN_PER_TICK);
            changed = true;
        }
        if (itemStored > 0) {
            itemStored = Math.max(0, itemStored - PASSIVE_ITEM_DRAIN_PER_TICK);
            if (itemStored == 0) {
                displayedItem = ItemStack.EMPTY;
            }
            changed = true;
        }
        if (storedFluidAmount > 0) {
            storedFluidAmount = Math.max(0, storedFluidAmount - PASSIVE_FLUID_DRAIN_PER_TICK);
            if (storedFluidAmount == 0) {
                storedFluid = FluidStack.EMPTY;
            } else {
                storedFluid.setAmount(storedFluidAmount);
            }
            changed = true;
        }

        boolean lit = energyStored > 0 || itemStored > 0 || storedFluidAmount > 0;
        BlockState currentState = getBlockState();
        if (currentState.getValue(CreativeSinkBlock.LIT) != lit) {
            level.setBlock(worldPosition, currentState.setValue(CreativeSinkBlock.LIT, lit), 3);
            changed = true;
        }

        if (changed) {
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStored);
        tag.putInt("item_stored", itemStored);
        if (!displayedItem.isEmpty()) {
            tag.put("display_item", displayedItem.save(registries, new CompoundTag()));
        }
        if (!storedFluid.isEmpty()) {
            tag.put("fluid", storedFluid.save(registries));
            tag.putInt("fluid_amount", storedFluidAmount);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStored = tag.getInt("energy");
        itemStored = tag.getInt("item_stored");
        displayedItem = tag.contains("display_item") ? ItemStack.parseOptional(registries, tag.getCompound("display_item")) : ItemStack.EMPTY;
        storedFluid = tag.contains("fluid") ? FluidStack.parseOptional(registries, tag.getCompound("fluid")) : FluidStack.EMPTY;
        storedFluidAmount = tag.getInt("fluid_amount");
        if (!storedFluid.isEmpty()) {
            storedFluid.setAmount(storedFluidAmount);
        }
    }
}
