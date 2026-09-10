package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterSeparatorBlockEntity;
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

public class MatterSeparatorMenu extends AbstractMatterMachineMenu {
    public MatterSeparatorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterSeparatorBlockEntity.SEPARATOR_DATA_COUNT));
    }

    public MatterSeparatorMenu(int containerId, Inventory playerInventory, MatterSeparatorBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_SEPARATOR.get(), containerId, playerInventory, blockEntity, data);
    }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                this.addSlot(createOutputOnlySlot(MatterSeparatorBlockEntity.OUTPUT_SLOT_START + index, 62 + column * 18, 18 + row * 18));
            }
        }
        this.addSlot(createBucketInputSlot(MatterSeparatorBlockEntity.REFINED_BUCKET_INPUT_SLOT, 8, 72, AbstractMatterMachineMenu::refinedMatterBucketGhost));
        this.addSlot(createOutputOnlySlot(MatterSeparatorBlockEntity.REFINED_BUCKET_OUTPUT_SLOT, 41, 72));
        this.addSlot(createBucketInputSlot(MatterSeparatorBlockEntity.SLUDGE_BUCKET_INPUT_SLOT, 119, 72, AbstractMatterMachineMenu::emptyBucketGhost));
        this.addSlot(createOutputOnlySlot(MatterSeparatorBlockEntity.SLUDGE_BUCKET_OUTPUT_SLOT, 152, 72));
        this.addSlot(createCrystalSlot(MatterSeparatorBlockEntity.SLOT_CRYSTAL, 8, 108));
        this.addSlot(createEnergyInputSlot(MatterSeparatorBlockEntity.SLOT_POWER_INPUT, 152, 108));
    }

    @Override
    protected void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    @Override
    protected void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    public int getSludgeAmount() {
        return data.get(MatterSeparatorBlockEntity.DATA_SLUDGE_AMOUNT);
    }

    public int getSludgeCapacity() {
        return data.get(MatterSeparatorBlockEntity.DATA_SLUDGE_CAPACITY);
    }

    public int getScaledSludgeAmount(int height) {
        int amount = getSludgeAmount();
        int capacity = getSludgeCapacity();
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, amount * height / capacity);
    }

    @Override
    public FluidStack getFluidStack() {
        FluidStack fluid = super.getFluidStack();
        return !fluid.isEmpty() || getFluidAmount() <= 0 ? fluid : new FluidStack(ModFluids.REFINED_MATTER.get(), 1);
    }

    public FluidStack getSludgeFluidStack() {
        FluidStack fluid = ((MatterSeparatorBlockEntity) blockEntity).getSludgeFluidStack();
        return !fluid.isEmpty() || getSludgeAmount() <= 0 ? fluid : new FluidStack(ModFluids.MATTER_SLUDGE.get(), 1);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_SEPARATOR.get());
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
                if (!moveToMachineContainerSlot(sourceStack, MatterSeparatorBlockEntity.SLOT_CRYSTAL)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, MatterSeparatorBlockEntity.SLOT_POWER_INPUT)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.REFINED_MATTER_BUCKET.get())) {
                if (!moveToMachineContainerSlot(sourceStack, MatterSeparatorBlockEntity.REFINED_BUCKET_INPUT_SLOT)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(Items.BUCKET)) {
                if (!moveToMachineContainerSlot(sourceStack, MatterSeparatorBlockEntity.SLUDGE_BUCKET_INPUT_SLOT)) {
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

    private static MatterSeparatorBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, MatterSeparatorBlockEntity.class, "Matter Separator");
    }
}
