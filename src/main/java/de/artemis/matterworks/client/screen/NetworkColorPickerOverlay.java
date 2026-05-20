package de.artemis.matterworks.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.BiConsumer;
import java.util.function.IntFunction;

final class NetworkColorPickerOverlay {
    private static final int SWATCH_SIZE = 16;
    private static final int ICON_OFFSET = 0;

    private final int[] relativeXs;
    private final int[] relativeYs;

    NetworkColorPickerOverlay(int[] relativeXs, int[] relativeYs) {
        this.relativeXs = relativeXs;
        this.relativeYs = relativeYs;
    }

    void render(GuiGraphics guiGraphics, int leftPos, int topPos, int screenWidth, int screenHeight, IntFunction<DyeColor> colorGetter) {
        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            guiGraphics.renderItem(getConcreteStack(colorGetter.apply(index)), x + ICON_OFFSET, y + ICON_OFFSET);
        }
    }

    boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int screenWidth,
            int screenHeight,
            BiConsumer<Integer, DyeColor> setter,
            IntFunction<DyeColor> colorGetter
    ) {
        if (button != 0 && button != 1) {
            return false;
        }

        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                GuiWidgets.playButtonClickSound();
                setter.accept(index, Screen.hasShiftDown() ? DyeColor.WHITE : cycleColor(colorGetter.apply(index), button == 1));
                return true;
            }
        }

        return false;
    }

    boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollY,
            int leftPos,
            int topPos,
            int screenWidth,
            int screenHeight,
            BiConsumer<Integer, DyeColor> setter,
            IntFunction<DyeColor> colorGetter
    ) {
        if (scrollY == 0.0D) {
            return false;
        }

        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                GuiWidgets.playButtonClickSound();
                setter.accept(index, cycleColor(colorGetter.apply(index), scrollY < 0.0D));
                return true;
            }
        }

        return false;
    }

    void renderTooltip(
            GuiGraphics guiGraphics,
            Font font,
            int leftPos,
            int topPos,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY,
            IntFunction<DyeColor> colorGetter
    ) {
        for (int index = 0; index < relativeXs.length; index++) {
            int x = leftPos + relativeXs[index];
            int y = topPos + relativeYs[index];
            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                guiGraphics.renderTooltip(font, Component.translatable("color.minecraft." + colorGetter.apply(index).getName()), mouseX, mouseY);
                return;
            }
        }
    }

    private static DyeColor cycleColor(DyeColor current, boolean backwards) {
        DyeColor[] colors = DyeColor.values();
        int nextIndex = backwards
                ? (current.getId() - 1 + colors.length) % colors.length
                : (current.getId() + 1) % colors.length;
        return DyeColor.byId(nextIndex);
    }

    private static ItemStack getConcreteStack(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_CONCRETE.getDefaultInstance();
            case ORANGE -> Items.ORANGE_CONCRETE.getDefaultInstance();
            case MAGENTA -> Items.MAGENTA_CONCRETE.getDefaultInstance();
            case LIGHT_BLUE -> Items.LIGHT_BLUE_CONCRETE.getDefaultInstance();
            case YELLOW -> Items.YELLOW_CONCRETE.getDefaultInstance();
            case LIME -> Items.LIME_CONCRETE.getDefaultInstance();
            case PINK -> Items.PINK_CONCRETE.getDefaultInstance();
            case GRAY -> Items.GRAY_CONCRETE.getDefaultInstance();
            case LIGHT_GRAY -> Items.LIGHT_GRAY_CONCRETE.getDefaultInstance();
            case CYAN -> Items.CYAN_CONCRETE.getDefaultInstance();
            case PURPLE -> Items.PURPLE_CONCRETE.getDefaultInstance();
            case BLUE -> Items.BLUE_CONCRETE.getDefaultInstance();
            case BROWN -> Items.BROWN_CONCRETE.getDefaultInstance();
            case GREEN -> Items.GREEN_CONCRETE.getDefaultInstance();
            case RED -> Items.RED_CONCRETE.getDefaultInstance();
            case BLACK -> Items.BLACK_CONCRETE.getDefaultInstance();
        };
    }
}
