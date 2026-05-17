package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.network.SetPylonIdPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MatterPylonScreen extends AbstractContainerScreen<MatterPylonMenu> {
    private static final int ID_SUBMIT_DEBOUNCE_TICKS = 8;

    private EditBox idBox;
    private Button modeButton;
    private final List<Button> channelButtons = new ArrayList<>();
    private String lastSubmittedId = "";
    private int pendingSubmitTicks = -1;
    private boolean userEditedIdBox;
    private boolean suppressIdResponder;
    private int selectedChannel = -1;

    public MatterPylonScreen(MatterPylonMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        if (selectedChannel < 0 || !menu.supportsChannel(selectedChannel)) {
            selectedChannel = menu.getFirstSupportedChannel();
        }
        this.idBox = new EditBox(this.font, this.leftPos + 16, this.topPos + 36, 58, 18, Component.translatable("screen.matterworks.matter_pylon.id"));
        this.idBox.setMaxLength(10);
        this.idBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        setIdBoxValue(menu.hasSyncedState(selectedChannel) ? Integer.toString(menu.getPylonId(selectedChannel)) : "");
        this.lastSubmittedId = this.idBox.getValue();
        this.idBox.setResponder(value -> {
            if (suppressIdResponder) {
                return;
            }
            userEditedIdBox = true;
            pendingSubmitTicks = value.isEmpty() ? -1 : ID_SUBMIT_DEBOUNCE_TICKS;
        });
        this.addRenderableWidget(this.idBox);

        this.channelButtons.clear();
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            final int channelIndex = channel;
            Button channelButton = this.addRenderableWidget(Button.builder(getChannelButtonLabel(channel), button -> switchChannel(channelIndex))
                    .bounds(this.leftPos + 16 + channel * 36, this.topPos + 60, 32, 20)
                    .build());
            this.channelButtons.add(channelButton);
        }

        this.modeButton = this.addRenderableWidget(Button.builder(getModeButtonLabel(), button -> {
            Minecraft minecraft = this.minecraft;
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selectedChannel);
            }
        }).bounds(this.leftPos + 88, this.topPos + 34, 72, 20).build());
        this.modeButton.active = menu.hasSyncedState(selectedChannel);
        refreshChannelWidgets();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (modeButton != null) {
            modeButton.setMessage(getModeButtonLabel());
            modeButton.active = menu.hasSyncedState(selectedChannel);
        }
        refreshChannelWidgets();
        if (idBox != null) {
            if (!menu.hasSyncedState(selectedChannel)) {
                return;
            }
            if (!userEditedIdBox) {
                String syncedValue = Integer.toString(menu.getPylonId(selectedChannel));
                if (!idBox.getValue().equals(syncedValue)) {
                    setIdBoxValue(syncedValue);
                    lastSubmittedId = syncedValue;
                }
            } else if (idBox.isFocused()) {
                if (pendingSubmitTicks >= 0) {
                    pendingSubmitTicks--;
                    if (pendingSubmitTicks <= 0) {
                        submitId(false);
                    }
                }
            } else {
                pendingSubmitTicks = -1;
                String syncedValue = Integer.toString(menu.getPylonId(selectedChannel));
                if (!idBox.getValue().equals(syncedValue)) {
                    setIdBoxValue(syncedValue);
                }
                userEditedIdBox = false;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (idBox != null && idBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            submitId(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        submitId(true);
        super.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xFF2B2B31);
        guiGraphics.fill(left + 2, top + 2, left + this.imageWidth - 2, top + this.imageHeight - 2, 0xFF3A3A42);
        guiGraphics.fill(left + 14, top + 34, left + 76, top + 56, 0xFF1B1B1F);
        guiGraphics.fill(left + 7, top + 84, left + 61, top + 124, 0xFF1B1B1F);
        guiGraphics.fill(left + 79, top + 84, left + 133, top + 124, 0xFF1B1B1F);
        guiGraphics.fill(left + 7, top + 130, left + 169, top + 184, 0xFF1B1B1F);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xE0E0E0, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.id"), 16, 20, 0xC5C7CC, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.mode"), 88, 20, 0xC5C7CC, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.channel"), 16, 48, 0xC5C7CC, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.item_filter"), 16, 74, menu.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_ITEMS) ? 0xC5C7CC : 0x707078, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.import_short"), 16, 84, 0x9FB4D8, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 16, 94, 0xA8D5A2, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 34, 94, 0xD8A0A0, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.export_short"), 16, 102, 0xD8C29F, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 16, 112, 0xA8D5A2, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 34, 112, 0xD8A0A0, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.fluid_filter"), 88, 74, menu.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_FLUIDS) ? 0xC5C7CC : 0x707078, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.import_short"), 88, 84, 0x9FB4D8, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 88, 94, 0xA8D5A2, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 106, 94, 0xD8A0A0, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.export_short"), 88, 102, 0xD8C29F, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 88, 112, 0xA8D5A2, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 106, 112, 0xD8A0A0, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 120, 0xC5C7CC, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.link_hint"), 16, 204, 0x8B8B93, false);
    }

    private Component getModeButtonLabel() {
        return menu.hasSyncedState(selectedChannel) ? Component.translatable(menu.getMode(selectedChannel).translationKey()) : Component.literal("...");
    }

    private void submitId(boolean normalizeField) {
        if (idBox == null) {
            return;
        }

        String rawValue = idBox.getValue().trim();
        if (rawValue.isEmpty()) {
            return;
        }

        int parsed = Mth.clamp(parseIntSafely(rawValue), MatterPylonBlockEntity.DEFAULT_PYLON_ID, Integer.MAX_VALUE);
        String normalized = Integer.toString(parsed);
        pendingSubmitTicks = -1;

        if (normalizeField) {
            setIdBoxValue(normalized);
        }

        if (normalized.equals(lastSubmittedId)) {
            return;
        }

        PacketDistributor.sendToServer(new SetPylonIdPayload(menu.getBlockPos(), selectedChannel, parsed));
        lastSubmittedId = normalized;
        userEditedIdBox = false;
    }

    private void switchChannel(int channel) {
        if (channel == selectedChannel) {
            return;
        }

        submitId(true);
        selectedChannel = channel;
        pendingSubmitTicks = -1;
        userEditedIdBox = false;

        if (menu.hasSyncedState(selectedChannel)) {
            String syncedValue = Integer.toString(menu.getPylonId(selectedChannel));
            setIdBoxValue(syncedValue);
            lastSubmittedId = syncedValue;
        } else {
            setIdBoxValue("");
            lastSubmittedId = "";
        }

        if (modeButton != null) {
            modeButton.setMessage(getModeButtonLabel());
            modeButton.active = menu.hasSyncedState(selectedChannel);
        }
        refreshChannelWidgets();
    }

    private void refreshChannelWidgets() {
        for (int channel = 0; channel < channelButtons.size(); channel++) {
            Button button = channelButtons.get(channel);
            button.setMessage(getChannelButtonLabel(channel));
            button.visible = menu.supportsChannel(channel);
            button.active = menu.supportsChannel(channel) && channel != selectedChannel;
        }
    }

    private static Component getChannelButtonLabel(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> Component.translatable("screen.matterworks.matter_pylon.channel.energy.short");
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> Component.translatable("screen.matterworks.matter_pylon.channel.items.short");
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> Component.translatable("screen.matterworks.matter_pylon.channel.fluids.short");
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> Component.translatable("screen.matterworks.matter_pylon.channel.redstone.short");
            default -> Component.literal(Integer.toString(channel + 1));
        };
    }

    private void setIdBoxValue(String value) {
        suppressIdResponder = true;
        idBox.setValue(value);
        suppressIdResponder = false;
    }

    private static int parseIntSafely(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return MatterPylonBlockEntity.DEFAULT_PYLON_ID;
        }
    }
}
