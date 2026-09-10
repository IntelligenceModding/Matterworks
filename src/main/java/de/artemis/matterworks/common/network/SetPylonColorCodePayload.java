package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.menu.MatterNetworkControllerMenu;
import de.artemis.matterworks.common.menu.MatterPylonMenu;
import de.artemis.matterworks.common.menu.SingularityLinkMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
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
            Player player = context.player();
            if (!isValidChannel(payload.channel())
                    || !isValidColorIndex(payload.index())
                    || !canEditColor(player, payload.pos())) {
                return;
            }

            if (player.level().getBlockEntity(payload.pos()) instanceof MatterPylonBlockEntity blockEntity) {
                blockEntity.setNetworkColor(payload.channel(), payload.index(), DyeColor.byId(payload.colorId()));
            }
        });
    }

    private static boolean isValidChannel(int channel) {
        return channel >= 0 && channel < MatterPylonBlockEntity.CHANNEL_COUNT;
    }

    private static boolean isValidColorIndex(int index) {
        return index >= 0 && index < MatterPylonBlockEntity.NETWORK_COLOR_CODE_PARTS;
    }

    private static boolean canEditColor(Player player, BlockPos pos) {
        if (PayloadMenuGuards.hasOpenMenu(player, pos, MatterPylonMenu.class)) {
            return true;
        }
        if (PayloadMenuGuards.hasOpenMenu(player, pos, SingularityLinkMenu.class)) {
            return true;
        }
        if (player.containerMenu instanceof MatterNetworkControllerMenu menu
                && menu.stillValid(player)
                && menu.getBlockEntity().isConnectedTargetPosition(pos)) {
            return true;
        }
        return false;
    }
}
