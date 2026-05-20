package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.AbstractMatterMachineBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStabilizerBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

public class MatterStabilizerMenu extends AbstractMatterMachineMenu {
    private static final int MENU_RAW_INPUT_SLOT = 0;
    private static final int MENU_RAW_OUTPUT_SLOT = 1;
    private static final int MENU_REFINED_INPUT_SLOT = 2;
    private static final int MENU_REFINED_OUTPUT_SLOT = 3;
    private static final int MENU_UNSTABLE_INPUT_SLOT = 4;
    private static final int MENU_UNSTABLE_OUTPUT_SLOT = 5;
    private static final int MENU_CRYSTAL_SLOT = 6;
    private static final int MENU_ENERGY_INPUT_SLOT = 7;

    public MatterStabilizerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterStabilizerBlockEntity.STABILIZER_DATA_COUNT));
    }

    public MatterStabilizerMenu(int containerId, Inventory playerInventory, MatterStabilizerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_STABILIZER.get(), containerId, playerInventory, blockEntity, data);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(createBucketInputSlot(MatterStabilizerBlockEntity.RAW_BUCKET_INPUT_SLOT, 8, 72));
        this.addSlot(createOutputOnlySlot(MatterStabilizerBlockEntity.RAW_BUCKET_OUTPUT_SLOT, 42, 72));
        this.addSlot(createBucketInputSlot(MatterStabilizerBlockEntity.REFINED_BUCKET_INPUT_SLOT, 63, 72));
        this.addSlot(createOutputOnlySlot(MatterStabilizerBlockEntity.REFINED_BUCKET_OUTPUT_SLOT, 97, 72));
        this.addSlot(createBucketInputSlot(MatterStabilizerBlockEntity.UNSTABLE_BUCKET_INPUT_SLOT, 118, 72));
        this.addSlot(createOutputOnlySlot(MatterStabilizerBlockEntity.UNSTABLE_BUCKET_OUTPUT_SLOT, 152, 72));
        this.addSlot(createCrystalSlot(AbstractMatterMachineBlockEntity.CRYSTAL_SLOT, 8, 108));
        this.addSlot(createEnergyInputSlot(AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT, 152, 108));
    }

    @Override
    protected void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }
    }

    @Override
    protected void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            this.addSlot(new Slot(inventory, slot, 8 + slot * 18, 198));
        }
    }

    public int getRawMatterAmount() {
        return data.get(MatterStabilizerBlockEntity.DATA_RAW_FLUID_AMOUNT);
    }

    public int getRawMatterCapacity() {
        return data.get(MatterStabilizerBlockEntity.DATA_RAW_FLUID_CAPACITY);
    }

    public int getUnstableMatterAmount() {
        return data.get(MatterStabilizerBlockEntity.DATA_UNSTABLE_FLUID_AMOUNT);
    }

    public int getUnstableMatterCapacity() {
        return data.get(MatterStabilizerBlockEntity.DATA_UNSTABLE_FLUID_CAPACITY);
    }

    public int getScaledRawMatterAmount(int height) {
        int amount = getRawMatterAmount();
        int capacity = getRawMatterCapacity();
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, amount * height / capacity);
    }

    public int getScaledRefinedMatterAmount(int height) {
        return getScaledFluidAmount(height);
    }

    public int getScaledUnstableMatterAmount(int height) {
        int amount = getUnstableMatterAmount();
        int capacity = getUnstableMatterCapacity();
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, amount * height / capacity);
    }

    public FluidStack getRawMatterFluidStack() {
        return ((MatterStabilizerBlockEntity) blockEntity).getRawMatterFluidStack();
    }

    public FluidStack getRefinedMatterFluidStack() {
        return getFluidStack();
    }

    public FluidStack getUnstableMatterFluidStack() {
        return ((MatterStabilizerBlockEntity) blockEntity).getUnstableMatterFluidStack();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_STABILIZER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copiedStack = ItemStack.EMPTY;
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        copiedStack = sourceStack.copy();

        if (index < machineSlotCount) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copiedStack);
        } else {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!this.moveItemStackTo(sourceStack, MENU_CRYSTAL_SLOT, MENU_CRYSTAL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!this.moveItemStackTo(sourceStack, MENU_ENERGY_INPUT_SLOT, MENU_ENERGY_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.RAW_MATTER_BUCKET.get())) {
                if (!this.moveItemStackTo(sourceStack, MENU_RAW_INPUT_SLOT, MENU_RAW_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.REFINED_MATTER_BUCKET.get())) {
                if (!this.moveItemStackTo(sourceStack, MENU_REFINED_INPUT_SLOT, MENU_REFINED_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(Items.BUCKET)) {
                if (!this.moveItemStackTo(sourceStack, MENU_UNSTABLE_INPUT_SLOT, MENU_UNSTABLE_INPUT_SLOT + 1, false)
                        && !this.moveItemStackTo(sourceStack, MENU_REFINED_INPUT_SLOT, MENU_REFINED_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < playerHotbarStart) {
                if (!this.moveItemStackTo(sourceStack, playerHotbarStart, playerHotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        if (sourceStack.getCount() == copiedStack.getCount()) {
            return ItemStack.EMPTY;
        }

        sourceSlot.onTake(player, sourceStack);
        return copiedStack;
    }

    private static MatterStabilizerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterStabilizerBlockEntity stabilizerBlockEntity) {
            return stabilizerBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Stabilizer block entity at " + pos);
    }
}
