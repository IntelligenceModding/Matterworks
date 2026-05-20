package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
import de.artemis.matterworks.common.blockentity.FluidTankBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStorageBarrelBlockEntity;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.network.OpenMatterPrimaryMenuPayload;
import de.artemis.matterworks.common.network.SetPylonColorCodePayload;
import de.artemis.matterworks.common.network.SetPylonIdPayload;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MatterPylonScreen extends AbstractRenamableContainerScreen<MatterPylonMenu> {
    private static final int ID_SUBMIT_DEBOUNCE_TICKS = 8;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "textures/gui/network_tab.png");
    private static final int CHANNEL_BUTTONS_X = 7;
    private static final int CHANNEL_BUTTONS_Y = 41;
    private static final int CHANNEL_BUTTON_GAP = 6;
    private static final int TEXTURE_SIZE = 256;
    private static final int FILTER_LABEL_Y = 64;
    private static final int FILTER_IMPORT_CENTER_X = 26;
    private static final int FILTER_EXPORT_CENTER_X = 66;
    private static final int NETWORK_CODE_CENTER_X = 130;
    private static final int NETWORK_CODE_LABEL_Y = 64;
    private static final int POWER_CRYSTALS_CENTER_X = 130;
    private static final int POWER_CRYSTALS_LABEL_Y = 96;
    private static final int FILTER_SECTION_X = 8;
    private static final int FILTER_SECTION_Y = 75;
    private static final int FILTER_SECTION_WIDTH = 76;
    private static final int FILTER_SECTION_HEIGHT = 16;
    private static final int NETWORK_BACKGROUND_FILL = 0xFFC6C6C6;

    private EditBox idBox;
    private Button modeButton;
    private final List<ChannelButtonEntry> channelButtons = new ArrayList<>();
    private String lastSubmittedId = "";
    private int pendingSubmitTicks = -1;
    private boolean userEditedIdBox;
    private boolean suppressIdResponder;
    private int selectedChannel = -1;
    private final NetworkColorPickerOverlay colorPicker = new NetworkColorPickerOverlay(new int[]{92, 122, 152}, new int[]{75, 75, 75});

    public MatterPylonScreen(MatterPylonMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (selectedChannel < 0 || !menu.supportsChannel(selectedChannel)) {
            selectedChannel = menu.getFirstSupportedChannel();
        }
        menu.setActiveFilterChannel(selectedChannel);
        this.idBox = GuiWidgets.textField(this.font, this.leftPos + 8, this.topPos + 18, 76, 16, Component.translatable("screen.matterworks.matter_pylon.id"));
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
        List<Integer> supportedChannels = getSupportedChannels();
        ChannelButtonSprite sprite = getChannelButtonSprite(supportedChannels.size());
        float channelLabelScale = getChannelLabelScale(supportedChannels, sprite.width());
        for (int index = 0; index < supportedChannels.size(); index++) {
            int channel = supportedChannels.get(index);
            int channelX = this.leftPos + CHANNEL_BUTTONS_X + index * (sprite.width() + CHANNEL_BUTTON_GAP);
            GuiWidgets.SpriteSelectableButton channelButton = this.addRenderableWidget(GuiWidgets.spriteSelectableButton(
                    channelX,
                    this.topPos + CHANNEL_BUTTONS_Y,
                    sprite.width(),
                    sprite.height(),
                    getChannelButtonLabel(channel),
                    button -> switchChannel(channel),
                    TEXTURE,
                    sprite.u(),
                    sprite.v(),
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            ).setTextScale(channelLabelScale));
            this.channelButtons.add(new ChannelButtonEntry(channel, channelButton));
        }

        this.modeButton = this.addRenderableWidget(GuiWidgets.panelButton(this.leftPos + 91, this.topPos + 17, 78, 18, getModeButtonLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, selectedChannel);
            }
        }));
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
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return TopCategoryTabs.keyPressed(keyCode, buildTabs());
    }

    @Override
    public void onClose() {
        submitId(true);
        super.onClose();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderInactiveFilterCover(guiGraphics);
        colorPicker.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, menu::getNetworkColor);
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, this.titleLabelX, this.titleLabelY, 0x404040);
        drawCenteredLabel(guiGraphics, Component.literal("Code"), NETWORK_CODE_CENTER_X, NETWORK_CODE_LABEL_Y);
        if (menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_ITEMS
                || menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_FLUIDS) {
            drawCenteredLabel(guiGraphics, Component.literal("Import"), FILTER_IMPORT_CENTER_X, FILTER_LABEL_Y);
            drawCenteredLabel(guiGraphics, Component.literal("Output"), FILTER_EXPORT_CENTER_X, FILTER_LABEL_Y);
        }
        if (menu.supportsUpgradeCrystals()) {
            drawCenteredLabel(guiGraphics, Component.literal("Crystals"), POWER_CRYSTALS_CENTER_X, POWER_CRYSTALS_LABEL_Y);
        }
        guiGraphics.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        colorPicker.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY, menu::getNetworkColor);
        TopCategoryTabs.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
        if (renderFilterSlotTooltip(guiGraphics, mouseX, mouseY)) {
            return;
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TopCategoryTabs.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, buildTabs())) {
            return true;
        }
        if (colorPicker.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight, this::setNetworkColor, menu::getNetworkColor)) {
            return true;
        }
        if (button == 1 && modeButton != null && modeButton.isMouseOver(mouseX, mouseY)) {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                GuiWidgets.playButtonClickSound();
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, MatterPylonMenu.BUTTON_CYCLE_MODE_CHANNEL_1_BACKWARD + selectedChannel);
            }
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
        menu.setActiveFilterChannel(selectedChannel);
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
        for (ChannelButtonEntry entry : channelButtons) {
            entry.button().setMessage(getChannelButtonLabel(entry.channel()));
            entry.button().active = true;
            entry.button().visible = true;
            entry.button().setSelected(entry.channel() == selectedChannel);
        }
    }

    private List<Integer> getSupportedChannels() {
        List<Integer> supportedChannels = new ArrayList<>();
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            if (menu.supportsChannel(channel)) {
                supportedChannels.add(channel);
            }
        }
        return supportedChannels;
    }

    private float getChannelLabelScale(List<Integer> supportedChannels, int buttonWidth) {
        int maxLabelWidth = 0;
        for (int channel : supportedChannels) {
            maxLabelWidth = Math.max(maxLabelWidth, this.font.width(getChannelButtonLabel(channel)));
        }
        if (maxLabelWidth <= 0) {
            return 1.0F;
        }
        return Mth.clamp((buttonWidth - 6) / (float) maxLabelWidth, 0.35F, 1.0F);
    }

    private static ChannelButtonSprite getChannelButtonSprite(int channelCount) {
        return switch (channelCount) {
            case 1 -> new ChannelButtonSprite(0, 238, 162, 18);
            case 2 -> new ChannelButtonSprite(178, 238, 78, 18);
            case 3 -> new ChannelButtonSprite(206, 219, 50, 18);
            default -> new ChannelButtonSprite(220, 200, 36, 18);
        };
    }

    private static Component getChannelButtonLabel(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> Component.literal("Energy");
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> Component.literal("Items");
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> Component.literal("Liquids");
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> Component.literal("Redstone");
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

    private void setNetworkColor(int index, DyeColor color) {
        PacketDistributor.sendToServer(new SetPylonColorCodePayload(menu.getBlockPos(), index, color.getId()));
    }

    private List<TopCategoryTabs.Tab> buildTabs() {
        if (!hasPrimaryTab()) {
            return List.of();
        }
        List<TopCategoryTabs.Tab> tabs = new ArrayList<>();
        ItemStack machineIcon = menu.getBlockEntity().getBlockState().getBlock().asItem().getDefaultInstance();
        ItemStack networkIcon = ModBlocks.MATTER_PYLON.get().asItem().getDefaultInstance();
        tabs.add(new TopCategoryTabs.Tab(machineIcon, menu.getBlockEntity().getDisplayName(), false, () -> openPrimaryTab(null)));
        for (SideConfigType type : getPrimarySideConfigTypes()) {
            tabs.add(new TopCategoryTabs.Tab(getSideConfigTabIcon(type), Component.literal(type.getLabel() + " Config"), false, () -> openPrimaryTab(type)));
        }
        tabs.add(new TopCategoryTabs.Tab(networkIcon, Component.literal("Network"), true, () -> {
        }));
        return tabs;
    }

    private boolean hasPrimaryTab() {
        return menu.getBlockEntity() instanceof EnergyCellBlockEntity
                || menu.getBlockEntity() instanceof MatterBatteryPortBlockEntity
                || menu.getBlockEntity() instanceof FluidTankBlockEntity
                || menu.getBlockEntity() instanceof MatterStorageBarrelBlockEntity;
    }

    private List<SideConfigType> getPrimarySideConfigTypes() {
        if (menu.getBlockEntity() instanceof EnergyCellBlockEntity) {
            return List.of(SideConfigType.ITEMS, SideConfigType.ENERGY);
        }
        if (menu.getBlockEntity() instanceof FluidTankBlockEntity) {
            return List.of(SideConfigType.ITEMS, SideConfigType.FLUIDS);
        }
        if (menu.getBlockEntity() instanceof MatterStorageBarrelBlockEntity) {
            return List.of(SideConfigType.ITEMS);
        }
        return List.of();
    }

    private ItemStack getSideConfigTabIcon(SideConfigType type) {
        return switch (type) {
            case ITEMS -> net.minecraft.world.item.Items.HOPPER.getDefaultInstance();
            case FLUIDS -> net.minecraft.world.item.Items.WATER_BUCKET.getDefaultInstance();
            case ENERGY -> net.minecraft.world.item.Items.REDSTONE.getDefaultInstance();
        };
    }

    private void openPrimaryTab(SideConfigType type) {
        if (type == null) {
            PendingMachineTabSelection.clear(menu.getBlockPos());
        } else {
            PendingMachineTabSelection.set(menu.getBlockPos(), type);
        }
        PacketDistributor.sendToServer(new OpenMatterPrimaryMenuPayload(menu.getBlockPos(), menu.isRemoteAccess()));
    }

    private boolean renderFilterSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component tooltip = null;
        if (menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_ITEMS) {
            if (isMouseOverSlot(mouseX, mouseY, 0)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, 1)) {
                tooltip = Component.literal("Blacklist");
            } else if (isMouseOverSlot(mouseX, mouseY, 2)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, 3)) {
                tooltip = Component.literal("Blacklist");
            }
        } else if (menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_FLUIDS) {
            if (isMouseOverSlot(mouseX, mouseY, 4)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, 5)) {
                tooltip = Component.literal("Blacklist");
            } else if (isMouseOverSlot(mouseX, mouseY, 6)) {
                tooltip = Component.literal("Whitelist");
            } else if (isMouseOverSlot(mouseX, mouseY, 7)) {
                tooltip = Component.literal("Blacklist");
            }
        }

        if (tooltip == null) {
            return false;
        }

        guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        return true;
    }

    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }
        var slot = menu.slots.get(slotIndex);
        int slotX = this.leftPos + slot.x;
        int slotY = this.topPos + slot.y;
        return mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16;
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawString(this.font, label, centerX - this.font.width(label) / 2, y, 0x404040, false);
    }

    private void renderInactiveFilterCover(GuiGraphics guiGraphics) {
        if (menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_ITEMS
                || menu.getActiveFilterChannel() == MatterPylonBlockEntity.CHANNEL_FLUIDS) {
            return;
        }

        guiGraphics.fill(
                leftPos + FILTER_SECTION_X - 1,
                topPos + FILTER_SECTION_Y - 1,
                leftPos + FILTER_SECTION_X + FILTER_SECTION_WIDTH + 1,
                topPos + FILTER_SECTION_Y + FILTER_SECTION_HEIGHT + 1,
                NETWORK_BACKGROUND_FILL
        );
    }

    private record ChannelButtonEntry(int channel, GuiWidgets.SpriteSelectableButton button) {
    }

    private record ChannelButtonSprite(int u, int v, int width, int height) {
    }
}

