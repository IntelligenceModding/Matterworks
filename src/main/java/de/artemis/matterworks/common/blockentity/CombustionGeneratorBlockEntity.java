package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.debug.SideConfigDebugTracker;
import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.io.ConfiguredEnergyStorage;
import de.artemis.matterworks.common.io.ConfiguredItemHandler;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.io.SideConfigurationData;
import de.artemis.matterworks.common.menu.CombustionGeneratorMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CombustionGeneratorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements MenuProvider, CustomNamedBlockEntity, SideConfigurableBlockEntity {
    public static final int FUEL_SLOT_START = 0;
    public static final int FUEL_SLOT_COUNT = 6;
    public static final int SLOT_BOOST = FUEL_SLOT_START + FUEL_SLOT_COUNT;
    public static final int SLOT_POWER_BANK = SLOT_BOOST + 1;
    public static final int SLOT_COUNT = SLOT_POWER_BANK + 1;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_COUNT = 2;
    public static final int BASE_ENERGY_PER_FUEL_UNIT = PowerCrystalEffects.COMBUSTION_GENERATOR_BASE_ENERGY_PER_TICK;
    public static final int MAX_POWER_BANK_TRANSFER_PER_TICK = 200;
    public static final int MAX_SIDE_TRANSFER_PER_TICK = 200;
    private static final int HISTORY_SAMPLE_INTERVAL = 4;
    private static final int HISTORY_LENGTH = 120;
    private static final int COAL_BURN_TIME = 1600;
    private static final long REFERENCE_FUEL_CAPACITY_MILLI_BURN = (long) FUEL_SLOT_COUNT * 64L * COAL_BURN_TIME * 1000L;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (getSlotRole(slot)) {
                case FUEL -> isFuel(stack);
                case BOOST -> PowerCrystalEffects.hasCombustionGeneratorEffect(stack);
                case POWER_BANK -> EnergyItemHelper.canReceiveEnergy(stack);
                case NONE -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_BOOST || slot == SLOT_POWER_BANK ? 1 : 64;
        }
    };
    private final GeneratorEnergyStorage energyStorage = new GeneratorEnergyStorage(PowerCrystalEffects.COMBUSTION_GENERATOR_BASE_ENERGY_CAPACITY);
    private final IEnergyStorage externalEnergyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return energyStorage.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAPACITY -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };
    private final SideConfigurationData sideConfiguration = new SideConfigurationData(SideAccessMode.BOTH, SideAccessMode.DISABLED, SideAccessMode.OUTPUT);
    private final IItemHandler inputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return FUEL_SLOT_COUNT + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < FUEL_SLOT_COUNT) {
                return itemHandler.getStackInSlot(FUEL_SLOT_START + slot);
            }
            return slot == FUEL_SLOT_COUNT ? itemHandler.getStackInSlot(SLOT_BOOST) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= 0 && slot < FUEL_SLOT_COUNT) {
                return itemHandler.insertItem(FUEL_SLOT_START + slot, stack, simulate);
            }
            return slot == FUEL_SLOT_COUNT ? itemHandler.insertItem(SLOT_BOOST, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= 0 && slot < FUEL_SLOT_COUNT) {
                return itemHandler.getSlotLimit(FUEL_SLOT_START + slot);
            }
            return slot == FUEL_SLOT_COUNT ? itemHandler.getSlotLimit(SLOT_BOOST) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= 0 && slot < FUEL_SLOT_COUNT) {
                return itemHandler.isItemValid(FUEL_SLOT_START + slot, stack);
            }
            return slot == FUEL_SLOT_COUNT && itemHandler.isItemValid(SLOT_BOOST, stack);
        }
    };
    private final IItemHandler outputAutomationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? itemHandler.getStackInSlot(SLOT_POWER_BANK) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? itemHandler.extractItem(SLOT_POWER_BANK, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? itemHandler.getSlotLimit(SLOT_POWER_BANK) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final IItemHandler[] configuredItemHandlers = createConfiguredItemHandlers();
    private final IEnergyStorage[] configuredEnergyHandlers = createConfiguredEnergyHandlers();
    private final GeneratorHistorySample[] generationHistory = createEmptyHistory();

    private String customName = "";
    private long storedFuelEnergy;
    private int historySize;
    private int currentGenerationRate;
    private int currentFuelUsageMilliRate;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COMBUSTION_GENERATOR.get(), pos, blockState);
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

    public static void tick(Level level, BlockPos pos, BlockState state, CombustionGeneratorBlockEntity blockEntity) {
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

    public int getHistorySize() {
        return historySize;
    }

    public int getHistoryCapacity() {
        return HISTORY_LENGTH;
    }

    public GeneratorHistorySample getHistorySample(int index) {
        if (index < 0 || index >= historySize) {
            return GeneratorHistorySample.EMPTY;
        }
        return generationHistory[index];
    }

    public int getCurrentGenerationRate() {
        return currentGenerationRate;
    }

    public int getCurrentFuelUsageMilliRate() {
        return currentFuelUsageMilliRate;
    }

    public long getStoredFuelMilliBurn() {
        return Math.max(0L, storedFuelEnergy * 1000L / BASE_ENERGY_PER_FUEL_UNIT);
    }

    public long getFuelCapacityMilliBurn() {
        return REFERENCE_FUEL_CAPACITY_MILLI_BURN;
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
        syncVisualState();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CombustionGeneratorMenu(containerId, playerInventory, this, data);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        sideConfiguration.writeToTag(tag);
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putLong("stored_fuel_energy", storedFuelEnergy);
        if (!customName.isEmpty()) {
            tag.putString("custom_name", customName);
        }
        writeGenerationTelemetryTag(tag);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putLong("stored_fuel_energy", storedFuelEnergy);
        sideConfiguration.writeToTag(tag);
        writeGenerationTelemetryTag(tag);
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
        energyStorage.setStoredEnergy(tag.getInt("energy"));
        storedFuelEnergy = tag.contains("stored_fuel_energy", Tag.TAG_LONG)
                ? Math.max(0L, tag.getLong("stored_fuel_energy"))
                : Math.max(0L, tag.getInt("active_fuel_energy"));
        sideConfiguration.readFromTag(tag, this::sanitizeSideAccessMode);
        readGenerationTelemetryTag(tag);
        customName = normalizeCustomName(tag.getString("custom_name"));
        syncEnergyCapacity();
    }

    private void serverTick() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        int energyBefore = energyStorage.getEnergyStored();
        int energyCapacityBefore = energyStorage.getMaxEnergyStored();
        long storedFuelEnergyBefore = storedFuelEnergy;
        int generationBefore = currentGenerationRate;
        int fuelUsageBefore = currentFuelUsageMilliRate;

        syncEnergyCapacity();
        absorbFuelItems();
        transferEnergyToPowerBank();
        pushEnergyToNeighbors();
        currentGenerationRate = generateEnergyFromFuel();
        currentFuelUsageMilliRate = toMilliBurnRate(currentGenerationRate);

        boolean sampled = sampleGenerationHistory(serverLevel);
        if (sampled
                || energyBefore != energyStorage.getEnergyStored()
                || energyCapacityBefore != energyStorage.getMaxEnergyStored()
                || storedFuelEnergyBefore != storedFuelEnergy
                || generationBefore != currentGenerationRate
                || fuelUsageBefore != currentFuelUsageMilliRate) {
            setChanged();
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void syncEnergyCapacity() {
        int desiredCapacity = PowerCrystalEffects.getCombustionGeneratorEnergyCapacity(itemHandler.getStackInSlot(SLOT_BOOST));
        if (energyStorage.getMaxEnergyStored() != desiredCapacity) {
            energyStorage.setCapacity(desiredCapacity);
            setChanged();
        }
    }

    private void transferEnergyToPowerBank() {
        ItemStack targetStack = itemHandler.getStackInSlot(SLOT_POWER_BANK);
        IEnergyStorage targetEnergy = EnergyItemHelper.getEnergyStorage(targetStack);
        if (targetEnergy == null) {
            return;
        }

        int moved = EnergyItemHelper.transferEnergy(energyStorage, targetEnergy, MAX_POWER_BANK_TRANSFER_PER_TICK);
        if (moved > 0) {
            setChanged();
        }
    }

    private void pushEnergyToNeighbors() {
        for (Direction direction : Direction.values()) {
            if (energyStorage.getEnergyStored() <= 0) {
                return;
            }
            if (!getSideAccessMode(SideConfigType.ENERGY, direction).allowsOutput()) {
                continue;
            }

            BlockPos targetPos = worldPosition.relative(direction);
            IEnergyStorage targetStorage = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.getOpposite());
            if (targetStorage == null || !targetStorage.canReceive()) {
                continue;
            }

            int moved = EnergyItemHelper.transferEnergy(energyStorage, targetStorage, MAX_SIDE_TRANSFER_PER_TICK);
            if (moved > 0) {
                setChanged();
            }
        }
    }

    private int generateEnergyFromFuel() {
        int energyRoom = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (energyRoom <= 0) {
            return 0;
        }

        if (storedFuelEnergy <= 0L) {
            return 0;
        }

        int desiredGeneration = getEnergyPerTick();
        int generated = (int) Math.min(Math.min(desiredGeneration, energyRoom), storedFuelEnergy);
        if (generated <= 0) {
            return 0;
        }

        energyStorage.receiveEnergy(generated, false);
        storedFuelEnergy = Math.max(0L, storedFuelEnergy - generated);
        return generated;
    }

    private void absorbFuelItems() {
        for (int slot = FUEL_SLOT_START; slot < FUEL_SLOT_START + FUEL_SLOT_COUNT; slot++) {
            while (absorbFuelSlot(slot)) {
                setChanged();
            }
        }
    }

    private boolean absorbFuelSlot(int slot) {
        ItemStack fuelStack = itemHandler.getStackInSlot(slot);
        if (fuelStack.isEmpty()) {
            return false;
        }

        int burnTime = getFuelBurnTime(fuelStack);
        if (burnTime <= 0) {
            return false;
        }

        long fuelEnergyPerItem = (long) burnTime * BASE_ENERGY_PER_FUEL_UNIT;
        long remainingCapacity = getFuelCapacityEnergy() - storedFuelEnergy;
        if (fuelEnergyPerItem <= 0L || remainingCapacity < fuelEnergyPerItem) {
            return false;
        }

        ItemStack remainder = fuelStack.getCraftingRemainingItem();
        if (remainder.isEmpty()) {
            int itemsToConsume = (int) Math.min(fuelStack.getCount(), remainingCapacity / fuelEnergyPerItem);
            if (itemsToConsume <= 0) {
                return false;
            }
            storedFuelEnergy += fuelEnergyPerItem * itemsToConsume;
            fuelStack.shrink(itemsToConsume);
            consumeBoostCharge(itemsToConsume);
            return true;
        }

        storedFuelEnergy += fuelEnergyPerItem;
        fuelStack.shrink(1);
        if (fuelStack.isEmpty()) {
            itemHandler.setStackInSlot(slot, remainder.copy());
        }
        consumeBoostCharge();
        return true;
    }

    private void consumeBoostCharge() {
        ItemStack boostStack = itemHandler.getStackInSlot(SLOT_BOOST);
        if (PowerCrystalEffects.isActive(boostStack)) {
            PowerCrystalData.drainCharge(boostStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST);
        }
    }

    private void consumeBoostCharge(int amount) {
        ItemStack boostStack = itemHandler.getStackInSlot(SLOT_BOOST);
        if (PowerCrystalEffects.isActive(boostStack)) {
            PowerCrystalData.drainCharge(boostStack, PowerCrystalEffects.CRYSTAL_CHARGE_COST * Math.max(1, amount));
        }
    }

    private int getEnergyPerTick() {
        return PowerCrystalEffects.getCombustionGeneratorEnergyPerTick(itemHandler.getStackInSlot(SLOT_BOOST));
    }

    private boolean sampleGenerationHistory(net.minecraft.server.level.ServerLevel serverLevel) {
        if (serverLevel.getGameTime() % HISTORY_SAMPLE_INTERVAL != 0L) {
            return false;
        }

        GeneratorHistorySample sample = new GeneratorHistorySample(currentGenerationRate, currentFuelUsageMilliRate);
        if (historySize < HISTORY_LENGTH) {
            generationHistory[historySize] = sample;
            historySize++;
        } else {
            System.arraycopy(generationHistory, 1, generationHistory, 0, HISTORY_LENGTH - 1);
            generationHistory[HISTORY_LENGTH - 1] = sample;
        }
        return true;
    }

    private static int toMilliBurnRate(int energyPerTick) {
        if (energyPerTick <= 0) {
            return 0;
        }
        return (int) ((energyPerTick * 1000L + BASE_ENERGY_PER_FUEL_UNIT / 2L) / BASE_ENERGY_PER_FUEL_UNIT);
    }

    private static long getFuelCapacityEnergy() {
        return REFERENCE_FUEL_CAPACITY_MILLI_BURN * BASE_ENERGY_PER_FUEL_UNIT / 1000L;
    }

    private void writeGenerationTelemetryTag(CompoundTag tag) {
        tag.putInt("current_generation_rate", currentGenerationRate);
        tag.putInt("current_fuel_usage_rate", currentFuelUsageMilliRate);
        tag.putInt("history_size", historySize);

        ListTag historyTag = new ListTag();
        for (int index = 0; index < historySize; index++) {
            GeneratorHistorySample sample = generationHistory[index];
            CompoundTag sampleTag = new CompoundTag();
            sampleTag.putInt("generation_rate", sample.generationRate());
            sampleTag.putInt("fuel_usage_rate", sample.fuelUsageMilliRate());
            historyTag.add(sampleTag);
        }
        tag.put("generation_history", historyTag);
    }

    private void readGenerationTelemetryTag(CompoundTag tag) {
        currentGenerationRate = Math.max(0, tag.getInt("current_generation_rate"));
        currentFuelUsageMilliRate = Math.max(0, tag.getInt("current_fuel_usage_rate"));

        clearHistory();
        ListTag historyTag = tag.getList("generation_history", Tag.TAG_COMPOUND);
        int loadedSize = Math.min(HISTORY_LENGTH, historyTag.size());
        for (int index = 0; index < loadedSize; index++) {
            CompoundTag sampleTag = historyTag.getCompound(index);
            generationHistory[index] = new GeneratorHistorySample(
                    sampleTag.getInt("generation_rate"),
                    sampleTag.getInt("fuel_usage_rate")
            );
        }
        historySize = Math.min(HISTORY_LENGTH, Math.max(tag.getInt("history_size"), loadedSize));
    }

    private void clearHistory() {
        for (int index = 0; index < HISTORY_LENGTH; index++) {
            generationHistory[index] = GeneratorHistorySample.EMPTY;
        }
        historySize = 0;
    }

    private void loadInventory(CompoundTag inventoryTag, HolderLookup.Provider registries) {
        int serializedSize = inventoryTag.contains("Size", Tag.TAG_INT) ? inventoryTag.getInt("Size") : SLOT_COUNT;
        ItemStackHandler serializedHandler = new ItemStackHandler(Math.max(1, serializedSize));
        serializedHandler.deserializeNBT(registries, inventoryTag);

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }

        if (serializedSize == 1) {
            itemHandler.setStackInSlot(FUEL_SLOT_START, serializedHandler.getStackInSlot(0));
            return;
        }

        int slotsToCopy = Math.min(serializedHandler.getSlots(), itemHandler.getSlots());
        for (int slot = 0; slot < slotsToCopy; slot++) {
            itemHandler.setStackInSlot(slot, serializedHandler.getStackInSlot(slot));
        }
    }

    private static GeneratorHistorySample[] createEmptyHistory() {
        GeneratorHistorySample[] history = new GeneratorHistorySample[HISTORY_LENGTH];
        for (int index = 0; index < HISTORY_LENGTH; index++) {
            history[index] = GeneratorHistorySample.EMPTY;
        }
        return history;
    }

    private static boolean isFuel(ItemStack stack) {
        return getFuelBurnTime(stack) > 0;
    }

    private static int getFuelBurnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : Math.max(0, stack.getBurnTime(RecipeType.SMELTING));
    }

    private Component getDefaultName() {
        return Component.translatable(ModBlocks.COMBUSTION_GENERATOR.get().getDescriptionId());
    }

    @Override
    public boolean supportsSideConfigType(SideConfigType type) {
        return type == SideConfigType.ITEMS || type == SideConfigType.ENERGY;
    }

    @Override
    public boolean supportsSideConfigInput(SideConfigType type) {
        return type == SideConfigType.ITEMS;
    }

    @Override
    public boolean supportsSideConfigOutput(SideConfigType type) {
        return type == SideConfigType.ITEMS || type == SideConfigType.ENERGY;
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
        if (sideConfiguration.set(type, side, sanitizeSideAccessMode(type, side, mode))) {
            setChanged();
            syncVisualState();
        }
    }

    private void syncVisualState() {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static String normalizeCustomName(String customName) {
        String normalized = customName.strip();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private SideAccessMode sanitizeSideAccessMode(SideConfigType type, Direction side, SideAccessMode requestedMode) {
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

    private static SlotRole getSlotRole(int slot) {
        if (slot >= FUEL_SLOT_START && slot < FUEL_SLOT_START + FUEL_SLOT_COUNT) {
            return SlotRole.FUEL;
        }
        if (slot == SLOT_BOOST) {
            return SlotRole.BOOST;
        }
        if (slot == SLOT_POWER_BANK) {
            return SlotRole.POWER_BANK;
        }
        return SlotRole.NONE;
    }

    public record GeneratorHistorySample(int generationRate, int fuelUsageMilliRate) {
        public static final GeneratorHistorySample EMPTY = new GeneratorHistorySample(0, 0);
    }

    private enum SlotRole {
        FUEL,
        BOOST,
        POWER_BANK,
        NONE
    }

    private static final class GeneratorEnergyStorage extends EnergyStorage {
        private GeneratorEnergyStorage(int capacity) {
            super(capacity, capacity, capacity);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            return extracted;
        }

        private void setStoredEnergy(int amount) {
            this.energy = Mth.clamp(amount, 0, this.capacity);
        }

        private void setCapacity(int amount) {
            this.capacity = Math.max(0, amount);
            if (this.energy > this.capacity) {
                this.energy = this.capacity;
            }
        }
    }
}
