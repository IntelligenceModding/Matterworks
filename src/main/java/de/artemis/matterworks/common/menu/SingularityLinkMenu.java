package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.SingularityLinkBlockEntity;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SingularityLinkMenu extends AbstractContainerMenu implements NamedBlockMenu {
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final SingularityLinkBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean remoteAccess;

    public SingularityLinkMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(
                containerId,
                playerInventory,
                resolveBlockEntity(playerInventory, extraData.readBlockPos()),
                new SimpleContainerData(SingularityLinkBlockEntity.DATA_COUNT),
                extraData.readableBytes() > 0 && extraData.readBoolean()
        );
    }

    public SingularityLinkMenu(int containerId, Inventory playerInventory, SingularityLinkBlockEntity blockEntity, ContainerData data) {
        this(containerId, playerInventory, blockEntity, data, false);
    }

    public SingularityLinkMenu(int containerId, Inventory playerInventory, SingularityLinkBlockEntity blockEntity, ContainerData data, boolean remoteAccess) {
        super(ModMenuTypes.SINGULARITY_LINK.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.remoteAccess = remoteAccess;

        addSlot(new SlotItemHandler(blockEntity.getSingularityHandler(), SingularityLinkBlockEntity.SINGULARITY_SLOT, 80, 24));
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    @Override
    public BlockPos getBlockPos() {
        return blockEntity.getBlockPos();
    }

    @Override
    public String getBlockDisplayName() {
        return blockEntity.getDisplayName().getString();
    }

    public boolean isStructureFormed() {
        return data.get(SingularityLinkBlockEntity.DATA_FORMED) > 0;
    }

    public boolean hasLinkedPartner() {
        return data.get(SingularityLinkBlockEntity.DATA_LINKED) > 0;
    }

    public net.minecraft.world.item.DyeColor getNetworkColor(int index) {
        return blockEntity.getNetworkColor(SingularityLinkBlockEntity.CHANNEL_ENERGY, index);
    }

    @Override
    public boolean stillValid(Player player) {
        return remoteAccess
                ? player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                : blockEntity.isMatterNetworkMenuStillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copiedStack = sourceStack.copy();

        if (index == SingularityLinkBlockEntity.SINGULARITY_SLOT) {
            if (!moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (sourceStack.is(de.artemis.matterworks.common.registry.ModItems.MATTER_SINGULARITY.get())) {
            if (!moveItemStackTo(sourceStack, SingularityLinkBlockEntity.SINGULARITY_SLOT, SingularityLinkBlockEntity.SINGULARITY_SLOT + 1, false)) {
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

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 142));
        }
    }

    private static SingularityLinkBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof SingularityLinkBlockEntity linkBlockEntity) {
            return linkBlockEntity;
        }
        throw new IllegalStateException("Missing Singularity Link block entity at " + pos);
    }
}
