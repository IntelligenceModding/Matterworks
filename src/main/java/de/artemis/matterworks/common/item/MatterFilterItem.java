package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.menu.MatterFilterMenu;
import de.artemis.matterworks.common.tooltip.MatterFilterTooltip;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class MatterFilterItem extends Item {
    private final boolean fluidFilter;

    public MatterFilterItem(Properties properties, boolean fluidFilter) {
        super(properties.stacksTo(1));
        this.fluidFilter = fluidFilter;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new MatterFilterMenu(containerId, inventory, InteractionHand.MAIN_HAND, fluidFilter),
                    stack.getHoverName()
            );
            serverPlayer.openMenu(provider, buffer -> buffer.writeBoolean(fluidFilter));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new MatterFilterTooltip(stack.copyWithCount(1), fluidFilter));
    }

    public boolean isFluidFilter() {
        return fluidFilter;
    }
}
