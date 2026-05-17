package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.debug.PylonDebugOverlayState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetPylonDebugOverlayPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetPylonDebugOverlayPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_pylon_debug_overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPylonDebugOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.enabled()),
            buffer -> new SetPylonDebugOverlayPayload(buffer.readBoolean())
    );

    @Override
    public Type<SetPylonDebugOverlayPayload> type() {
        return TYPE;
    }

    public static void handle(SetPylonDebugOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PylonDebugOverlayState.setEnabled(payload.enabled()));
    }
}
