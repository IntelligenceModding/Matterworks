package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.SingularityLinkMenu;
import de.artemis.matterworks.common.network.SetPylonColorCodePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

public class SingularityLinkScreen extends AbstractRenamableContainerScreen<SingularityLinkMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/generic_small.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int NETWORK_CODE_CENTER_X = 88;
    private static final int NETWORK_CODE_LABEL_Y = 45;
    private static final int STATUS_LABEL_X = 8;
    private static final int STATUS_FORMED_Y = 18;
    private static final int STATUS_LINKED_Y = 30;

    private final NetworkColorPickerOverlay colorPicker = new NetworkColorPickerOverlay(new int[]{53, 80, 107}, new int[]{56, 56, 56});

    public SingularityLinkScreen(SingularityLinkMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        colorPicker.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, menu::getNetworkColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, titleLabelX, titleLabelY, 0x404040);
        drawCenteredLabel(guiGraphics, Component.literal("Code"), NETWORK_CODE_CENTER_X, NETWORK_CODE_LABEL_Y);
        guiGraphics.drawString(font, menu.isStructureFormed()
                ? Component.translatable("screen.matterworks.singularity_link.formed")
                : Component.translatable("screen.matterworks.singularity_link.incomplete"), STATUS_LABEL_X, STATUS_FORMED_Y, 0x404040, false);
        guiGraphics.drawString(font, menu.hasLinkedPartner()
                ? Component.translatable("screen.matterworks.singularity_link.linked")
                : Component.translatable("screen.matterworks.singularity_link.waiting"), STATUS_LABEL_X, STATUS_LINKED_Y, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        colorPicker.renderTooltip(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY, menu::getNetworkColor);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (colorPicker.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight, this::setNetworkColor, menu::getNetworkColor)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (colorPicker.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, imageHeight, this::setNetworkColor, menu::getNetworkColor)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void setNetworkColor(int index, DyeColor color) {
        PacketDistributor.sendToServer(new SetPylonColorCodePayload(menu.getBlockPos(), MatterPylonBlockEntity.CHANNEL_ENERGY, index, color.getId()));
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component text, int centerX, int y) {
        int textWidth = font.width(text);
        guiGraphics.drawString(font, text, centerX - textWidth / 2, y, 0x404040, false);
    }
}
