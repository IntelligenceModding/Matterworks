package de.artemis.matterworks.common.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record MatterFilterTooltip(ItemStack filterStack, boolean fluidFilter) implements TooltipComponent {
}
