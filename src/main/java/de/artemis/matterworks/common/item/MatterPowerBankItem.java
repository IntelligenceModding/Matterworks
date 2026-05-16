package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.energy.EnergyItemHelper;
import de.artemis.matterworks.common.energy.PowerBankData;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MatterPowerBankItem extends Item {
    public static final int CAPACITY = 100_000;
    public static final int MAX_TRANSFER = 1_000;
    public static final int AUTO_CHARGE_PER_TICK = 500;
    private static final int TOOLTIP_BAR_WIDTH = 40;

    public MatterPowerBankItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public ItemStack createChargedStack() {
        ItemStack stack = new ItemStack(this);
        PowerBankData.setEnergyStored(stack, CAPACITY, CAPACITY);
        return stack;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.round(getChargeRatio(stack) * 13.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3DDC84;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        if (PowerBankData.isAutoChargeEnabled(stack)) {
            EnergyItemHelper.chargePlayerEquipment(stack, player, AUTO_CHARGE_PER_TICK);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide()) {
            boolean enabled = !PowerBankData.isAutoChargeEnabled(stack);
            PowerBankData.setAutoChargeEnabled(stack, enabled);
            player.displayClientMessage(
                    Component.translatable(
                            enabled
                                    ? "message.matterworks.matter_power_bank.auto_charge_enabled"
                                    : "message.matterworks.matter_power_bank.auto_charge_disabled"
                    ),
                    true
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        int energyStored = PowerBankData.getEnergyStored(stack, CAPACITY);
        boolean autoChargeEnabled = PowerBankData.isAutoChargeEnabled(stack);
        tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_power_bank").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                autoChargeEnabled
                        ? "tooltip.matterworks.matter_power_bank.auto_charge_enabled"
                        : "tooltip.matterworks.matter_power_bank.auto_charge_disabled"
        ).withStyle(autoChargeEnabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_power_bank.toggle").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(TooltipBarHelper.buildBar(
                getChargeRatio(stack),
                TOOLTIP_BAR_WIDTH,
                ChatFormatting.GREEN,
                ChatFormatting.DARK_GRAY,
                energyStored + " / " + CAPACITY + " FE"
        ));
    }

    private float getChargeRatio(ItemStack stack) {
        return PowerBankData.getEnergyStored(stack, CAPACITY) / (float) CAPACITY;
    }
}
