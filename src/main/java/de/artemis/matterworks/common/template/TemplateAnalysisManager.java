package de.artemis.matterworks.common.template;

import de.artemis.matterworks.common.matter.MatterValueManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TemplateAnalysisManager {
    private TemplateAnalysisManager() {
    }

    public static int getRequiredItemCount(ItemStack stack, Level level) {
        return MatterValueManager.getRequiredItemCount(stack, level);
    }
}
