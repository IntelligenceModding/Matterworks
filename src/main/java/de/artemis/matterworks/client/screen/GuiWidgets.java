package de.artemis.matterworks.client.screen;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

public final class GuiWidgets {
    private static final ResourceLocation VANILLA_BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation VANILLA_BUTTON_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");
    private static final ResourceLocation VANILLA_BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final int BUTTON_TEXT = 0xFFE0E0E0;
    private static final int BUTTON_TEXT_DISABLED = 0xFFA0A0A0;
    private static final int TEXT_FIELD_TEXT = 0xFFE0E0E0;
    private static final int TEXT_FIELD_TEXT_UNEDITABLE = 0xFFA0A0A0;
    private static final int HORIZONTAL_TEXT_PADDING = 6;
    private static final int CONTENT_BACKGROUND = 0xFF1F1F1F;
    private static final int GRAPH_GRID = 0x22373737;
    private static final int GRAPH_HOVER_LINE = 0x66FFFFFF;
    private static final int GRAPH_HOVER_POINT = 0xFFFFFFFF;
    private static Double pendingMouseGuiX;
    private static Double pendingMouseGuiY;

    private GuiWidgets() {
    }

    public static PanelTextField textField(Font font, int x, int y, int width, int height, Component message) {
        return new PanelTextField(font, x, y, width, height, message);
    }

