package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.GraviticCondenserBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
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

public class GraviticCondenserMenu extends AbstractMatterMachineMenu {
    public GraviticCondenserMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(GraviticCondenserBlockEntity.CONDENSER_DATA_COUNT));
    }

    public GraviticCondenserMenu(int containerId, Inventory playerInventory, GraviticCondenserBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.GRAVITIC_CONDENSER.get(), containerId, playerInventory, blockEntity, data);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(createOutputOnlySlot(GraviticCondenserBlockEntity.OUTPUT_SLOT, 80, 36));
        this.addSlot(createBucketInputSlot(GraviticCondenserBlockEntity.RAW_BUCKET_INPUT_SLOT, 8, 72));
        this.addSlot(createOutputOnlySlot(GraviticCondenserBlockEntity.RAW_BUCKET_OUTPUT_SLOT, 41, 72));
        this.addSlot(createBucketInputSlot(GraviticCondenserBlockEntity.SLUDGE_BUCKET_INPUT_SLOT, 119, 72));
        this.addSlot(createOutputOnlySlot(GraviticCondenserBlockEntity.SLUDGE_BUCKET_OUTPUT_SLOT, 152, 72));
        this.addSlot(createCrystalSlot(GraviticCondenserBlockEntity.SLOT_CRYSTAL, 8, 108));
        this.addSlot(createEnergyInputSlot(GraviticCondenserBlockEntity.SLOT_POWER_INPUT, 152, 108));
    }

    @Override
    protected void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    @Override
    protected void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    public int getUnstableAmount() {
        return data.get(GraviticCondenserBlockEntity.DATA_UNSTABLE_AMOUNT);
    }

    public int getUnstableCapacity() {
        return data.get(GraviticCondenserBlockEntity.DATA_UNSTABLE_CAPACITY);
    }

    public int getScaledUnstableAmount(int height) {
        int amount = getUnstableAmount();
        int capacity = getUnstableCapacity();
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, amount * height / capacity);
    }

    @Override
    public FluidStack getFluidStack() {
        FluidStack fluid = super.getFluidStack();
        return !fluid.isEmpty() || getFluidAmount() <= 0 ? fluid : new FluidStack(ModFluids.RAW_MATTER.get(), 1);
    }

    public FluidStack getUnstableFluidStack() {
        FluidStack fluid = ((GraviticCondenserBlockEntity) blockEntity).getUnstableFluidStack();
        return !fluid.isEmpty() || getUnstableAmount() <= 0 ? fluid : new FluidStack(ModFluids.UNSTABLE_MATTER.get(), 1);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.GRAVITIC_CONDENSER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < machineSlotCount) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInventoryStart) {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, GraviticCondenserBlockEntity.SLOT_CRYSTAL)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, GraviticCondenserBlockEntity.SLOT_POWER_INPUT)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.RAW_MATTER_BUCKET.get())) {
                if (!moveToMachineContainerSlot(sourceStack, GraviticCondenserBlockEntity.RAW_BUCKET_INPUT_SLOT)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(Items.BUCKET)) {
                if (!moveToMachineContainerSlot(sourceStack, GraviticCondenserBlockEntity.SLUDGE_BUCKET_INPUT_SLOT)) {
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

    private static GraviticCondenserBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, GraviticCondenserBlockEntity.class, "Gravitic Condenser");
    }
}
