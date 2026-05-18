package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMatterPrimaryMenuPayload(BlockPos pos, boolean remoteAccess) implements CustomPacketPayload {
    public static final Type<OpenMatterPrimaryMenuPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "open_matter_primary_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMatterPrimaryMenuPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenMatterPrimaryMenuPayload::pos,
                    net.minecraft.network.codec.ByteBufCodecs.BOOL, OpenMatterPrimaryMenuPayload::remoteAccess,
                    OpenMatterPrimaryMenuPayload::new
            );

    @Override
    public Type<OpenMatterPrimaryMenuPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMatterPrimaryMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.pos()) instanceof MatterPylonBlockEntity networkNode
                    && (payload.remoteAccess()
                    || context.player().distanceToSqr(payload.pos().getX() + 0.5D, payload.pos().getY() + 0.5D, payload.pos().getZ() + 0.5D) <= 64.0D)) {
                networkNode.openPrimaryMenu(context.player(), payload.remoteAccess());
            }
        });
    }
}
