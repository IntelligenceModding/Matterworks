package de.artemis.matterworks.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

public final class GuiWidgets {
    private static final ResourceLocation VANILLA_BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation VANILLA_BUTTON_DISABLED = ResourceLocation.withDefaultNamespace("widget/button_disabled");
    private static final ResourceLocation VANILLA_BUTTON_HIGHLIGHTED = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation ENERGY_FILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static final ResourceLocation PROGRESS_FILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final int BUTTON_TEXT = 0xFFE0E0E0;
    private static final int BUTTON_TEXT_DISABLED = 0xFFA0A0A0;
    private static final int TEXT_FIELD_TEXT = 0xFFE0E0E0;
    private static final int TEXT_FIELD_TEXT_UNEDITABLE = 0xFFA0A0A0;
    private static final int HORIZONTAL_TEXT_PADDING = 6;
    private static final int CONTENT_BACKGROUND = 0xFF1F1F1F;
    private static final int GRAPH_GRID = 0x22373737;
    private static final int GRAPH_HOVER_LINE = 0x66FFFFFF;
    private static final int GRAPH_HOVER_POINT = 0xFFFFFFFF;
    private static final int ENERGY_FILL_TINT = 0xFFE23D2D;
    private static final int ENERGY_FILL_HIGHLIGHT = 0x88FF8A80;
    private static final int PROGRESS_FILL_TINT = 0xFF8A2BE2;
    private static final int PROGRESS_FILL_HIGHLIGHT = 0x88C68BFF;
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

    public static ClippedSelectablePanelButton clippedLeftAlignedSelectablePanelButton(
            int x,
            int y,
            int width,
            int height,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom,
            Component message,
            Button.OnPress onPress
    ) {
        return new ClippedSelectablePanelButton(x, y, width, height, clipLeft, clipTop, clipRight, clipBottom, message, onPress, TextAlignment.LEFT);
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
        drawInsetGraph(
                guiGraphics,
                frameLeft,
                frameTop,
                frameWidth,
                frameHeight,
                historySize,
                historyCapacity,
                currentValue,
                hoveredVisibleIndex,
                new GraphSeries(visibleSampleValueGetter, lineColor, areaColor)
        );
    }

    public static void drawInsetGraph(
            GuiGraphics guiGraphics,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int historySize,
            int historyCapacity,
            int currentValue,
            int hoveredVisibleIndex,
            GraphSeries... series
    ) {
        int chartLeft = frameLeft + 2;
        int chartTop = frameTop + 2;
        int chartWidth = frameWidth - 4;
        int chartHeight = frameHeight - 4;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, frameLeft, frameTop, frameWidth, frameHeight);
        guiGraphics.fill(chartLeft, chartTop, chartLeft + chartWidth, chartTop + chartHeight, CONTENT_BACKGROUND);
        drawGraphGrid(guiGraphics, chartLeft, chartTop, chartWidth, chartHeight);

        if (historyCapacity <= 0 || series.length == 0) {
            return;
        }

        int minValue = Math.min(0, currentValue);
        int maxValue = Math.max(0, currentValue);
        for (int visibleIndex = 0; visibleIndex < historyCapacity; visibleIndex++) {
            for (GraphSeries graphSeries : series) {
                int sample = graphSeries.visibleSampleValueGetter().applyAsInt(visibleIndex);
                minValue = Math.min(minValue, sample);
                maxValue = Math.max(maxValue, sample);
            }
        }
        if (minValue == 0 && maxValue == 0) {
            maxValue = 1;
        }
        int zeroY = getSampleY(0, minValue, maxValue, chartTop, chartHeight);
        if (minValue < 0 && maxValue > 0) {
            guiGraphics.fill(chartLeft, zeroY, chartLeft + chartWidth, zeroY + 1, GRAPH_GRID | 0x22000000);
        }

