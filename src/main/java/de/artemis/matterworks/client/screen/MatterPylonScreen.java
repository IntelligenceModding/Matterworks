package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterFluidTankBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStorageBarrelBlockEntity;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.network.OpenMatterPrimaryMenuPayload;
import de.artemis.matterworks.common.network.SetPylonColorCodePayload;
import de.artemis.matterworks.common.network.SetPylonIdPayload;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MatterPylonScreen extends AbstractRenamableContainerScreen<MatterPylonMenu> {
    private static final int ID_SUBMIT_DEBOUNCE_TICKS = 8;

    private EditBox idBox;
    private Button modeButton;
    private final List<Button> channelButtons = new ArrayList<>();
    private String lastSubmittedId = "";
    private int pendingSubmitTicks = -1;
    private boolean userEditedIdBox;
    private boolean suppressIdResponder;
    private int selectedChannel = -1;
    private final NetworkColorPickerOverlay colorPicker = new NetworkColorPickerOverlay(new int[]{88, 108, 128}, new int[]{138, 138, 138});

    public MatterPylonScreen(MatterPylonMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
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
        VanillaGuiHelper.drawScreenBackground(guiGraphics, left, top, this.imageWidth, this.imageHeight);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 14, top + 34, 62, 22);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 7, top + 84, 54, 40);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 79, top + 84, 54, 40);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 7, top + 130, 72, 24);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 7, top + 156, 162, 50);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, left, top);
        colorPicker.render(guiGraphics, leftPos, topPos, imageWidth, imageHeight, menu::getNetworkColor);
        TopCategoryTabs.render(guiGraphics, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderEditableTitle(guiGraphics, this.titleLabelX, this.titleLabelY, 0x404040);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.id"), 16, 20, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.mode"), 88, 20, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.channel"), 16, 48, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.item_filter"), 16, 74, menu.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_ITEMS) ? 0x404040 : 0x707070, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.import_short"), 16, 84, 0x406090, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 16, 94, 0x507D50, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 34, 94, 0x8D5050, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.export_short"), 16, 102, 0x8B6A2B, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 16, 112, 0x507D50, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 34, 112, 0x8D5050, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.fluid_filter"), 88, 74, menu.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_FLUIDS) ? 0x404040 : 0x707070, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.import_short"), 88, 84, 0x406090, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 88, 94, 0x507D50, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 106, 94, 0x8D5050, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.export_short"), 88, 102, 0x8B6A2B, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.whitelist_short"), 88, 112, 0x507D50, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.blacklist_short"), 106, 112, 0x8D5050, false);
        guiGraphics.drawString(this.font, Component.literal("Power Crystals"), 16, 120, menu.supportsUpgradeCrystals() ? 0x404040 : 0x707070, false);
        guiGraphics.drawString(this.font, Component.literal("Code"), 88, 120, 0x404040, false);
        guiGraphics.drawString(this.font, Component.literal("R"), 18, 132, 0xE14B4B, false);
        guiGraphics.drawString(this.font, Component.literal("G"), 36, 132, 0x55D26A, false);
        guiGraphics.drawString(this.font, Component.literal("B"), 54, 132, 0x4C86F5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, 146, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.matterworks.matter_network.link_hint"), 16, 228, 0x606060, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        colorPicker.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY, menu::getNetworkColor);
        TopCategoryTabs.renderTooltip(guiGraphics, this.font, leftPos, topPos, imageWidth, mouseX, mouseY, buildTabs());
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (TopCategoryTabs.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, buildTabs())) {
            return true;
        }
        if (colorPicker.mouseClicked(mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight, this::setNetworkColor)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
                || menu.getBlockEntity() instanceof MatterFluidTankBlockEntity
                || menu.getBlockEntity() instanceof MatterStorageBarrelBlockEntity;
    }

    private List<SideConfigType> getPrimarySideConfigTypes() {
        if (menu.getBlockEntity() instanceof EnergyCellBlockEntity) {
            return List.of(SideConfigType.ITEMS, SideConfigType.ENERGY);
        }
        if (menu.getBlockEntity() instanceof MatterFluidTankBlockEntity) {
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
}
