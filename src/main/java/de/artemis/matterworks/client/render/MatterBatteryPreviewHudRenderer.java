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
        guiGraphics.drawString(minecraft.font, MatterBatteryPreviewState.isToolDriven() ? "Matter Architect" : "Battery Preview", left, top, 0xFFFFFF, false);
        guiGraphics.drawString(minecraft.font, "Size " + MatterBatteryPreviewState.getWidth() + "x" + MatterBatteryPreviewState.getHeight() + "x" + MatterBatteryPreviewState.getDepth(), left, top + 10, MatterBatteryPreviewState.isValid() ? 0xC8F3FF : 0xFF8B8B, false);
        guiGraphics.drawString(minecraft.font, "Layer " + (MatterBatteryPreviewState.getSelectedLayer() + 1) + "/" + MatterBatteryPreviewState.getLayerCount(), left, top + 20, 0xC8F3FF, false);
        if (MatterBatteryPreviewState.isLocked()) {
            guiGraphics.drawString(minecraft.font, "Locked Preview", left, top + 32, 0x9EE7B8, false);
            guiGraphics.drawString(minecraft.font, "Shift+Wheel or PgUp/PgDn Layer", left, top + 42, 0xB0B0B0, false);
            guiGraphics.drawString(minecraft.font, "RMB Place From Inventory", left, top + 52, 0x9EE7B8, false);
        } else {
            guiGraphics.drawString(minecraft.font, "RMB Lock Preview", left, top + 32, MatterBatteryPreviewState.isValid() ? 0x9EE7B8 : 0xFF8B8B, false);
            guiGraphics.drawString(minecraft.font, "Shift+Wheel Move Look Direction", left, top + 42, 0xB0B0B0, false);
            guiGraphics.drawString(minecraft.font, "Ctrl+Wheel Resize Look Direction", left, top + 52, 0xB0B0B0, false);
        }

        MatterBatteryPreviewRenderer.PreviewHit previewHit = MatterBatteryPreviewRenderer.findTargetedPreviewHit(minecraft);
        if (previewHit != null) {
            String required = MatterBatteryPreviewRenderer.getRequirementLabel(previewHit.localPos());
            String placed = MatterBatteryPreviewRenderer.getPlacementLabel(previewHit.actualState());
            guiGraphics.drawString(minecraft.font, "Required: " + required, left, top + 66, 0xFFFFFF, false);
            guiGraphics.drawString(minecraft.font, "Placed: " + placed, left, top + 76, 0xD9D9D9, false);
        }
    }
}