    public static PanelButton panelButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new PanelButton(x, y, width, height, message, onPress, TextAlignment.CENTER);
    }

    public static PanelButton leftAlignedPanelButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new PanelButton(x, y, width, height, message, onPress, TextAlignment.LEFT);
    }

    public static SelectablePanelButton selectablePanelButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new SelectablePanelButton(x, y, width, height, message, onPress, TextAlignment.CENTER);
    }

    public static SelectablePanelButton leftAlignedSelectablePanelButton(int x, int y, int width, int height, Component message, Button.OnPress onPress) {
        return new SelectablePanelButton(x, y, width, height, message, onPress, TextAlignment.LEFT);
    }

    public static SpriteSelectableButton spriteSelectableButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Button.OnPress onPress,
            ResourceLocation texture,
            int u,
            int v,
            int textureWidth,
            int textureHeight
    ) {
        return new SpriteSelectableButton(x, y, width, height, message, onPress, texture, u, v, textureWidth, textureHeight);
    }

    public static void playButtonClickSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public static void rememberMousePosition(double mouseX, double mouseY) {
        pendingMouseGuiX = mouseX;
        pendingMouseGuiY = mouseY;
    }

    public static void rememberCurrentMousePosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        Window window = minecraft.getWindow();
        pendingMouseGuiX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / Math.max(1.0, window.getScreenWidth());
        pendingMouseGuiY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / Math.max(1.0, window.getScreenHeight());
    }

    public static void restoreRememberedMousePosition() {
        if (pendingMouseGuiX == null || pendingMouseGuiY == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            clearRememberedMousePosition();
            return;
        }

        Window window = minecraft.getWindow();
        double rawX = pendingMouseGuiX * window.getWidth() / Math.max(1.0, window.getGuiScaledWidth());
        double rawY = pendingMouseGuiY * window.getHeight() / Math.max(1.0, window.getGuiScaledHeight());
        GLFW.glfwSetCursorPos(window.getWindow(), rawX, rawY);
        clearRememberedMousePosition();
    }

    private static void clearRememberedMousePosition() {
        pendingMouseGuiX = null;
        pendingMouseGuiY = null;
    }

    public static void drawInsetGraph(
            GuiGraphics guiGraphics,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int historySize,
            int historyCapacity,
            IntUnaryOperator visibleSampleValueGetter,
            int currentValue,
            int hoveredVisibleIndex,
            int lineColor,
            int areaColor
    ) {
        int chartLeft = frameLeft + 2;
        int chartTop = frameTop + 2;
        int chartWidth = frameWidth - 4;
        int chartHeight = frameHeight - 4;
        int bottom = chartTop + chartHeight - 1;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, frameLeft, frameTop, frameWidth, frameHeight);
        guiGraphics.fill(chartLeft, chartTop, chartLeft + chartWidth, chartTop + chartHeight, CONTENT_BACKGROUND);
        drawGraphGrid(guiGraphics, chartLeft, chartTop, chartWidth, chartHeight);

        if (historyCapacity <= 0) {
            return;
        }

        int minValue = Math.min(0, currentValue);
        int maxValue = Math.max(0, currentValue);
        for (int visibleIndex = 0; visibleIndex < historyCapacity; visibleIndex++) {
            int sample = visibleSampleValueGetter.applyAsInt(visibleIndex);
            minValue = Math.min(minValue, sample);
            maxValue = Math.max(maxValue, sample);
        }
        if (minValue == 0 && maxValue == 0) {
            maxValue = 1;
        }
        int zeroY = getSampleY(0, minValue, maxValue, chartTop, chartHeight);
        if (minValue < 0 && maxValue > 0) {
            guiGraphics.fill(chartLeft, zeroY, chartLeft + chartWidth, zeroY + 1, GRAPH_GRID | 0x22000000);
        }

        for (int visibleIndex = 0; visibleIndex < historyCapacity - 1; visibleIndex++) {
            int current = visibleSampleValueGetter.applyAsInt(visibleIndex);
            int next = visibleSampleValueGetter.applyAsInt(visibleIndex + 1);
            int x1 = getSampleX(visibleIndex, historyCapacity, chartLeft, chartWidth);
            int y1 = getSampleY(current, minValue, maxValue, chartTop, chartHeight);
            int x2 = getSampleX(visibleIndex + 1, historyCapacity, chartLeft, chartWidth);
            int y2 = getSampleY(next, minValue, maxValue, chartTop, chartHeight);
            drawAreaSegment(guiGraphics, x1, y1, x2, y2, zeroY, areaColor);
            drawLineSegment(guiGraphics, x1, y1, x2, y2, lineColor);
        }

        if (historySize == 1) {
            int x = getSampleX(historyCapacity - 1, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(visibleSampleValueGetter.applyAsInt(historyCapacity - 1), minValue, maxValue, chartTop, chartHeight);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, lineColor);
        } else if (historySize > 1) {
            int latestVisibleIndex = historyCapacity - 1;
            int x = getSampleX(latestVisibleIndex, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(visibleSampleValueGetter.applyAsInt(latestVisibleIndex), minValue, maxValue, chartTop, chartHeight);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, lineColor);
        }

        if (hoveredVisibleIndex >= 0 && hoveredVisibleIndex < historyCapacity) {
            int sample = visibleSampleValueGetter.applyAsInt(hoveredVisibleIndex);
            int x = getSampleX(hoveredVisibleIndex, historyCapacity, chartLeft, chartWidth);
            int y = getSampleY(sample, minValue, maxValue, chartTop, chartHeight);
            guiGraphics.fill(x, chartTop, x + 1, chartTop + chartHeight, GRAPH_HOVER_LINE);
            guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, GRAPH_HOVER_POINT);
            guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, lineColor);
        }
    }

    public static Component formatRateComponent(int rate, String unit, boolean signed) {
        return Component.literal(formatRateText(rate, unit, signed));
    }

    public static String formatRateText(int rate, String unit, boolean signed) {
        String prefix = signed && rate > 0 ? "+" : "";
        return prefix + rate + " " + unit;
    }

    public static Component formatDecimalRateComponent(double rate, String unit) {
        return Component.literal(formatDecimalRateText(rate, unit));
    }

    public static String formatDecimalRateText(double rate, String unit) {
        return String.format(Locale.ROOT, "%.2f %s", rate, unit);
    }

    public static void drawInsetVerticalFillBar(
            GuiGraphics guiGraphics,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int filledHeight,
            int fillColor,
            int highlightColor
    ) {
        int barLeft = frameLeft + 2;
        int barTop = frameTop + 2;
        int barWidth = frameWidth - 4;
        int barHeight = frameHeight - 4;
        int barBottom = barTop + barHeight;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, frameLeft, frameTop, frameWidth, frameHeight);
        guiGraphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, CONTENT_BACKGROUND);
        if (filledHeight <= 0) {
            return;
        }

        int clampedHeight = Math.min(filledHeight, barHeight);
        int fillTop = barBottom - clampedHeight;
        guiGraphics.fill(barLeft, fillTop, barLeft + barWidth, barBottom, fillColor);
        guiGraphics.fill(barLeft, fillTop, barLeft + barWidth, Math.min(barBottom, fillTop + 2), highlightColor);
    }

    public static void fillHorizontalGauge(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int filledWidth,
            int height,
            int fillColor,
            int highlightColor
    ) {
        if (filledWidth <= 0 || height <= 0) {
            return;
        }

        guiGraphics.fill(x, y, x + filledWidth, y + height, fillColor);
        guiGraphics.fill(x, y, x + filledWidth, Math.min(y + height, y + 2), highlightColor);
    }

    public static void fillVerticalGauge(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int filledHeight,
            int fillColor,
            int highlightColor
    ) {
        if (filledHeight <= 0 || width <= 0 || height <= 0) {
            return;
        }

        int clampedHeight = Math.min(filledHeight, height);
        int fillTop = y + height - clampedHeight;
        guiGraphics.fill(x, fillTop, x + width, y + height, fillColor);
        guiGraphics.fill(x, fillTop, x + width, Math.min(y + height, fillTop + 2), highlightColor);
    }

    public static int getFluidFillColor(FluidStack fluidStack, int fallbackColor) {
        if (fluidStack == null || fluidStack.isEmpty()) {
            return fallbackColor;
        }
        int tint = IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack);
        return tint == 0 ? fallbackColor : 0xFF000000 | (tint & 0x00FFFFFF);
    }

    public static int getFluidHighlightColor(FluidStack fluidStack, int fallbackColor) {
        return lightenColor(getFluidFillColor(fluidStack, fallbackColor), 0.22F);
    }

    public static void tintSlotInterior(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int fillColor,
            int highlightColor
    ) {
        if (width <= 2 || height <= 2) {
            return;
        }

        int innerLeft = x + 1;
        int innerTop = y + 1;
        int innerRight = x + width - 1;
        int innerBottom = y + height - 1;
        guiGraphics.fill(innerLeft, innerTop, innerRight, innerBottom, fillColor);
        guiGraphics.fill(innerLeft, innerTop, innerRight, Math.min(innerBottom, innerTop + 2), highlightColor);
    }

    public static int getHoveredHistorySampleIndex(int mouseX, int mouseY, int frameLeft, int frameTop, int frameWidth, int frameHeight, int historyCapacity) {
        if (historyCapacity <= 0) {
            return -1;
        }

        int chartLeft = frameLeft + 2;
        int chartTop = frameTop + 2;
        int chartWidth = frameWidth - 4;
        int chartHeight = frameHeight - 4;
        if (mouseX < chartLeft || mouseX >= chartLeft + chartWidth || mouseY < chartTop || mouseY >= chartTop + chartHeight) {
            return -1;
        }
        if (historyCapacity == 1) {
            return 0;
        }
        float relative = (mouseX - chartLeft) / (float) Math.max(1, chartWidth - 1);
        return Mth.clamp(Math.round(relative * (historyCapacity - 1)), 0, historyCapacity - 1);
    }

    public static int getHistoryIndexForVisibleIndex(int visibleIndex, int historySize, int historyCapacity) {
        int leadingEmptySlots = Math.max(0, historyCapacity - historySize);
        if (visibleIndex < leadingEmptySlots) {
            return -1;
        }
        int historyIndex = visibleIndex - leadingEmptySlots;
        return historyIndex >= 0 && historyIndex < historySize ? historyIndex : -1;
    }

    private enum TextAlignment {
        CENTER,
        LEFT
    }

    public static class PanelButton extends Button {
        private final TextAlignment alignment;
        private float textScale = 1.0F;

        public PanelButton(int x, int y, int width, int height, Component message, OnPress onPress, TextAlignment alignment) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.alignment = alignment;
        }

        public PanelButton setTextScale(float textScale) {
            this.textScale = Math.max(0.25F, textScale);
            return this;
        }

        protected boolean isSelectedStyle() {
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            drawVanillaButton(guiGraphics, this.getX(), this.getY(), this.width, this.height, this.active, this.isHoveredOrFocused() || isSelectedStyle());
            drawText(guiGraphics, getTextColor());
        }

        protected int getTextColor() {
            return this.active ? BUTTON_TEXT : BUTTON_TEXT_DISABLED;
        }

        private void drawText(GuiGraphics guiGraphics, int color) {
            Font font = Minecraft.getInstance().font;
            int scaledTextHeight = Math.max(1, Math.round(8 * textScale));
            int textY = this.getY() + (this.height - scaledTextHeight) / 2;
            if (textScale != 1.0F) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(textScale, textScale, 1.0F);
                if (alignment == TextAlignment.LEFT) {
                    guiGraphics.drawString(font, this.getMessage(), Math.round((this.getX() + HORIZONTAL_TEXT_PADDING) / textScale), Math.round(textY / textScale), color, false);
                } else {
                    guiGraphics.drawCenteredString(font, this.getMessage(), Math.round((this.getX() + this.width / 2.0F) / textScale), Math.round(textY / textScale), color);
                }
                guiGraphics.pose().popPose();
                return;
            }
            if (alignment == TextAlignment.LEFT) {
                guiGraphics.drawString(font, this.getMessage(), this.getX() + HORIZONTAL_TEXT_PADDING, textY, color, false);
                return;
            }
            guiGraphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, textY, color);
        }
    }

    public static class SelectablePanelButton extends PanelButton {
        private boolean selected;

        public SelectablePanelButton(int x, int y, int width, int height, Component message, OnPress onPress, TextAlignment alignment) {
            super(x, y, width, height, message, onPress, alignment);
        }

        @Override
        public SelectablePanelButton setTextScale(float textScale) {
            super.setTextScale(textScale);
            return this;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected boolean isSelectedStyle() {
            return selected;
        }
    }

    public static class PanelTextField extends EditBox {
        public PanelTextField(Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
            this.setBordered(true);
            this.setTextColor(TEXT_FIELD_TEXT);
            this.setTextColorUneditable(TEXT_FIELD_TEXT_UNEDITABLE);
        }
    }

    public static class SpriteSelectableButton extends Button {
        private final ResourceLocation texture;
        private final int u;
        private final int v;
        private final int textureWidth;
        private final int textureHeight;
        private boolean selected;
        private float textScale = 1.0F;

        public SpriteSelectableButton(
                int x,
                int y,
                int width,
                int height,
                Component message,
                OnPress onPress,
                ResourceLocation texture,
                int u,
                int v,
                int textureWidth,
                int textureHeight
        ) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        public SpriteSelectableButton setSelected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public SpriteSelectableButton setTextScale(float textScale) {
            this.textScale = Math.max(0.25F, textScale);
            return this;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            drawVanillaButton(guiGraphics, this.getX(), this.getY(), this.width, this.height, this.active, this.isHoveredOrFocused() || selected);
            drawText(guiGraphics, getTextColor());
        }

        private int getTextColor() {
            return this.active ? BUTTON_TEXT : BUTTON_TEXT_DISABLED;
        }

        private void drawText(GuiGraphics guiGraphics, int color) {
            Font font = Minecraft.getInstance().font;
            int scaledTextHeight = Math.max(1, Math.round(8 * textScale));
            int textY = this.getY() + (this.height - scaledTextHeight) / 2;
            if (textScale != 1.0F) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(textScale, textScale, 1.0F);
                guiGraphics.drawCenteredString(
                        font,
                        this.getMessage(),
                        Math.round((this.getX() + this.width / 2.0F) / textScale),
                        Math.round(textY / textScale),
                        color
                );
                guiGraphics.pose().popPose();
                return;
            }
            guiGraphics.drawCenteredString(font, this.getMessage(), this.getX() + this.width / 2, textY, color);
        }
    }

    private static void drawVanillaButton(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean active, boolean highlighted) {
        ResourceLocation sprite = !active
                ? VANILLA_BUTTON_DISABLED
                : highlighted
                ? VANILLA_BUTTON_HIGHLIGHTED
                : VANILLA_BUTTON;
        guiGraphics.blitSprite(sprite, x, y, width, height);
    }

    private static void drawGraphGrid(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        for (int step = 1; step < 4; step++) {
            int y = top + Math.round(step * (height - 1) / 4.0F);
            guiGraphics.fill(left, y, right, y + 1, GRAPH_GRID);
        }
        for (int step = 1; step < 4; step++) {
            int x = left + Math.round(step * (width - 1) / 4.0F);
            guiGraphics.fill(x, top, x + 1, bottom, GRAPH_GRID);
        }
    }

    private static int lightenColor(int color, float amount) {
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        red = Mth.clamp(Math.round(red + (255 - red) * amount), 0, 255);
        green = Mth.clamp(Math.round(green + (255 - green) * amount), 0, 255);
        blue = Mth.clamp(Math.round(blue + (255 - blue) * amount), 0, 255);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int getSampleX(int sampleIndex, int historyCapacity, int chartLeft, int chartWidth) {
        if (historyCapacity <= 1) {
            return chartLeft + chartWidth - 1;
        }
        return chartLeft + Math.round(sampleIndex * (chartWidth - 1) / (float) (historyCapacity - 1));
    }

    private static int getSampleY(int value, int minValue, int maxValue, int chartTop, int chartHeight) {
        int bottom = chartTop + chartHeight - 1;
        int range = maxValue - minValue;
        if (range <= 0) {
            return bottom;
        }
        float normalized = (value - minValue) / (float) range;
        return bottom - Math.round(normalized * (chartHeight - 1));
    }

    private static void drawAreaSegment(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int baselineY, int color) {
        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        for (int x = startX; x <= endX; x++) {
            float delta = endX == startX ? 0.0F : (x - startX) / (float) (endX - startX);
            int y = Math.round(Mth.lerp(delta, y1, y2));
            int fillTop = Math.min(y, baselineY);
            int fillBottom = Math.max(y, baselineY) + 1;
            guiGraphics.fill(x, fillTop, x + 1, fillBottom, color);
        }
    }

    private static void drawLineSegment(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            float delta = step / (float) steps;
            int x = Math.round(Mth.lerp(delta, x1, x2));
            int y = Math.round(Mth.lerp(delta, y1, y2));
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
