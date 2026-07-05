package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.FluidTankBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.fluid.FluidItemHelper;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FluidTankMenu extends AbstractBaseMenu implements NamedBlockMenu, SideConfigMenuAccess {
    private static final int MACHINE_SLOT_COUNT = FluidTankBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final FluidTankBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean remoteAccess;

    public FluidTankMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                resolveBlockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(FluidTankBlockEntity.DATA_COUNT),
                extraData.readableBytes() > 0 && extraData.readBoolean()
        );
    }

    public FluidTankMenu(int containerId, Inventory playerInventory, FluidTankBlockEntity blockEntity, ContainerData data) {
        this(containerId, playerInventory, blockEntity, data, false);
    }

    public FluidTankMenu(int containerId, Inventory playerInventory, FluidTankBlockEntity blockEntity, ContainerData data, boolean remoteAccess) {
        super(ModMenuTypes.FLUID_TANK.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.remoteAccess = remoteAccess;

        this.addSlot(new CrystalSlot(blockEntity.getItemHandler(), FluidTankBlockEntity.CRYSTAL_SLOT, 8, 108));
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), FluidTankBlockEntity.DRAIN_SLOT, 69, 108));
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), FluidTankBlockEntity.FILL_SLOT, 91, 108));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    public FluidTankBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean isRemoteAccess() {
        return remoteAccess;
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
    public boolean hasNetworkTab() {
        return true;
    }

    public int getFluidAmount() {
        return data.get(FluidTankBlockEntity.DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return data.get(FluidTankBlockEntity.DATA_FLUID_CAPACITY);
    }

    public int getScaledFluidAmount(int height) {
        int fluidAmount = getFluidAmount();
        int fluidCapacity = getFluidCapacity();
        if (fluidAmount <= 0 || fluidCapacity <= 0) {
            return 0;
        }
        return Math.max(1, fluidAmount * height / fluidCapacity);
    }

    public FluidTankBlockEntity.TransferHistorySample getHistorySample(int index) {
        return blockEntity.getHistorySample(index);
    }

    public int getHistorySize() {
        return blockEntity.getHistorySize();
    }

    public int getHistoryCapacity() {
        return blockEntity.getHistoryCapacity();
    }

    public int getCurrentInputRate() {
        return blockEntity.getCurrentInputRate();
    }

    public int getCurrentOutputRate() {
        return blockEntity.getCurrentOutputRate();
    }

    public int getCurrentTransitRate() {
        return blockEntity.getCurrentTransitRate();
    }

    public int getCurrentTotalTransferRate() {
        return blockEntity.getCurrentTotalTransferRate();
    }

    public int getCurrentNetTransferRate() {
        return getCurrentInputRate() - getCurrentOutputRate();
    }

    public FluidStack getFluidStack() {
        return blockEntity.getFluidStack();
    }

    public DyeColor getNetworkColor(int index) {
        return blockEntity.getNetworkColor(MatterPylonBlockEntity.CHANNEL_ENERGY, index);
    }

    @Override
    public boolean stillValid(Player player) {
        return remoteAccess
                ? player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                : stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.FLUID_TANK.get());
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
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
            if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, FluidTankBlockEntity.CRYSTAL_SLOT)) {
                return ItemStack.EMPTY;
            }
        } else if (FluidItemHelper.isFluidItem(sourceStack)) {
            boolean preferFill = FluidItemHelper.isEmptyFluidContainer(sourceStack) && !FluidItemHelper.isFilledFluidContainer(sourceStack);
            boolean moved = preferFill
                    ? moveFluidContainerToFillFirst(sourceStack)
                    : moveFluidContainerToDrainFirst(sourceStack);
            if (!moved) {
                if (index < PLAYER_HOTBAR_START) {
                    if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index < PLAYER_HOTBAR_START) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
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

    private boolean moveFluidContainerToDrainFirst(ItemStack sourceStack) {
        boolean moved = false;
        if (FluidItemHelper.isFilledFluidContainer(sourceStack)) {
            moved = moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, FluidTankBlockEntity.DRAIN_SLOT);
        }
        if (!sourceStack.isEmpty()) {
            moved = moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, FluidTankBlockEntity.FILL_SLOT) || moved;
        }
        return moved;
    }

    private boolean moveFluidContainerToFillFirst(ItemStack sourceStack) {
        boolean moved = moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, FluidTankBlockEntity.FILL_SLOT);
        if (!sourceStack.isEmpty()) {
            moved = moveFluidContainerToDrainFirst(sourceStack) || moved;
        }
        return moved;
    }

    private void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    private void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    private static FluidTankBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, FluidTankBlockEntity.class, "Fluid Tank");
    }

    private static final class CrystalSlot extends SlotItemHandler {
        private CrystalSlot(net.neoforged.neoforge.items.IItemHandler itemHandler, int slot, int x, int y) {
            super(itemHandler, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return PowerCrystalEffects.isPowerCrystal(stack);
        }
    }
}
