package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MatterNetworkTrackingRenderer {
    private MatterNetworkTrackingRenderer() {
    }

    public static void renderTrackingOverlay(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos) {
        if (!MatterNetworkTrackingState.isTracking()) {
            return;
        }

        BlockPos pos = MatterNetworkTrackingState.getTargetPos();
        float minX = (float) (pos.getX() - cameraPos.x);
        float minY = (float) (pos.getY() - cameraPos.y);
        float minZ = (float) (pos.getZ() - cameraPos.z);
        float maxX = minX + 1.0F;
        float maxY = minY + 1.0F;
        float maxZ = minZ + 1.0F;

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        addFace(consumer, matrix, minX, minY, minZ, maxX, maxY, minZ, 0.18F, 0.82F, 1.0F, 0.40F);
        addFace(consumer, matrix, minX, minY, maxZ, maxX, maxY, maxZ, 0.18F, 0.82F, 1.0F, 0.40F);
        addFace(consumer, matrix, minX, minY, minZ, minX, maxY, maxZ, 0.18F, 0.82F, 1.0F, 0.28F);
        addFace(consumer, matrix, maxX, minY, minZ, maxX, maxY, maxZ, 0.18F, 0.82F, 1.0F, 0.28F);
        addFace(consumer, matrix, minX, maxY, minZ, maxX, maxY, maxZ, 0.36F, 0.95F, 1.0F, 0.22F);
    }

    private static void addFace(VertexConsumer consumer, Matrix4f matrix,
                                float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ,
                                float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, minX, minY, minZ).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(red, green, blue, alpha);
    }
}
