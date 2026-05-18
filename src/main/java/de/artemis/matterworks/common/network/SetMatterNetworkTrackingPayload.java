package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.client.render.MatterNetworkTrackingState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetMatterNetworkTrackingPayload(boolean tracking, BlockPos targetPos, String label) implements CustomPacketPayload {
    public static final Type<SetMatterNetworkTrackingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_matter_network_tracking"));

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, SetMatterNetworkTrackingPayload> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.tracking());
                        buffer.writeBlockPos(payload.targetPos());
                        buffer.writeUtf(payload.label());
                    },
                    buffer -> new SetMatterNetworkTrackingPayload(buffer.readBoolean(), buffer.readBlockPos(), buffer.readUtf())
            );

    @Override
    public Type<SetMatterNetworkTrackingPayload> type() {
        return TYPE;
    }

    public static void handle(SetMatterNetworkTrackingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.tracking()) {
                MatterNetworkTrackingState.setTarget(payload.targetPos(), payload.label());
            } else {
                MatterNetworkTrackingState.clear();
            }
        });
    }
}
