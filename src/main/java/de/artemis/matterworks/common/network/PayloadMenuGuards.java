package de.artemis.matterworks.common.network;

import de.artemis.matterworks.common.menu.NamedBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

final class PayloadMenuGuards {
    private PayloadMenuGuards() {
    }

    static <T extends NamedBlockMenu> boolean hasOpenMenu(Player player, BlockPos pos, Class<T> menuType) {
        return menuType.isInstance(player.containerMenu)
                && menuType.cast(player.containerMenu).getBlockPos().equals(pos)
                && player.containerMenu.stillValid(player);
    }

    static boolean hasOpenNamedMenu(Player player, BlockPos pos) {
        return player.containerMenu instanceof NamedBlockMenu menu
                && menu.getBlockPos().equals(pos)
                && player.containerMenu.stillValid(player);
    }

    static boolean hasOpenRemoteNamedMenu(Player player, BlockPos pos) {
        return player.containerMenu instanceof NamedBlockMenu menu
                && menu.getBlockPos().equals(pos)
                && menu.isRemoteAccess()
                && player.containerMenu.stillValid(player);
    }
}
