package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetPylonIdPayload(BlockPos pos, int channel, int pylonId) implements CustomPacketPayload {
    public static final Type<SetPylonIdPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_pylon_id"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPylonIdPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SetPylonIdPayload::pos,
            ByteBufCodecs.VAR_INT,
            SetPylonIdPayload::channel,
            ByteBufCodecs.VAR_INT,
            SetPylonIdPayload::pylonId,
            SetPylonIdPayload::new
    );

    @Override
    public Type<SetPylonIdPayload> type() {
        return TYPE;
    }

    public static void handle(SetPylonIdPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.pos()) instanceof MatterPylonBlockEntity pylonBlockEntity
                    && context.player().distanceToSqr(payload.pos().getX() + 0.5D, payload.pos().getY() + 0.5D, payload.pos().getZ() + 0.5D) <= 64.0D) {
                pylonBlockEntity.setPylonId(payload.channel(), payload.pylonId());
            }
        });
    }
}
