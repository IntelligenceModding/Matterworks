package de.artemis.matterworks.client.render;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class MatterBatteryCoreBlockEntityRenderer implements BlockEntityRenderer<MatterBatteryCoreBlockEntity> {
    public MatterBatteryCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MatterBatteryCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
    }
}
