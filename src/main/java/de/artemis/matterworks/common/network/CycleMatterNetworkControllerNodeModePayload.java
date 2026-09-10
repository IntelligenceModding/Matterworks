package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleMatterNetworkControllerNodeModePayload(BlockPos controllerPos, BlockPos targetPos, int channel, boolean backward) implements CustomPacketPayload {
    public static final Type<CycleMatterNetworkControllerNodeModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "cycle_matter_network_controller_node_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CycleMatterNetworkControllerNodeModePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CycleMatterNetworkControllerNodeModePayload::controllerPos,
            BlockPos.STREAM_CODEC,
            CycleMatterNetworkControllerNodeModePayload::targetPos,
            ByteBufCodecs.VAR_INT,
            CycleMatterNetworkControllerNodeModePayload::channel,
            ByteBufCodecs.BOOL,
            CycleMatterNetworkControllerNodeModePayload::backward,
            CycleMatterNetworkControllerNodeModePayload::new
    );

    @Override
    public Type<CycleMatterNetworkControllerNodeModePayload> type() {
        return TYPE;
    }

    public static void handle(CycleMatterNetworkControllerNodeModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!canUseControllerMenu(player, payload.controllerPos())) {
                return;
            }

            if (player.level().getBlockEntity(payload.controllerPos()) instanceof MatterNetworkControllerBlockEntity controller) {
                controller.handleCycleTargetMode(player, payload.targetPos(), payload.channel(), payload.backward());
            }
        });
    }

    private static boolean canUseControllerMenu(Player player, BlockPos controllerPos) {
        return PayloadMenuGuards.hasOpenMenu(player, controllerPos, MatterNetworkControllerMenu.class);
    }
}
