package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.SingularityLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SingularityLinkActionPayload(BlockPos linkPos, int action) implements CustomPacketPayload {
    public static final int ACTION_LOCATE = 0;
    public static final int ACTION_CONNECT = 1;
    public static final int ACTION_DISCONNECT = 2;

    public static final Type<SingularityLinkActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "singularity_link_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SingularityLinkActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SingularityLinkActionPayload::linkPos,
            ByteBufCodecs.VAR_INT,
            SingularityLinkActionPayload::action,
            SingularityLinkActionPayload::new
    );

    @Override
    public Type<SingularityLinkActionPayload> type() {
        return TYPE;
    }

    public static void handle(SingularityLinkActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.linkPos()) instanceof SingularityLinkBlockEntity link
                    && context.player().containerMenu instanceof de.artemis.matterworks.common.menu.SingularityLinkMenu menu
                    && menu.getBlockPos().equals(payload.linkPos())) {
                link.handleMenuAction(context.player(), payload.action());
            }
        });
    }
}
