package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.SideConfigMenuAccess;
import de.artemis.matterworks.common.network.SetSideConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class MachineSideConfigOverlay {
    private static final int PANEL_MARGIN_X = 10;
    private static final int PANEL_MARGIN_Y = 18;
    private static final int NODE_SIZE = 22;
    private static final int CORE_SIZE = 38;
    private static final int LEGEND_WIDTH = 56;
    private static final RelativeSide[] RELATIVE_SIDES = {
            RelativeSide.UP,
            RelativeSide.FRONT,
            RelativeSide.LEFT,
            RelativeSide.RIGHT,
            RelativeSide.BACK,
            RelativeSide.DOWN
    };

    public <T extends SideConfigMenuAccess> void render(
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
        int panelLeft = leftPos + PANEL_MARGIN_X;
        int panelTop = topPos + PANEL_MARGIN_Y;
        int panelWidth = imageWidth - PANEL_MARGIN_X * 2;
        int panelHeight = imageHeight - PANEL_MARGIN_Y - 10;
        int centerX = panelLeft + (panelWidth - LEGEND_WIDTH) / 2 - 6;
        int centerY = panelTop + panelHeight / 2 + 6;
        Direction frontFacing = menu.getSideConfigFrontFacing();

        VanillaGuiHelper.drawInsetPanel(guiGraphics, panelLeft, panelTop, panelWidth, panelHeight);
        guiGraphics.fill(panelLeft + 5, panelTop + 5, panelLeft + panelWidth - 5, panelTop + 19, getTypeAccent(type));
        guiGraphics.drawString(font, Component.literal(type.getLabel() + " Side Configuration"), panelLeft + 10, panelTop + 9, 0xFF202020, false);
        guiGraphics.drawString(font, Component.literal("Front: " + getDirectionName(frontFacing)), panelLeft + 10, panelTop + 24, 0xFF404040, false);
        guiGraphics.drawString(font, Component.literal("Click a side to cycle"), panelLeft + 10, panelTop + 35, 0xFF505050, false);

        renderCore(guiGraphics, menu.getPrimaryTabIcon(), centerX, centerY, type);
        renderConnectors(guiGraphics, centerX, centerY);
        for (RelativeSide side : RELATIVE_SIDES) {
            renderSideNode(guiGraphics, font, menu, type, side, frontFacing, centerX, centerY, mouseX, mouseY);
        }
        renderLegend(guiGraphics, font, panelLeft + panelWidth - LEGEND_WIDTH + 4, panelTop + 48);
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
        int panelLeft = leftPos + PANEL_MARGIN_X;
        int panelTop = topPos + PANEL_MARGIN_Y;
        int panelWidth = imageWidth - PANEL_MARGIN_X * 2;
        int panelHeight = imageHeight - PANEL_MARGIN_Y - 10;
        int centerX = panelLeft + (panelWidth - LEGEND_WIDTH) / 2 - 6;
        int centerY = panelTop + panelHeight / 2 + 6;
        Direction frontFacing = menu.getSideConfigFrontFacing();
        for (RelativeSide side : RELATIVE_SIDES) {
            int[] node = getNodeBounds(centerX, centerY, side);
            if (isInside(mouseX, mouseY, node[0], node[1], NODE_SIZE, NODE_SIZE)) {
                Direction worldSide = resolveWorldDirection(frontFacing, side);
                SideAccessMode mode = menu.getSideAccessMode(type, worldSide);
                guiGraphics.renderTooltip(
                        font,
                        Component.literal(side.label + " (" + getDirectionName(worldSide) + "): " + mode.getShortLabel()),
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
        if (button != 0) {
            return false;
        }
        int panelLeft = leftPos + PANEL_MARGIN_X;
        int panelTop = topPos + PANEL_MARGIN_Y;
        int panelWidth = imageWidth - PANEL_MARGIN_X * 2;
        int panelHeight = imageHeight - PANEL_MARGIN_Y - 10;
        int centerX = panelLeft + (panelWidth - LEGEND_WIDTH) / 2 - 6;
        int centerY = panelTop + panelHeight / 2 + 6;
        Direction frontFacing = menu.getSideConfigFrontFacing();
        for (RelativeSide side : RELATIVE_SIDES) {
            int[] node = getNodeBounds(centerX, centerY, side);
            if (isInside(mouseX, mouseY, node[0], node[1], NODE_SIZE, NODE_SIZE)) {
                Direction worldSide = resolveWorldDirection(frontFacing, side);
                SideAccessMode nextMode = getNextMode(menu, type, worldSide);
                PacketDistributor.sendToServer(new SetSideConfigPayload(menu.getBlockPos(), type.ordinal(), worldSide.ordinal(), nextMode.ordinal()));
                return true;
            }
        }
        return isInside(mouseX, mouseY, panelLeft, panelTop, panelWidth, panelHeight);
    }

    private void renderCore(GuiGraphics guiGraphics, ItemStack icon, int centerX, int centerY, SideConfigType type) {
        int left = centerX - CORE_SIZE / 2;
        int top = centerY - CORE_SIZE / 2;
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left, top, CORE_SIZE, CORE_SIZE);
        guiGraphics.fill(left + 4, top + 4, left + CORE_SIZE - 4, top + CORE_SIZE - 4, getTypeAccent(type));
        guiGraphics.renderItem(icon, centerX - 8, centerY - 8);
    }

    private void renderConnectors(GuiGraphics guiGraphics, int centerX, int centerY) {
        guiGraphics.fill(centerX - 2, centerY - 42, centerX + 2, centerY - 18, 0xFF6A6A6A);
        guiGraphics.fill(centerX - 2, centerY + 18, centerX + 2, centerY + 42, 0xFF6A6A6A);
        guiGraphics.fill(centerX - 42, centerY - 2, centerX - 18, centerY + 2, 0xFF6A6A6A);
        guiGraphics.fill(centerX + 18, centerY - 2, centerX + 42, centerY + 2, 0xFF6A6A6A);
        guiGraphics.fill(centerX - 33, centerY - 33, centerX - 18, centerY - 18, 0xFF6A6A6A);
        guiGraphics.fill(centerX + 18, centerY + 18, centerX + 33, centerY + 33, 0xFF6A6A6A);
    }

    private <T extends SideConfigMenuAccess> void renderSideNode(
            GuiGraphics guiGraphics,
            Font font,
            T menu,
            SideConfigType type,
            RelativeSide side,
            Direction frontFacing,
            int centerX,
            int centerY,
            int mouseX,
            int mouseY
    ) {
        int[] node = getNodeBounds(centerX, centerY, side);
        Direction worldSide = resolveWorldDirection(frontFacing, side);
        SideAccessMode mode = menu.getSideAccessMode(type, worldSide);
        boolean hovered = isInside(mouseX, mouseY, node[0], node[1], NODE_SIZE, NODE_SIZE);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, node[0], node[1], NODE_SIZE, NODE_SIZE);
        guiGraphics.fill(node[0] + 3, node[1] + 3, node[0] + NODE_SIZE - 3, node[1] + NODE_SIZE - 3, getModeColor(mode));
        if (hovered) {
            guiGraphics.fill(node[0] + 1, node[1] + 1, node[0] + NODE_SIZE - 1, node[1] + 2, 0xFFFFFFFF);
        }
        guiGraphics.drawCenteredString(font, side.shortLabel, node[0] + NODE_SIZE / 2, node[1] + 4, 0xFF202020);
        guiGraphics.drawCenteredString(font, mode.getShortLabel(), node[0] + NODE_SIZE / 2, node[1] + 13, 0xFF202020);
    }

    private void renderLegend(GuiGraphics guiGraphics, Font font, int left, int top) {
        List<SideAccessMode> modes = new ArrayList<>(List.of(SideAccessMode.DISABLED, SideAccessMode.INPUT, SideAccessMode.OUTPUT, SideAccessMode.BOTH));
        guiGraphics.drawString(font, Component.literal("Mode"), left, top - 14, 0xFF404040, false);
        for (int index = 0; index < modes.size(); index++) {
            SideAccessMode mode = modes.get(index);
            int y = top + index * 16;
            VanillaGuiHelper.drawInsetPanel(guiGraphics, left, y, 10, 10);
            guiGraphics.fill(left + 2, y + 2, left + 8, y + 8, getModeColor(mode));
            guiGraphics.drawString(font, mode.getShortLabel(), left + 14, y + 1, 0xFF404040, false);
        }
    }

    private static int[] getNodeBounds(int centerX, int centerY, RelativeSide side) {
        return switch (side) {
            case UP -> new int[]{centerX - NODE_SIZE / 2, centerY - 52};
            case DOWN -> new int[]{centerX - NODE_SIZE / 2, centerY + 30};
            case LEFT -> new int[]{centerX - 52, centerY - NODE_SIZE / 2};
            case RIGHT -> new int[]{centerX + 30, centerY - NODE_SIZE / 2};
            case FRONT -> new int[]{centerX - 46, centerY - 46};
            case BACK -> new int[]{centerX + 24, centerY + 24};
        };
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int getTypeAccent(SideConfigType type) {
        return switch (type) {
            case ITEMS -> 0xFFD9C07A;
            case FLUIDS -> 0xFF86B9E8;
            case ENERGY -> 0xFFE8A186;
        };
    }

    private static int getModeColor(SideAccessMode mode) {
        return switch (mode) {
            case DISABLED -> 0xFF8A8A8A;
            case INPUT -> 0xFF6D9ED6;
            case OUTPUT -> 0xFFD6A45C;
            case BOTH -> 0xFF7FB06D;
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

        SideAccessMode current = menu.getSideAccessMode(type, direction);
        int index = allowedModes.indexOf(current);
        if (index < 0) {
            return allowedModes.get(0);
        }
        return allowedModes.get((index + 1) % allowedModes.size());
    }
}
