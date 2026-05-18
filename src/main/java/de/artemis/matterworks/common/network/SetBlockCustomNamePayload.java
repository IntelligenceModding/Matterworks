package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.CustomNamedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetBlockCustomNamePayload(BlockPos blockPos, String customName) implements CustomPacketPayload {
    public static final Type<SetBlockCustomNamePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_block_custom_name"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetBlockCustomNamePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(payload.blockPos());
                        buffer.writeUtf(payload.customName(), 64);
                    },
                    buffer -> new SetBlockCustomNamePayload(buffer.readBlockPos(), buffer.readUtf(64))
            );

    @Override
    public Type<SetBlockCustomNamePayload> type() {
        return TYPE;
    }

    public static void handle(SetBlockCustomNamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().level().getBlockEntity(payload.blockPos()) instanceof CustomNamedBlockEntity blockEntity) {
                blockEntity.setCustomNameText(payload.customName());
            }
        });
    }
}
