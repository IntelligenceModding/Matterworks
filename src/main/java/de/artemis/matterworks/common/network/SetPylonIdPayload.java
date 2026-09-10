package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
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
            Player player = context.player();
            if (!isValidChannel(payload.channel()) || !canEditPylon(player, payload.pos())) {
                return;
            }

            if (player.level().getBlockEntity(payload.pos()) instanceof MatterPylonBlockEntity pylonBlockEntity) {
                pylonBlockEntity.setPylonId(payload.channel(), payload.pylonId());
            }
        });
    }

    private static boolean isValidChannel(int channel) {
        return channel >= 0 && channel < MatterPylonBlockEntity.CHANNEL_COUNT;
    }

    private static boolean canEditPylon(Player player, BlockPos pos) {
        return PayloadMenuGuards.hasOpenMenu(player, pos, MatterPylonMenu.class);
    }
}
