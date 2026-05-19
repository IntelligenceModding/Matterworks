package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.multiblock.MatterBatteryPreviewPlacementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlaceMatterBatteryPreviewBlockPayload(BlockPos controllerPos, BlockPos targetPos, int handOrdinal) implements CustomPacketPayload {
    public static final Type<PlaceMatterBatteryPreviewBlockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "place_matter_battery_preview_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceMatterBatteryPreviewBlockPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PlaceMatterBatteryPreviewBlockPayload::controllerPos,
                    BlockPos.STREAM_CODEC, PlaceMatterBatteryPreviewBlockPayload::targetPos,
                    ByteBufCodecs.VAR_INT, PlaceMatterBatteryPreviewBlockPayload::handOrdinal,
                    PlaceMatterBatteryPreviewBlockPayload::new
            );

    @Override
    public Type<PlaceMatterBatteryPreviewBlockPayload> type() {
        return TYPE;
    }

    public static void handle(PlaceMatterBatteryPreviewBlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            InteractionHand[] hands = InteractionHand.values();
            InteractionHand hand = payload.handOrdinal >= 0 && payload.handOrdinal < hands.length
                    ? hands[payload.handOrdinal]
                    : InteractionHand.MAIN_HAND;
            MatterBatteryPreviewPlacementHelper.placeFromInventory(context.player(), payload.controllerPos(), payload.targetPos(), hand);
        });
    }
}
