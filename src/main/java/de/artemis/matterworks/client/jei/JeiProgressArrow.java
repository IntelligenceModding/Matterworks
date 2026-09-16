package de.artemis.matterworks.client.jei;

import mezz.jei.api.gui.drawable.IDrawableStatic;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

final class JeiProgressArrow {
    private static final int MILLIS_PER_TICK = 50;

    private final IDrawableStatic emptyArrow;
    private final IDrawableStatic filledArrow;

    JeiProgressArrow(IDrawableStatic emptyArrow, IDrawableStatic filledArrow) {
        this.emptyArrow = emptyArrow;
        this.filledArrow = filledArrow;
    }

    void draw(GuiGraphics guiGraphics, int x, int y, int processTicks) {
        emptyArrow.draw(guiGraphics, x, y);

        int width = filledArrow.getWidth();
        int cycleMillis = Math.max(1, processTicks) * MILLIS_PER_TICK;
        int elapsedMillis = (int) (Util.getMillis() % cycleMillis);
        int filledWidth = Math.max(1, (int) Math.ceil(width * elapsedMillis / (double) cycleMillis));
        filledArrow.draw(guiGraphics, x, y, 0, 0, 0, width - filledWidth);
    }
}
