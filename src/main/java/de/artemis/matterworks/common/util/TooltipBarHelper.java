package de.artemis.matterworks.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class TooltipBarHelper {
    private static final char BAR_SEGMENT = '|';

    private TooltipBarHelper() {
    }

    public static Component buildBar(float ratio, int width, ChatFormatting filledColor, ChatFormatting emptyColor, String suffix) {
        int filledSegments = Math.round(Mth.clamp(ratio, 0.0F, 1.0F) * width);
        String segment = String.valueOf(BAR_SEGMENT);
        return Component.literal("[")
                .withStyle(emptyColor)
                .append(Component.literal(segment.repeat(filledSegments)).withStyle(filledColor))
                .append(Component.literal(segment.repeat(Math.max(0, width - filledSegments))).withStyle(emptyColor))
                .append(Component.literal("] ").withStyle(emptyColor))
                .append(Component.literal(suffix).withStyle(ChatFormatting.GRAY));
    }
}
