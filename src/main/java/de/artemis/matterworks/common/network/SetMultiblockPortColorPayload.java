package de.artemis.matterworks.common.network;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MultiblockPortBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetMultiblockPortColorPayload(BlockPos pos, int colorId) implements CustomPacketPayload {
    public static final Type<SetMultiblockPortColorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "set_multiblock_port_color"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetMultiblockPortColorPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetMultiblockPortColorPayload::pos,
                    ByteBufCodecs.VAR_INT, SetMultiblockPortColorPayload::colorId,
                    SetMultiblockPortColorPayload::new
            );

    @Override
    public Type<SetMultiblockPortColorPayload> type() {
        return TYPE;
    }

    public static void handle(SetMultiblockPortColorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null
                    || !(minecraft.level.getBlockEntity(payload.pos()) instanceof MultiblockPortBlockEntity portBlockEntity)) {
                return;
            }
            portBlockEntity.applySyncedPortColor(DyeColor.byId(payload.colorId()));
        });
    }
}
