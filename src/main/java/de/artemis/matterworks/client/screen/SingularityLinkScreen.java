package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.blockentity.SingularityLinkSavedData;
import de.artemis.matterworks.common.menu.SingularityLinkMenu;
import de.artemis.matterworks.common.network.SetPylonColorCodePayload;
import de.artemis.matterworks.common.network.SingularityLinkActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class SingularityLinkScreen extends AbstractRenamableContainerScreen<SingularityLinkMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/singularity_link.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int INFO_LEFT = 9;
    private static final int INFO_TOP = 19;
    private static final int INFO_WIDTH = 46;
    private static final int INFO_HEIGHT = 49;
    private static final float INFO_TEXT_SCALE = 0.75F;
    private static final int LOCATE_BUTTON_X = 7;
    private static final int LOCATE_BUTTON_Y = 74;
    private static final int CONNECT_BUTTON_X = 90;
    private static final int CONNECT_BUTTON_Y = 74;
    private static final int BUTTON_WIDTH = 79;
    private static final int BUTTON_HEIGHT = 18;
    private static final int ENERGY_TANK_X = 119;
    private static final int ENERGY_TANK_Y = 18;
    private static final int ENERGY_TANK_WIDTH = 48;
    private static final int ENERGY_TANK_HEIGHT = 51;
    private final NetworkColorPickerOverlay colorPicker = new NetworkColorPickerOverlay(new int[]{62, 80, 98}, new int[]{54, 54, 54});
    private Button locateButton;
    private Button connectButton;

    public SingularityLinkScreen(SingularityLinkMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 188;
        inventoryLabelX = 8;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        locateButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + LOCATE_BUTTON_X, topPos + LOCATE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Locate"), button ->
                PacketDistributor.sendToServer(new SingularityLinkActionPayload(menu.getBlockPos(), SingularityLinkActionPayload.ACTION_LOCATE))));
        connectButton = addRenderableWidget(GuiWidgets.panelButton(leftPos + CONNECT_BUTTON_X, topPos + CONNECT_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, getConnectButtonLabel(), button ->
                PacketDistributor.sendToServer(new SingularityLinkActionPayload(menu.getBlockPos(), menu.hasLinkedPartner()
                        ? SingularityLinkActionPayload.ACTION_DISCONNECT
                        : SingularityLinkActionPayload.ACTION_CONNECT))));
        updateButtonStates();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateButtonStates();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        colorPicker.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, menu::getNetworkColor);
        VanillaGuiHelper.drawGhostSlotItems(guiGraphics, menu, leftPos, topPos);
        GuiWidgets.fillVerticalEnergyGauge(
                guiGraphics,
                leftPos + ENERGY_TANK_X,
                topPos + ENERGY_TANK_Y,
                ENERGY_TANK_WIDTH,
                ENERGY_TANK_HEIGHT,
                menu.getScaledEnergyAmount(ENERGY_TANK_HEIGHT)
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, titleLabelX, titleLabelY, 0x404040);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        int y = INFO_TOP;
        int maxY = INFO_TOP + INFO_HEIGHT - Mth.ceil(font.lineHeight * INFO_TEXT_SCALE);
        guiGraphics.enableScissor(leftPos + INFO_LEFT, topPos + INFO_TOP, leftPos + INFO_LEFT + INFO_WIDTH, topPos + INFO_TOP + INFO_HEIGHT);
        for (Component line : buildInfoLines()) {
            if (y > maxY) {
                break;
            }
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE, 1.0F);
            guiGraphics.drawString(font, fitInfoLine(line), Mth.floor(INFO_LEFT / INFO_TEXT_SCALE), Mth.floor(y / INFO_TEXT_SCALE), 0x404040, false);
            guiGraphics.pose().popPose();
            y += Mth.ceil((font.lineHeight + 1) * INFO_TEXT_SCALE);
        }
        guiGraphics.disableScissor();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        colorPicker.renderTooltip(guiGraphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY, menu::getNetworkColor);
        renderEnergyTooltip(guiGraphics, mouseX, mouseY);
        renderConnectTooltip(guiGraphics, mouseX, mouseY);
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

    private void updateButtonStates() {
        if (locateButton == null || connectButton == null) {
            return;
        }

        SingularityLinkSavedData.ConnectAvailability availability = menu.getConnectAvailability();
        locateButton.active = menu.hasLinkedPartner();
        connectButton.setMessage(getConnectButtonLabel());
        connectButton.active = availability == SingularityLinkSavedData.ConnectAvailability.AVAILABLE
                || availability == SingularityLinkSavedData.ConnectAvailability.LINKED;
    }

    private Component getConnectButtonLabel() {
        return Component.literal(menu.hasLinkedPartner() ? "Disconnect" : "Connect");
    }

    private List<Component> buildInfoLines() {
        SingularityLinkSavedData.ConnectAvailability availability = menu.getConnectAvailability();
        return List.of(
                Component.literal(menu.isStructureFormed() ? "Frame: Formed" : "Frame: Incomplete"),
                Component.literal(menu.hasInsertedSingularity() ? "Core: Ready" : "Core: Missing"),
                Component.literal("Link: " + getStatusText(availability)),
                Component.literal("Flow: " + (menu.getBlockEntity().hasActiveSingularityTransfer() ? "Active" : "Idle")),
                Component.literal("FE: " + formatCompact(menu.getEnergyStored()) + "/" + formatCompact(menu.getEnergyCapacity()))
        );
    }

    private String getStatusText(SingularityLinkSavedData.ConnectAvailability availability) {
        return switch (availability) {
            case LINKED -> "Linked";
            case AVAILABLE -> "Ready";
            case PAIR_OCCUPIED -> "Pair Full";
            case MULTIPLE_MATCHES -> "Ambiguous";
            case NO_MATCH -> "No Match";
            case UNAVAILABLE -> menu.isStructureFormed() ? "Offline" : "Invalid";
        };
    }

    private void renderEnergyTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = leftPos + ENERGY_TANK_X;
        int y = topPos + ENERGY_TANK_Y;
        if (mouseX >= x && mouseX < x + ENERGY_TANK_WIDTH && mouseY >= y && mouseY < y + ENERGY_TANK_HEIGHT) {
            guiGraphics.renderTooltip(font, Component.literal(menu.getEnergyStored() + " / " + menu.getEnergyCapacity() + " FE"), mouseX, mouseY);
        }
    }

    private void renderConnectTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (connectButton == null || connectButton.active || !connectButton.isHovered()) {
            return;
        }

        Component tooltip = switch (menu.getConnectAvailability()) {
            case UNAVAILABLE -> Component.literal("Build the frame and insert a singularity");
            case NO_MATCH -> Component.literal("No matching singularity link was found");
            case PAIR_OCCUPIED -> Component.literal("That color code already has a linked pair");
            case MULTIPLE_MATCHES -> Component.literal("More than one matching link was found");
            case LINKED, AVAILABLE -> null;
        };
        if (tooltip != null) {
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private void setNetworkColor(int index, DyeColor color) {
        PacketDistributor.sendToServer(new SetPylonColorCodePayload(menu.getBlockPos(), MatterPylonBlockEntity.CHANNEL_ENERGY, index, color.getId()));
    }

    private Component fitInfoLine(Component line) {
        String text = line.getString();
        int maxWidth = Math.max(1, Mth.floor(INFO_WIDTH / INFO_TEXT_SCALE) - 1);
        if (font.width(text) <= maxWidth) {
            return line;
        }

        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return Component.literal(font.plainSubstrByWidth(text, maxWidth));
        }

        String trimmed = font.plainSubstrByWidth(text, maxWidth - ellipsisWidth);
        return Component.literal(trimmed + ellipsis);
    }

    private static String formatCompact(int value) {
        if (value >= 1_000_000) {
            return (value / 1_000_000) + "M";
        }
        if (value >= 1_000) {
            return (value / 1_000) + "k";
        }
        return Integer.toString(Math.max(0, value));
    }
}
