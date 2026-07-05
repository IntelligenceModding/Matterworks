package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.item.MatterArchitectItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MoveMatterArchitectSelectionPayload(int handOrdinal, int dx, int dy, int dz) implements CustomPacketPayload {
    public static final Type<MoveMatterArchitectSelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "move_matter_architect_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoveMatterArchitectSelectionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MoveMatterArchitectSelectionPayload::handOrdinal,
                    ByteBufCodecs.VAR_INT, MoveMatterArchitectSelectionPayload::dx,
                    ByteBufCodecs.VAR_INT, MoveMatterArchitectSelectionPayload::dy,
                    ByteBufCodecs.VAR_INT, MoveMatterArchitectSelectionPayload::dz,
                    MoveMatterArchitectSelectionPayload::new
            );

    @Override
    public Type<MoveMatterArchitectSelectionPayload> type() {
        return TYPE;
    }

    public static void handle(MoveMatterArchitectSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            InteractionHand[] hands = InteractionHand.values();
            InteractionHand hand = payload.handOrdinal >= 0 && payload.handOrdinal < hands.length
                    ? hands[payload.handOrdinal]
                    : InteractionHand.MAIN_HAND;
            ItemStack stack = context.player().getItemInHand(hand);
            if (!(stack.getItem() instanceof MatterArchitectItem)) {
                return;
            }
            MatterArchitectItem.shiftSelection(stack, payload.dx(), payload.dy(), payload.dz());
            context.player().getInventory().setChanged();
        });
    }
}
