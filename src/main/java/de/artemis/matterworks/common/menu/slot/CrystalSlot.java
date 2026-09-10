package de.artemis.matterworks.common.menu.slot;

import de.artemis.matterworks.common.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class CrystalSlot extends InputSlot implements GhostItemSlot {
    private static final long GHOST_CYCLE_INTERVAL_MILLIS = 1_000L;

    public CrystalSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public ItemStack getGhostItemStack() {
        return getCyclingGhostItemStack();
    }

    public static ItemStack getCyclingGhostItemStack() {
        return switch ((int) ((Util.getMillis() / GHOST_CYCLE_INTERVAL_MILLIS) % 3L)) {
            case 0 -> ModItems.CRIMSON_POWER_CRYSTAL.get().getDefaultInstance();
            case 1 -> ModItems.AZURE_POWER_CRYSTAL.get().getDefaultInstance();
            default -> ModItems.VERDANT_POWER_CRYSTAL.get().getDefaultInstance();
        };
    }
}
