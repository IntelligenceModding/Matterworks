package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.blockentity.MultiblockPortBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class MultiblockPortBlockItem extends BlockItem {
    public MultiblockPortBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (MultiblockPortBlockEntity.hasPortColor(stack)) {
            return MultiblockPortBlockEntity.getPortDisplayName(MultiblockPortBlockEntity.getPortColor(stack));
        }
        return getBlock().getName();
    }
}
