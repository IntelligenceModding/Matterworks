package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.world.level.material.Fluids;

public class CreativeSourceBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 20000;
    public static final int ITEM_CAPACITY = 64;
    public static final int FLUID_CAPACITY = 8000;
    private static final int GENERATION_PER_TICK = 60;
    private static final int MAX_TRANSFER = 120;
    private static final int ITEM_GENERATION_PER_TICK = 8;
    private static final int MAX_ITEM_TRANSFER = 16;
    private static final int FLUID_GENERATION_PER_TICK = 250;
    private static final int MAX_FLUID_TRANSFER = 250;
    private static final ItemStack SOURCE_ITEM = new ItemStack(Items.COBBLESTONE);

    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(Math.min(maxExtract, MAX_TRANSFER), energyStored);
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
    private final ItemStackHandler itemInventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return itemInventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemInventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemInventory.extractItem(slot, Math.min(amount, MAX_ITEM_TRANSFER), simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemInventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final FluidTank fluidTank = new FluidTank(FLUID_CAPACITY, stack -> stack.getFluid().isSame(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? fluidTank.getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? fluidTank.getTankCapacity(0) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!resource.isEmpty() && resource.getFluid().isSame(Fluids.WATER)) {
                return fluidTank.drain(Math.min(resource.getAmount(), MAX_FLUID_TRANSFER), action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return fluidTank.drain(Math.min(maxDrain, MAX_FLUID_TRANSFER), action);
        }
    };

    private int energyStored;

    public CreativeSourceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CREATIVE_SOURCE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeSourceBlockEntity blockEntity) {
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
        float itemFill = itemInventory.getStackInSlot(0).getCount() / (float) ITEM_CAPACITY;
        float fluidFill = fluidTank.getFluidAmount() / (float) FLUID_CAPACITY;
        return Mth.floor(Math.max(energyFill, Math.max(itemFill, fluidFill)) * 15.0F);
    }

    public void reportStatus(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.matterworks.creative_source.status",
                energyStored,
                ENERGY_CAPACITY,
                itemInventory.getStackInSlot(0).getCount(),
                fluidTank.getFluidAmount(),
                FLUID_CAPACITY
        ), true);
    }

    private void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        int nextEnergy = Math.min(ENERGY_CAPACITY, energyStored + GENERATION_PER_TICK);
        if (nextEnergy != energyStored) {
            energyStored = nextEnergy;
            setChanged();
        }

        ItemStack stack = itemInventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            itemInventory.setStackInSlot(0, SOURCE_ITEM.copyWithCount(Math.min(ITEM_GENERATION_PER_TICK, ITEM_CAPACITY)));
        } else if (stack.getCount() < ITEM_CAPACITY) {
            stack.grow(Math.min(ITEM_GENERATION_PER_TICK, ITEM_CAPACITY - stack.getCount()));
            itemInventory.setStackInSlot(0, stack);
        }

        if (fluidTank.getFluidAmount() < FLUID_CAPACITY) {
            fluidTank.fill(new FluidStack(Fluids.WATER, FLUID_GENERATION_PER_TICK), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energyStored);
        tag.put("items", itemInventory.serializeNBT(registries));
        tag.put("fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStored = tag.getInt("energy");
        if (tag.contains("items")) {
            itemInventory.deserializeNBT(registries, tag.getCompound("items"));
        }
        if (tag.contains("fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("fluid"));
        }
    }
}
