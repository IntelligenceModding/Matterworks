package de.artemis.matterworks.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.matterworks.common.filter.MatterFilterData;
import de.artemis.matterworks.common.tooltip.MatterFilterTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class MatterFilterClientTooltipComponent implements ClientTooltipComponent {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/filter_tooltip.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 78;
    private static final int SLOT_WIDTH = 18;
    private static final int SLOT_HEIGHT = 18;
    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 3;
    private static final int SLOT_START_X = 7;
    private static final int SLOT_START_Y = 7;

    private final boolean fluidFilter;
    private final ItemStack[] itemEntries;
    private final FluidStack[] fluidEntries;

    public MatterFilterClientTooltipComponent(MatterFilterTooltip tooltip) {
        this.fluidFilter = tooltip.fluidFilter();
        this.itemEntries = new ItemStack[MatterFilterData.SLOT_COUNT];
        this.fluidEntries = new FluidStack[MatterFilterData.SLOT_COUNT];

        for (int slot = 0; slot < MatterFilterData.SLOT_COUNT; slot++) {
            itemEntries[slot] = ItemStack.EMPTY;
            fluidEntries[slot] = FluidStack.EMPTY;
        }

        if (fluidFilter) {
            FluidStack[] loadedFluids = MatterFilterData.loadFluidEntries(tooltip.filterStack());
            System.arraycopy(loadedFluids, 0, fluidEntries, 0, Math.min(loadedFluids.length, fluidEntries.length));
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        SimpleContainer container = new SimpleContainer(MatterFilterData.SLOT_COUNT);
        MatterFilterData.loadItemsIntoContainer(tooltip.filterStack(), minecraft.level.registryAccess(), container);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            itemEntries[slot] = container.getItem(slot);
        }
    }

    @Override
    public int getHeight() {
        return PANEL_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        return PANEL_WIDTH;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.blit(TEXTURE, x, y, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int column = 0; column < GRID_COLUMNS; column++) {
                int slotIndex = row * GRID_COLUMNS + column;
                int slotX = x + SLOT_START_X + column * SLOT_WIDTH;
                int slotY = y + SLOT_START_Y + row * SLOT_HEIGHT;
                renderSlot(guiGraphics, font, slotIndex, slotX, slotY);
            }
        }
    }

    private void renderSlot(GuiGraphics guiGraphics, Font font, int slotIndex, int x, int y) {
        if (fluidFilter) {
            renderFluidSlot(guiGraphics, slotIndex, x, y);
            return;
        }

        ItemStack entry = itemEntries[slotIndex];
        if (!entry.isEmpty()) {
            guiGraphics.renderItem(entry, x + 1, y + 1, slotIndex);
        }
    }

    private void renderFluidSlot(GuiGraphics guiGraphics, int slotIndex, int x, int y) {
        FluidStack entry = fluidEntries[slotIndex];
        if (entry.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(entry.getFluid());
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(extensions.getStillTexture(entry));
        int tint = extensions.getTintColor(entry);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;

        RenderSystem.setShaderColor(red, green, blue, 0.95F);
        guiGraphics.blit(x + 1, y + 2, 0, 16, 16, sprite);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.fill(x + 1, y + 2, x + 17, y + 18, 0x22FFFFFF);
    }
}