        for (int visibleIndex = 0; visibleIndex < historyCapacity - 1; visibleIndex++) {
            int x1 = getSampleX(visibleIndex, historyCapacity, chartLeft, chartWidth);
            int x2 = getSampleX(visibleIndex + 1, historyCapacity, chartLeft, chartWidth);
            for (GraphSeries graphSeries : series) {
                int current = graphSeries.visibleSampleValueGetter().applyAsInt(visibleIndex);
                int next = graphSeries.visibleSampleValueGetter().applyAsInt(visibleIndex + 1);
                int y1 = getSampleY(current, minValue, maxValue, chartTop, chartHeight);
                int y2 = getSampleY(next, minValue, maxValue, chartTop, chartHeight);
                drawAreaSegment(guiGraphics, x1, y1, x2, y2, zeroY, graphSeries.areaColor());
                drawLineSegment(guiGraphics, x1, y1, x2, y2, graphSeries.lineColor());
            }
        }

        if (historySize > 0) {
            int latestVisibleIndex = historyCapacity - 1;
            int x = getSampleX(latestVisibleIndex, historyCapacity, chartLeft, chartWidth);
            for (GraphSeries graphSeries : series) {
                int y = getSampleY(graphSeries.visibleSampleValueGetter().applyAsInt(latestVisibleIndex), minValue, maxValue, chartTop, chartHeight);
                guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, graphSeries.lineColor());
            }
        }

        if (hoveredVisibleIndex >= 0 && hoveredVisibleIndex < historyCapacity) {
            int x = getSampleX(hoveredVisibleIndex, historyCapacity, chartLeft, chartWidth);
            guiGraphics.fill(x, chartTop, x + 1, chartTop + chartHeight, GRAPH_HOVER_LINE);
            for (GraphSeries graphSeries : series) {
                int sample = graphSeries.visibleSampleValueGetter().applyAsInt(hoveredVisibleIndex);
                int y = getSampleY(sample, minValue, maxValue, chartTop, chartHeight);
                guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, GRAPH_HOVER_POINT);
                guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, graphSeries.lineColor());
            }
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

    public static void drawInsetVerticalAnimatedFillBar(
            GuiGraphics guiGraphics,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int filledHeight,
            ResourceLocation spriteLocation,
            int fillTint,
            int highlightColor
    ) {
        int barLeft = frameLeft + 2;
        int barTop = frameTop + 2;
        int barWidth = frameWidth - 4;
        int barHeight = frameHeight - 4;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, frameLeft, frameTop, frameWidth, frameHeight);
        guiGraphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, CONTENT_BACKGROUND);
        renderVerticalTiledFill(guiGraphics, spriteLocation, fillTint, highlightColor, barLeft, barTop, barWidth, barHeight, filledHeight, 0.92F);
    }

    public static void drawInsetVerticalEnergyBar(
            GuiGraphics guiGraphics,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int filledHeight
    ) {
        int barLeft = frameLeft + 2;
        int barTop = frameTop + 2;
        int barWidth = frameWidth - 4;
        int barHeight = frameHeight - 4;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, frameLeft, frameTop, frameWidth, frameHeight);
        guiGraphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, CONTENT_BACKGROUND);
        renderVerticalTiledFill(guiGraphics, ENERGY_FILL_TEXTURE, ENERGY_FILL_TINT, ENERGY_FILL_HIGHLIGHT, barLeft, barTop, barWidth, barHeight, filledHeight, 0.92F);
    }

    public static void drawInsetVerticalFluidBar(
            GuiGraphics guiGraphics,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int filledHeight,
            FluidStack fluidStack
    ) {
        int barLeft = frameLeft + 2;
        int barTop = frameTop + 2;
        int barWidth = frameWidth - 4;
        int barHeight = frameHeight - 4;

        VanillaGuiHelper.drawInsetPanel(guiGraphics, frameLeft, frameTop, frameWidth, frameHeight);
        guiGraphics.fill(barLeft, barTop, barLeft + barWidth, barTop + barHeight, CONTENT_BACKGROUND);
        renderVerticalFluidFill(guiGraphics, barLeft, barTop, barWidth, barHeight, filledHeight, fluidStack, 0.95F);
    }

    public static void fillHorizontalEnergyGauge(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int filledWidth,
            int height
    ) {
        if (filledWidth <= 0 || height <= 0) {
            return;
        }
        renderHorizontalEnergyFill(guiGraphics, x, y, filledWidth, height, 0.90F);
    }

    public static void fillHorizontalProgressGauge(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int filledWidth,
            int height
    ) {
        if (filledWidth <= 0 || height <= 0) {
            return;
        }
        renderHorizontalProgressFill(guiGraphics, x, y, filledWidth, height, 0.90F);
    }

    public static void fillVerticalEnergyGauge(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int filledHeight
    ) {
        renderVerticalTiledFill(guiGraphics, ENERGY_FILL_TEXTURE, ENERGY_FILL_TINT, ENERGY_FILL_HIGHLIGHT, x, y, width, height, filledHeight, 0.90F);
    }

    public static void fillVerticalFluidGauge(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int filledHeight,
            FluidStack fluidStack
    ) {
        renderVerticalFluidFill(guiGraphics, x, y, width, height, filledHeight, fluidStack, 0.90F);
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

    private static void renderVerticalFluidFill(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int filledHeight,
            FluidStack fluidStack,
            float alpha
    ) {
        if (fluidStack == null || fluidStack.isEmpty() || filledHeight <= 0 || width <= 0 || height <= 0) {
            return;
        }

        int clampedHeight = Math.min(filledHeight, height);
        int fillTop = y + height - clampedHeight;
        renderFluidTiledRect(guiGraphics, fluidStack, x, fillTop, width, clampedHeight, alpha);
    }

    private static void renderVerticalTiledFill(
            GuiGraphics guiGraphics,
            ResourceLocation spriteLocation,
            int fillTint,
            int highlightColor,
            int x,
            int y,
            int width,
            int height,
            int filledHeight,
            float alpha
    ) {
        if (filledHeight <= 0 || width <= 0 || height <= 0) {
            return;
        }

        int clampedHeight = Math.min(filledHeight, height);
        int fillTop = y + height - clampedHeight;
        renderAtlasTiledRect(guiGraphics, spriteLocation, fillTint, x, fillTop, width, clampedHeight, alpha);
        guiGraphics.fill(x, fillTop, x + width, Math.min(y + height, fillTop + 1), highlightColor);
    }

    private static void renderHorizontalEnergyFill(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        renderAtlasTiledRect(guiGraphics, ENERGY_FILL_TEXTURE, ENERGY_FILL_TINT, x, y, width, height, alpha);
        guiGraphics.fill(x, y, x + width, Math.min(y + height, y + 1), ENERGY_FILL_HIGHLIGHT);
        guiGraphics.fill(Math.max(x, x + width - 1), y, x + width, y + height, ENERGY_FILL_HIGHLIGHT);
    }

    private static void renderHorizontalProgressFill(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        renderAtlasTiledRect(guiGraphics, PROGRESS_FILL_TEXTURE, PROGRESS_FILL_TINT, x, y, width, height, alpha);
        guiGraphics.fill(x, y, x + width, Math.min(y + height, y + 1), PROGRESS_FILL_HIGHLIGHT);
        guiGraphics.fill(Math.max(x, x + width - 1), y, x + width, y + height, PROGRESS_FILL_HIGHLIGHT);
    }

    private static void renderFluidTiledRect(
            GuiGraphics guiGraphics,
            FluidStack fluidStack,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {
        if (fluidStack.isEmpty() || width <= 0 || height <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(fluidStack));
        int tint = extensions.getTintColor(fluidStack);
        renderTiledSprite(guiGraphics, sprite, tint, x, y, width, height, alpha);
    }

    private static void renderAtlasTiledRect(
            GuiGraphics guiGraphics,
            ResourceLocation spriteLocation,
            int tint,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(spriteLocation);
        renderTiledSprite(guiGraphics, sprite, tint, x, y, width, height, alpha);
    }

    private static void renderTiledSprite(
            GuiGraphics guiGraphics,
            TextureAtlasSprite sprite,
            int tint,
            int x,
            int y,
            int width,
            int height,
            float alpha
    ) {
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        guiGraphics.enableScissor(x, y, x + width, y + height);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        for (int drawX = x; drawX < x + width; drawX += 16) {
            for (int drawY = y + height - 16; drawY > y - 16; drawY -= 16) {
                guiGraphics.blit(drawX, drawY, 0, 16, 16, sprite);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.disableScissor();
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

    public static int getHoveredHistoryIndex(
            int mouseX,
            int mouseY,
            int frameLeft,
            int frameTop,
            int frameWidth,
            int frameHeight,
            int historySize,
            int historyCapacity
    ) {
        int visibleIndex = getHoveredHistorySampleIndex(mouseX, mouseY, frameLeft, frameTop, frameWidth, frameHeight, historyCapacity);
        return visibleIndex < 0 ? -1 : getHistoryIndexForVisibleIndex(visibleIndex, historySize, historyCapacity);
    }

    public static int getHistoryIndexForVisibleIndex(int visibleIndex, int historySize, int historyCapacity) {
        int leadingEmptySlots = Math.max(0, historyCapacity - historySize);
        if (visibleIndex < leadingEmptySlots) {
            return -1;
        }
        int historyIndex = visibleIndex - leadingEmptySlots;
        return historyIndex >= 0 && historyIndex < historySize ? historyIndex : -1;
    }

    public record GraphSeries(IntUnaryOperator visibleSampleValueGetter, int lineColor, int areaColor) {
    }

    private enum TextAlignment {
        CENTER,
        LEFT
    }

    public static class PanelButton extends Button {
        private final TextAlignment alignment;
        private float textScale = 1.0F;
        private int textOffsetY;

        public PanelButton(int x, int y, int width, int height, Component message, OnPress onPress, TextAlignment alignment) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.alignment = alignment;
        }

        public PanelButton setTextScale(float textScale) {
            this.textScale = Math.max(0.25F, textScale);
            return this;
        }

        public PanelButton setTextOffsetY(int textOffsetY) {
            this.textOffsetY = textOffsetY;
            return this;
        }

        protected boolean isSelectedStyle() {
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            drawVanillaButton(guiGraphics, this.getX(), this.getY(), this.width, this.height, this.active, this.isHovered() || isSelectedStyle());
            drawText(guiGraphics, getTextColor());
        }

        protected int getTextColor() {
            return this.active ? BUTTON_TEXT : BUTTON_TEXT_DISABLED;
        }

        private void drawText(GuiGraphics guiGraphics, int color) {
            Font font = Minecraft.getInstance().font;
            int scaledTextHeight = Math.max(1, Math.round(8 * textScale));
            int textY = this.getY() + (this.height - scaledTextHeight) / 2 + textOffsetY;
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

        @Override
        public SelectablePanelButton setTextOffsetY(int textOffsetY) {
            super.setTextOffsetY(textOffsetY);
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

    public static class ClippedSelectablePanelButton extends SelectablePanelButton {
        private final int clipLeft;
        private final int clipTop;
        private final int clipRight;
        private final int clipBottom;

        public ClippedSelectablePanelButton(
                int x,
                int y,
                int width,
                int height,
                int clipLeft,
                int clipTop,
                int clipRight,
                int clipBottom,
                Component message,
                OnPress onPress,
                TextAlignment alignment
        ) {
            super(x, y, width, height, message, onPress, alignment);
            this.clipLeft = clipLeft;
            this.clipTop = clipTop;
            this.clipRight = clipRight;
            this.clipBottom = clipBottom;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.disableScissor();
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
            drawVanillaButton(guiGraphics, this.getX(), this.getY(), this.width, this.height, this.active, this.isHovered() || selected);
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
