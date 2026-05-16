package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import de.artemis.matterworks.common.upgrade.PowerCrystalEffects;
import de.artemis.matterworks.common.util.TooltipBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class PowerCrystalItem extends Item {
    private static final int TOOLTIP_BAR_WIDTH = 40;

    private final ChatFormatting chargeColor;
    private final String descriptionKey;

    public PowerCrystalItem(Properties properties, ChatFormatting chargeColor, String descriptionKey) {
        super(properties.stacksTo(1));
        this.chargeColor = chargeColor;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(chargeColor);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(1, Math.round(PowerCrystalData.getChargeRatio(stack) * 13.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return PowerCrystalEffects.getBarColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(TooltipBarHelper.buildBar(
                PowerCrystalData.getChargeRatio(stack),
                TOOLTIP_BAR_WIDTH,
                PowerCrystalData.getChargePercent(stack) > 0 ? chargeColor : ChatFormatting.DARK_GRAY,
                ChatFormatting.DARK_GRAY,
                PowerCrystalData.getChargePercent(stack) + "%"
        ));
    }
}
