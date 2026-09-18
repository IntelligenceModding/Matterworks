package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.menu.slot.GhostItemSlot;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MatterBatteryCoreMenu extends AbstractBaseMenu implements NamedBlockMenu {
    public static final int CAPACITOR_CELL_MENU_SLOT = 0;
    private static final int MACHINE_SLOT_COUNT = MatterBatteryCoreBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final MatterBatteryCoreBlockEntity blockEntity;
    private final ContainerData data;
    private final BlockPos initialSelectedPortPos;

    public MatterBatteryCoreMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                resolveBlockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(MatterBatteryCoreBlockEntity.DATA_COUNT),
                readInitialSelectedPortPos(extraData)
        );
    }

    public MatterBatteryCoreMenu(int containerId, Inventory playerInventory, MatterBatteryCoreBlockEntity blockEntity, ContainerData data) {
        this(containerId, playerInventory, blockEntity, data, null);
    }

    public MatterBatteryCoreMenu(int containerId, Inventory playerInventory, MatterBatteryCoreBlockEntity blockEntity, ContainerData data, BlockPos initialSelectedPortPos) {
        super(ModMenuTypes.MATTER_BATTERY_CORE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.initialSelectedPortPos = initialSelectedPortPos;

        this.addSlot(new CapacitorCellSlot(this, 8, 108));
        for (int slot = 0; slot < MatterBatteryCoreBlockEntity.CHARGE_SLOT_COUNT; slot++) {
            this.addSlot(new ChargeTargetSlot(
                    blockEntity.getItemHandler(),
                    MatterBatteryCoreBlockEntity.CHARGE_SLOT_START + slot,
                    35 + slot * 18,
                    108
            ));
        }
        this.addSlot(new PowerBankSlot(
                blockEntity.getItemHandler(),
                MatterBatteryCoreBlockEntity.SLOT_POWER_BANK,
                152,
                108
        ));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public MatterBatteryCoreBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getInitialSelectedPortPos() {
        return initialSelectedPortPos;
    }

    @Override
    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    public boolean isFormed() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_FORMED) > 0;
    }

    public int getEnergyStored() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_CAPACITY);
    }

    public int getTransferRate() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_TRANSFER);
    }

    public int getCellCount() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_CELLS);
    }

    public int getCapacitorCellCount() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_CAPACITOR_CELLS);
    }

    public int getMaxCapacitorCellCount() {
        return data.get(MatterBatteryCoreBlockEntity.DATA_MAX_CAPACITOR_CELLS);
    }

    public int getScaledEnergyAmount(int height) {
        int energyStored = getEnergyStored();
        int energyCapacity = getEnergyCapacity();
        if (energyStored <= 0 || energyCapacity <= 0) {
            return 0;
        }
        return Math.max(1, energyStored * height / energyCapacity);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isMenuStillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index == CAPACITOR_CELL_MENU_SLOT) {
            if (!moveCapacitorCellsToPlayer(sourceStack)) {
                return ItemStack.EMPTY;
            }
            blockEntity.setCapacitorCellCount(sourceStack.getCount());
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.is(ModBlocks.MATTER_CAPACITOR_CELL.get().asItem())) {
            int room = Math.max(0, blockEntity.getMaxCapacitorCellCount() - blockEntity.getCapacitorCellCount());
            int moved = Math.min(room, sourceStack.getCount());
            if (moved <= 0) {
                return ItemStack.EMPTY;
            }
            blockEntity.setCapacitorCellCount(blockEntity.getCapacitorCellCount() + moved);
            sourceStack.shrink(moved);
        } else if (EnergyItemHelper.isPowerBank(sourceStack)) {
            boolean moved = false;
            if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                moved = moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterBatteryCoreBlockEntity.SLOT_POWER_BANK);
            }
            if (!moved && EnergyItemHelper.canReceiveEnergy(sourceStack)) {
                moved = moveToContainerSlotRange(sourceStack, MACHINE_SLOT_COUNT, MatterBatteryCoreBlockEntity.CHARGE_SLOT_START, MatterBatteryCoreBlockEntity.CHARGE_SLOT_COUNT);
            }
            if (!moved && !moveBetweenPlayerInventories(sourceStack, index)) {
                return ItemStack.EMPTY;
            }
        } else if (isUsableEnergyItem(sourceStack)) {
            boolean moved = false;
            if (EnergyItemHelper.canReceiveEnergy(sourceStack)) {
                moved = moveToContainerSlotRange(sourceStack, MACHINE_SLOT_COUNT, MatterBatteryCoreBlockEntity.CHARGE_SLOT_START, MatterBatteryCoreBlockEntity.CHARGE_SLOT_COUNT);
            }
            if (!moved && EnergyItemHelper.canProvideEnergy(sourceStack)) {
                moved = moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterBatteryCoreBlockEntity.SLOT_POWER_BANK);
            }
            if (!moved && !moveBetweenPlayerInventories(sourceStack, index)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveBetweenPlayerInventories(sourceStack, index)) {
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

    private void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    private void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    private static MatterBatteryCoreBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, MatterBatteryCoreBlockEntity.class, "Matter Battery Core");
    }

    private static BlockPos readInitialSelectedPortPos(RegistryFriendlyByteBuf extraData) {
        return extraData.readableBytes() >= Long.BYTES ? extraData.readBlockPos() : null;
    }

    private boolean moveBetweenPlayerInventories(ItemStack stack, int index) {
        if (index < PLAYER_HOTBAR_START) {
            return this.moveItemStackTo(stack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false);
        }
        return this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false);
    }

    private boolean moveCapacitorCellsToPlayer(ItemStack stack) {
        return this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false);
    }

    private static boolean isUsableEnergyItem(ItemStack stack) {
        return EnergyItemHelper.getEnergyStorage(stack) != null
                && !(stack.getItem() instanceof de.artemis.matterworks.common.item.PowerCrystalItem);
    }

    private static final class ChargeTargetSlot extends SlotItemHandler {
        private ChargeTargetSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnergyItemHelper.canReceiveEnergy(stack);
        }
    }

    private static final class CapacitorCellSlot extends Slot implements GhostItemSlot {
        private final MatterBatteryCoreMenu menu;

        private CapacitorCellSlot(MatterBatteryCoreMenu menu, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModBlocks.MATTER_CAPACITOR_CELL.get().asItem());
        }

        @Override
        public ItemStack getGhostItemStack() {
            return getMaxStackSize() <= 0 ? ItemStack.EMPTY : new ItemStack(ModBlocks.MATTER_CAPACITOR_CELL.get());
        }

        @Override
        public boolean mayPickup(Player player) {
            return menu.getCapacitorCellCount() > 0;
        }

        @Override
        public ItemStack getItem() {
            int count = menu.getCapacitorCellCount();
            return count <= 0 ? ItemStack.EMPTY : new ItemStack(ModBlocks.MATTER_CAPACITOR_CELL.get(), count);
        }

        @Override
        public boolean hasItem() {
            return menu.getCapacitorCellCount() > 0;
        }

        @Override
        public ItemStack remove(int amount) {
            int removed = Math.min(Math.max(0, amount), menu.getCapacitorCellCount());
            if (removed <= 0) {
                return ItemStack.EMPTY;
            }
            menu.blockEntity.setCapacitorCellCount(menu.getCapacitorCellCount() - removed);
            return new ItemStack(ModBlocks.MATTER_CAPACITOR_CELL.get(), removed);
        }

        @Override
        public void set(ItemStack stack) {
            int count = stack.isEmpty() ? 0 : stack.is(ModBlocks.MATTER_CAPACITOR_CELL.get().asItem()) ? stack.getCount() : menu.getCapacitorCellCount();
            menu.blockEntity.setCapacitorCellCount(Math.min(count, getMaxStackSize()));
        }

        @Override
        public int getMaxStackSize() {
            return Math.max(0, menu.getMaxCapacitorCellCount());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return mayPlace(stack) ? getMaxStackSize() : 0;
        }

        @Override
        public void setChanged() {
            menu.blockEntity.setChanged();
        }
    }

    private static final class PowerBankSlot extends SlotItemHandler implements GhostItemSlot {
        private PowerBankSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnergyItemHelper.canProvideEnergy(stack);
        }

        @Override
        public ItemStack getGhostItemStack() {
            return ModItems.MATTER_POWER_BANK.get().createChargedStack();
        }
    }
}
