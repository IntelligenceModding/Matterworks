package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class MatterNetworkTrackingHudRenderer {
    private static final ResourceLocation LOCATOR_BAR_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_background.png");
    private static final ResourceLocation LOCATOR_BAR_ARROW_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_arrow_up.png");
    private static final ResourceLocation LOCATOR_BAR_ARROW_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_arrow_down.png");
    private static final ResourceLocation[] DOT_TEXTURES = {
            ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_dot/default_0.png"),
            ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_dot/default_1.png"),
            ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_dot/default_2.png"),
            ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_dot/default_3.png")
    };
    private static final ResourceLocation LOCATOR_BAR_BOWTIE_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/hud/locator_bar_dot/bowtie.png");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BACKGROUND_CAP_WIDTH = 2;
    private static final int BACKGROUND_TEXTURE_WIDTH = 12;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 5;
    private static final int DOT_TEXTURE_SIZE = 9;
    private static final int ARROW_TEXTURE_WIDTH = 7;
    private static final int ARROW_TEXTURE_HEIGHT = 5;
    private static final int ARROW_SPRITE_TEXTURE_HEIGHT = 10;
    private static final float HALF_VISIBLE_ANGLE = Mth.DEG_TO_RAD * 60.0F;
    private static final double HORIZONTAL_EPSILON = 1.0E-4D;
    private static final int NEAR_DISTANCE = 128;
    private static final int FAR_DISTANCE = 332;
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
        drawBackground(guiGraphics, left, top);
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
        if (Double.isNaN(relativeYaw)) {
            return;
        }

        int distance = Mth.floor(Math.sqrt(dx * dx + dy * dy + dz * dz));
        int distanceBucket = getDistanceBucket(distance);
        boolean outsideVisibleRange = Math.abs(relativeYaw) > HALF_VISIBLE_ANGLE;
        float clampedYaw = (float) Mth.clamp(relativeYaw, -HALF_VISIBLE_ANGLE, HALF_VISIBLE_ANGLE);
        float ratio = (clampedYaw + HALF_VISIBLE_ANGLE) / (HALF_VISIBLE_ANGLE * 2.0F);
        int availableWidth = BAR_WIDTH - DOT_TEXTURE_SIZE;
        int centerX = left + DOT_TEXTURE_SIZE / 2 + Mth.floor(Mth.clamp(ratio, 0.0F, 1.0F) * availableWidth);
        int centerY = top + 2;

        ResourceLocation dotTexture = outsideVisibleRange
                ? DOT_TEXTURES[DOT_TEXTURES.length - 1]
                : horizontalDistance < HORIZONTAL_EPSILON ? LOCATOR_BAR_BOWTIE_TEXTURE : DOT_TEXTURES[distanceBucket];
        drawDot(guiGraphics, centerX, centerY, dotTexture);

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

    private static void drawBackground(GuiGraphics guiGraphics, int left, int top) {
        int middleWidth = BAR_WIDTH - BACKGROUND_CAP_WIDTH * 2;
        int middleTextureWidth = BACKGROUND_TEXTURE_WIDTH - BACKGROUND_CAP_WIDTH * 2;

        guiGraphics.blit(
                LOCATOR_BAR_BACKGROUND_TEXTURE,
                left,
                top,
                0,
                0,
                BACKGROUND_CAP_WIDTH,
                BAR_HEIGHT,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );
        guiGraphics.blit(
                LOCATOR_BAR_BACKGROUND_TEXTURE,
                left + BACKGROUND_CAP_WIDTH,
                top,
                BACKGROUND_CAP_WIDTH,
                0,
                middleWidth,
                BAR_HEIGHT,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );
        guiGraphics.blit(
                LOCATOR_BAR_BACKGROUND_TEXTURE,
                left + BAR_WIDTH - BACKGROUND_CAP_WIDTH,
                top,
                BACKGROUND_TEXTURE_WIDTH - BACKGROUND_CAP_WIDTH,
                0,
                BACKGROUND_CAP_WIDTH,
                BAR_HEIGHT,
                BACKGROUND_TEXTURE_WIDTH,
                BACKGROUND_TEXTURE_HEIGHT
        );
    }

    private static void drawDot(GuiGraphics guiGraphics, int centerX, int centerY, ResourceLocation texture) {
        guiGraphics.blit(
                texture,
                centerX - DOT_TEXTURE_SIZE / 2,
                centerY - DOT_TEXTURE_SIZE / 2,
                0,
                0,
                DOT_TEXTURE_SIZE,
                DOT_TEXTURE_SIZE,
                DOT_TEXTURE_SIZE,
                DOT_TEXTURE_SIZE
        );
    }

    private static void drawArrowUp(GuiGraphics guiGraphics, int centerX, int baseY) {
        guiGraphics.blit(
                LOCATOR_BAR_ARROW_UP_TEXTURE,
                centerX - ARROW_TEXTURE_WIDTH / 2,
                baseY - ARROW_TEXTURE_HEIGHT + 2,
                0,
                0,
                ARROW_TEXTURE_WIDTH,
                ARROW_TEXTURE_HEIGHT,
                ARROW_TEXTURE_WIDTH,
                ARROW_SPRITE_TEXTURE_HEIGHT
        );
    }

    private static void drawArrowDown(GuiGraphics guiGraphics, int centerX, int baseY) {
        guiGraphics.blit(
                LOCATOR_BAR_ARROW_DOWN_TEXTURE,
                centerX - ARROW_TEXTURE_WIDTH / 2,
                baseY - 2,
                0,
                0,
                ARROW_TEXTURE_WIDTH,
                ARROW_TEXTURE_HEIGHT,
                ARROW_TEXTURE_WIDTH,
                ARROW_SPRITE_TEXTURE_HEIGHT
        );
    }
}
