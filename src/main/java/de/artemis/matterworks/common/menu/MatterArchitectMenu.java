package de.artemis.matterworks.common.menu;

import de.artemis.matterworks.common.item.MatterArchitectItem;
import de.artemis.matterworks.common.multiblock.MatterArchitectBlueprintType;
import de.artemis.matterworks.common.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MatterArchitectMenu extends AbstractBaseMenu {
    private final Player owner;
    private final InteractionHand hand;

    public MatterArchitectMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, readHand(extraData));
    }

    public MatterArchitectMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenuTypes.MATTER_ARCHITECT.get(), containerId);
        this.owner = playerInventory.player;
        this.hand = hand;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getArchitectStack() {
        return owner.getItemInHand(hand);
    }

    public MatterArchitectBlueprintType getSelectedBlueprintType() {
        return MatterArchitectItem.getBlueprintType(getArchitectStack());
    }

    public boolean hasSelection() {
        return MatterArchitectItem.hasCornerA(getArchitectStack());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        ItemStack stack = getArchitectStack();
        if (!(stack.getItem() instanceof MatterArchitectItem)) {
            return false;
        }

        for (MatterArchitectBlueprintType type : MatterArchitectBlueprintType.values()) {
            if (id == type.id()) {
                MatterArchitectItem.setBlueprintType(stack, type);
                player.getInventory().setChanged();
                player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.selected", type.displayName()), true);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack stack = getArchitectStack();
        return !stack.isEmpty() && stack.getItem() instanceof MatterArchitectItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static InteractionHand readHand(RegistryFriendlyByteBuf extraData) {
        InteractionHand[] hands = InteractionHand.values();
        int handOrdinal = extraData.readVarInt();
        return handOrdinal >= 0 && handOrdinal < hands.length ? hands[handOrdinal] : InteractionHand.MAIN_HAND;
    }
}
