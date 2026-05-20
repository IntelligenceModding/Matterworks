package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.debug.SideConfigDebugTracker;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.ConfiguredEnergyStorage;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.matter.MatterValueManager;
import de.artemis.matterworks.common.menu.MatterAnalyzerMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.template.EncodedTemplateData;
import de.artemis.matterworks.common.template.TemplateAnalysisManager;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MatterAnalyzerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements MenuProvider, CustomNamedBlockEntity, SideConfigurableBlockEntity {
    public static final int SLOT_POWER_INPUT = 0;
    public static final int SLOT_EMPTY_TEMPLATE = 1;
    public static final int SLOT_ACTIVE_TEMPLATE = 2;
    public static final int SLOT_ITEM_INPUT = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_CRYSTAL = 5;
    private static final int SLOT_LEGACY_OVERFLOW = 6;
    private static final int SLOT_COUNT = 7;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_TEMPLATE_PROGRESS = 2;
    public static final int DATA_TEMPLATE_REQUIRED = 3;
    public static final int DATA_ENERGY = 4;
    public static final int DATA_ENERGY_CAPACITY = 5;
    public static final int DATA_PROGRESS_COLOR = 6;
    public static final int DATA_COUNT = 7;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot == SLOT_CRYSTAL) {
                clampEnergyToCurrentCapacity();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return MatterAnalyzerBlockEntity.this.isItemValid(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case SLOT_POWER_INPUT, SLOT_ACTIVE_TEMPLATE, SLOT_CRYSTAL -> 1;
                case SLOT_EMPTY_TEMPLATE -> 64;
                case SLOT_ITEM_INPUT, SLOT_OUTPUT, SLOT_LEGACY_OVERFLOW -> 64;
                default -> 64;
            };
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> getCurrentProcessTime();
                case DATA_TEMPLATE_PROGRESS -> EncodedTemplateData.getAnalysisProgress(itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE));
                case DATA_TEMPLATE_REQUIRED -> EncodedTemplateData.hasEncodedItem(itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE))
                        ? EncodedTemplateData.getRequiredCount(itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE))
                        : 0;
                case DATA_ENERGY -> energyStored;
                case DATA_ENERGY_CAPACITY -> getCurrentEnergyCapacity();
                case DATA_PROGRESS_COLOR -> getEffectiveProgressBarColor();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.INPUT, SideAccessMode.DISABLED, SideAccessMode.INPUT);
    private final IItemHandler[] configuredItemHandlers = createConfiguredItemHandlers();
    private final IEnergyStorage[] configuredEnergyHandlers = createConfiguredEnergyHandlers();

    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 5;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return switch (slot) {
                case 0 -> itemHandler.getStackInSlot(SLOT_POWER_INPUT);
                case 1 -> itemHandler.getStackInSlot(SLOT_EMPTY_TEMPLATE);
                case 2 -> itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE);
                case 3 -> itemHandler.getStackInSlot(SLOT_ITEM_INPUT);
                case 4 -> itemHandler.getStackInSlot(SLOT_CRYSTAL);
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return switch (slot) {
                case 0 -> itemHandler.insertItem(SLOT_POWER_INPUT, stack, simulate);
                case 1 -> itemHandler.insertItem(SLOT_EMPTY_TEMPLATE, stack, simulate);
                case 2 -> itemHandler.insertItem(SLOT_ACTIVE_TEMPLATE, stack, simulate);
                case 3 -> itemHandler.insertItem(SLOT_ITEM_INPUT, stack, simulate);
                case 4 -> itemHandler.insertItem(SLOT_CRYSTAL, stack, simulate);
                default -> stack;
            };
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0, 2, 4 -> 1;
                case 1 -> 64;
                case 3 -> 64;
                default -> 0;
            };
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> itemHandler.isItemValid(SLOT_POWER_INPUT, stack);
                case 1 -> itemHandler.isItemValid(SLOT_EMPTY_TEMPLATE, stack);
                case 2 -> itemHandler.isItemValid(SLOT_ACTIVE_TEMPLATE, stack);
                case 3 -> itemHandler.isItemValid(SLOT_ITEM_INPUT, stack);
                case 4 -> itemHandler.isItemValid(SLOT_CRYSTAL, stack);
                default -> false;
            };
        }
    };

    private final IItemHandler outputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(SLOT_OUTPUT) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? itemHandler.extractItem(SLOT_OUTPUT, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? itemHandler.getSlotLimit(SLOT_OUTPUT) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }

            int received = Math.min(maxReceive, getCurrentEnergyCapacity() - energyStored);
            if (received > 0 && !simulate) {
                energyStored += received;
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return getCurrentEnergyCapacity();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    private int progress;
    private int energyStored;
    private ItemStack activeProcessCrystal = ItemStack.EMPTY;
    private boolean processCrystalLatched;
    private String customName = "";

    public MatterAnalyzerBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_ANALYZER.get(), pos, blockState);
        initializeDefaults();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            SideConfigDebugTracker.onClientLoad(this);
        }
    }

    @Override
    public void setRemoved() {
        SideConfigDebugTracker.onClientUnload(this);
        super.setRemoved();
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MatterAnalyzerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public @Nullable IItemHandler getAutomationHandler(@Nullable Direction side) {
        return side == null ? itemHandler : configuredItemHandlers[side.ordinal()];
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        return side == null ? externalEnergyStorage : configuredEnergyHandlers[side.ordinal()];
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        transferEnergyFromPowerInputItem();
        moveCompletedTemplateToOutput();
        promoteEmptyTemplateToActive();

        boolean changed = false;
        if (canProcess() && hasEnoughEnergy()) {
            if (progress == 0) {
                latchProcessCrystal();
            }
            energyStored -= getCurrentEnergyPerTick();
            progress++;
            changed = true;
            if (progress >= getCurrentProcessTime()) {
                progress = 0;
                processItem();
                clearLatchedProcessCrystal();
                promoteEmptyTemplateToActive();
                changed = true;
            }
        } else if (progress != 0) {
            progress = 0;
            clearLatchedProcessCrystal();
            changed = true;
        }

        moveCompletedTemplateToOutput();
        if (changed) {
            setChanged();
        }
    }

    public SimpleContainer createDropInventory() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i).copy());
        }
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return customName.isEmpty() ? getDefaultName() : Component.literal(customName);
    }

    @Override
    public String getCustomNameText() {
        return customName;
    }

    @Override
    public void setCustomNameText(String customName) {
        String normalized = normalizeCustomName(customName);
        if (this.customName.equals(normalized)) {
            return;
        }
        this.customName = normalized;
        setChanged();
        syncCustomName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MatterAnalyzerMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        sideConfiguration.writeToTag(tag);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStored);
        tag.putInt("progress", progress);
        sideConfiguration.writeToTag(tag);
        if (processCrystalLatched) {
            tag.putBoolean("process_crystal_latched", true);
        }
        if (!activeProcessCrystal.isEmpty()) {
            tag.put("active_process_crystal", activeProcessCrystal.saveOptional(registries));
        }
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            loadInventory(tag.getCompound("inventory"), registries);
        }
        sideConfiguration.readFromTag(tag, this::sanitizeSideAccessMode);
        if (tag.contains("energy")) {
            energyStored = tag.getInt("energy");
            clampEnergyToCurrentCapacity();
        }
        progress = tag.getInt("progress");
        processCrystalLatched = tag.getBoolean("process_crystal_latched");
        if (tag.contains("active_process_crystal")) {
            activeProcessCrystal = ItemStack.parseOptional(registries, tag.getCompound("active_process_crystal"));
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
        customName = normalizeCustomName(tag.getString("custom_name"));
    }

    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_ANALYZER.get().getDescriptionId());
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return type != SideConfigType.FLUIDS;
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return type == SideConfigType.ITEMS || type == SideConfigType.ENERGY;
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return type == SideConfigType.ITEMS;
    }

    @Override
    public SideAccessMode getSideAccessMode(SideConfigType type, Direction side) {
        return sideConfiguration.get(type, side);
    }

    @Override
    public void setSideAccessMode(SideConfigType type, Direction side, SideAccessMode mode) {
        if (!supportsSideConfigType(type)) {
            return;
        }
        if (sideConfiguration.set(type, side, sanitizeSideAccessMode(type, mode))) {
            setChanged();
            syncCustomName();
        }
    }

    private boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_POWER_INPUT -> EnergyItemHelper.canProvideEnergy(stack);
            case SLOT_EMPTY_TEMPLATE -> stack.is(ModItems.EMPTY_TEMPLATE.get());
            case SLOT_ACTIVE_TEMPLATE -> stack.is(ModItems.ENCODED_TEMPLATE.get())
                    && EncodedTemplateData.hasEncodedItem(stack)
                    && !EncodedTemplateData.isComplete(stack);
            case SLOT_ITEM_INPUT -> canAcceptAnalyzerInput(stack);
            case SLOT_OUTPUT, SLOT_LEGACY_OVERFLOW -> false;
            case SLOT_CRYSTAL -> PowerCrystalEffects.isPowerCrystal(stack);
            default -> false;
        };
    }

    private boolean canProcess() {
        ItemStack activeTemplateStack = itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE);
        ItemStack itemStack = itemHandler.getStackInSlot(SLOT_ITEM_INPUT);
        if (!canEncode(itemStack)) {
            return false;
        }
        if (!activeTemplateStack.is(ModItems.ENCODED_TEMPLATE.get()) || !EncodedTemplateData.hasEncodedItem(activeTemplateStack)) {
            return false;
        }
        if (EncodedTemplateData.isComplete(activeTemplateStack)
                || EncodedTemplateData.getEncodedItem(activeTemplateStack) != itemStack.getItem()) {
            return false;
        }

        int nextProgress = EncodedTemplateData.getAnalysisProgress(activeTemplateStack)
                + PowerCrystalEffects.getAnalyzerProgressPerProcess(getEffectiveCrystalStack());
        return nextProgress < EncodedTemplateData.getRequiredCount(activeTemplateStack)
                || canMoveCompletedTemplateToOutput(activeTemplateStack);
    }

    private void processItem() {
        ItemStack activeTemplateStack = itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE);
        ItemStack itemStack = itemHandler.getStackInSlot(SLOT_ITEM_INPUT);
        if (!canProcess()) {
            return;
        }

        EncodedTemplateData.addAnalysisProgress(activeTemplateStack, PowerCrystalEffects.getAnalyzerProgressPerProcess(getEffectiveCrystalStack()));
        itemStack.shrink(1);
        moveCompletedTemplateToOutput();
    }

    private void moveCompletedTemplateToOutput() {
        ItemStack activeTemplateStack = itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE);
        if (!activeTemplateStack.is(ModItems.ENCODED_TEMPLATE.get()) || !EncodedTemplateData.isComplete(activeTemplateStack)) {
            return;
        }

        if (!canMoveCompletedTemplateToOutput(activeTemplateStack)) {
            return;
        }

        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, activeTemplateStack.copy());
        } else {
            outputStack.grow(1);
        }
        itemHandler.setStackInSlot(SLOT_ACTIVE_TEMPLATE, ItemStack.EMPTY);
    }

    private boolean canMoveCompletedTemplateToOutput(ItemStack templateStack) {
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        return outputStack.isEmpty()
                || (ItemStack.isSameItemSameComponents(outputStack, templateStack)
                && outputStack.getCount() < outputStack.getMaxStackSize());
    }

    private void promoteEmptyTemplateToActive() {
        if (!itemHandler.getStackInSlot(SLOT_ACTIVE_TEMPLATE).isEmpty()) {
            return;
        }

        ItemStack emptyTemplateStack = itemHandler.getStackInSlot(SLOT_EMPTY_TEMPLATE);
        ItemStack itemStack = itemHandler.getStackInSlot(SLOT_ITEM_INPUT);
        if (!emptyTemplateStack.is(ModItems.EMPTY_TEMPLATE.get()) || !canEncode(itemStack) || level == null) {
            return;
        }

        int requiredCount = TemplateAnalysisManager.getRequiredItemCount(itemStack, level);
        ItemStack activeTemplate = EncodedTemplateData.createEncodedTemplate(itemStack.getItem(), requiredCount);
        emptyTemplateStack.shrink(1);
        itemHandler.setStackInSlot(SLOT_ACTIVE_TEMPLATE, activeTemplate);
    }

    private boolean canAcceptAnalyzerInput(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.is(ModItems.EMPTY_TEMPLATE.get())
                && !stack.is(ModItems.ENCODED_TEMPLATE.get())
                && !PowerCrystalEffects.isPowerCrystal(stack)
                && !EnergyItemHelper.canProvideEnergy(stack)
                && canEncode(stack);
    }

    private boolean canEncode(ItemStack stack) {
        return !stack.isEmpty() && !MatterValueManager.isPatternEncodingBlocked(stack);
    }

    private int getCurrentProcessTime() {
        return PowerCrystalEffects.getAnalyzerProcessTime(getEffectiveCrystalStack());
    }

    public int getEffectiveProgressBarColor() {
        ItemStack crystalStack = getEffectiveCrystalStack();
        return PowerCrystalEffects.isActive(crystalStack) ? PowerCrystalEffects.getBarColor(crystalStack) : 0xB67CFF;
    }

    private boolean hasEnoughEnergy() {
        return energyStored >= getCurrentEnergyPerTick();
    }

    private int getCurrentEnergyPerTick() {
        return PowerCrystalEffects.getAnalyzerEnergyPerTick(getEffectiveCrystalStack());
    }

    private int getCurrentEnergyCapacity() {
        return PowerCrystalEffects.getAnalyzerEnergyCapacity(getEffectiveCrystalStack());
    }

    private void clampEnergyToCurrentCapacity() {
        energyStored = Math.min(energyStored, getCurrentEnergyCapacity());
    }

    private void transferEnergyFromPowerInputItem() {
        ItemStack energyStack = itemHandler.getStackInSlot(SLOT_POWER_INPUT);
        if (energyStack.isEmpty()) {
            return;
        }

        IEnergyStorage itemEnergy = EnergyItemHelper.getEnergyStorage(energyStack);
        if (itemEnergy == null) {
            return;
        }

        int missingEnergy = getCurrentEnergyCapacity() - energyStored;
        if (missingEnergy > 0) {
            int moved = EnergyItemHelper.transferEnergy(itemEnergy, externalEnergyStorage, missingEnergy);
            if (moved > 0) {
                setChanged();
            }
        }
    }

    private ItemStack getCrystalStack() {
        return itemHandler.getStackInSlot(SLOT_CRYSTAL);
    }

    private ItemStack getEffectiveCrystalStack() {
        return progress > 0 && processCrystalLatched ? activeProcessCrystal : getCrystalStack();
    }

    private void latchProcessCrystal() {
        processCrystalLatched = true;
        ItemStack crystalStack = getCrystalStack();
        if (PowerCrystalEffects.isActive(crystalStack)) {
            activeProcessCrystal = crystalStack.copy();
            PowerCrystalData.drainCharge(crystalStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST);
        } else {
            activeProcessCrystal = ItemStack.EMPTY;
        }
    }

    private void clearLatchedProcessCrystal() {
        activeProcessCrystal = ItemStack.EMPTY;
        processCrystalLatched = false;
    }

    private void loadInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        int serializedSize = inventoryTag.contains("Size") ? inventoryTag.getInt("Size") : SLOT_COUNT;
        if (serializedSize > 6) {
            migrateLegacyInventory(inventoryTag, registries);
            return;
        }

        itemHandler.deserializeNBT(registries, inventoryTag);
    }

    private void migrateLegacyInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        ItemStackHandler legacyHandler = new ItemStackHandler(7);
        legacyHandler.deserializeNBT(registries, inventoryTag);

        itemHandler.setStackInSlot(SLOT_POWER_INPUT, legacyHandler.getStackInSlot(0).copy());
        moveLegacyTemplateStack(legacyHandler.getStackInSlot(2).copy());
        itemHandler.setStackInSlot(SLOT_ITEM_INPUT, legacyHandler.getStackInSlot(3).copy());
        mergeTemplateIntoOutput(legacyHandler.getStackInSlot(4).copy());
        itemHandler.setStackInSlot(SLOT_CRYSTAL, legacyHandler.getStackInSlot(6).copy());

        ItemStack legacyEnergyOutput = legacyHandler.getStackInSlot(1).copy();
        if (!legacyEnergyOutput.isEmpty()) {
            if (itemHandler.getStackInSlot(SLOT_POWER_INPUT).isEmpty()) {
                itemHandler.setStackInSlot(SLOT_POWER_INPUT, legacyEnergyOutput);
            } else {
                itemHandler.setStackInSlot(SLOT_LEGACY_OVERFLOW, legacyEnergyOutput);
            }
        }

        ItemStack legacyRejected = legacyHandler.getStackInSlot(5).copy();
        if (!legacyRejected.isEmpty()) {
            if (itemHandler.getStackInSlot(SLOT_LEGACY_OVERFLOW).isEmpty()) {
                itemHandler.setStackInSlot(SLOT_LEGACY_OVERFLOW, legacyRejected);
            } else {
                itemHandler.getStackInSlot(SLOT_LEGACY_OVERFLOW).grow(legacyRejected.getCount());
            }
        }
    }

    private void moveLegacyTemplateStack(ItemStack legacyTemplate) {
        if (legacyTemplate.isEmpty()) {
            return;
        }
        if (legacyTemplate.is(ModItems.EMPTY_TEMPLATE.get())) {
            itemHandler.setStackInSlot(SLOT_EMPTY_TEMPLATE, legacyTemplate);
            return;
        }
        if (legacyTemplate.is(ModItems.ENCODED_TEMPLATE.get()) && EncodedTemplateData.isComplete(legacyTemplate)) {
            mergeTemplateIntoOutput(legacyTemplate);
            return;
        }
        itemHandler.setStackInSlot(SLOT_ACTIVE_TEMPLATE, legacyTemplate);
    }

    private void mergeTemplateIntoOutput(ItemStack templateStack) {
        if (templateStack.isEmpty()) {
            return;
        }
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, templateStack);
            return;
        }
        if (ItemStack.isSameItemSameComponents(outputStack, templateStack)) {
            outputStack.grow(templateStack.getCount());
        } else if (itemHandler.getStackInSlot(SLOT_LEGACY_OVERFLOW).isEmpty()) {
            itemHandler.setStackInSlot(SLOT_LEGACY_OVERFLOW, templateStack);
        }
    }

    private static String normalizeCustomName(String customName) {
        String normalized = customName.strip();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private void syncCustomName() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void initializeDefaults() {
        sideConfiguration.set(SideConfigType.ITEMS, Direction.DOWN, SideAccessMode.OUTPUT);
    }

    private SideAccessMode sanitizeSideAccessMode(SideConfigType type, Direction side, SideAccessMode requestedMode) {
        return sanitizeSideAccessMode(type, requestedMode);
    }

    private SideAccessMode sanitizeSideAccessMode(SideConfigType type, SideAccessMode requestedMode) {
        boolean canInput = supportsSideConfigInput(type);
        boolean canOutput = supportsSideConfigOutput(type);
        if (requestedMode == SideAccessMode.BOTH && !(canInput && canOutput)) {
            return canInput ? SideAccessMode.INPUT : canOutput ? SideAccessMode.OUTPUT : SideAccessMode.DISABLED;
        }
        if (requestedMode == SideAccessMode.INPUT && !canInput) {
            return canOutput ? SideAccessMode.OUTPUT : SideAccessMode.DISABLED;
        }
        if (requestedMode == SideAccessMode.OUTPUT && !canOutput) {
            return canInput ? SideAccessMode.INPUT : SideAccessMode.DISABLED;
        }
        return requestedMode;
    }

    private IItemHandler[] createConfiguredItemHandlers() {
        IItemHandler[] handlers = new IItemHandler[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredItemHandler(
                    () -> getSideAccessMode(SideConfigType.ITEMS, side),
                    () -> inputAutomationHandler,
                    () -> outputAutomationHandler
            );
        }
        return handlers;
    }

    private IEnergyStorage[] createConfiguredEnergyHandlers() {
        IEnergyStorage[] handlers = new IEnergyStorage[Direction.values().length];
        for (Direction side : Direction.values()) {
            handlers[side.ordinal()] = new ConfiguredEnergyStorage(
                    () -> getSideAccessMode(SideConfigType.ENERGY, side),
                    () -> externalEnergyStorage
            );
        }
        return handlers;
    }
}
