package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.CombustionGeneratorBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.slot.GhostItemSlot;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class CombustionGeneratorMenu extends AbstractBaseMenu implements NamedBlockMenu, SideConfigMenuAccess {
    private static final int MACHINE_SLOT_COUNT = CombustionGeneratorBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final CombustionGeneratorBlockEntity blockEntity;
    private final ContainerData data;

    public CombustionGeneratorMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(CombustionGeneratorBlockEntity.DATA_COUNT));
    }

    public CombustionGeneratorMenu(int containerId, Inventory playerInventory, CombustionGeneratorBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.COMBUSTION_GENERATOR.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addMachineSlots();
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public int getEnergyStored() {
        return data.get(CombustionGeneratorBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(CombustionGeneratorBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getScaledEnergyAmount(int height) {
        int stored = getEnergyStored();
        int capacity = getEnergyCapacity();
        if (stored <= 0 || capacity <= 0) {
            return 0;
        }
        return Math.max(1, stored * height / capacity);
    }

    public long getStoredFuelMilliBurn() {
        return blockEntity.getStoredFuelMilliBurn();
    }

    public long getFuelCapacityMilliBurn() {
        return blockEntity.getFuelCapacityMilliBurn();
    }

    public int getScaledFuelAmount(int height) {
        long stored = getStoredFuelMilliBurn();
        long capacity = getFuelCapacityMilliBurn();
        if (stored <= 0L || capacity <= 0L) {
            return 0;
        }
        return Math.max(1, (int) Math.min(height, stored * height / capacity));
    }

    public int getCurrentGenerationRate() {
        return blockEntity.getCurrentGenerationRate();
    }

    public int getCurrentFuelUsageMilliRate() {
        return blockEntity.getCurrentFuelUsageMilliRate();
    }

    public int getHistorySize() {
        return blockEntity.getHistorySize();
    }

    public int getHistoryCapacity() {
        return blockEntity.getHistoryCapacity();
    }

    public CombustionGeneratorBlockEntity.GeneratorHistorySample getHistorySample(int index) {
        return blockEntity.getHistorySample(index);
    }

    @Override
    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return blockEntity.supportsSideConfigType(type);
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return blockEntity.supportsSideConfigInput(type);
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return blockEntity.supportsSideConfigOutput(type);
    }

    @Override
    public SideAccessMode getSideAccessMode(SideConfigType type, net.minecraft.core.Direction side) {
        return blockEntity.getSideAccessMode(type, side);
    }

    @Override
    public net.minecraft.core.Direction getSideConfigFrontFacing() {
        return SideConfigOrientation.resolveFrontFacing(blockEntity.getBlockState());
    }

    @Override
    public ItemStack getPrimaryTabIcon() {
        return blockEntity.getBlockState().getBlock().asItem().getDefaultInstance();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.COMBUSTION_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
            if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, CombustionGeneratorBlockEntity.SLOT_BOOST)
                    && !moveToContainerSlotRange(sourceStack, MACHINE_SLOT_COUNT, CombustionGeneratorBlockEntity.FUEL_SLOT_START, CombustionGeneratorBlockEntity.FUEL_SLOT_COUNT)) {
                return ItemStack.EMPTY;
            }
        } else if (EnergyItemHelper.canReceiveEnergy(sourceStack)) {
            if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, CombustionGeneratorBlockEntity.SLOT_POWER_BANK)) {
                return ItemStack.EMPTY;
            }
        } else if (isFuel(sourceStack)) {
            if (!moveToContainerSlotRange(sourceStack, MACHINE_SLOT_COUNT, CombustionGeneratorBlockEntity.FUEL_SLOT_START, CombustionGeneratorBlockEntity.FUEL_SLOT_COUNT)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_HOTBAR_START) {
            if (!moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
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

    private void addMachineSlots() {
        for (int column = 0; column < CombustionGeneratorBlockEntity.FUEL_SLOT_COUNT; column++) {
            addSlot(new FuelSlot(
                    blockEntity.getItemHandler(),
                    CombustionGeneratorBlockEntity.FUEL_SLOT_START + column,
                    35 + column * 18,
                    108
            ));
        }
        addSlot(new BoostCrystalSlot(blockEntity.getItemHandler(), CombustionGeneratorBlockEntity.SLOT_BOOST, 8, 108));
        addSlot(new PowerBankSlot(blockEntity.getItemHandler(), CombustionGeneratorBlockEntity.SLOT_POWER_BANK, 152, 108));
    }

    private void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    private void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    private static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0;
    }

    private static CombustionGeneratorBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, CombustionGeneratorBlockEntity.class, "Combustion Generator");
    }

    private static final class FuelSlot extends SlotItemHandler {
        private FuelSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isFuel(stack);
        }
    }

    private static final class BoostCrystalSlot extends SlotItemHandler implements GhostItemSlot {
        private BoostCrystalSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PowerCrystalEffects.hasCombustionGeneratorEffect(stack);
        }

        @Override
        public ItemStack getGhostItemStack() {
            return de.artemis.matterworks.common.menu.slot.CrystalSlot.getCyclingGhostItemStack();
        }
    }

    private static final class PowerBankSlot extends SlotItemHandler implements GhostItemSlot {
        private PowerBankSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnergyItemHelper.canReceiveEnergy(stack);
        }

        @Override
        public ItemStack getGhostItemStack() {
            return ModItems.MATTER_POWER_BANK.get().getDefaultInstance();
        }
    }
}
