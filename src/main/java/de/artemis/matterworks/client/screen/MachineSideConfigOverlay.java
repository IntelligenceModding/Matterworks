package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.SideConfigMenuAccess;
import de.artemis.matterworks.common.network.SetSideConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class MachineSideConfigOverlay {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/side_configuration.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 222;
    private static final RelativeSide[] RELATIVE_SIDES = {
            RelativeSide.UP,
            RelativeSide.FRONT,
            RelativeSide.LEFT,
            RelativeSide.RIGHT,
            RelativeSide.BACK,
            RelativeSide.DOWN
    };

    public void renderBackground(GuiGraphics guiGraphics, int leftPos, int topPos) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, 256, 256);
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
        int[] centerBounds = getCenterBounds(leftPos, topPos);
        renderCenterDisplay(guiGraphics, menu.getPrimaryTabIcon(), centerBounds[0], centerBounds[1], centerBounds[2], centerBounds[3]);
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
                        Component.literal(sideText + ": " + mode.getShortLabel()),
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
        Direction frontFacing = menu.getSideConfigFrontFacing();
        for (RelativeSide side : RELATIVE_SIDES) {
            int[] node = getNodeBounds(leftPos, topPos, side);
            if (isInside(mouseX, mouseY, node[0], node[1], node[2], node[3])) {
                Direction worldSide = resolveWorldDirection(frontFacing, side);
                SideAccessMode targetMode = button == 1
                        ? getPreviousMode(menu, type, worldSide)
                        : getNextMode(menu, type, worldSide);
                GuiWidgets.playButtonClickSound();
                PacketDistributor.sendToServer(new SetSideConfigPayload(menu.getBlockPos(), type.ordinal(), worldSide.ordinal(), targetMode.ordinal()));
                return true;
            }
        }
        return isInside(mouseX, mouseY, leftPos, topPos, TEXTURE_WIDTH, TEXTURE_HEIGHT);
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
                getModeLabel(mode),
                node[0] + node[2] / 2,
                node[1] + (node[3] - font.lineHeight) / 2,
                1.0F,
                0xFFE0E0E0
        );
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
            case BOTH -> 0xFF2E7A73;
        };
    }

    private static int getModeHighlightColor(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> 0xFF777777;
            case INPUT -> 0xFF5B84BA;
            case OUTPUT -> 0xFFBF8241;
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

    private static String getModeLabel(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> "Off";
            case INPUT -> "In";
            case OUTPUT -> "Out";
            case BOTH -> "I/O";
        };
    }

    private static void drawScaledCenteredString(GuiGraphics guiGraphics, Font font, String text, int centerX, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, Math.round((centerX - font.width(text) * scale / 2.0F) / scale), Math.round(y / scale), color, false);
        guiGraphics.pose().popPose();
    }

    private static void renderCenterDisplay(GuiGraphics guiGraphics, ItemStack icon, int x, int y, int width, int height) {
        float scale = Math.min(width / 16.0F, height / 16.0F);
        float renderSize = 16.0F * scale;
        float offsetX = x + (width - renderSize) / 2.0F;
        float offsetY = y + (height - renderSize) / 2.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(offsetX, offsetY, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.renderItem(icon, 0, 0);
        guiGraphics.pose().popPose();
    }

    private enum RelativeSide {
        UP("Up", "U"),
        FRONT("Front", "F"),
        LEFT("Left", "L"),
        RIGHT("Right", "R"),
        BACK("Back", "B"),
        DOWN("Down", "D");

        private final String label;
        private final String shortLabel;

        RelativeSide(String label, String shortLabel) {
            this.label = label;
            this.shortLabel = shortLabel;
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
        List<SideAccessMode> allowedModes = new ArrayList<>();
        allowedModes.add(SideAccessMode.DISABLED);
        if (menu.supportsSideConfigInput(type)) {
            allowedModes.add(SideAccessMode.INPUT);
        }
        if (menu.supportsSideConfigOutput(type)) {
            allowedModes.add(SideAccessMode.OUTPUT);
        }
        if (menu.supportsSideConfigInput(type) && menu.supportsSideConfigOutput(type)) {
            allowedModes.add(SideAccessMode.BOTH);
        }
        return allowedModes;
    }
}
