package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.transport.PylonMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Arrays;

public class MatterPylonMenu extends AbstractContainerMenu {
    public static final int BUTTON_CYCLE_MODE_CHANNEL_1 = 0;
    public static final int BUTTON_CYCLE_MODE_CHANNEL_2 = 1;
    public static final int BUTTON_CYCLE_MODE_CHANNEL_3 = 2;
    public static final int BUTTON_CYCLE_MODE_CHANNEL_4 = 3;
    private static final int PLAYER_INVENTORY_START = 8;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final MatterPylonBlockEntity blockEntity;
    private final ContainerData data;

    public MatterPylonMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), createUnsyncedData());
    }

    public MatterPylonMenu(int containerId, Inventory playerInventory, MatterPylonBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_PYLON.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_WHITELIST, 16, 86, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_BLACKLIST, 34, 86, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_WHITELIST, 16, 104, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_BLACKLIST, 34, 104, MatterPylonBlockEntity.CHANNEL_ITEMS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_WHITELIST, 88, 86, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_BLACKLIST, 106, 86, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_WHITELIST, 88, 104, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addSlot(new FilterSlot(blockEntity, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_BLACKLIST, 106, 104, MatterPylonBlockEntity.CHANNEL_FLUIDS));
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    public boolean supportsChannel(int channel) {
        return blockEntity.supportsConfiguredChannel(channel);
    }

    public boolean supportsFilterChannel(int channel) {
        return blockEntity.supportsFilterChannel(channel);
    }

    public int getFirstSupportedChannel() {
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            if (supportsChannel(channel)) {
                return channel;
            }
        }
        return 0;
    }

    public int getPylonId(int channel) {
        return Math.max(MatterPylonBlockEntity.DEFAULT_PYLON_ID, data.get(MatterPylonBlockEntity.getPylonIdDataIndex(channel)));
    }

    public PylonMode getMode(int channel) {
        return PylonMode.values()[Math.max(0, Math.min(PylonMode.values().length - 1, data.get(MatterPylonBlockEntity.getModeDataIndex(channel))))];
    }

    public boolean hasSyncedState(int channel) {
        return data.get(MatterPylonBlockEntity.getModeDataIndex(channel)) >= 0
                && data.get(MatterPylonBlockEntity.getPylonIdDataIndex(channel)) >= MatterPylonBlockEntity.DEFAULT_PYLON_ID;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BUTTON_CYCLE_MODE_CHANNEL_1 && id <= BUTTON_CYCLE_MODE_CHANNEL_4) {
            blockEntity.cycleMode(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.isMatterNetworkMenuStillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() == ModItems.MATTER_ITEM_FILTER.get()) {
            boolean moved = false;
            if (supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_ITEMS)) {
                moved = moveItemStackTo(sourceStack, MatterPylonBlockEntity.FILTER_SLOT_ITEM_IMPORT_WHITELIST, MatterPylonBlockEntity.FILTER_SLOT_ITEM_EXPORT_BLACKLIST + 1, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.getItem() == ModItems.MATTER_FLUID_FILTER.get()) {
            boolean moved = false;
            if (supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_FLUIDS)) {
                moved = moveItemStackTo(sourceStack, MatterPylonBlockEntity.FILTER_SLOT_FLUID_IMPORT_WHITELIST, MatterPylonBlockEntity.FILTER_SLOT_FLUID_EXPORT_BLACKLIST + 1, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        } else {
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
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 112 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 170));
        }
    }

    private static MatterPylonBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof MatterPylonBlockEntity pylonBlockEntity) {
            return pylonBlockEntity;
        }
        throw new IllegalStateException("Missing Matter Pylon block entity at " + pos);
    }

    private static ContainerData createUnsyncedData() {
        int[] values = new int[MatterPylonBlockEntity.DATA_COUNT];
        Arrays.fill(values, -1);
        return new ContainerData() {
            @Override
            public int get(int index) {
                return values[index];
            }

            @Override
            public void set(int index, int value) {
                values[index] = value;
            }

            @Override
            public int getCount() {
                return values.length;
            }
        };
    }

    private static final class FilterSlot extends SlotItemHandler {
        private final MatterPylonBlockEntity blockEntity;
        private final int channel;

        private FilterSlot(MatterPylonBlockEntity blockEntity, int slot, int x, int y, int channel) {
            super(blockEntity.getFilterHandler(), slot, x, y);
            this.blockEntity = blockEntity;
            this.channel = channel;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!blockEntity.supportsFilterChannel(channel)) {
                return false;
            }
            if (channel == MatterPylonBlockEntity.CHANNEL_ITEMS) {
                return stack.getItem() == ModItems.MATTER_ITEM_FILTER.get();
            }
            if (channel == MatterPylonBlockEntity.CHANNEL_FLUIDS) {
                return stack.getItem() == ModItems.MATTER_FLUID_FILTER.get();
            }
            return false;
        }
    }
}
