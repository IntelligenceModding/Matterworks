package de.artemis.matterworks.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

public final class MatterBatteryPreviewHudRenderer {
    private MatterBatteryPreviewHudRenderer() {
    }

    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> render(guiGraphics);

    public static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !MatterBatteryPreviewState.isActive()) {
            return;
        }

        int left = 8;
        int top = 8;
        guiGraphics.drawString(minecraft.font, "Battery Preview", left, top, 0xFFFFFF, false);
        guiGraphics.drawString(minecraft.font, "Layer " + (MatterBatteryPreviewState.getSelectedLayer() + 1) + "/" + MatterBatteryPreviewState.getLayerCount(), left, top + 10, 0xC8F3FF, false);
        guiGraphics.drawString(minecraft.font, "Frame  Casing  Cell", left, top + 22, 0xB0B0B0, false);
        guiGraphics.drawString(minecraft.font, "RMB Place From Inventory", left, top + 32, 0x9EE7B8, false);

        MatterBatteryPreviewRenderer.PreviewHit previewHit = MatterBatteryPreviewRenderer.findTargetedPreviewHit(minecraft);
        if (previewHit != null) {
            String required = MatterBatteryPreviewRenderer.getRequirementLabel(previewHit.localPos());
            String placed = MatterBatteryPreviewRenderer.getPlacementLabel(previewHit.actualState());
            guiGraphics.drawString(minecraft.font, "Required: " + required, left, top + 46, 0xFFFFFF, false);
            guiGraphics.drawString(minecraft.font, "Placed: " + placed, left, top + 56, 0xD9D9D9, false);
        }
    }
}
