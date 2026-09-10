package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import de.artemis.matterworks.common.blockentity.PowerCrystalOreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PowerCrystalOreBlockEntityRenderer implements BlockEntityRenderer<PowerCrystalOreBlockEntity> {
    public PowerCrystalOreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PowerCrystalOreBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
    }
}
