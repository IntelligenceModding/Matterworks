package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.debug.SideConfigDebugOverlayState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSideConfigDebugOverlayPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetSideConfigDebugOverlayPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_side_config_debug_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetSideConfigDebugOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.enabled()),
            buffer -> new SetSideConfigDebugOverlayPayload(buffer.readBoolean())
    );

    @Override
    public Type<SetSideConfigDebugOverlayPayload> type() {
        return TYPE;
    }

    public static void handle(SetSideConfigDebugOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SideConfigDebugOverlayState.setEnabled(payload.enabled()));
    }
}
