package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.client.render.MatterBatteryFormationOverlayState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShowMatterBatteryFormationPayload(BlockPos minPos, int sizeX, int sizeY, int sizeZ, int durationTicks, int pulseTypeOrdinal) implements CustomPacketPayload {
    public static final Type<ShowMatterBatteryFormationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "show_matter_battery_formation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowMatterBatteryFormationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ShowMatterBatteryFormationPayload::minPos,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ShowMatterBatteryFormationPayload::sizeX,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ShowMatterBatteryFormationPayload::sizeY,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ShowMatterBatteryFormationPayload::sizeZ,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ShowMatterBatteryFormationPayload::durationTicks,
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT, ShowMatterBatteryFormationPayload::pulseTypeOrdinal,
                    ShowMatterBatteryFormationPayload::new
            );

    @Override
    public Type<ShowMatterBatteryFormationPayload> type() {
        return TYPE;
    }

    public static void handle(ShowMatterBatteryFormationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            MatterBatteryFormationOverlayState.PulseType[] pulseTypes = MatterBatteryFormationOverlayState.PulseType.values();
            int clampedOrdinal = Math.max(0, Math.min(payload.pulseTypeOrdinal(), pulseTypes.length - 1));
            MatterBatteryFormationOverlayState.show(
                    payload.minPos(),
                    payload.sizeX(),
                    payload.sizeY(),
                    payload.sizeZ(),
                    payload.durationTicks(),
                    pulseTypes[clampedOrdinal]
            );
        });
    }
}
