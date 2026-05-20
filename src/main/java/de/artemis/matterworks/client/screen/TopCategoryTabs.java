package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class TopCategoryTabs {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/top_inventory_tabs.png");
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 26;
    private static final int ACTIVE_TAB_HEIGHT = 30;
    private static final int ACTIVE_TAB_U = 0;
    private static final int ACTIVE_TAB_V = 0;
    private static final int INACTIVE_TAB_U = 29;
    private static final int INACTIVE_TAB_V = 4;
    private static final int TAB_SPACING = 2;
    private static final int EDGE_PADDING = 8;

    private TopCategoryTabs() {
    }

    public static void render(GuiGraphics guiGraphics, int leftPos, int topPos, int contentWidth, int mouseX, int mouseY, List<Tab> tabs) {
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            if (!tab.active()) {
                renderTab(guiGraphics, leftPos, topPos, contentWidth, mouseX, mouseY, index, tab, tabs.size());
            }
        }
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            if (tab.active()) {
                renderTab(guiGraphics, leftPos, topPos, contentWidth, mouseX, mouseY, index, tab, tabs.size());
            }
        }
    }

    public static void renderTooltip(GuiGraphics guiGraphics, Font font, int leftPos, int topPos, int contentWidth, int mouseX, int mouseY, List<Tab> tabs) {
        for (int index = 0; index < tabs.size(); index++) {
            if (isMouseOverTab(leftPos, topPos, contentWidth, mouseX, mouseY, index, tabs.get(index), tabs.size())) {
                guiGraphics.renderTooltip(font, tabs.get(index).tooltip(), mouseX, mouseY);
                return;
            }
        }
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button, int leftPos, int topPos, int contentWidth, List<Tab> tabs) {
        if (button != 0) {
            return false;
        }
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            if (isMouseOverTab(leftPos, topPos, contentWidth, mouseX, mouseY, index, tab, tabs.size())) {
                if (!tab.active()) {
                    GuiWidgets.rememberMousePosition(mouseX, mouseY);
                    tab.onClick().run();
                }
                return true;
            }
        }
        return false;
    }

    public static boolean keyPressed(int keyCode, List<Tab> tabs) {
        if (keyCode != GLFW.GLFW_KEY_LEFT && keyCode != GLFW.GLFW_KEY_RIGHT) {
            return false;
        }
        if (tabs.isEmpty()) {
            return false;
        }

        int activeIndex = getActiveTabIndex(tabs);
        if (activeIndex < 0) {
            return false;
        }

        int targetIndex = activeIndex + (keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
        if (targetIndex < 0 || targetIndex >= tabs.size()) {
            return false;
        }

        Tab target = tabs.get(targetIndex);
        if (target.active()) {
            return false;
        }

        GuiWidgets.rememberCurrentMousePosition();
        target.onClick().run();
        return true;
    }

    private static void renderTab(GuiGraphics guiGraphics, int leftPos, int topPos, int contentWidth, int mouseX, int mouseY, int index, Tab tab, int tabCount) {
        int x = getTabX(leftPos, contentWidth, index, tabCount);
        int y = getTabY(topPos, tab);
        int height = tab.active() ? ACTIVE_TAB_HEIGHT : TAB_HEIGHT;
        boolean hovered = isMouseOverTab(leftPos, topPos, contentWidth, mouseX, mouseY, index, tab, tabCount);

        int u = tab.active() ? ACTIVE_TAB_U : INACTIVE_TAB_U;
        int v = tab.active() ? ACTIVE_TAB_V : INACTIVE_TAB_V;
        guiGraphics.blit(TEXTURE, x, y, u, v, TAB_WIDTH, height, 256, 256);
        if (hovered && !tab.active()) {
            guiGraphics.fill(x, y, x + TAB_WIDTH, y + height, 0x18FFFFFF);
        }

        ItemStack icon = tab.icon();
        int iconX = x + Math.max(0, (TAB_WIDTH - 16) / 2);
        int iconY = y + (height - 16) / 2 - (tab.active() ? 1 : 0);
        guiGraphics.renderItem(icon, iconX, iconY);
    }

    private static boolean isMouseOverTab(int leftPos, int topPos, int contentWidth, double mouseX, double mouseY, int index, Tab tab, int tabCount) {
        int x = getTabX(leftPos, contentWidth, index, tabCount);
        int y = getTabY(topPos, tab);
        int height = tab.active() ? ACTIVE_TAB_HEIGHT : TAB_HEIGHT;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + height;
    }

    private static int getTabX(int leftPos, int contentWidth, int index, int tabCount) {
        int totalWidth = tabCount * TAB_WIDTH + Math.max(0, tabCount - 1) * TAB_SPACING;
        int offset = Math.max(EDGE_PADDING, (contentWidth - totalWidth) / 2);
        return leftPos + offset + index * (TAB_WIDTH + TAB_SPACING);
    }

    private static int getTabY(int topPos, Tab tab) {
        return topPos - (tab.active() ? ACTIVE_TAB_HEIGHT - 2 : TAB_HEIGHT);
    }

    private static int getActiveTabIndex(List<Tab> tabs) {
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).active()) {
                return index;
            }
        }
        return -1;
    }

    public record Tab(ItemStack icon, Component tooltip, boolean active, Runnable onClick) {
    }
}
