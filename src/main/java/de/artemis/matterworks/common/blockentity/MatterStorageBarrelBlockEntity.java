package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.MatterStorageBarrelMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.world.PylonChunkLoading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class MatterStorageBarrelBlockEntity extends MatterPylonBlockEntity implements Container {
    public static final int SLOT_COUNT = 54;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public MatterStorageBarrelBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_STORAGE_BARREL.get(), pos, blockState);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot).copy());
        }
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.MATTER_STORAGE_BARREL.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterStorageBarrelMenu(containerId, playerInventory, this);
    }

    @Override
    protected boolean supportsChannel(int channel) {
        return channel == CHANNEL_ITEMS;
    }

    @Override
    protected boolean canLinkTo(MatterPylonBlockEntity other) {
        return other.getType() == ModBlockEntities.MATTER_PYLON.get()
                || other.getType() == ModBlockEntities.MATTER_STORAGE_BARREL.get();
    }

    @Override
    protected net.neoforged.neoforge.items.IItemHandler getAttachedItemHandler() {
        return itemHandler;
    }

    @Override
    protected Component getMatterNetworkMenuTitle() {
        return Component.translatable("screen.matterworks.matter_network.storage_barrel");
    }

    @Override
    protected void refreshChunkLoadingTickets(ServerLevel serverLevel) {
        PylonChunkLoading.forceNodeTickets(serverLevel, worldPosition);
    }

    @Override
    protected void releaseChunkLoadingTickets(ServerLevel serverLevel) {
        PylonChunkLoading.releaseNodeTickets(serverLevel, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            if (!itemHandler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
