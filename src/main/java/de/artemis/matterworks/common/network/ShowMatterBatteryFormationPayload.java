package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.client.render.MatterBatteryFormationOverlayState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShowMatterBatteryFormationPayload(BlockPos originPos, int durationTicks) implements CustomPacketPayload {
    public static final Type<ShowMatterBatteryFormationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "show_matter_battery_formation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowMatterBatteryFormationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ShowMatterBatteryFormationPayload::originPos,
                    ByteBufCodecs.VAR_INT, ShowMatterBatteryFormationPayload::durationTicks,
                    ShowMatterBatteryFormationPayload::new
            );

    @Override
    public Type<ShowMatterBatteryFormationPayload> type() {
        return TYPE;
    }

    public static void handle(ShowMatterBatteryFormationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MatterBatteryFormationOverlayState.show(payload.originPos(), payload.durationTicks()));
    }
}
