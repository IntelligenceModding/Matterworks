package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterRecyclerBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
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
import net.neoforged.neoforge.fluids.FluidStack;

public class MatterRecyclerMenu extends AbstractMatterMachineMenu {
    public MatterRecyclerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterRecyclerBlockEntity.DATA_COUNT));
    }

    public MatterRecyclerMenu(int containerId, Inventory playerInventory, MatterRecyclerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_RECYCLER.get(), containerId, playerInventory, blockEntity, data);
    }

    @Override
    protected void addMachineSlots() {
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 6; column++) {
                int index = row * 6 + column;
                int x = 8 + column * 18;
                int y = 18 + row * 18;
                this.addSlot(createMachineSlot(MatterRecyclerBlockEntity.INPUT_SLOT_START + index, x, y));
            }
        }
        this.addSlot(createBucketInputSlot(MatterRecyclerBlockEntity.SLOT_BUCKET_INPUT, 119, 72));
        this.addSlot(createOutputOnlySlot(MatterRecyclerBlockEntity.SLOT_BUCKET_OUTPUT, 152, 72));
        this.addSlot(createCrystalSlot(MatterRecyclerBlockEntity.SLOT_CRYSTAL, 8, 108));
        this.addSlot(createEnergyInputSlot(MatterRecyclerBlockEntity.SLOT_POWER_INPUT, 152, 108));
    }

    @Override
    protected void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    @Override
    protected void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_RECYCLER.get());
    }

    @Override
    public FluidStack getFluidStack() {
        FluidStack fluid = super.getFluidStack();
        return !fluid.isEmpty() || getFluidAmount() <= 0 ? fluid : new FluidStack(ModFluids.RAW_MATTER.get(), 1);
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

        if (index < machineSlotCount) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= playerInventoryStart) {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, MatterRecyclerBlockEntity.SLOT_CRYSTAL)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!moveToMachineContainerSlot(sourceStack, MatterRecyclerBlockEntity.SLOT_POWER_INPUT)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(net.minecraft.world.item.Items.BUCKET)) {
                if (!moveToMachineContainerSlot(sourceStack, MatterRecyclerBlockEntity.SLOT_BUCKET_INPUT)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveToMachineContainerSlotRange(sourceStack, MatterRecyclerBlockEntity.INPUT_SLOT_START, MatterRecyclerBlockEntity.INPUT_SLOT_COUNT)) {
                if (index < playerHotbarStart) {
                    if (!this.moveItemStackTo(sourceStack, playerHotbarStart, playerHotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerHotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
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

    private static MatterRecyclerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, MatterRecyclerBlockEntity.class, "Matter Recycler");
    }
}
