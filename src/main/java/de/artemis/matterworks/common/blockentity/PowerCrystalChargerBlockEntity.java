package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.PowerCrystalChargerMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public class PowerCrystalChargerBlockEntity extends AbstractMatterMachineBlockEntity implements MenuProvider {
    public static final int TICKS_PER_PERCENT = 10;
    public static final int ENERGY_CAPACITY = 10000;
    public static final int ENERGY_PER_TICK = 20;

    public PowerCrystalChargerBlockEntity(BlockPos pos, BlockState blockState) {
        super(
                ModBlockEntities.POWER_CRYSTAL_CHARGER.get(),
                pos,
                blockState,
                0,
                fluidStack -> false,
                ENERGY_CAPACITY
        );
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, PowerCrystalChargerBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PowerCrystalChargerMenu(containerId, playerInventory, this, getData());
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.POWER_CRYSTAL_CHARGER.get().getDescriptionId());
    }

    @Override
    protected boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
        if (slot == ENERGY_ITEM_INPUT_SLOT) {
            return isEnergyItem(stack);
        }
        if (slot == ENERGY_ITEM_OUTPUT_SLOT || slot == INVALID_OUTPUT_SLOT || slot == FLUID_BUCKET_INPUT_SLOT || slot == FLUID_BUCKET_OUTPUT_SLOT || slot == CRYSTAL_SLOT) {
            return false;
        }
        return slot == INPUT_SLOT && PowerCrystalEffects.isPowerCrystal(stack);
    }

    @Override
    protected boolean canProcess() {
        net.minecraft.world.item.ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        if (!PowerCrystalEffects.isPowerCrystal(inputStack)) {
            return false;
        }
        return PowerCrystalData.getChargePercent(inputStack) < PowerCrystalData.MAX_CHARGE;
    }

    @Override
    protected void processItem() {
        net.minecraft.world.item.ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        if (!PowerCrystalEffects.isPowerCrystal(inputStack)) {
            return;
        }

        int currentCharge = PowerCrystalData.getChargePercent(inputStack);
        PowerCrystalData.setChargePercent(inputStack, Math.min(PowerCrystalData.MAX_CHARGE, currentCharge + 1));
        setChanged();
    }

    @Override
    protected int getMaxProgress() {
        return TICKS_PER_PERCENT;
    }

    @Override
    protected int getEnergyPerTick() {
        return ENERGY_PER_TICK;
    }

    @Override
    public int getEffectiveProgressBarColor() {
        net.minecraft.world.item.ItemStack displayStack = itemHandler.getStackInSlot(INPUT_SLOT);
        return PowerCrystalEffects.isPowerCrystal(displayStack) ? PowerCrystalEffects.getBarColor(displayStack) : super.getEffectiveProgressBarColor();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        migrateLegacyOutputSlot();
    }

    private void migrateLegacyOutputSlot() {
        net.minecraft.world.item.ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
        net.minecraft.world.item.ItemStack outputStack = itemHandler.getStackInSlot(INVALID_OUTPUT_SLOT);
        if (inputStack.isEmpty() && PowerCrystalEffects.isPowerCrystal(outputStack)) {
            itemHandler.setStackInSlot(INPUT_SLOT, outputStack.copy());
            itemHandler.setStackInSlot(INVALID_OUTPUT_SLOT, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
