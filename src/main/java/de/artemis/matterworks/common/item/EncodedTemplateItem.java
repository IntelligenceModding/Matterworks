package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.template.EncodedTemplateData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EncodedTemplateItem extends Item {
    private static final int INCOMPLETE_BAR_COLOR = 0xB67CFF;
    private static final int COMPLETE_BAR_COLOR = 0x5CFF97;
    private static final int TOOLTIP_PROGRESS_BAR_WIDTH = 40;
    private static final char TOOLTIP_PROGRESS_SEGMENT = '|';

    public EncodedTemplateItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component encodedName = EncodedTemplateData.getEncodedItemName(stack);
        if (encodedName != null) {
            return Component.translatable("item.matterworks.encoded_template.filled", encodedName);
        }

        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return EncodedTemplateData.isComplete(stack) || super.isFoil(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return EncodedTemplateData.hasEncodedItem(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!EncodedTemplateData.hasEncodedItem(stack)) {
            return 0;
        }

        return Math.max(1, Math.round(EncodedTemplateData.getProgressRatio(stack) * 13.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return EncodedTemplateData.isComplete(stack) ? COMPLETE_BAR_COLOR : INCOMPLETE_BAR_COLOR;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag flag
    ) {
        Component encodedName = EncodedTemplateData.getEncodedItemName(stack);
        if (encodedName != null) {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.encoded_with", encodedName).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable(
                    "tooltip.matterworks.pattern_progress",
                    EncodedTemplateData.getAnalysisProgress(stack),
                    EncodedTemplateData.getRequiredCount(stack)
            ).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(buildTooltipProgressBar(stack));
        }
    }

    private static Component buildTooltipProgressBar(ItemStack stack) {
        int filledSegments = Math.round(EncodedTemplateData.getProgressRatio(stack) * TOOLTIP_PROGRESS_BAR_WIDTH);
        String segment = String.valueOf(TOOLTIP_PROGRESS_SEGMENT);
        return Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(segment.repeat(filledSegments)).withStyle(
                        EncodedTemplateData.isComplete(stack) ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE
                ))
                .append(Component.literal(segment.repeat(Math.max(0, TOOLTIP_PROGRESS_BAR_WIDTH - filledSegments))).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(EncodedTemplateData.getProgressPercent(stack) + "%").withStyle(ChatFormatting.GRAY));
    }
}
