package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MatterNetworkControllerActionPayload(BlockPos controllerPos, BlockPos targetPos, int action) implements CustomPacketPayload {
    public static final int ACTION_OPEN_GUI = 0;
    public static final int ACTION_LOCATE = 1;
    public static final Type<MatterNetworkControllerActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "matter_network_controller_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MatterNetworkControllerActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            MatterNetworkControllerActionPayload::controllerPos,
            BlockPos.STREAM_CODEC,
            MatterNetworkControllerActionPayload::targetPos,
            ByteBufCodecs.VAR_INT,
            MatterNetworkControllerActionPayload::action,
            MatterNetworkControllerActionPayload::new
    );

    @Override
    public Type<MatterNetworkControllerActionPayload> type() {
        return TYPE;
    }

    public static void handle(MatterNetworkControllerActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.controllerPos()) instanceof MatterNetworkControllerBlockEntity controller
                    && controller.isControllerMenuStillValid(context.player(), false)) {
                controller.handleAction(context.player(), payload.targetPos(), payload.action());
            }
        });
    }
}
