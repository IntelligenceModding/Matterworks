package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(Matterworks.MOD_ID)
                .playToClient(ShowMatterBatteryFormationPayload.TYPE, ShowMatterBatteryFormationPayload.STREAM_CODEC, ShowMatterBatteryFormationPayload::handle)
                .playToClient(SetPylonDebugOverlayPayload.TYPE, SetPylonDebugOverlayPayload.STREAM_CODEC, SetPylonDebugOverlayPayload::handle)
                .playToClient(SetSideConfigDebugOverlayPayload.TYPE, SetSideConfigDebugOverlayPayload.STREAM_CODEC, SetSideConfigDebugOverlayPayload::handle)
                .playToClient(SetMatterNetworkTrackingPayload.TYPE, SetMatterNetworkTrackingPayload.STREAM_CODEC, SetMatterNetworkTrackingPayload::handle)
                .playToServer(SetBlockCustomNamePayload.TYPE, SetBlockCustomNamePayload.STREAM_CODEC, SetBlockCustomNamePayload::handle)
                .playToServer(PlaceMatterBatteryPreviewBlockPayload.TYPE, PlaceMatterBatteryPreviewBlockPayload.STREAM_CODEC, PlaceMatterBatteryPreviewBlockPayload::handle)
                .playToServer(MoveMatterArchitectSelectionPayload.TYPE, MoveMatterArchitectSelectionPayload.STREAM_CODEC, MoveMatterArchitectSelectionPayload::handle)
                .playToServer(ResizeMatterArchitectSelectionPayload.TYPE, ResizeMatterArchitectSelectionPayload.STREAM_CODEC, ResizeMatterArchitectSelectionPayload::handle)
                .playToServer(ConfigureMatterBatteryPortPayload.TYPE, ConfigureMatterBatteryPortPayload.STREAM_CODEC, ConfigureMatterBatteryPortPayload::handle)
                .playToServer(MatterNetworkControllerActionPayload.TYPE, MatterNetworkControllerActionPayload.STREAM_CODEC, MatterNetworkControllerActionPayload::handle)
                .playToServer(OpenMatterPrimaryMenuPayload.TYPE, OpenMatterPrimaryMenuPayload.STREAM_CODEC, OpenMatterPrimaryMenuPayload::handle)
                .playToServer(OpenMatterNetworkMenuPayload.TYPE, OpenMatterNetworkMenuPayload.STREAM_CODEC, OpenMatterNetworkMenuPayload::handle)
                .playToServer(SetSideConfigPayload.TYPE, SetSideConfigPayload.STREAM_CODEC, SetSideConfigPayload::handle)
                .playToServer(SetPylonColorCodePayload.TYPE, SetPylonColorCodePayload.STREAM_CODEC, SetPylonColorCodePayload::handle)
                .playToServer(SetPylonIdPayload.TYPE, SetPylonIdPayload.STREAM_CODEC, SetPylonIdPayload::handle)
                .playToServer(SingularityLinkActionPayload.TYPE, SingularityLinkActionPayload.STREAM_CODEC, SingularityLinkActionPayload::handle)
                .playToServer(SetMatterNetworkControllerSelectedNodePayload.TYPE, SetMatterNetworkControllerSelectedNodePayload.STREAM_CODEC, SetMatterNetworkControllerSelectedNodePayload::handle)
                .playToServer(SetMatterNetworkControllerNodeIdPayload.TYPE, SetMatterNetworkControllerNodeIdPayload.STREAM_CODEC, SetMatterNetworkControllerNodeIdPayload::handle)
                .playToServer(CycleMatterNetworkControllerNodeModePayload.TYPE, CycleMatterNetworkControllerNodeModePayload.STREAM_CODEC, CycleMatterNetworkControllerNodeModePayload::handle);
    }
}
