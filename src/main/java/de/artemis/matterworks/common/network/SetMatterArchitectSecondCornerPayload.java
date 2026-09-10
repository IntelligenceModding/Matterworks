package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.item.MatterArchitectItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetMatterArchitectSecondCornerPayload(int handOrdinal, BlockPos targetPos, int frontOrdinal) implements CustomPacketPayload {
    private static final double MAX_AIR_TARGET_DISTANCE = 9.5D;
    public static final Type<SetMatterArchitectSecondCornerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_matter_architect_second_corner"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetMatterArchitectSecondCornerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetMatterArchitectSecondCornerPayload::handOrdinal,
                    BlockPos.STREAM_CODEC, SetMatterArchitectSecondCornerPayload::targetPos,
                    ByteBufCodecs.VAR_INT, SetMatterArchitectSecondCornerPayload::frontOrdinal,
                    SetMatterArchitectSecondCornerPayload::new
            );

    @Override
    public Type<SetMatterArchitectSecondCornerPayload> type() {
        return TYPE;
    }

    public static void handle(SetMatterArchitectSecondCornerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!isValidAirTarget(player, payload.targetPos())) {
                return;
            }

            InteractionHand[] hands = InteractionHand.values();
            InteractionHand hand = payload.handOrdinal >= 0 && payload.handOrdinal < hands.length
                    ? hands[payload.handOrdinal]
                    : InteractionHand.MAIN_HAND;
            Direction[] directions = Direction.values();
            Direction front = payload.frontOrdinal >= 0 && payload.frontOrdinal < directions.length
                    ? directions[payload.frontOrdinal]
                    : player.getDirection();
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof MatterArchitectItem)) {
                return;
            }

            if (MatterArchitectItem.setSecondCorner(stack, payload.targetPos(), front, player)) {
                player.getInventory().setChanged();
            }
        });
    }

    private static boolean isValidAirTarget(Player player, BlockPos targetPos) {
        Vec3 eyePos = player.getEyePosition();
        return Vec3.atCenterOf(targetPos).distanceToSqr(eyePos) <= MAX_AIR_TARGET_DISTANCE * MAX_AIR_TARGET_DISTANCE
                && player.level().isLoaded(targetPos);
    }
}
