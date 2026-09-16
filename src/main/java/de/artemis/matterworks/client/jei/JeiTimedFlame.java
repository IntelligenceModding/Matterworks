package de.artemis.matterworks.client.jei;

import mezz.jei.api.gui.drawable.IDrawableStatic;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

final class JeiTimedFlame {
    private static final int MILLIS_PER_TICK = 50;

    private final IDrawableStatic emptyFlame;
    private final IDrawableStatic filledFlame;

    JeiTimedFlame(IDrawableStatic emptyFlame, IDrawableStatic filledFlame) {
        this.emptyFlame = emptyFlame;
        this.filledFlame = filledFlame;
    }

    void draw(GuiGraphics guiGraphics, int x, int y, int burnTicks) {
        emptyFlame.draw(guiGraphics, x, y);

        int height = filledFlame.getHeight();
        int cycleMillis = Math.max(1, burnTicks) * MILLIS_PER_TICK;
        int elapsedMillis = (int) (Util.getMillis() % cycleMillis);
        int filledHeight = Math.max(1, (int) Math.ceil(height * elapsedMillis / (double) cycleMillis));
        filledFlame.draw(guiGraphics, x, y, height - filledHeight, 0, 0, 0);
    }
}
