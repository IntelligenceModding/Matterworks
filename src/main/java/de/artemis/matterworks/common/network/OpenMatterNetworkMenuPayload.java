package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMatterNetworkMenuPayload(BlockPos pos, boolean remoteAccess) implements CustomPacketPayload {
    public static final Type<OpenMatterNetworkMenuPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "open_matter_network_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMatterNetworkMenuPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenMatterNetworkMenuPayload::pos,
                    ByteBufCodecs.BOOL, OpenMatterNetworkMenuPayload::remoteAccess,
                    OpenMatterNetworkMenuPayload::new
            );

    @Override
    public Type<OpenMatterNetworkMenuPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMatterNetworkMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level().getBlockEntity(payload.pos()) instanceof MatterPylonBlockEntity networkNode
                    && canOpenMenu(player, payload.pos(), payload.remoteAccess())) {
                networkNode.openMatterNetworkMenu(player, payload.remoteAccess());
            }
        });
    }

    private static boolean canOpenMenu(Player player, BlockPos pos, boolean remoteAccess) {
        if (!remoteAccess) {
            return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
        }
        return PayloadMenuGuards.hasOpenRemoteNamedMenu(player, pos);
    }
}
