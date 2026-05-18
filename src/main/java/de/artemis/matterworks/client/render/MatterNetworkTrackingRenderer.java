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
        addFace(consumer, matrix,
                minX, minY, minZ,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                maxX, minY, minZ,
                0.18F, 0.82F, 1.0F, 0.40F);
        addFace(consumer, matrix,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                minX, minY, maxZ,
                0.18F, 0.82F, 1.0F, 0.40F);
        addFace(consumer, matrix,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                minX, minY, minZ,
                0.18F, 0.82F, 1.0F, 0.28F);
        addFace(consumer, matrix,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                maxX, minY, maxZ,
                0.18F, 0.82F, 1.0F, 0.28F);
        addFace(consumer, matrix,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                maxX, maxY, minZ,
                0.36F, 0.95F, 1.0F, 0.22F);
        addFace(consumer, matrix,
                minX, minY, maxZ,
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                0.12F, 0.64F, 0.84F, 0.22F);
    }

    private static void addFace(VertexConsumer consumer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x4, y4, z4).setColor(red, green, blue, alpha);
    }
}
