package de.artemis.matterworks.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;

final class NetworkColorPickerOverlay {
    private static final int SWATCH_SIZE = 16;
    private static final int PALETTE_COLUMNS = 4;
    private static final int PALETTE_ROWS = 4;
    private static final int PALETTE_CELL_SIZE = 14;
    private static final int PALETTE_PADDING = 4;
    private static final int PALETTE_GAP = 4;

    private final int[] relativeXs;
    private final int[] relativeYs;
    private int openIndex = -1;

    NetworkColorPickerOverlay(int[] relativeXs, int[] relativeYs) {
        this.relativeXs = relativeXs;
        this.relativeYs = relativeYs;
    }

    void render(GuiGraphics guiGraphics, int leftPos, int topPos, int screenWidth, int screenHeight, IntFunction<DyeColor> colorGetter) {
        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            DyeColor color = colorGetter.apply(index);
            VanillaGuiHelper.drawInsetPanel(guiGraphics, x, y, SWATCH_SIZE, SWATCH_SIZE);
            if (index == openIndex) {
                guiGraphics.fill(x + 1, y + 1, x + SWATCH_SIZE - 1, y + 2, 0xFFFFFFFF);
            }
            guiGraphics.fill(x + 3, y + 3, x + SWATCH_SIZE - 3, y + SWATCH_SIZE - 3, colorValue(color));
        }

        if (openIndex >= 0) {
            int paletteLeft = getPaletteLeft(leftPos, screenWidth);
            int paletteTop = getPaletteTop(topPos, screenHeight);
            int paletteWidth = PALETTE_COLUMNS * PALETTE_CELL_SIZE + PALETTE_PADDING * 2;
            int paletteHeight = PALETTE_ROWS * PALETTE_CELL_SIZE + PALETTE_PADDING * 2;
            VanillaGuiHelper.drawInsetPanel(guiGraphics, paletteLeft, paletteTop, paletteWidth, paletteHeight);

            DyeColor current = colorGetter.apply(openIndex);
            DyeColor[] colors = DyeColor.values();
            for (int colorIndex = 0; colorIndex < colors.length; colorIndex++) {
                int column = colorIndex % PALETTE_COLUMNS;
                int row = colorIndex / PALETTE_COLUMNS;
                int cellX = paletteLeft + PALETTE_PADDING + column * PALETTE_CELL_SIZE;
                int cellY = paletteTop + PALETTE_PADDING + row * PALETTE_CELL_SIZE;
                VanillaGuiHelper.drawInsetPanel(guiGraphics, cellX, cellY, PALETTE_CELL_SIZE - 1, PALETTE_CELL_SIZE - 1);
                if (colors[colorIndex] == current) {
                    guiGraphics.fill(cellX + 1, cellY + 1, cellX + PALETTE_CELL_SIZE - 2, cellY + 2, 0xFFFFFFFF);
                }
                guiGraphics.fill(cellX + 2, cellY + 2, cellX + PALETTE_CELL_SIZE - 3, cellY + PALETTE_CELL_SIZE - 3, colorValue(colors[colorIndex]));
            }
        }
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, int leftPos, int topPos, int screenWidth, int screenHeight,
                         BiConsumer<Integer, DyeColor> setter) {
        if (button != 0) {
            return false;
        }

        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                openIndex = openIndex == index ? -1 : index;
                return true;
            }
        }

        if (openIndex >= 0) {
            int paletteLeft = getPaletteLeft(leftPos, screenWidth);
            int paletteTop = getPaletteTop(topPos, screenHeight);
            DyeColor[] colors = DyeColor.values();
            for (int colorIndex = 0; colorIndex < colors.length; colorIndex++) {
                int column = colorIndex % PALETTE_COLUMNS;
                int row = colorIndex / PALETTE_COLUMNS;
                int cellX = paletteLeft + PALETTE_PADDING + column * PALETTE_CELL_SIZE;
                int cellY = paletteTop + PALETTE_PADDING + row * PALETTE_CELL_SIZE;
                if (mouseX >= cellX && mouseX < cellX + PALETTE_CELL_SIZE - 1 && mouseY >= cellY && mouseY < cellY + PALETTE_CELL_SIZE - 1) {
                    setter.accept(openIndex, colors[colorIndex]);
                    openIndex = -1;
                    return true;
                }
            }
            openIndex = -1;
        }

        return false;
    }

    void renderTooltip(GuiGraphics guiGraphics, Font font, int leftPos, int topPos, int screenWidth, int screenHeight,
                       int mouseX, int mouseY, IntFunction<DyeColor> colorGetter) {
        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                guiGraphics.renderTooltip(font, Component.translatable("color.minecraft." + colorGetter.apply(index).getName()), mouseX, mouseY);
                return;
            }
        }

        if (openIndex >= 0) {
            int paletteLeft = getPaletteLeft(leftPos, screenWidth);
            int paletteTop = getPaletteTop(topPos, screenHeight);
            DyeColor[] colors = DyeColor.values();
            for (int colorIndex = 0; colorIndex < colors.length; colorIndex++) {
                int column = colorIndex % PALETTE_COLUMNS;
                int row = colorIndex / PALETTE_COLUMNS;
                int cellX = paletteLeft + PALETTE_PADDING + column * PALETTE_CELL_SIZE;
                int cellY = paletteTop + PALETTE_PADDING + row * PALETTE_CELL_SIZE;
                if (mouseX >= cellX && mouseX < cellX + PALETTE_CELL_SIZE - 1 && mouseY >= cellY && mouseY < cellY + PALETTE_CELL_SIZE - 1) {
                    guiGraphics.renderTooltip(font, Component.translatable("color.minecraft." + colors[colorIndex].getName()), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    private int getPaletteLeft(int leftPos, int screenWidth) {
        int paletteWidth = PALETTE_COLUMNS * PALETTE_CELL_SIZE + PALETTE_PADDING * 2;
        int swatchLeft = leftPos + relativeXs[Math.max(0, openIndex)];
        return Mth.clamp(swatchLeft, leftPos + 4, leftPos + screenWidth - paletteWidth - 4);
    }

    private int getPaletteTop(int topPos, int screenHeight) {
        int paletteHeight = PALETTE_ROWS * PALETTE_CELL_SIZE + PALETTE_PADDING * 2;
        int below = topPos + relativeYs[Math.max(0, openIndex)] + SWATCH_SIZE + PALETTE_GAP;
        int maxTop = topPos + screenHeight - paletteHeight - 4;
        if (below <= maxTop) {
            return below;
        }
        return Math.max(topPos + 4, topPos + relativeYs[Math.max(0, openIndex)] - paletteHeight - PALETTE_GAP);
    }

    private static int colorValue(DyeColor color) {
        return switch (color) {
            case WHITE -> 0xFFF9FFFE;
            case ORANGE -> 0xFFF9801D;
            case MAGENTA -> 0xFFC74EBD;
            case LIGHT_BLUE -> 0xFF3AB3DA;
            case YELLOW -> 0xFFFED83D;
            case LIME -> 0xFF80C71F;
            case PINK -> 0xFFF38BAA;
            case GRAY -> 0xFF474F52;
            case LIGHT_GRAY -> 0xFF9D9D97;
            case CYAN -> 0xFF169C9C;
            case PURPLE -> 0xFF8932B8;
            case BLUE -> 0xFF3C44AA;
            case BROWN -> 0xFF835432;
            case GREEN -> 0xFF5E7C16;
            case RED -> 0xFFB02E26;
            case BLACK -> 0xFF1D1D21;
        };
    }
}
