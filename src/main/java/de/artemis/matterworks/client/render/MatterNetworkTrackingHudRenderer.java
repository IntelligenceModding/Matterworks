package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.player.LocalPlayer;

public final class MatterNetworkTrackingHudRenderer {
    private static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final float HALF_VISIBLE_ANGLE = Mth.DEG_TO_RAD * 60.0F;
    private static final double HORIZONTAL_EPSILON = 1.0E-4D;
    private static final int NEAR_DISTANCE = 128;
    private static final int FAR_DISTANCE = 332;
    private static final int[] DOT_RADII = {3, 2, 1, 1};
    private static final int DOT_COLOR = 0xFF4CC9F0;
    private static final int DOT_OUTLINE_COLOR = 0xFF0E131A;
    private static final int ARROW_COLOR = 0xFFBDEBFF;
    private static final float VERTICAL_ARROW_THRESHOLD = 25.0F;

    private MatterNetworkTrackingHudRenderer() {
    }

    public static final LayeredDraw.Layer LAYER = (guiGraphics, deltaTracker) -> render(guiGraphics);

    public static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !MatterNetworkTrackingState.isTracking()) {
            return;
        }

        double dx = MatterNetworkTrackingState.getTargetPos().getX() + 0.5D - player.getX();
        double dy = MatterNetworkTrackingState.getTargetPos().getY() + 0.5D - player.getEyeY();
        double dz = MatterNetworkTrackingState.getTargetPos().getZ() + 0.5D - player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        int left = guiGraphics.guiWidth() / 2 - 91;
        int top = guiGraphics.guiHeight() - 32 + 3;

        RenderSystem.enableBlend();
        guiGraphics.blitSprite(EXPERIENCE_BAR_BACKGROUND_SPRITE, left, top, BAR_WIDTH, BAR_HEIGHT);
        renderTrackedBlockIndicator(guiGraphics, player, left, top, dx, dy, dz, horizontalDistance);
        RenderSystem.disableBlend();
    }

    private static void renderTrackedBlockIndicator(
            GuiGraphics guiGraphics,
            LocalPlayer player,
            int left,
            int top,
            double dx,
            double dy,
            double dz,
            double horizontalDistance
    ) {
        double relativeYaw = computeRelativeYaw(player, dx, dz, horizontalDistance);
        int distance = Mth.floor(Math.sqrt(dx * dx + dy * dy + dz * dz));
        boolean offscreen = Double.isNaN(relativeYaw) || Math.abs(relativeYaw) > HALF_VISIBLE_ANGLE;
        int dotRadius = offscreen ? 1 : DOT_RADII[getDistanceBucket(distance)];
        float clampedYaw = Double.isNaN(relativeYaw)
                ? 0.0F
                : (float) Mth.clamp(relativeYaw, -HALF_VISIBLE_ANGLE, HALF_VISIBLE_ANGLE);
        float ratio = (clampedYaw + HALF_VISIBLE_ANGLE) / (HALF_VISIBLE_ANGLE * 2.0F);
        int availableWidth = BAR_WIDTH - 2 - dotRadius * 2;
        int centerX = left + 1 + dotRadius + Mth.floor(Mth.clamp(ratio, 0.0F, 1.0F) * availableWidth);
        int centerY = top + 2;

        drawDot(guiGraphics, centerX, centerY, dotRadius, DOT_COLOR);

        float verticalAngle = (float) Math.toDegrees(Math.atan2(dy, Math.max(horizontalDistance, HORIZONTAL_EPSILON)));
        if (verticalAngle > VERTICAL_ARROW_THRESHOLD) {
            drawArrowUp(guiGraphics, centerX, top - 3);
        } else if (verticalAngle < -VERTICAL_ARROW_THRESHOLD) {
            drawArrowDown(guiGraphics, centerX, top + BAR_HEIGHT + 3);
        }
    }

    private static double computeRelativeYaw(LocalPlayer player, double dx, double dz, double horizontalDistance) {
        if (horizontalDistance < HORIZONTAL_EPSILON) {
            return 0.0D;
        }

        double lookX = player.getLookAngle().x;
        double lookZ = player.getLookAngle().z;
        double targetX = dx / horizontalDistance;
        double targetZ = dz / horizontalDistance;
        double cross = lookX * targetZ - lookZ * targetX;
        double dot = lookX * targetX + lookZ * targetZ;
        return Math.atan2(cross, dot);
    }

    private static int getDistanceBucket(int distance) {
        if (distance <= NEAR_DISTANCE) {
            return 0;
        }
        if (distance >= FAR_DISTANCE) {
            return 3;
        }

        float normalized = (distance - NEAR_DISTANCE) / (float) (FAR_DISTANCE - NEAR_DISTANCE);
        return 1 + Mth.clamp((int) Math.floor(normalized * 2.0F), 0, 2);
    }

    private static void drawDot(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        guiGraphics.fill(centerX - radius - 1, centerY - radius - 1, centerX + radius + 2, centerY + radius + 2, DOT_OUTLINE_COLOR);
        guiGraphics.fill(centerX - radius, centerY - radius, centerX + radius + 1, centerY + radius + 1, color);
    }

    private static void drawArrowUp(GuiGraphics guiGraphics, int centerX, int baseY) {
        guiGraphics.fill(centerX, baseY - 3, centerX + 1, baseY + 1, ARROW_COLOR);
        guiGraphics.fill(centerX - 2, baseY - 1, centerX + 3, baseY, ARROW_COLOR);
    }

    private static void drawArrowDown(GuiGraphics guiGraphics, int centerX, int baseY) {
        guiGraphics.fill(centerX, baseY - 1, centerX + 1, baseY + 3, ARROW_COLOR);
        guiGraphics.fill(centerX - 2, baseY, centerX + 3, baseY + 1, ARROW_COLOR);
    }
}
