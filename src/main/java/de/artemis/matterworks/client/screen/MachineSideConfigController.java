package de.artemis.matterworks.client.screen;

import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.menu.SideConfigMenuAccess;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class MachineSideConfigController {
    private static final int SIDE_CONFIG_PLAYER_INVENTORY_X = 8;
    private static final int SIDE_CONFIG_PLAYER_INVENTORY_Y = 140;
    private static final int SIDE_CONFIG_PLAYER_HOTBAR_Y = 198;
    private static final int HIDDEN_SLOT_X = -10_000;
    private static final int HIDDEN_SLOT_Y = -10_000;
    private static final Field SLOT_X_FIELD = findSlotField("x", "f_40220_");
    private static final Field SLOT_Y_FIELD = findSlotField("y", "f_40221_");
    private static final Map<BlockPos, SideConfigType> LAST_SELECTED_TYPES = new HashMap<>();
    private final MachineSideConfigOverlay overlay = new MachineSideConfigOverlay();
    private final Map<Slot, SlotPosition> originalSlotPositions = new IdentityHashMap<>();
    private BlockPos blockPos;
    private SideConfigType selectedType;

    public void init(SideConfigMenuAccess menu) {
        blockPos = menu.getBlockPos().immutable();
        SideConfigType pendingType = PendingMachineTabSelection.consume(menu.getBlockPos());
        if (pendingType != null && menu.supportsSideConfigType(pendingType)) {
            selectedType = pendingType;
            LAST_SELECTED_TYPES.put(blockPos, selectedType);
            return;
        }

        SideConfigType rememberedType = LAST_SELECTED_TYPES.get(blockPos);
        if (rememberedType != null && menu.supportsSideConfigType(rememberedType)) {
            selectedType = rememberedType;
        }
    }

    public boolean isShowing() {
        return selectedType != null;
    }

    public boolean isVisualMode() {
        return selectedType != null && overlay.isVisualMode();
    }

    public void renderBackground(GuiGraphics guiGraphics, int leftPos, int topPos) {
        if (selectedType != null) {
            overlay.renderBackground(guiGraphics, leftPos, topPos);
        }
    }

    public void syncSlotLayout(AbstractContainerMenu menu) {
        int playerSlotOrdinal = 0;
        for (Slot slot : menu.slots) {
            SlotPosition original = originalSlotPositions.computeIfAbsent(slot, ignored -> new SlotPosition(slot.x, slot.y));
            if (!isShowing()) {
                setSlotPosition(slot, original.x(), original.y());
                continue;
            }

            if (slot.container instanceof Inventory) {
                if (playerSlotOrdinal < 27) {
                    int row = playerSlotOrdinal / 9;
                    int column = playerSlotOrdinal % 9;
                    setSlotPosition(slot, SIDE_CONFIG_PLAYER_INVENTORY_X + column * 18, SIDE_CONFIG_PLAYER_INVENTORY_Y + row * 18);
                } else {
                    int hotbarSlot = playerSlotOrdinal - 27;
                    setSlotPosition(slot, SIDE_CONFIG_PLAYER_INVENTORY_X + hotbarSlot * 18, SIDE_CONFIG_PLAYER_HOTBAR_Y);
                }
                playerSlotOrdinal++;
                continue;
            }

            setSlotPosition(slot, HIDDEN_SLOT_X, HIDDEN_SLOT_Y);
        }
    }

    public void renderOverlay(
            GuiGraphics guiGraphics,
            Font font,
            SideConfigMenuAccess menu,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int titleX,
            int titleY,
            int titleColor,
            String titleText,
            int inventoryLabelX,
            int inventoryLabelY,
            int mouseX,
            int mouseY
    ) {
        if (selectedType != null) {
            overlay.render(guiGraphics, font, menu, selectedType, leftPos, topPos, imageWidth, imageHeight, titleX, titleY, titleColor, titleText, inventoryLabelX, inventoryLabelY, mouseX, mouseY);
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

    public boolean mouseDragged(
            SideConfigMenuAccess menu,
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight
    ) {
        return selectedType != null
                && overlay.mouseDragged(menu, selectedType, mouseX, mouseY, button, dragX, dragY, leftPos, topPos, imageWidth, imageHeight);
    }

    public boolean mouseReleased(
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
                && overlay.mouseReleased(menu, selectedType, mouseX, mouseY, button, leftPos, topPos, imageWidth, imageHeight);
    }

    public List<TopCategoryTabs.Tab> buildTabs(SideConfigMenuAccess menu, Runnable networkAction) {
        List<TopCategoryTabs.Tab> tabs = new ArrayList<>();
        tabs.add(new TopCategoryTabs.Tab(menu.getPrimaryTabIcon(), Component.literal(menu.getBlockDisplayName()), selectedType == null, () -> setSelectedType(null)));
        for (SideConfigType type : getSupportedTypes(menu)) {
            tabs.add(new TopCategoryTabs.Tab(getTabIcon(type), Component.literal(type.getLabel() + " Config"), type == selectedType, () -> setSelectedType(type)));
        }
        if (networkAction != null) {
            tabs.add(new TopCategoryTabs.Tab(ModBlocks.MATTER_PYLON.get().asItem().getDefaultInstance(), Component.literal("Network"), false, networkAction));
        }
        return tabs;
    }

    private void setSelectedType(SideConfigType type) {
        if (selectedType != type) {
            overlay.cancelVisualDrag();
        }
        selectedType = type;
        if (blockPos != null) {
            if (type == null) {
                LAST_SELECTED_TYPES.remove(blockPos);
            } else {
                LAST_SELECTED_TYPES.put(blockPos, type);
            }
        }
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

    private static void setSlotPosition(Slot slot, int x, int y) {
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to update slot position", exception);
        }
    }

    private static Field findSlotField(String... names) {
        for (String name : names) {
            try {
                Field field = Slot.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("Failed to locate slot position field");
    }

    private record SlotPosition(int x, int y) {
    }
}
