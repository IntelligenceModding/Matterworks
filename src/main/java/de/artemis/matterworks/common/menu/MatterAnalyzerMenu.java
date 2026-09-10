package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.blockentity.MatterAnalyzerBlockEntity;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.slot.CrystalSlot;
import de.artemis.matterworks.common.menu.slot.EnergyInputSlot;
import de.artemis.matterworks.common.menu.slot.GhostHintSlot;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import de.artemis.matterworks.common.template.EncodedTemplateData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MatterAnalyzerMenu extends AbstractBaseMenu implements NamedBlockMenu, SideConfigMenuAccess {
    private static final int MACHINE_SLOT_COUNT = MatterAnalyzerBlockEntity.SLOT_CRYSTAL + 1;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int PLAYER_HOTBAR_END = PLAYER_HOTBAR_START + 9;

    private final MatterAnalyzerBlockEntity blockEntity;
    private final ContainerData data;

    public MatterAnalyzerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, extraData.readBlockPos()), new SimpleContainerData(MatterAnalyzerBlockEntity.DATA_COUNT));
    }

    public MatterAnalyzerMenu(int containerId, Inventory playerInventory, MatterAnalyzerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_ANALYZER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        this.addSlot(new EnergyInputSlot(blockEntity.getItemHandler(), MatterAnalyzerBlockEntity.SLOT_POWER_INPUT, 152, 108));
        this.addSlot(new GhostHintSlot(
                blockEntity.getItemHandler(),
                MatterAnalyzerBlockEntity.SLOT_EMPTY_TEMPLATE,
                80,
                57,
                () -> ModItems.EMPTY_TEMPLATE.get().getDefaultInstance()
        ));
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), MatterAnalyzerBlockEntity.SLOT_ACTIVE_TEMPLATE, 80, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ENCODED_TEMPLATE.get())
                        && EncodedTemplateData.hasEncodedItem(stack)
                        && !EncodedTemplateData.isComplete(stack);
            }
        });
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), MatterAnalyzerBlockEntity.SLOT_ITEM_INPUT, 26, 45));
        this.addSlot(new SlotItemHandler(blockEntity.getItemHandler(), MatterAnalyzerBlockEntity.SLOT_OUTPUT, 134, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new CrystalSlot(blockEntity.getItemHandler(), MatterAnalyzerBlockEntity.SLOT_CRYSTAL, 8, 108));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
        addDataSlots(data);
    }

    public boolean isProcessing() {
        return data.get(MatterAnalyzerBlockEntity.DATA_PROGRESS) > 0;
    }

    public int getProgress() {
        return data.get(MatterAnalyzerBlockEntity.DATA_PROGRESS);
    }

    public int getMaxProgress() {
        return data.get(MatterAnalyzerBlockEntity.DATA_MAX_PROGRESS);
    }

    public int getScaledProgress(int width) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * width / maxProgress;
    }

    public int getTemplateProgress() {
        return data.get(MatterAnalyzerBlockEntity.DATA_TEMPLATE_PROGRESS);
    }

    public int getTemplateRequired() {
        return data.get(MatterAnalyzerBlockEntity.DATA_TEMPLATE_REQUIRED);
    }

    public int getEnergyStored() {
        return data.get(MatterAnalyzerBlockEntity.DATA_ENERGY);
    }

    public int getEnergyCapacity() {
        return data.get(MatterAnalyzerBlockEntity.DATA_ENERGY_CAPACITY);
    }

    public int getScaledEnergyAmount(int width) {
        int energyStored = getEnergyStored();
        int energyCapacity = getEnergyCapacity();
        if (energyStored <= 0 || energyCapacity <= 0) {
            return 0;
        }
        return Math.max(1, energyStored * width / energyCapacity);
    }

    public int getProgressBarColor() {
        return 0xFF000000 | data.get(MatterAnalyzerBlockEntity.DATA_PROGRESS_COLOR);
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
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, ModBlocks.MATTER_ANALYZER.get());
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
            if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START) {
            if (PowerCrystalEffects.isPowerCrystal(sourceStack)) {
                if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterAnalyzerBlockEntity.SLOT_CRYSTAL)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyItemHelper.canProvideEnergy(sourceStack)) {
                if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterAnalyzerBlockEntity.SLOT_POWER_INPUT)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.EMPTY_TEMPLATE.get())) {
                if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterAnalyzerBlockEntity.SLOT_EMPTY_TEMPLATE)) {
                    return ItemStack.EMPTY;
                }
            } else if (sourceStack.is(ModItems.ENCODED_TEMPLATE.get())
                    && EncodedTemplateData.hasEncodedItem(sourceStack)
                    && !EncodedTemplateData.isComplete(sourceStack)) {
                if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterAnalyzerBlockEntity.SLOT_ACTIVE_TEMPLATE)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveToContainerSlot(sourceStack, MACHINE_SLOT_COUNT, MatterAnalyzerBlockEntity.SLOT_ITEM_INPUT)) {
                if (index < PLAYER_HOTBAR_START) {
                    if (!this.moveItemStackTo(sourceStack, PLAYER_HOTBAR_START, PLAYER_HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(sourceStack, PLAYER_INVENTORY_START, PLAYER_HOTBAR_START, false)) {
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

    private void addPlayerInventory(Inventory inventory) {
        addPlayerInventorySlots(inventory, 8, 140);
    }

    private void addPlayerHotbar(Inventory inventory) {
        addPlayerHotbarSlots(inventory, 8, 198);
    }

    private static MatterAnalyzerBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return MenuHelper.resolveBlockEntity(inventory, pos, MatterAnalyzerBlockEntity.class, "Matter Analyzer");
    }
}
