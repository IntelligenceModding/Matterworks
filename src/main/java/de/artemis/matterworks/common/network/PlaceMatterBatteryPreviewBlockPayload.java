package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockLayout;
import de.artemis.matterworks.common.multiblock.MatterBatteryPreviewPlacementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlaceMatterBatteryPreviewBlockPayload(BlockPos originPos, int frontOrdinal, int width, int height, int depth, BlockPos targetPos, int handOrdinal) implements CustomPacketPayload {
    public static final Type<PlaceMatterBatteryPreviewBlockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "place_matter_battery_preview_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaceMatterBatteryPreviewBlockPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBlockPos(payload.originPos());
                        buffer.writeVarInt(payload.frontOrdinal());
                        buffer.writeVarInt(payload.width());
                        buffer.writeVarInt(payload.height());
                        buffer.writeVarInt(payload.depth());
                        buffer.writeBlockPos(payload.targetPos());
                        buffer.writeVarInt(payload.handOrdinal());
                    },
                    buffer -> new PlaceMatterBatteryPreviewBlockPayload(
                            buffer.readBlockPos(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readBlockPos(),
                            buffer.readVarInt()
                    )
            );

    @Override
    public Type<PlaceMatterBatteryPreviewBlockPayload> type() {
        return TYPE;
    }

    public static void handle(PlaceMatterBatteryPreviewBlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            InteractionHand[] hands = InteractionHand.values();
            InteractionHand hand = payload.handOrdinal >= 0 && payload.handOrdinal < hands.length
                    ? hands[payload.handOrdinal]
                    : InteractionHand.MAIN_HAND;
            Direction[] directions = Direction.values();
            Direction front = payload.frontOrdinal >= 0 && payload.frontOrdinal < directions.length
                    ? directions[payload.frontOrdinal]
                    : Direction.NORTH;
            if (!front.getAxis().isHorizontal()
                    || !MatterBatteryMultiblockLayout.isValidSize(payload.width(), payload.height(), payload.depth())
                    || !canPlacePreviewBlock(context.player(), payload, front)) {
                return;
            }
            MatterBatteryPreviewPlacementHelper.placeFromInventory(
                    context.player(),
                    payload.originPos(),
                    front,
                    payload.width(),
                    payload.height(),
                    payload.depth(),
                    payload.targetPos(),
                    hand
            );
        });
    }

    private static boolean canPlacePreviewBlock(net.minecraft.world.entity.player.Player player, PlaceMatterBatteryPreviewBlockPayload payload, Direction front) {
        if (!player.level().isLoaded(payload.targetPos())) {
            return false;
        }
        if (player.distanceToSqr(payload.targetPos().getX() + 0.5D, payload.targetPos().getY() + 0.5D, payload.targetPos().getZ() + 0.5D) > 64.0D) {
            return false;
        }

        MatterBatteryMultiblockLayout.WorldBounds bounds = MatterBatteryMultiblockLayout.getWorldBounds(
                payload.originPos(), front, payload.width(), payload.height(), payload.depth()
        );
        BlockPos minPos = bounds.minPos();
        BlockPos maxPos = minPos.offset(bounds.sizeX() - 1, bounds.sizeY() - 1, bounds.sizeZ() - 1);
        BlockPos targetPos = payload.targetPos();
        return targetPos.getX() >= minPos.getX() && targetPos.getX() <= maxPos.getX()
                && targetPos.getY() >= minPos.getY() && targetPos.getY() <= maxPos.getY()
                && targetPos.getZ() >= minPos.getZ() && targetPos.getZ() <= maxPos.getZ();
    }
}
