package de.artemis.matterworks.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.matterworks.common.menu.slot.GhostItemSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class VanillaGuiHelper {
    private static final int PANEL_OUTER = 0xFF8B8B8B;
    private static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
    private static final int PANEL_SHADOW = 0xFF373737;
    private static final int PANEL_FILL = 0xFFC6C6C6;
    private static final int INSET_SHADOW = 0xFF555555;
    private static final int INSET_HIGHLIGHT = 0xFF8B8B8B;
    private static final int INSET_FILL = 0xFF8B8B8B;
    private static final int SLOT_SHADOW = 0xFF373737;
    private static final int SLOT_HIGHLIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;

    private VanillaGuiHelper() {
    }

    public static void drawScreenBackground(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        guiGraphics.fill(left, top, left + width, top + height, PANEL_OUTER);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, PANEL_HIGHLIGHT);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, PANEL_FILL);
        guiGraphics.fill(left + width - 2, top + 2, left + width - 1, top + height - 1, PANEL_SHADOW);
        guiGraphics.fill(left + 2, top + height - 2, left + width - 2, top + height - 1, PANEL_SHADOW);
    }

    public static void drawInsetPanel(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        guiGraphics.fill(left, top, left + width, top + height, INSET_SHADOW);
        guiGraphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, INSET_HIGHLIGHT);
        guiGraphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, INSET_FILL);
    }

    public static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, SLOT_SHADOW);
        guiGraphics.fill(x + 1, y + 1, x + 18, y + 18, SLOT_HIGHLIGHT);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
        guiGraphics.fill(x + 16, y + 2, x + 17, y + 17, SLOT_SHADOW);
        guiGraphics.fill(x + 2, y + 16, x + 16, y + 17, SLOT_SHADOW);
    }

    public static void drawMenuSlots(GuiGraphics guiGraphics, AbstractContainerMenu menu, int leftPos, int topPos) {
        for (Slot slot : menu.slots) {
            drawSlot(guiGraphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
        drawGhostSlotItems(guiGraphics, menu, leftPos, topPos);
    }

    public static void drawGhostSlotItems(GuiGraphics guiGraphics, AbstractContainerMenu menu, int leftPos, int topPos) {
        for (Slot slot : menu.slots) {
            if (!slot.isActive() || slot.hasItem() || !(slot instanceof GhostItemSlot ghostItemSlot)) {
                continue;
            }

            ItemStack ghostStack = ghostItemSlot.getGhostItemStack();
            if (ghostStack.isEmpty()) {
                continue;
            }

            drawGhostItem(guiGraphics, ghostStack, leftPos + slot.x, topPos + slot.y);
        }
    }

    private static void drawGhostItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F);
        guiGraphics.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    public static void drawVerticalBarFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawInsetPanel(guiGraphics, x, y, width, height);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF1F1F1F);
    }

    public static void drawHorizontalBarFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawInsetPanel(guiGraphics, x, y, width, height);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF1F1F1F);
    }

    public static void fillVerticalBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int filledHeight, int color) {
        if (filledHeight <= 0) {
            return;
        }
        guiGraphics.fill(x, y + height - filledHeight, x + width, y + height, color);
    }

    public static void fillHorizontalBar(GuiGraphics guiGraphics, int x, int y, int filledWidth, int height, int color) {
        if (filledWidth <= 0) {
            return;
        }
        guiGraphics.fill(x, y, x + filledWidth, y + height, color);
    }
}
