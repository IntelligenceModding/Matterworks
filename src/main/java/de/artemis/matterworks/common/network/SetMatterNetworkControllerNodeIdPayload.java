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

public record SetMatterNetworkControllerNodeIdPayload(BlockPos controllerPos, BlockPos targetPos, int channel, int pylonId) implements CustomPacketPayload {
    public static final Type<SetMatterNetworkControllerNodeIdPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_matter_network_controller_node_id"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetMatterNetworkControllerNodeIdPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetMatterNetworkControllerNodeIdPayload::controllerPos,
            BlockPos.STREAM_CODEC,
            SetMatterNetworkControllerNodeIdPayload::targetPos,
            ByteBufCodecs.VAR_INT,
            SetMatterNetworkControllerNodeIdPayload::channel,
            ByteBufCodecs.VAR_INT,
            SetMatterNetworkControllerNodeIdPayload::pylonId,
            SetMatterNetworkControllerNodeIdPayload::new
    );

    @Override
    public Type<SetMatterNetworkControllerNodeIdPayload> type() {
        return TYPE;
    }

    public static void handle(SetMatterNetworkControllerNodeIdPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!canUseControllerMenu(player, payload.controllerPos())) {
                return;
            }

            if (player.level().getBlockEntity(payload.controllerPos()) instanceof MatterNetworkControllerBlockEntity controller) {
                controller.handleSetTargetPylonId(player, payload.targetPos(), payload.channel(), payload.pylonId());
            }
        });
    }

    private static boolean canUseControllerMenu(Player player, BlockPos controllerPos) {
        return PayloadMenuGuards.hasOpenMenu(player, controllerPos, MatterNetworkControllerMenu.class);
    }
}
