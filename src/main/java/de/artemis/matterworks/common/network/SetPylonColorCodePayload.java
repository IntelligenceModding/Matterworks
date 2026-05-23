package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetPylonColorCodePayload(BlockPos pos, int channel, int index, int colorId) implements CustomPacketPayload {
    public static final Type<SetPylonColorCodePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_pylon_color_code"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPylonColorCodePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(payload.pos());
                        buffer.writeVarInt(payload.channel());
                        buffer.writeVarInt(payload.index());
                        buffer.writeVarInt(payload.colorId());
                    },
                    buffer -> new SetPylonColorCodePayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt())
            );

    @Override
    public Type<SetPylonColorCodePayload> type() {
        return TYPE;
    }

    public static void handle(SetPylonColorCodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.pos()) instanceof MatterPylonBlockEntity blockEntity) {
                blockEntity.setNetworkColor(payload.channel(), payload.index(), DyeColor.byId(payload.colorId()));
            }
        });
    }
}
