package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.menu.NamedBlockMenu;
import de.artemis.matterworks.common.network.SetBlockCustomNamePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractRenamableContainerScreen<T extends AbstractContainerMenu & NamedBlockMenu> extends AbstractContainerScreen<T> {
    private static final int MAX_NAME_LENGTH = 64;
    private static final String TITLE_TRUNCATION_SUFFIX = "(...)";

    private EditBox nameEditBox;
    private boolean editingName;
    private String editingOriginalName = "";

    protected AbstractRenamableContainerScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        nameEditBox = GuiWidgets.textField(font, 0, 0, getEditableTitleWidth(), 14, Component.empty());
        nameEditBox.setMaxLength(MAX_NAME_LENGTH);
        nameEditBox.setVisible(false);
        refreshNameEditBoxBounds();
        addRenderableWidget(nameEditBox);
        GuiWidgets.restoreRememberedMousePosition();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshNameEditBoxBounds();
        if (!editingName && nameEditBox != null) {
            nameEditBox.setValue(menu.getBlockDisplayName());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, getEditableTitleX(), getEditableTitleY(), getEditableTitleColor());
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editingName && nameEditBox != null && !nameEditBox.isMouseOver(mouseX, mouseY)) {
            finishNameEdit(true);
        }
        if (!editingName && button == 0 && isWithinTitleBounds(mouseX, mouseY)) {
            beginNameEdit();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingName && nameEditBox != null && nameEditBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                finishNameEdit(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                finishNameEdit(false);
                return true;
            }
            if (nameEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return isTypingKey(keyCode);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (editingName && nameEditBox != null && nameEditBox.isFocused()) {
            return nameEditBox.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        finishNameEdit(true);
        super.onClose();
    }

    protected void renderEditableTitle(GuiGraphics guiGraphics, int x, int y, int color) {
        if (!editingName) {
            guiGraphics.drawString(font, getDisplayedTitleText(), x, y, color, false);
        }
    }

    protected int getEditableTitleX() {
        return titleLabelX;
    }

    protected int getEditableTitleY() {
        return titleLabelY;
    }

    protected int getEditableTitleWidth() {
        return Math.max(24, getEditableTitleRightEdge() - getEditableTitleX());
    }

    protected int getEditableTitleRightEdge() {
        return imageWidth - 8;
    }

    protected int getEditableTitleColor() {
        return 0x404040;
    }

    protected boolean isEditingName() {
        return editingName;
    }

    protected void refreshNameEditBoxBounds() {
        if (nameEditBox != null) {
            nameEditBox.setPosition(leftPos + getEditableTitleX() - 4, topPos + getEditableTitleY() - 2);
            nameEditBox.setWidth(getEditableTitleWidth());
        }
    }

    private void beginNameEdit() {
        if (nameEditBox == null) {
            return;
        }
        editingName = true;
        editingOriginalName = menu.getBlockDisplayName();
        nameEditBox.setValue(editingOriginalName);
        nameEditBox.setVisible(true);
        setFocused(nameEditBox);
        nameEditBox.setFocused(true);
        nameEditBox.setHighlightPos(0);
        nameEditBox.setCursorPosition(editingOriginalName.length());
    }

    private void finishNameEdit(boolean submit) {
        if (!editingName || nameEditBox == null) {
            return;
        }
        if (submit) {
            String normalized = normalizeName(nameEditBox.getValue());
            if (!normalized.equals(editingOriginalName)) {
                PacketDistributor.sendToServer(new SetBlockCustomNamePayload(menu.getBlockPos(), normalized));
            }
        }
        editingName = false;
        nameEditBox.setFocused(false);
        nameEditBox.setVisible(false);
        setFocused(null);
    }

    private boolean isWithinTitleBounds(double mouseX, double mouseY) {
        int x = leftPos + getEditableTitleX();
        int y = topPos + getEditableTitleY();
        int width = font.width(getDisplayedTitleText());
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 10;
    }

    protected String getDisplayedTitleText() {
        String title = menu.getBlockDisplayName();
        int maxWidth = getEditableTitleWidth();
        if (font.width(title) <= maxWidth) {
            return title;
        }

        int suffixWidth = font.width(TITLE_TRUNCATION_SUFFIX);
        if (suffixWidth >= maxWidth) {
            return font.plainSubstrByWidth(TITLE_TRUNCATION_SUFFIX, maxWidth);
        }

        return font.plainSubstrByWidth(title, maxWidth - suffixWidth) + TITLE_TRUNCATION_SUFFIX;
    }

    private static String normalizeName(String value) {
        String normalized = value.strip();
        return normalized.length() > MAX_NAME_LENGTH ? normalized.substring(0, MAX_NAME_LENGTH) : normalized;
    }

    private static boolean isTypingKey(int keyCode) {
        return keyCode != GLFW.GLFW_KEY_ENTER
                && keyCode != GLFW.GLFW_KEY_KP_ENTER
                && keyCode != GLFW.GLFW_KEY_ESCAPE;
    }
}
