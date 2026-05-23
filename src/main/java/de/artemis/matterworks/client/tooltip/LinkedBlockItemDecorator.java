package de.artemis.matterworks.client.tooltip;

import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.item.NetworkRemoteTerminalItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import org.jetbrains.annotations.NotNull;

public class LinkedBlockItemDecorator implements IItemDecorator {
    @Override
    public boolean render(@NotNull GuiGraphics guiGraphics, @NotNull Font font, ItemStack stack, int xOffset, int yOffset) {
        ItemStack overlayStack = stack.getItem() instanceof NetworkDataCardItem
                ? NetworkDataCardItem.getStoredBlockIcon(stack)
                : stack.getItem() instanceof NetworkRemoteTerminalItem
                ? NetworkRemoteTerminalItem.getLinkedBlockIcon(stack)
                : ItemStack.EMPTY;
        if (overlayStack.isEmpty()) {
            return false;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xOffset + 8.0F, yOffset, 50.0F);
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        guiGraphics.renderItem(overlayStack, 0, 0);
        guiGraphics.pose().popPose();
        return false;
    }
}
