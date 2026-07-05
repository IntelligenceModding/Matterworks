package de.artemis.matterworks.common.network;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.io.SideAccessMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfigureMatterBatteryPortPayload(BlockPos controllerPos, BlockPos portPos, int modeOrdinal, int maxTransfer) implements CustomPacketPayload {
    public static final Type<ConfigureMatterBatteryPortPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "configure_matter_battery_port"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureMatterBatteryPortPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ConfigureMatterBatteryPortPayload::controllerPos,
            BlockPos.STREAM_CODEC,
            ConfigureMatterBatteryPortPayload::portPos,
            ByteBufCodecs.VAR_INT,
            ConfigureMatterBatteryPortPayload::modeOrdinal,
            ByteBufCodecs.VAR_INT,
            ConfigureMatterBatteryPortPayload::maxTransfer,
            ConfigureMatterBatteryPortPayload::new
    );

    @Override
    public Type<ConfigureMatterBatteryPortPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureMatterBatteryPortPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level().getBlockEntity(payload.controllerPos()) instanceof MatterBatteryCoreBlockEntity controller)
                    || !controller.isMenuStillValid(context.player())) {
                return;
            }

            SideAccessMode[] modes = SideAccessMode.values();
            int clampedOrdinal = Math.max(0, Math.min(payload.modeOrdinal(), modes.length - 1));
            controller.configurePort(payload.portPos(), modes[clampedOrdinal], payload.maxTransfer());
        });
    }
}
