package de.artemis.matterworks.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MenuHelper {
    private MenuHelper() {
    }

    public static <T extends BlockEntity> T resolveBlockEntity(Inventory inventory, BlockPos pos, Class<T> type, String label) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (type.isInstance(blockEntity)) {
            return type.cast(blockEntity);
        }
        throw new IllegalStateException("Missing " + label + " block entity at " + pos);
    }
}
