package de.artemis.matterworks.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;

import java.util.Optional;

public final class MatterNetworkLocatorManager {
    private static final String LOCATOR_KEY = "MatterNetworkLocator";
    private static final String TARGET_X_KEY = "MatterNetworkLocatorX";
    private static final String TARGET_Y_KEY = "MatterNetworkLocatorY";
    private static final String TARGET_Z_KEY = "MatterNetworkLocatorZ";

    private MatterNetworkLocatorManager() {
    }

    public static void giveLocatorCompass(Player player, ServerLevel level, BlockPos pos, String label) {
        clearLocatorCompasses(player);

        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos)), true));

        CompoundTag tag = compass.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(LOCATOR_KEY, true);
        tag.putInt(TARGET_X_KEY, pos.getX());
        tag.putInt(TARGET_Y_KEY, pos.getY());
        tag.putInt(TARGET_Z_KEY, pos.getZ());
        CustomData.set(DataComponents.CUSTOM_DATA, compass, tag);

        if (!player.getInventory().add(compass)) {
            player.drop(compass, false);
        }
    }

    public static void clearLocatorCompasses(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isLocatorCompass(stack)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    public static boolean isLocatorCompass(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getBoolean(LOCATOR_KEY);
    }

    public static boolean clearLocatorCompassForTarget(Player player, BlockPos targetPos) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isLocatorCompass(stack)) {
                continue;
            }
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.getInt(TARGET_X_KEY) == targetPos.getX()
                    && tag.getInt(TARGET_Y_KEY) == targetPos.getY()
                    && tag.getInt(TARGET_Z_KEY) == targetPos.getZ()) {
                inventory.setItem(slot, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }
}
