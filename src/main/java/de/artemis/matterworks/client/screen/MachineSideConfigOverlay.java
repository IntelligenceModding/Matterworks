package de.artemis.matterworks.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.SideConfigMenuAccess;
import de.artemis.matterworks.common.network.SetSideConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class MachineSideConfigOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/side_configuration.png");
    private static final ResourceLocation VISUAL_TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/config_screen.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 222;
    private static final int VISUAL_VIEWPORT_X = 8;
    private static final int VISUAL_VIEWPORT_Y = 18;
    private static final int VISUAL_VIEWPORT_WIDTH = 160;
    private static final int VISUAL_VIEWPORT_HEIGHT = 106;
    private static final int VISUAL_MAIN_SIZE = 32;
    private static final int VISUAL_BACK_BUTTON_MIN_LEFT = 161;
    private static final int VISUAL_BACK_BUTTON_RIGHT = 168;
    private static final int VISUAL_BACK_BUTTON_TOP = 6;
    private static final int VISUAL_BACK_BUTTON_BOTTOM = 13;
    private static final float VISUAL_BACK_BUTTON_TEXT_SCALE = 0.70F;
    private static final String VISUAL_BACK_BUTTON_TEXT = "Back";
    private static final int VISUAL_CONTROL_GAP = 2;
    private static final int VISUAL_TOGGLE_BUTTON_MIN_WIDTH = 12;
    private static final String VISUAL_TEXT_TOGGLE_TEXT = "Text";
    private static final String VISUAL_OVERLAY_TOGGLE_TEXT = "Overlay";
    private static final String VISUAL_GHOST_TOGGLE_TEXT = "Ghost";
    private static final String VISUAL_CLEAR_BUTTON_TEXT = "Clear";
    private static final String VISUAL_ALL_BUTTON_TEXT = "All";
    private static final String CENTER_VISUAL_MODE_TEXT = "3D";
    private static final double VISUAL_SCALE = 34.0D;
    private static final float VISUAL_FACE_HALF_SIZE = 0.505F;
    private static final float VISUAL_FACE_OFFSET = 0.512F;
    private static final double VISUAL_PICK_HALF_SIZE = 0.5D;
    private static final double VISUAL_PICK_TOLERANCE = 0.75D;
    private static final float VISUAL_FACE_LABEL_SCALE = 0.013F;
    private static final float VISUAL_FACE_LABEL_OFFSET = 0.018F;
    private static final float VISUAL_FACE_OVERLAY_ALPHA = 0.42F;
    private static final float VISUAL_GHOST_ALPHA = 0.48F;
    private static final double VISUAL_CLICK_DRAG_THRESHOLD = 3.0D;
    private static final RelativeSide[] RELATIVE_SIDES = {
            RelativeSide.UP,
            RelativeSide.FRONT,
            RelativeSide.LEFT,
            RelativeSide.RIGHT,
            RelativeSide.BACK,
            RelativeSide.DOWN
    };
    private static boolean visualMode;
    private static boolean visualTextVisible = true;
    private static boolean visualOverlayVisible = true;
    private static boolean visualGhostVisible = true;
    private static double visualYaw = Math.toRadians(35.0D);
    private static double visualPitch = Math.toRadians(24.0D);
    private boolean draggingVisualCamera;
    private double visualDragDistance;
    private int visualPressedButton = -1;

    public void renderBackground(GuiGraphics guiGraphics, int leftPos, int topPos) {
        guiGraphics.blit(visualMode ? VISUAL_TEXTURE : TEXTURE, leftPos, topPos, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, 256, 256);
    }

    public void cancelVisualDrag() {
        draggingVisualCamera = false;
        visualPressedButton = -1;
        visualDragDistance = 0.0D;
    }

    public boolean isVisualMode() {
        return visualMode;
    }

    public <T extends SideConfigMenuAccess> void render(
            GuiGraphics guiGraphics,
            Font font,
            T menu,
            SideConfigType type,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int titleX,
            int titleY,
            int titleColor,
            String titleText,
            int inventoryLabelX,
            int inventoryLabelY,
            int mouseX,
            int mouseY
    ) {
        if (visualMode) {
            renderVisualConfig(guiGraphics, font, menu, type, leftPos, topPos, titleX, titleY, titleColor, titleText, inventoryLabelX, inventoryLabelY, mouseX, mouseY);
            return;
        }

        int[] centerBounds = getCenterBounds(leftPos, topPos);
        renderCenterDisplay(
                guiGraphics,
                font,
                menu.getPrimaryTabIcon(),
                centerBounds[0],
                centerBounds[1],
                centerBounds[2],
                centerBounds[3],
                isInside(mouseX, mouseY, centerBounds[0], centerBounds[1], centerBounds[2], centerBounds[3])
        );
        if (!titleText.isEmpty()) {
            guiGraphics.drawString(font, titleText, leftPos + titleX, topPos + titleY, titleColor, false);
        }
        guiGraphics.drawString(font, Component.translatable("container.inventory"), leftPos + inventoryLabelX, topPos + inventoryLabelY, 0x404040, false);

        Direction frontFacing = menu.getSideConfigFrontFacing();
        for (RelativeSide side : RELATIVE_SIDES) {
            renderSideNode(guiGraphics, font, menu, type, side, frontFacing, leftPos, topPos);
        }
    }

    public <T extends SideConfigMenuAccess> void renderTooltip(
            GuiGraphics guiGraphics,
            Font font,
            T menu,
            SideConfigType type,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY
    ) {
        if (visualMode) {
            renderVisualTooltip(guiGraphics, font, menu, type, leftPos, topPos, mouseX, mouseY);
            return;
        }

        int[] centerBounds = getCenterBounds(leftPos, topPos);
        if (isInside(mouseX, mouseY, centerBounds[0], centerBounds[1], centerBounds[2], centerBounds[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Open 3D View"), mouseX, mouseY);
            return;
        }

        Direction frontFacing = menu.getSideConfigFrontFacing();
        for (RelativeSide side : RELATIVE_SIDES) {
            int[] node = getNodeBounds(leftPos, topPos, side);
            if (isInside(mouseX, mouseY, node[0], node[1], node[2], node[3])) {
                Direction worldSide = resolveWorldDirection(frontFacing, side);
                SideAccessMode mode = menu.getSideAccessMode(type, worldSide);
                String sideText = shouldShowWorldDirection(side)
                        ? side.label + " (" + getDirectionName(worldSide) + ")"
                        : side.label;
                guiGraphics.renderTooltip(
                        font,
                        Component.literal(sideText + ": " + menu.getSideAccessModeLabel(type, worldSide, mode)),
                        mouseX,
                        mouseY
                );
                return;
            }
        }
    }

    public <T extends SideConfigMenuAccess> boolean mouseClicked(
            T menu,
            SideConfigType type,
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (button != 0 && button != 1) {
            return false;
        }
        if (visualMode) {
            return mouseClickedVisual(menu, type, mouseX, mouseY, button, leftPos, topPos);
        }

        int[] center = getCenterBounds(leftPos, topPos);
        if (button == 0 && isInside(mouseX, mouseY, center[0], center[1], center[2], center[3])) {
            visualMode = true;
            GuiWidgets.playButtonClickSound();
            return true;
        }

        Direction frontFacing = menu.getSideConfigFrontFacing();
        for (RelativeSide side : RELATIVE_SIDES) {
            int[] node = getNodeBounds(leftPos, topPos, side);
            if (isInside(mouseX, mouseY, node[0], node[1], node[2], node[3])) {
                Direction worldSide = resolveWorldDirection(frontFacing, side);
                changeSideMode(menu, type, worldSide, button);
                return true;
            }
        }
        return isInside(mouseX, mouseY, leftPos, topPos, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    public <T extends SideConfigMenuAccess> boolean mouseDragged(
            T menu,
            SideConfigType type,
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (!visualMode || !draggingVisualCamera || button != visualPressedButton) {
            return false;
        }
        visualYaw += dragX * 0.018D;
        visualPitch = Mth.clamp((float) (visualPitch + dragY * 0.018D), (float) Math.toRadians(-60.0D), (float) Math.toRadians(60.0D));
        visualDragDistance += Math.abs(dragX) + Math.abs(dragY);
        return true;
    }

    public <T extends SideConfigMenuAccess> boolean mouseReleased(
            T menu,
            SideConfigType type,
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        if (!visualMode || !draggingVisualCamera || button != visualPressedButton) {
            return false;
        }

        boolean shortClick = visualDragDistance <= VISUAL_CLICK_DRAG_THRESHOLD;
        Direction clickedSide = shortClick ? getVisualSideAt(menu, type, mouseX, mouseY, leftPos, topPos) : null;
        draggingVisualCamera = false;
        visualPressedButton = -1;
        visualDragDistance = 0.0D;

        if (!shortClick) {
            return true;
        }
        if (clickedSide != null) {
            changeSideMode(menu, type, clickedSide, button);
            return true;
        }
        return true;
    }

    private <T extends SideConfigMenuAccess> void renderSideNode(
            GuiGraphics guiGraphics,
            Font font,
            T menu,
            SideConfigType type,
            RelativeSide side,
            Direction frontFacing,
            int leftPos,
            int topPos
    ) {
        int[] node = getNodeBounds(leftPos, topPos, side);
        Direction worldSide = resolveWorldDirection(frontFacing, side);
        SideAccessMode mode = menu.getSideAccessMode(type, worldSide);
        GuiWidgets.tintSlotInterior(guiGraphics, node[0], node[1], node[2], node[3], getModeFillColor(mode), getModeHighlightColor(mode));
        drawScaledCenteredString(
                guiGraphics,
                font,
                menu.getSideAccessModeShortLabel(type, worldSide, mode),
                node[0] + node[2] / 2,
                node[1] + (node[3] - font.lineHeight) / 2,
                getTextScale(font, menu.getSideAccessModeShortLabel(type, worldSide, mode), node[2] - 2),
                0xFFE0E0E0
        );
    }

    private <T extends SideConfigMenuAccess> void renderVisualConfig(
            GuiGraphics guiGraphics,
            Font font,
            T menu,
            SideConfigType type,
            int leftPos,
            int topPos,
            int titleX,
            int titleY,
            int titleColor,
            String titleText,
            int inventoryLabelX,
            int inventoryLabelY,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.drawString(font, Component.translatable("container.inventory"), leftPos + inventoryLabelX, topPos + inventoryLabelY, 0x404040, false);

        int viewportX = leftPos + VISUAL_VIEWPORT_X;
        int viewportY = topPos + VISUAL_VIEWPORT_Y;
        guiGraphics.enableScissor(viewportX, viewportY, viewportX + VISUAL_VIEWPORT_WIDTH, viewportY + VISUAL_VIEWPORT_HEIGHT);
        try {
            renderWorldPreview(guiGraphics, font, menu, type, leftPos, topPos);
            guiGraphics.flush();
        } finally {
            guiGraphics.disableScissor();
        }
        renderVisualControls(guiGraphics, font, leftPos, topPos, mouseX, mouseY);
    }

    private <T extends SideConfigMenuAccess> void renderVisualTooltip(
            GuiGraphics guiGraphics,
            Font font,
            T menu,
            SideConfigType type,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY
    ) {
        int[] textButton = getVisualTextToggleBounds(font, leftPos, topPos);
        if (isInside(mouseX, mouseY, textButton[0], textButton[1], textButton[2], textButton[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Text: " + getToggleStateLabel(visualTextVisible)), mouseX, mouseY);
            return;
        }

        int[] overlayButton = getVisualOverlayToggleBounds(font, leftPos, topPos);
        if (isInside(mouseX, mouseY, overlayButton[0], overlayButton[1], overlayButton[2], overlayButton[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Overlay: " + getToggleStateLabel(visualOverlayVisible)), mouseX, mouseY);
            return;
        }

        int[] ghostButton = getVisualGhostToggleBounds(font, leftPos, topPos);
        if (isInside(mouseX, mouseY, ghostButton[0], ghostButton[1], ghostButton[2], ghostButton[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Ghost Blocks: " + getToggleStateLabel(visualGhostVisible)), mouseX, mouseY);
            return;
        }

        int[] clearButton = getVisualClearButtonBounds(font, leftPos, topPos);
        if (isInside(mouseX, mouseY, clearButton[0], clearButton[1], clearButton[2], clearButton[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Set all sides Off"), mouseX, mouseY);
            return;
        }

        int[] allButton = getVisualAllButtonBounds(font, leftPos, topPos);
        if (isInside(mouseX, mouseY, allButton[0], allButton[1], allButton[2], allButton[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Cycle all sides"), mouseX, mouseY);
            return;
        }

        int[] backButton = getVisualBackButtonBounds(font, leftPos, topPos);
        if (isInside(mouseX, mouseY, backButton[0], backButton[1], backButton[2], backButton[3])) {
            guiGraphics.renderTooltip(font, Component.literal("Back"), mouseX, mouseY);
            return;
        }

        Direction hoveredSide = getVisualSideAt(menu, type, mouseX, mouseY, leftPos, topPos);
        if (hoveredSide == null) {
            return;
        }

        SideAccessMode mode = menu.getSideAccessMode(type, hoveredSide);
        guiGraphics.renderTooltip(
                font,
                Component.literal(getDirectionName(hoveredSide) + ": " + menu.getSideAccessModeLabel(type, hoveredSide, mode)),
                mouseX,
                mouseY
        );
    }

    private <T extends SideConfigMenuAccess> boolean mouseClickedVisual(
            T menu,
            SideConfigType type,
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos
    ) {
        int[] backButton = getVisualBackButtonBounds(Minecraft.getInstance().font, leftPos, topPos);
        if (isInside(mouseX, mouseY, backButton[0], backButton[1], backButton[2], backButton[3])) {
            if (button == 0) {
                visualMode = false;
                draggingVisualCamera = false;
                visualPressedButton = -1;
                visualDragDistance = 0.0D;
                GuiWidgets.playButtonClickSound();
            }
            return true;
        }

        int[] textButton = getVisualTextToggleBounds(Minecraft.getInstance().font, leftPos, topPos);
        if (isInside(mouseX, mouseY, textButton[0], textButton[1], textButton[2], textButton[3])) {
            if (button == 0) {
                visualTextVisible = !visualTextVisible;
                GuiWidgets.playButtonClickSound();
            }
            return true;
        }

        int[] overlayButton = getVisualOverlayToggleBounds(Minecraft.getInstance().font, leftPos, topPos);
        if (isInside(mouseX, mouseY, overlayButton[0], overlayButton[1], overlayButton[2], overlayButton[3])) {
            if (button == 0) {
                visualOverlayVisible = !visualOverlayVisible;
                GuiWidgets.playButtonClickSound();
            }
            return true;
        }

        int[] ghostButton = getVisualGhostToggleBounds(Minecraft.getInstance().font, leftPos, topPos);
        if (isInside(mouseX, mouseY, ghostButton[0], ghostButton[1], ghostButton[2], ghostButton[3])) {
            if (button == 0) {
                visualGhostVisible = !visualGhostVisible;
                GuiWidgets.playButtonClickSound();
            }
            return true;
        }

        int[] clearButton = getVisualClearButtonBounds(Minecraft.getInstance().font, leftPos, topPos);
        if (isInside(mouseX, mouseY, clearButton[0], clearButton[1], clearButton[2], clearButton[3])) {
            if (button == 0) {
                clearSideModes(menu, type);
            }
            return true;
        }

        int[] allButton = getVisualAllButtonBounds(Minecraft.getInstance().font, leftPos, topPos);
        if (isInside(mouseX, mouseY, allButton[0], allButton[1], allButton[2], allButton[3])) {
            if (button == 0) {
                cycleAllSideModes(menu, type);
            }
            return true;
        }

        if (!isInside(mouseX, mouseY, leftPos + VISUAL_VIEWPORT_X, topPos + VISUAL_VIEWPORT_Y, VISUAL_VIEWPORT_WIDTH, VISUAL_VIEWPORT_HEIGHT)) {
            return isInside(mouseX, mouseY, leftPos, topPos, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        draggingVisualCamera = true;
        visualPressedButton = button;
        visualDragDistance = 0.0D;
        return true;
    }

    private static void renderVisualControls(GuiGraphics guiGraphics, Font font, int leftPos, int topPos, int mouseX, int mouseY) {
        int[] textButton = getVisualTextToggleBounds(font, leftPos, topPos);
        renderVisualButton(guiGraphics, font, textButton, VISUAL_TEXT_TOGGLE_TEXT, visualTextVisible, mouseX, mouseY);

        int[] overlayButton = getVisualOverlayToggleBounds(font, leftPos, topPos);
        renderVisualButton(guiGraphics, font, overlayButton, VISUAL_OVERLAY_TOGGLE_TEXT, visualOverlayVisible, mouseX, mouseY);

        int[] ghostButton = getVisualGhostToggleBounds(font, leftPos, topPos);
        renderVisualButton(guiGraphics, font, ghostButton, VISUAL_GHOST_TOGGLE_TEXT, visualGhostVisible, mouseX, mouseY);

        int[] clearButton = getVisualClearButtonBounds(font, leftPos, topPos);
        renderVisualButton(guiGraphics, font, clearButton, VISUAL_CLEAR_BUTTON_TEXT, true, mouseX, mouseY);

        int[] allButton = getVisualAllButtonBounds(font, leftPos, topPos);
        renderVisualButton(guiGraphics, font, allButton, VISUAL_ALL_BUTTON_TEXT, true, mouseX, mouseY);

        int[] backButton = getVisualBackButtonBounds(font, leftPos, topPos);
        renderVisualButton(guiGraphics, font, backButton, VISUAL_BACK_BUTTON_TEXT, true, mouseX, mouseY);
    }

    private static void renderVisualButton(GuiGraphics guiGraphics, Font font, int[] button, String text, boolean active, int mouseX, int mouseY) {
        boolean hovered = isInside(mouseX, mouseY, button[0], button[1], button[2], button[3]);
        int fill = active
                ? hovered ? 0xFFE6E6E6 : 0xFFC6C6C6
                : hovered ? 0xFFC0C0C0 : 0xFFA8A8A8;
        int highlight = active
                ? hovered ? 0xFFFFFFFF : 0xFFF4F4F4
                : hovered ? 0xFFE0E0E0 : 0xFFD0D0D0;
        int shadow = 0xFF555555;
        int textColor = active ? 0xFF303030 : 0xFF666666;
        guiGraphics.fill(button[0], button[1], button[0] + button[2], button[1] + button[3], shadow);
        guiGraphics.fill(button[0], button[1], button[0] + button[2] - 1, button[1] + button[3] - 1, highlight);
        guiGraphics.fill(button[0] + 1, button[1] + 1, button[0] + button[2] - 1, button[1] + button[3] - 1, fill);
        guiGraphics.fill(button[0] + button[2] - 1, button[1] + 1, button[0] + button[2], button[1] + button[3], shadow);
        guiGraphics.fill(button[0] + 1, button[1] + button[3] - 1, button[0] + button[2], button[1] + button[3], shadow);

        float textWidth = font.width(text) * VISUAL_BACK_BUTTON_TEXT_SCALE;
        float textHeight = font.lineHeight * VISUAL_BACK_BUTTON_TEXT_SCALE;
        float textX = button[0] + (button[2] - textWidth) / 2.0F;
        float textY = button[1] + (button[3] - textHeight) / 2.0F + 0.5F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(VISUAL_BACK_BUTTON_TEXT_SCALE, VISUAL_BACK_BUTTON_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(
                font,
                text,
                Math.round(textX / VISUAL_BACK_BUTTON_TEXT_SCALE),
                Math.round(textY / VISUAL_BACK_BUTTON_TEXT_SCALE),
                textColor,
                false
        );
        guiGraphics.pose().popPose();
    }

    private static int[] getVisualBackButtonBounds(Font font, int leftPos, int topPos) {
        int minWidth = VISUAL_BACK_BUTTON_RIGHT - VISUAL_BACK_BUTTON_MIN_LEFT + 1;
        return getVisualControlButtonBounds(font, leftPos, topPos, 5, VISUAL_BACK_BUTTON_TEXT, minWidth);
    }

    private static int[] getVisualTextToggleBounds(Font font, int leftPos, int topPos) {
        return getVisualControlButtonBounds(font, leftPos, topPos, 0, VISUAL_TEXT_TOGGLE_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH);
    }

    private static int[] getVisualOverlayToggleBounds(Font font, int leftPos, int topPos) {
        return getVisualControlButtonBounds(font, leftPos, topPos, 4, VISUAL_OVERLAY_TOGGLE_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH);
    }

    private static int[] getVisualGhostToggleBounds(Font font, int leftPos, int topPos) {
        return getVisualControlButtonBounds(font, leftPos, topPos, 3, VISUAL_GHOST_TOGGLE_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH);
    }

    private static int[] getVisualClearButtonBounds(Font font, int leftPos, int topPos) {
        return getVisualControlButtonBounds(font, leftPos, topPos, 2, VISUAL_CLEAR_BUTTON_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH);
    }

    private static int[] getVisualAllButtonBounds(Font font, int leftPos, int topPos) {
        return getVisualControlButtonBounds(font, leftPos, topPos, 1, VISUAL_ALL_BUTTON_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH);
    }

    private static int[] getVisualControlButtonBounds(Font font, int leftPos, int topPos, int index, String text, int minWidth) {
        int[] widths = {
                getVisualButtonWidth(font, VISUAL_TEXT_TOGGLE_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH),
                getVisualButtonWidth(font, VISUAL_ALL_BUTTON_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH),
                getVisualButtonWidth(font, VISUAL_CLEAR_BUTTON_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH),
                getVisualButtonWidth(font, VISUAL_GHOST_TOGGLE_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH),
                getVisualButtonWidth(font, VISUAL_OVERLAY_TOGGLE_TEXT, VISUAL_TOGGLE_BUTTON_MIN_WIDTH),
                getVisualButtonWidth(font, VISUAL_BACK_BUTTON_TEXT, VISUAL_BACK_BUTTON_RIGHT - VISUAL_BACK_BUTTON_MIN_LEFT + 1)
        };
        int width = Math.max(minWidth, getVisualButtonWidth(font, text, minWidth));
        int totalWidth = 0;
        for (int buttonWidth : widths) {
            totalWidth += buttonWidth;
        }

        int left = leftPos + VISUAL_VIEWPORT_X;
        int right = leftPos + VISUAL_BACK_BUTTON_RIGHT;
        int availableWidth = right - left + 1;
        double gap = Math.max(0.0D, (availableWidth - totalWidth) / (double) (widths.length - 1));
        double x = left;
        for (int i = 0; i < index; i++) {
            x += widths[i] + gap;
        }
        if (index == widths.length - 1) {
            x = right - width + 1;
        }

        return new int[]{
                (int) Math.round(x),
                topPos + VISUAL_BACK_BUTTON_TOP,
                width,
                VISUAL_BACK_BUTTON_BOTTOM - VISUAL_BACK_BUTTON_TOP + 1
        };
    }

    private static int getVisualButtonWidth(Font font, String text, int minWidth) {
        int textWidth = Mth.ceil(font.width(text) * VISUAL_BACK_BUTTON_TEXT_SCALE) + 6;
        return Math.max(minWidth, textWidth);
    }

    private static String getToggleStateLabel(boolean enabled) {
        return enabled ? "On" : "Off";
    }

    private static String getVisualModeLabel(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> "Off";
            case INPUT -> "Input";
            case OUTPUT -> "Output";
            case BOTH -> "Both";
            case OUTPUT_PRIMARY -> "Output 1";
            case OUTPUT_SECONDARY -> "Output 2";
            case OUTPUT_TERTIARY -> "Output 3";
        };
    }

    private void renderWorldPreview(GuiGraphics guiGraphics, Font font, SideConfigMenuAccess menu, SideConfigType type, int leftPos, int topPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        guiGraphics.flush();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getVisualCenterX(leftPos), getVisualCenterY(topPos), 180.0F);
        guiGraphics.pose().scale((float) VISUAL_SCALE, (float) -VISUAL_SCALE, (float) VISUAL_SCALE);
        guiGraphics.pose().mulPose(Axis.XP.rotation((float) visualPitch));
        guiGraphics.pose().mulPose(Axis.YP.rotation((float) visualYaw));

        if (visualGhostVisible) {
            for (Direction direction : Direction.values()) {
                BlockPos blockPos = menu.getBlockPos().relative(direction);
                BlockState blockState = minecraft.level.getBlockState(blockPos);
                if (!blockState.isAir()) {
                    renderLivePreviewBlock(guiGraphics, minecraft, blockPos, direction.getStepX(), direction.getStepY(), direction.getStepZ(), true);
                }
            }
        }
        guiGraphics.flush();
        RenderSystem.depthMask(true);
        renderLivePreviewBlock(guiGraphics, minecraft, menu.getBlockPos(), 0, 0, 0, false);
        guiGraphics.flush();
        if (visualOverlayVisible) {
            renderSideConfigFaceOverlays(guiGraphics, menu, type);
        }
        if (visualTextVisible) {
            renderSideConfigFaceLabels(guiGraphics, font, menu, type);
        }

        guiGraphics.pose().popPose();
        guiGraphics.flush();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    private static void renderLivePreviewBlock(GuiGraphics guiGraphics, Minecraft minecraft, BlockPos blockPos, int x, int y, int z, boolean ghost) {
        if (minecraft.level == null) {
            return;
        }

        BlockState blockState = minecraft.level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x - 0.5D, y - 0.5D, z - 0.5D);
        ModelData modelData = minecraft.level.getModelDataManager().getAt(blockPos);
        int packedLight = LightTexture.FULL_BRIGHT;
        if (ghost) {
            renderGhostPreviewBlock(guiGraphics, minecraft, blockState, blockPos, modelData, packedLight);
        } else {
            minecraft.getBlockRenderer().renderSingleBlock(
                    blockState,
                    guiGraphics.pose(),
                    guiGraphics.bufferSource(),
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    null
            );
            renderLivePreviewBlockEntity(guiGraphics, minecraft, blockPos, packedLight);
        }
        guiGraphics.pose().popPose();
    }

    private static void renderGhostPreviewBlock(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            BlockState blockState,
            BlockPos blockPos,
            ModelData modelData,
            int packedLight
    ) {
        if (minecraft.level == null) {
            return;
        }

        if (blockState.getRenderShape() != RenderShape.MODEL) {
            renderGhostFallbackCube(guiGraphics, blockState, minecraft.level, blockPos, packedLight);
            return;
        }

        BakedModel model = minecraft.getBlockRenderer().getBlockModel(blockState);
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.translucent(), false));
        PoseStack.Pose pose = guiGraphics.pose().last();
        RandomSource random = RandomSource.create();
        boolean renderedAnyQuad = false;
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            renderedAnyQuad |= renderGhostQuadList(minecraft, blockState, blockPos, consumer, pose, model.getQuads(blockState, direction, random, modelData, null), packedLight);
        }
        random.setSeed(42L);
        renderedAnyQuad |= renderGhostQuadList(minecraft, blockState, blockPos, consumer, pose, model.getQuads(blockState, null, random, modelData, null), packedLight);

        if (!renderedAnyQuad) {
            renderGhostFallbackCube(guiGraphics, blockState, minecraft.level, blockPos, packedLight);
        }
    }

    private static boolean renderGhostQuadList(
            Minecraft minecraft,
            BlockState blockState,
            BlockPos blockPos,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            List<BakedQuad> quads,
            int packedLight
    ) {
        if (minecraft.level == null || quads.isEmpty()) {
            return false;
        }

        for (BakedQuad quad : quads) {
            float red = 1.0F;
            float green = 1.0F;
            float blue = 1.0F;
            if (quad.isTinted()) {
                int color = minecraft.getBlockColors().getColor(blockState, minecraft.level, blockPos, quad.getTintIndex());
                red = ((color >> 16) & 0xFF) / 255.0F;
                green = ((color >> 8) & 0xFF) / 255.0F;
                blue = (color & 0xFF) / 255.0F;
            }

            consumer.putBulkData(
                    pose,
                    quad,
                    red,
                    green,
                    blue,
                    VISUAL_GHOST_ALPHA,
                    packedLight,
                    OverlayTexture.NO_OVERLAY
            );
        }
        return true;
    }

    private static void renderGhostFallbackCube(GuiGraphics guiGraphics, BlockState blockState, net.minecraft.world.level.BlockAndTintGetter level, BlockPos blockPos, int packedLight) {
        int color = blockState.getMapColor(level, blockPos).col;
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.translucent(), false));
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float h = 0.5F;
        addQuad(consumer, matrix, -h, -h, h, h, -h, h, h, h, h, -h, h, h, red, green, blue, VISUAL_GHOST_ALPHA);
        addQuad(consumer, matrix, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, red, green, blue, VISUAL_GHOST_ALPHA);
        addQuad(consumer, matrix, -h, h, h, h, h, h, h, h, -h, -h, h, -h, red, green, blue, VISUAL_GHOST_ALPHA);
        addQuad(consumer, matrix, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, red, green, blue, VISUAL_GHOST_ALPHA);
        addQuad(consumer, matrix, h, -h, h, h, -h, -h, h, h, -h, h, h, h, red, green, blue, VISUAL_GHOST_ALPHA);
        addQuad(consumer, matrix, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, red, green, blue, VISUAL_GHOST_ALPHA);
    }

    private static void renderLivePreviewBlockEntity(GuiGraphics guiGraphics, Minecraft minecraft, BlockPos blockPos, int packedLight) {
        if (minecraft.level == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(blockPos);
        if (blockEntity == null || blockEntity.isRemoved() || !blockEntity.getType().isValid(blockEntity.getBlockState())) {
            return;
        }

        minecraft.getBlockEntityRenderDispatcher().renderItem(
                blockEntity,
                guiGraphics.pose(),
                guiGraphics.bufferSource(),
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
    }

    private void renderSideConfigFaceOverlays(GuiGraphics guiGraphics, SideConfigMenuAccess menu, SideConfigType type) {
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.debugQuads());
        Matrix4f matrix = guiGraphics.pose().last().pose();
        RenderSystem.depthMask(false);
        for (Direction direction : Direction.values()) {
            if (!isSideFacingCamera(direction)) {
                continue;
            }
            SideAccessMode mode = menu.getSideAccessMode(type, direction);
            int fillColor = getModeFillColor(mode);
            addSideOverlayQuad(
                    consumer,
                    matrix,
                    direction,
                    ((fillColor >> 16) & 0xFF) / 255.0F,
                    ((fillColor >> 8) & 0xFF) / 255.0F,
                    (fillColor & 0xFF) / 255.0F,
                    VISUAL_FACE_OVERLAY_ALPHA
            );
        }
        guiGraphics.flush();
        RenderSystem.depthMask(true);
    }

    private void renderSideConfigFaceLabels(GuiGraphics guiGraphics, Font font, SideConfigMenuAccess menu, SideConfigType type) {
        guiGraphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        for (Direction direction : Direction.values()) {
            if (!isSideFacingCamera(direction)) {
                continue;
            }

            SideAccessMode mode = menu.getSideAccessMode(type, direction);
            String label = getVisualModeLabel(mode);
            if (label.isEmpty()) {
                continue;
            }

            renderFaceLabel(guiGraphics, font, direction, label, getModeHighlightColor(mode));
        }
        guiGraphics.flush();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private boolean isSideFacingCamera(Direction direction) {
        return objectToView(direction.getStepX(), direction.getStepY(), direction.getStepZ()).z() > 1.0E-5D;
    }

    private VisualPoint objectToScreen(double x, double y, double z, int leftPos, int topPos) {
        VisualVector view = objectToView(x, y, z);
        return new VisualPoint(
                getVisualCenterX(leftPos) + view.x() * VISUAL_SCALE,
                getVisualCenterY(topPos) - view.y() * VISUAL_SCALE,
                view.z()
        );
    }

    private VisualVector objectToView(double x, double y, double z) {
        double yawCos = Math.cos(visualYaw);
        double yawSin = Math.sin(visualYaw);
        double yawedX = x * yawCos + z * yawSin;
        double yawedZ = -x * yawSin + z * yawCos;

        double pitchCos = Math.cos(visualPitch);
        double pitchSin = Math.sin(visualPitch);
        return new VisualVector(
                yawedX,
                y * pitchCos - yawedZ * pitchSin,
                y * pitchSin + yawedZ * pitchCos
        );
    }

    private static void renderFaceLabel(GuiGraphics guiGraphics, Font font, Direction direction, String label, int color) {
        float textWidth = font.width(label);
        float backgroundHalfWidth = textWidth * VISUAL_FACE_LABEL_SCALE * 0.5F + 0.055F;
        float backgroundHalfHeight = font.lineHeight * VISUAL_FACE_LABEL_SCALE * 0.5F + 0.035F;
        guiGraphics.pose().pushPose();
        orientToSide(guiGraphics, direction, VISUAL_FACE_OFFSET + VISUAL_FACE_LABEL_OFFSET);
        if (shouldRotateHorizontalFaceLabel(direction)) {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        Matrix4f matrix = guiGraphics.pose().last().pose();
        VertexConsumer background = guiGraphics.bufferSource().getBuffer(RenderType.debugQuads());
        addQuad(
                background,
                matrix,
                -backgroundHalfWidth, -backgroundHalfHeight, 0.0F,
                backgroundHalfWidth, -backgroundHalfHeight, 0.0F,
                backgroundHalfWidth, backgroundHalfHeight, 0.0F,
                -backgroundHalfWidth, backgroundHalfHeight, 0.0F,
                0.03F,
                0.035F,
                0.045F,
                0.58F
        );
        guiGraphics.flush();

        guiGraphics.pose().scale(VISUAL_FACE_LABEL_SCALE, -VISUAL_FACE_LABEL_SCALE, VISUAL_FACE_LABEL_SCALE);
        font.drawInBatch(
                label,
                -textWidth / 2.0F,
                -font.lineHeight / 2.0F,
                color,
                false,
                guiGraphics.pose().last().pose(),
                guiGraphics.bufferSource(),
                Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT
        );
        guiGraphics.pose().popPose();
    }

    private static boolean shouldRotateHorizontalFaceLabel(Direction direction) {
        if (direction != Direction.UP && direction != Direction.DOWN) {
            return false;
        }

        double projectedTextUpY = Math.cos(visualYaw) * Math.sin(visualPitch);
        if (direction == Direction.DOWN) {
            projectedTextUpY = -projectedTextUpY;
        }
        return projectedTextUpY < 0.0D;
    }

    private static void orientToSide(GuiGraphics guiGraphics, Direction direction, float offset) {
        guiGraphics.pose().translate(
                direction.getStepX() * offset,
                direction.getStepY() * offset,
                direction.getStepZ() * offset
        );
        switch (direction) {
            case NORTH -> guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(-90.0F));
            case UP -> guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(-90.0F));
            case DOWN -> guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> {
            }
        }
    }

    private static void addSideOverlayQuad(VertexConsumer consumer, Matrix4f matrix, Direction direction, float red, float green, float blue, float alpha) {
        float h = VISUAL_FACE_HALF_SIZE;
        float o = VISUAL_FACE_OFFSET;
        switch (direction) {
            case DOWN -> addQuad(consumer, matrix, -h, -o, h, h, -o, h, h, -o, -h, -h, -o, -h, red, green, blue, alpha);
            case UP -> addQuad(consumer, matrix, -h, o, -h, h, o, -h, h, o, h, -h, o, h, red, green, blue, alpha);
            case NORTH -> addQuad(consumer, matrix, h, -h, -o, h, h, -o, -h, h, -o, -h, -h, -o, red, green, blue, alpha);
            case SOUTH -> addQuad(consumer, matrix, -h, -h, o, -h, h, o, h, h, o, h, -h, o, red, green, blue, alpha);
            case WEST -> addQuad(consumer, matrix, -o, -h, -h, -o, h, -h, -o, h, h, -o, -h, h, red, green, blue, alpha);
            case EAST -> addQuad(consumer, matrix, o, -h, h, o, h, h, o, h, -h, o, -h, -h, red, green, blue, alpha);
        }
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x4, y4, z4).setColor(red, green, blue, alpha);
    }

    private <T extends SideConfigMenuAccess> Direction getVisualSideAt(
            T menu,
            SideConfigType type,
            double mouseX,
            double mouseY,
            int leftPos,
            int topPos
    ) {
        if (!isInside(mouseX, mouseY, leftPos + VISUAL_VIEWPORT_X, topPos + VISUAL_VIEWPORT_Y, VISUAL_VIEWPORT_WIDTH, VISUAL_VIEWPORT_HEIGHT)) {
            return null;
        }

        VisualFacePick bestPick = null;
        for (Direction direction : Direction.values()) {
            if (!isSideFacingCamera(direction)) {
                continue;
            }

            VisualPoint[] face = getProjectedPickFace(direction, leftPos, topPos);
            if (!containsProjectedFace(face, mouseX, mouseY)) {
                continue;
            }

            double depth = averageDepth(face);
            if (bestPick == null || depth > bestPick.depth()) {
                bestPick = new VisualFacePick(direction, depth);
            }
        }
        return bestPick == null ? null : bestPick.direction();
    }

    private VisualPoint[] getProjectedPickFace(Direction direction, int leftPos, int topPos) {
        double h = VISUAL_PICK_HALF_SIZE;
        return switch (direction) {
            case DOWN -> projectFace(leftPos, topPos, -h, -h, h, h, -h, h, h, -h, -h, -h, -h, -h);
            case UP -> projectFace(leftPos, topPos, -h, h, -h, h, h, -h, h, h, h, -h, h, h);
            case NORTH -> projectFace(leftPos, topPos, h, -h, -h, h, h, -h, -h, h, -h, -h, -h, -h);
            case SOUTH -> projectFace(leftPos, topPos, -h, -h, h, -h, h, h, h, h, h, h, -h, h);
            case WEST -> projectFace(leftPos, topPos, -h, -h, -h, -h, h, -h, -h, h, h, -h, -h, h);
            case EAST -> projectFace(leftPos, topPos, h, -h, h, h, h, h, h, h, -h, h, -h, -h);
        };
    }

    private VisualPoint[] projectFace(
            int leftPos,
            int topPos,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4
    ) {
        return new VisualPoint[]{
                objectToScreen(x1, y1, z1, leftPos, topPos),
                objectToScreen(x2, y2, z2, leftPos, topPos),
                objectToScreen(x3, y3, z3, leftPos, topPos),
                objectToScreen(x4, y4, z4, leftPos, topPos)
        };
    }

    private static boolean containsProjectedFace(VisualPoint[] face, double mouseX, double mouseY) {
        Boolean positive = null;
        for (int i = 0; i < face.length; i++) {
            VisualPoint a = face[i];
            VisualPoint b = face[(i + 1) % face.length];
            double edgeX = b.x() - a.x();
            double edgeY = b.y() - a.y();
            double cross = edgeX * (mouseY - a.y()) - edgeY * (mouseX - a.x());
            double edgeLength = Math.sqrt(edgeX * edgeX + edgeY * edgeY);
            if (edgeLength > 0.0D && Math.abs(cross) / edgeLength <= VISUAL_PICK_TOLERANCE) {
                continue;
            }
            boolean currentPositive = cross > 0.0D;
            if (positive == null) {
                positive = currentPositive;
            } else if (positive != currentPositive) {
                return false;
            }
        }
        return true;
    }

    private static double averageDepth(VisualPoint[] face) {
        double depth = 0.0D;
        for (VisualPoint point : face) {
            depth += point.z();
        }
        return depth / face.length;
    }

    private static int getVisualCenterX(int leftPos) {
        return leftPos + VISUAL_VIEWPORT_X + VISUAL_VIEWPORT_WIDTH / 2;
    }

    private static int getVisualCenterY(int topPos) {
        return topPos + VISUAL_VIEWPORT_Y + VISUAL_VIEWPORT_HEIGHT / 2;
    }

    private static int[] getNodeBounds(int leftPos, int topPos, RelativeSide side) {
        return switch (side) {
            case UP -> inclusiveBounds(leftPos + 76, topPos + 21, leftPos + 99, topPos + 44);
            case DOWN -> inclusiveBounds(leftPos + 76, topPos + 97, leftPos + 100, topPos + 120);
            case LEFT -> inclusiveBounds(leftPos + 38, topPos + 59, leftPos + 61, topPos + 82);
            case RIGHT -> inclusiveBounds(leftPos + 114, topPos + 59, leftPos + 137, topPos + 82);
            case FRONT -> inclusiveBounds(leftPos + 38, topPos + 21, leftPos + 61, topPos + 44);
            case BACK -> inclusiveBounds(leftPos + 114, topPos + 97, leftPos + 137, topPos + 120);
        };
    }

    private static int[] getCenterBounds(int leftPos, int topPos) {
        return inclusiveBounds(leftPos + 72, topPos + 55, leftPos + 103, topPos + 86);
    }

    private static int[] inclusiveBounds(int left, int top, int right, int bottom) {
        return new int[]{left, top, right - left + 1, bottom - top + 1};
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int getModeFillColor(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> 0xFF5E5E5E;
            case INPUT -> 0xFF365F95;
            case OUTPUT -> 0xFF9B6425;
            case OUTPUT_PRIMARY -> 0xFFA8702A;
            case OUTPUT_SECONDARY -> 0xFF8A5A9E;
            case OUTPUT_TERTIARY -> 0xFF9A4D4D;
            case BOTH -> 0xFF2E7A73;
        };
    }

    private static int getModeHighlightColor(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> 0xFF777777;
            case INPUT -> 0xFF5B84BA;
            case OUTPUT -> 0xFFBF8241;
            case OUTPUT_PRIMARY -> 0xFFD2924A;
            case OUTPUT_SECONDARY -> 0xFFA979C1;
            case OUTPUT_TERTIARY -> 0xFFBF6B6B;
            case BOTH -> 0xFF4C9A93;
        };
    }

    private static Direction resolveWorldDirection(Direction frontFacing, RelativeSide side) {
        return switch (side) {
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
            case FRONT -> frontFacing;
            case BACK -> frontFacing.getOpposite();
            case LEFT -> frontFacing.getCounterClockWise();
            case RIGHT -> frontFacing.getClockWise();
        };
    }

    private static String getDirectionName(Direction direction) {
        return Component.translatable("direction.minecraft." + direction.getName()).getString();
    }

    private static boolean shouldShowWorldDirection(RelativeSide side) {
        return side != RelativeSide.UP && side != RelativeSide.DOWN;
    }

    private static void drawScaledCenteredString(GuiGraphics guiGraphics, Font font, String text, int centerX, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, Math.round((centerX - font.width(text) * scale / 2.0F) / scale), Math.round(y / scale), color, false);
        guiGraphics.pose().popPose();
    }

    private static float getTextScale(Font font, String text, int maxWidth) {
        int width = font.width(text);
        if (width <= 0 || width <= maxWidth) {
            return 1.0F;
        }
        return Math.max(0.45F, maxWidth / (float) width);
    }

    private static void renderCenterDisplay(GuiGraphics guiGraphics, Font font, ItemStack icon, int x, int y, int width, int height, boolean hovered) {
        renderCenterButtonFrame(guiGraphics, x, y, width, height, hovered);

        float scale = Math.min(width / 16.0F, height / 16.0F);
        float renderSize = 16.0F * scale;
        float offsetX = x + (width - renderSize) / 2.0F;
        float offsetY = y + (height - renderSize) / 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.renderItem(icon, 0, 0);
        guiGraphics.pose().popPose();

        renderCenterModeBadge(guiGraphics, font, x, y, width, height, hovered);
    }

    private static void renderCenterButtonFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean hovered) {
        if (!hovered) {
            return;
        }

        int highlight = 0xCCA9D8FF;
        guiGraphics.fill(x, y, x + width, y + 1, highlight);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, highlight);
        guiGraphics.fill(x, y, x + 1, y + height, highlight);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, highlight);
    }

    private static void renderCenterModeBadge(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, boolean hovered) {
        int badgeWidth = font.width(CENTER_VISUAL_MODE_TEXT) + 5;
        int badgeHeight = font.lineHeight + 1;
        int badgeX = x + width - badgeWidth - 1;
        int badgeY = y + 1;
        guiGraphics.flush();
        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 120.0F);
        guiGraphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + badgeHeight, hovered ? 0xFF2B5F98 : 0xFF1E3144);
        guiGraphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 1, hovered ? 0xFFA9D8FF : 0xFF6C8DA8);
        guiGraphics.drawString(
                font,
                CENTER_VISUAL_MODE_TEXT,
                badgeX + 3,
                badgeY + 1,
                hovered ? 0xFFFFFFFF : 0xFFE5EDF5,
                false
        );
        guiGraphics.pose().popPose();
        guiGraphics.flush();
        RenderSystem.enableDepthTest();
    }

    private enum RelativeSide {
        UP("Up"),
        FRONT("Front"),
        LEFT("Left"),
        RIGHT("Right"),
        BACK("Back"),
        DOWN("Down");

        private final String label;

        RelativeSide(String label) {
            this.label = label;
        }
    }

    private static SideAccessMode getNextMode(SideConfigMenuAccess menu, SideConfigType type, Direction direction) {
        List<SideAccessMode> allowedModes = getAllowedModes(menu, type);
        SideAccessMode current = menu.getSideAccessMode(type, direction);
        int index = allowedModes.indexOf(current);
        if (index < 0) {
            return allowedModes.get(0);
        }
        return allowedModes.get((index + 1) % allowedModes.size());
    }

    private static SideAccessMode getPreviousMode(SideConfigMenuAccess menu, SideConfigType type, Direction direction) {
        List<SideAccessMode> allowedModes = getAllowedModes(menu, type);
        SideAccessMode current = menu.getSideAccessMode(type, direction);
        int index = allowedModes.indexOf(current);
        if (index < 0) {
            return allowedModes.get(0);
        }
        return allowedModes.get((index - 1 + allowedModes.size()) % allowedModes.size());
    }

    private static List<SideAccessMode> getAllowedModes(SideConfigMenuAccess menu, SideConfigType type) {
        return new ArrayList<>(menu.getAllowedSideAccessModes(type));
    }

    private static void changeSideMode(SideConfigMenuAccess menu, SideConfigType type, Direction direction, int button) {
        SideAccessMode mode = button == 1
                ? getPreviousMode(menu, type, direction)
                : getNextMode(menu, type, direction);
        PacketDistributor.sendToServer(new SetSideConfigPayload(menu.getBlockPos(), type.ordinal(), direction.ordinal(), mode.ordinal()));
        GuiWidgets.playButtonClickSound();
    }

    private static void clearSideModes(SideConfigMenuAccess menu, SideConfigType type) {
        for (Direction direction : Direction.values()) {
            if (menu.getSideAccessMode(type, direction) == SideAccessMode.DISABLED) {
                continue;
            }
            PacketDistributor.sendToServer(new SetSideConfigPayload(menu.getBlockPos(), type.ordinal(), direction.ordinal(), SideAccessMode.DISABLED.ordinal()));
        }
        GuiWidgets.playButtonClickSound();
    }

    private static void cycleAllSideModes(SideConfigMenuAccess menu, SideConfigType type) {
        List<SideAccessMode> allowedModes = getAllowedModes(menu, type);
        SideAccessMode sharedMode = getSharedSideMode(menu, type);
        int index = sharedMode == null ? 0 : allowedModes.indexOf(sharedMode);
        if (index < 0) {
            index = 0;
        }

        SideAccessMode targetMode = allowedModes.get((index + 1) % allowedModes.size());
        for (Direction direction : Direction.values()) {
            if (menu.getSideAccessMode(type, direction) == targetMode) {
                continue;
            }
            PacketDistributor.sendToServer(new SetSideConfigPayload(menu.getBlockPos(), type.ordinal(), direction.ordinal(), targetMode.ordinal()));
        }
        GuiWidgets.playButtonClickSound();
    }

    private static SideAccessMode getSharedSideMode(SideConfigMenuAccess menu, SideConfigType type) {
        SideAccessMode sharedMode = null;
        for (Direction direction : Direction.values()) {
            SideAccessMode mode = menu.getSideAccessMode(type, direction);
            if (sharedMode == null) {
                sharedMode = mode;
            } else if (sharedMode != mode) {
                return null;
            }
        }
        return sharedMode;
    }

    private record VisualVector(double x, double y, double z) {
    }

    private record VisualPoint(double x, double y, double z) {
    }

    private record VisualFacePick(Direction direction, double depth) {
    }
}
