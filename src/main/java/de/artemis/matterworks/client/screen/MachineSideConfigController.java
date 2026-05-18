package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.SideConfigMenuAccess;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class MachineSideConfigController {
    private final MachineSideConfigOverlay overlay = new MachineSideConfigOverlay();
    private SideConfigType selectedType;

    public void init(SideConfigMenuAccess menu) {
        SideConfigType pendingType = PendingMachineTabSelection.consume(menu.getBlockPos());
        if (pendingType != null && menu.supportsSideConfigType(pendingType)) {
            selectedType = pendingType;
        }
    }

    public boolean isShowing() {
        return selectedType != null;
    }

    public void renderOverlay(
            GuiGraphics guiGraphics,
            Font font,
            SideConfigMenuAccess menu,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY
    ) {
        if (selectedType != null) {
            overlay.render(guiGraphics, font, menu, selectedType, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    public void renderTooltip(
            GuiGraphics guiGraphics,
            Font font,
            SideConfigMenuAccess menu,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int mouseX,
            int mouseY
    ) {
        if (selectedType != null) {
            overlay.renderTooltip(guiGraphics, font, menu, selectedType, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(
            SideConfigMenuAccess menu,
            double mouseX,
            double mouseY,
            int button,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        return selectedType != null
                && overlay.mouseClicked(menu, selectedType, mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight);
    }

    public List<TopCategoryTabs.Tab> buildTabs(SideConfigMenuAccess menu, Runnable networkAction) {
        List<TopCategoryTabs.Tab> tabs = new ArrayList<>();
        tabs.add(new TopCategoryTabs.Tab(menu.getPrimaryTabIcon(), Component.literal(menu.getBlockDisplayName()), selectedType == null, () -> selectedType = null));
        for (SideConfigType type : getSupportedTypes(menu)) {
            tabs.add(new TopCategoryTabs.Tab(getTabIcon(type), Component.literal(type.getLabel() + " Config"), type == selectedType, () -> selectedType = type));
        }
        if (networkAction != null) {
            tabs.add(new TopCategoryTabs.Tab(ModBlocks.MATTER_PYLON.get().asItem().getDefaultInstance(), Component.literal("Network"), false, networkAction));
        }
        return tabs;
    }

    private static List<SideConfigType> getSupportedTypes(SideConfigMenuAccess menu) {
        List<SideConfigType> types = new ArrayList<>();
        for (SideConfigType type : SideConfigType.values()) {
            if (menu.supportsSideConfigType(type)) {
                types.add(type);
            }
        }
        return types;
    }

    private static ItemStack getTabIcon(SideConfigType type) {
        return switch (type) {
            case ITEMS -> Items.HOPPER.getDefaultInstance();
            case FLUIDS -> Items.WATER_BUCKET.getDefaultInstance();
            case ENERGY -> Items.REDSTONE.getDefaultInstance();
        };
    }
}
