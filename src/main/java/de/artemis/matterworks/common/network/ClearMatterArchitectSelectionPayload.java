package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.item.MatterArchitectItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearMatterArchitectSelectionPayload(int handOrdinal, boolean showMessage) implements CustomPacketPayload {
    public static final Type<ClearMatterArchitectSelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "clear_matter_architect_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearMatterArchitectSelectionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ClearMatterArchitectSelectionPayload::handOrdinal,
                    ByteBufCodecs.BOOL, ClearMatterArchitectSelectionPayload::showMessage,
                    ClearMatterArchitectSelectionPayload::new
            );

    @Override
    public Type<ClearMatterArchitectSelectionPayload> type() {
        return TYPE;
    }

    public static void handle(ClearMatterArchitectSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            InteractionHand[] hands = InteractionHand.values();
            InteractionHand hand = payload.handOrdinal >= 0 && payload.handOrdinal < hands.length
                    ? hands[payload.handOrdinal]
                    : InteractionHand.MAIN_HAND;
            ItemStack stack = context.player().getItemInHand(hand);
            if (!(stack.getItem() instanceof MatterArchitectItem)) {
                return;
            }

            MatterArchitectItem.clearSelection(stack);
            context.player().getInventory().setChanged();
            if (payload.showMessage()) {
                context.player().displayClientMessage(Component.translatable("message.matterworks.matter_architect.cleared"), true);
            }
        });
    }
}
