package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

public record SetMatterNetworkControllerSelectedNodePayload(BlockPos controllerPos, @Nullable BlockPos targetPos) implements CustomPacketPayload {
    public static final Type<SetMatterNetworkControllerSelectedNodePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_matter_network_controller_selected_node"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetMatterNetworkControllerSelectedNodePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBlockPos(payload.controllerPos());
                buffer.writeBoolean(payload.targetPos() != null);
                if (payload.targetPos() != null) {
                    buffer.writeBlockPos(payload.targetPos());
                }
            },
            buffer -> new SetMatterNetworkControllerSelectedNodePayload(
                    buffer.readBlockPos(),
                    buffer.readBoolean() ? buffer.readBlockPos() : null
            )
    );

    @Override
    public Type<SetMatterNetworkControllerSelectedNodePayload> type() {
        return TYPE;
    }

    public static void handle(SetMatterNetworkControllerSelectedNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.containerMenu instanceof MatterNetworkControllerMenu menu)
                    || !menu.getBlockPos().equals(payload.controllerPos())
                    || !menu.stillValid(player)) {
                return;
            }
            if (player.level().getBlockEntity(payload.controllerPos()) instanceof MatterNetworkControllerBlockEntity controller) {
                if (payload.targetPos() == null) {
                    menu.setSelectedTargetPos(null);
                    menu.broadcastChanges();
                    return;
                }
                if (controller.isConnectedTargetPosition(payload.targetPos())) {
                    menu.setSelectedTargetPos(payload.targetPos());
                    menu.broadcastChanges();
                }
            }
        });
    }
}
