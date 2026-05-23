package de.artemis.matterworks.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.matterworks.common.menu.MatterFilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class MatterFilterScreen extends AbstractContainerScreen<MatterFilterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "textures/gui/filter.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int TEXTURE_HEIGHT = 189;

    public MatterFilterScreen(MatterFilterMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 189;
        this.inventoryLabelY = 95;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(createMenuButton(7, 74, 52, 18, Component.translatable("screen.matterworks.matter_filter.clear"), MatterFilterMenu.BUTTON_CLEAR));
        addRenderableWidget(createMenuButton(63, 74, 50, 18, Component.translatable("screen.matterworks.matter_filter.sort"), MatterFilterMenu.BUTTON_SORT));
        addRenderableWidget(createMenuButton(117, 74, 52, 18, Component.translatable("screen.matterworks.matter_filter.clean"), MatterFilterMenu.BUTTON_CLEAN));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (menu.isFluidFilter()) {
            renderFluidGhosts(guiGraphics);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void renderFluidGhosts(GuiGraphics guiGraphics) {
        for (int slotIndex = 0; slotIndex < MatterFilterMenu.GHOST_SLOT_COUNT; slotIndex++) {
            FluidStack fluid = menu.getGhostFluid(slotIndex);
            if (fluid.isEmpty()) {
                continue;
            }

            var slot = menu.slots.get(slotIndex);
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            ResourceLocation stillTexture = extensions.getStillTexture(fluid);
            TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(stillTexture);
            int tint = extensions.getTintColor(fluid);
            float red = ((tint >> 16) & 0xFF) / 255.0F;
            float green = ((tint >> 8) & 0xFF) / 255.0F;
            float blue = (tint & 0xFF) / 255.0F;

            RenderSystem.setShaderColor(red, green, blue, 0.9F);
            guiGraphics.blit(leftPos + slot.x, topPos + slot.y, 0, 16, 16, sprite);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private Button createMenuButton(int x, int y, int width, int height, Component label, int buttonId) {
        return GuiWidgets.panelButton(leftPos + x, topPos + y, width, height, label, button -> {
            Minecraft minecraft = this.minecraft;
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            }
        });
    }
}
