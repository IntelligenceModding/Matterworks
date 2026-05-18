package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetSideConfigPayload(BlockPos pos, int typeOrdinal, int sideOrdinal, int modeOrdinal) implements CustomPacketPayload {
    public static final Type<SetSideConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_side_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetSideConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetSideConfigPayload::pos,
                    ByteBufCodecs.VAR_INT, SetSideConfigPayload::typeOrdinal,
                    ByteBufCodecs.VAR_INT, SetSideConfigPayload::sideOrdinal,
                    ByteBufCodecs.VAR_INT, SetSideConfigPayload::modeOrdinal,
                    SetSideConfigPayload::new
            );

    @Override
    public Type<SetSideConfigPayload> type() {
        return TYPE;
    }

    public static void handle(SetSideConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            SideConfigType[] types = SideConfigType.values();
            SideAccessMode[] modes = SideAccessMode.values();
            Direction[] directions = Direction.values();
            if (payload.typeOrdinal < 0 || payload.typeOrdinal >= types.length
                    || payload.sideOrdinal < 0 || payload.sideOrdinal >= directions.length
                    || payload.modeOrdinal < 0 || payload.modeOrdinal >= modes.length) {
                return;
            }

            if (context.player().level().getBlockEntity(payload.pos()) instanceof SideConfigurableBlockEntity configurableBlockEntity) {
                configurableBlockEntity.setSideAccessMode(types[payload.typeOrdinal], directions[payload.sideOrdinal], modes[payload.modeOrdinal]);
            }
        });
    }
}
