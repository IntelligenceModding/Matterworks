package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.AbstractMatterMachineBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.blockentity.MatterRecyclerBlockEntity;
import de.artemis.matterworks.common.registry.ModBlocks;
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

public class MatterRecyclerMenu extends AbstractMatterMachineMenu {
    public MatterRecyclerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterRecyclerBlockEntity.DATA_COUNT));
    }

    public MatterRecyclerMenu(int containerId, Inventory playerInventory, MatterRecyclerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_RECYCLER.get(), containerId, playerInventory, blockEntity, data);
    }

    @Override
    protected void addMachineSlots() {
        this.addSlot(createEnergyInputSlot(AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT, 8, 18));
        this.addSlot(createOutputOnlySlot(AbstractMatterMachineBlockEntity.ENERGY_ITEM_OUTPUT_SLOT, 8, 108));
        this.addSlot(createMachineSlot(AbstractMatterMachineBlockEntity.INPUT_SLOT, 44, 63));
        this.addSlot(createOutputOnlySlot(AbstractMatterMachineBlockEntity.INVALID_OUTPUT_SLOT, 116, 63));
        this.addSlot(createBucketInputSlot(AbstractMatterMachineBlockEntity.FLUID_BUCKET_INPUT_SLOT, 152, 18));
        this.addSlot(createOutputOnlySlot(AbstractMatterMachineBlockEntity.FLUID_BUCKET_OUTPUT_SLOT, 152, 108));
        this.addSlot(createCrystalSlot(AbstractMatterMachineBlockEntity.CRYSTAL_SLOT, 80, 108));
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_RECYCLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copiedStack = ItemStack.EMPTY;
        net.minecraft.world.inventory.Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        copiedStack = sourceStack.copy();

        if (index == AbstractMatterMachineBlockEntity.ENERGY_ITEM_OUTPUT_SLOT
                || index == AbstractMatterMachineBlockEntity.INVALID_OUTPUT_SLOT
                || index == AbstractMatterMachineBlockEntity.FLUID_BUCKET_OUTPUT_SLOT) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            sourceSlot.onQuickCraft(sourceStack, copiedStack);
        } else if (index >= playerInventoryStart) {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.CRYSTAL_SLOT, AbstractMatterMachineBlockEntity.CRYSTAL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(Items.BUCKET)) {
                if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.FLUID_BUCKET_INPUT_SLOT, AbstractMatterMachineBlockEntity.FLUID_BUCKET_INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.INPUT_SLOT, AbstractMatterMachineBlockEntity.INPUT_SLOT + 1, false)
                    && !this.moveItemStackTo(sourceStack, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT, AbstractMatterMachineBlockEntity.ENERGY_ITEM_INPUT_SLOT + 1, false)) {
                if (index < playerHotbarStart) {
                    if (!this.moveItemStackTo(sourceStack, playerHotbarStart, playerHotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, false)) {
            return ItemStack.EMPTY;
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

    private static MatterRecyclerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterRecyclerBlockEntity recyclerBlockEntity) {
            return recyclerBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Recycler block entity at " + pos);
    }
}
