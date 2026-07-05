package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.item.MatterArchitectItem;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResizeMatterArchitectSelectionPayload(int handOrdinal, int directionOrdinal, int amount) implements CustomPacketPayload {
    public static final Type<ResizeMatterArchitectSelectionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "resize_matter_architect_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResizeMatterArchitectSelectionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ResizeMatterArchitectSelectionPayload::handOrdinal,
                    ByteBufCodecs.VAR_INT, ResizeMatterArchitectSelectionPayload::directionOrdinal,
                    ByteBufCodecs.VAR_INT, ResizeMatterArchitectSelectionPayload::amount,
                    ResizeMatterArchitectSelectionPayload::new
            );

    @Override
    public Type<ResizeMatterArchitectSelectionPayload> type() {
        return TYPE;
    }

    public static void handle(ResizeMatterArchitectSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            InteractionHand[] hands = InteractionHand.values();
            InteractionHand hand = payload.handOrdinal >= 0 && payload.handOrdinal < hands.length
                    ? hands[payload.handOrdinal]
                    : InteractionHand.MAIN_HAND;
            Direction[] directions = Direction.values();
            Direction direction = payload.directionOrdinal >= 0 && payload.directionOrdinal < directions.length
                    ? directions[payload.directionOrdinal]
                    : Direction.NORTH;
            ItemStack stack = context.player().getItemInHand(hand);
            if (!(stack.getItem() instanceof MatterArchitectItem)) {
                return;
            }
            if (MatterArchitectItem.resizeSelection(stack, direction, payload.amount())) {
                context.player().getInventory().setChanged();
            }
        });
    }
}
