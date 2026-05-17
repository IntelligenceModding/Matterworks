package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(Matterworks.MOD_ID)
                .playToClient(SetPylonDebugOverlayPayload.TYPE, SetPylonDebugOverlayPayload.STREAM_CODEC, SetPylonDebugOverlayPayload::handle)
                .playToServer(OpenMatterNetworkMenuPayload.TYPE, OpenMatterNetworkMenuPayload.STREAM_CODEC, OpenMatterNetworkMenuPayload::handle)
                .playToServer(SetPylonIdPayload.TYPE, SetPylonIdPayload.STREAM_CODEC, SetPylonIdPayload::handle);
    }
}
