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
    public MatterFilterScreen(MatterFilterMenu menu, net.minecraft.world.entity.player.Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.matterworks.matter_filter.clear"), button -> {
            Minecraft minecraft = this.minecraft;
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MatterFilterMenu.BUTTON_CLEAR);
            }
        }).bounds(leftPos + 94, topPos + 62, 74, 20).build());
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
        int left = leftPos;
        int top = topPos;
        VanillaGuiHelper.drawScreenBackground(guiGraphics, left, top, imageWidth, imageHeight);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 6, top + 16, 164, 56);
        VanillaGuiHelper.drawInsetPanel(guiGraphics, left + 6, top + 84, 164, 56);
        VanillaGuiHelper.drawMenuSlots(guiGraphics, menu, left, top);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("screen.matterworks.matter_filter.entries"), 8, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, 74, 0x404040, false);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, net.minecraft.world.inventory.Slot slot) {
        super.renderSlot(guiGraphics, slot);
        if (menu.isFluidFilter() && slot.index < MatterFilterMenu.GHOST_SLOT_COUNT) {
            guiGraphics.fill(slot.x + 1, slot.y + 1, slot.x + 17, slot.y + 17, 0x33000000);
        }
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
            guiGraphics.blit(leftPos + slot.x + 1, topPos + slot.y + 1, 0, 16, 16, sprite);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
