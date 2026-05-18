package de.artemis.matterworks.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TopCategoryTabs {
    private static final int MAX_TAB_WIDTH = 28;
    private static final int MIN_TAB_WIDTH = 22;
    private static final int TAB_HEIGHT = 26;
    private static final int ACTIVE_TAB_HEIGHT = 30;
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
                    tab.onClick().run();
                }
                return true;
            }
        }
        return false;
    }

    private static void renderTab(GuiGraphics guiGraphics, int leftPos, int topPos, int contentWidth, int mouseX, int mouseY, int index, Tab tab, int tabCount) {
        int tabWidth = getTabWidth(contentWidth, tabCount);
        int x = getTabX(leftPos, contentWidth, index, tabCount);
        int y = getTabY(topPos, tab);
        int height = tab.active() ? ACTIVE_TAB_HEIGHT : TAB_HEIGHT;
        boolean hovered = isMouseOverTab(leftPos, topPos, contentWidth, mouseX, mouseY, index, tab, tabCount);

        int borderColor = tab.active() ? 0xFF8B8B8B : hovered ? 0xFF8B8B8B : 0xFF555555;
        int fillColor = tab.active() ? 0xFFC6C6C6 : hovered ? 0xFFB8B8B8 : 0xFFA0A0A0;
        int highlight = tab.active() ? 0xFFFFFFFF : 0xFFE6E6E6;
        int shadow = 0xFF373737;

        guiGraphics.fill(x, y, x + tabWidth, y + height, borderColor);
        guiGraphics.fill(x + 1, y + 1, x + tabWidth - 1, y + height - 1, fillColor);
        guiGraphics.fill(x + 1, y + 1, x + tabWidth - 1, y + 2, highlight);
        guiGraphics.fill(x + 1, y + 1, x + 2, y + height - 1, highlight);
        guiGraphics.fill(x + tabWidth - 2, y + 2, x + tabWidth - 1, y + height - 1, shadow);
        guiGraphics.fill(x + 2, y + height - 2, x + tabWidth - 2, y + height - 1, shadow);
        if (tab.active()) {
            guiGraphics.fill(x + 2, topPos, x + tabWidth - 2, topPos + 2, fillColor);
        }

        ItemStack icon = tab.icon();
        int iconX = x + Math.max(0, (tabWidth - 16) / 2);
        int iconY = y + (height - 16) / 2 - (tab.active() ? 1 : 0);
        guiGraphics.renderItem(icon, iconX, iconY);
    }

    private static boolean isMouseOverTab(int leftPos, int topPos, int contentWidth, double mouseX, double mouseY, int index, Tab tab, int tabCount) {
        int tabWidth = getTabWidth(contentWidth, tabCount);
        int x = getTabX(leftPos, contentWidth, index, tabCount);
        int y = getTabY(topPos, tab);
        int height = tab.active() ? ACTIVE_TAB_HEIGHT : TAB_HEIGHT;
        return mouseX >= x && mouseX < x + tabWidth && mouseY >= y && mouseY < y + height;
    }

    private static int getTabX(int leftPos, int contentWidth, int index, int tabCount) {
        int tabWidth = getTabWidth(contentWidth, tabCount);
        int totalWidth = tabCount * tabWidth + Math.max(0, tabCount - 1) * TAB_SPACING;
        int offset = Math.max(EDGE_PADDING, (contentWidth - totalWidth) / 2);
        return leftPos + offset + index * (tabWidth + TAB_SPACING);
    }

    private static int getTabWidth(int contentWidth, int tabCount) {
        if (tabCount <= 0) {
            return MAX_TAB_WIDTH;
        }
        int availableWidth = Math.max(MIN_TAB_WIDTH, contentWidth - EDGE_PADDING * 2 - Math.max(0, tabCount - 1) * TAB_SPACING);
        return Math.max(MIN_TAB_WIDTH, Math.min(MAX_TAB_WIDTH, availableWidth / tabCount));
    }

    private static int getTabY(int topPos, Tab tab) {
        return topPos - (tab.active() ? ACTIVE_TAB_HEIGHT - 2 : TAB_HEIGHT - 2);
    }

    public record Tab(ItemStack icon, Component tooltip, boolean active, Runnable onClick) {
    }
}
