package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.blockentity.SingularityLinkBlockEntity;
import de.artemis.matterworks.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class SingularityLinkBlockEntityRenderer extends MatterPylonBlockEntityRenderer<SingularityLinkBlockEntity> {
    private static final Vector3f ACTIVE_GLOW = new Vector3f(0.70F, 0.22F, 0.98F);
    private static final Vector3f OUTLINE_COLOR = new Vector3f(0.28F, 0.90F, 0.98F);
    private static final Vector3f CORNER_COLOR = new Vector3f(0.82F, 0.45F, 1.0F);
    private static final Map<Long, Long> LAST_PARTICLE_TICKS = new HashMap<>();
    private static final float EDGE_HALF_WIDTH = 0.030F;
    private static final float CORNER_HALF_SIZE = 0.07F;

    private final ItemRenderer itemRenderer;

    public SingularityLinkBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SingularityLinkBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderSingularity(blockEntity, partialTick, poseStack, buffer, packedOverlay);
        super.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }

    public static void renderStructureOverlays(Iterable<MatterPylonBlockEntity> nodes, PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos, long gameTime) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (MatterPylonBlockEntity node : nodes) {
            if (!(node instanceof SingularityLinkBlockEntity linkBlockEntity)) {
                continue;
            }
            if (!linkBlockEntity.isStructureFormed()) {
                continue;
            }
            SingularityLinkBlockEntity.StructureAxes axes = linkBlockEntity.getResolvedStructureAxes();
            if (axes == null) {
                continue;
            }
            renderStructureOverlay(linkBlockEntity.getBlockPos(), axes, cameraPos, matrix, consumer, gameTime);
        }
    }

    private void renderSingularity(SingularityLinkBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedOverlay) {
        if (!blockEntity.isStructureFormed() || !blockEntity.hasInsertedSingularity()) {
            return;
        }

        float animationTime = blockEntity.getLevel() == null ? partialTick : blockEntity.getLevel().getGameTime() + partialTick;
        boolean active = blockEntity.hasActiveSingularityTransfer();
        SingularityLinkBlockEntity.StructureAxes axes = blockEntity.getResolvedStructureAxes();
        Vector3f planeNormal = axes == null
                ? new Vector3f(0.0F, 0.0F, 1.0F)
                : directionVector(axes.horizontalAxis()).cross(directionVector(axes.verticalAxis()), new Vector3f()).normalize();
        float hoverOffset = active
                ? 0.02F + Mth.sin(animationTime * 0.22F) * 0.035F
                : 0.01F + Mth.sin(animationTime * 0.11F) * 0.02F;
        float spin = active ? animationTime * 9.5F : Mth.sin(animationTime * 0.08F) * 7.0F;
        float scale = active ? 0.82F + Mth.sin(animationTime * 0.25F) * 0.05F : 0.72F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D + hoverOffset, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        applyPlaneFacingRotation(poseStack, planeNormal);
        poseStack.mulPose(Axis.XP.rotationDegrees(active ? Mth.cos(animationTime * 0.18F) * 8.0F : 4.0F + Mth.sin(animationTime * 0.06F) * 3.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(active ? Mth.sin(animationTime * 0.21F) * 7.0F : Mth.cos(animationTime * 0.07F) * 3.5F));
        poseStack.scale(scale, scale, scale);

        ItemStack singularityStack = new ItemStack(ModItems.MATTER_SINGULARITY.get());
        itemRenderer.renderStatic(
                singularityStack,
                ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,
                packedOverlay,
                poseStack,
                buffer,
                blockEntity.getLevel(),
                0
        );
        poseStack.popPose();

        if (active) {
            emitActiveParticles(blockEntity, animationTime);
        }
    }

    private void emitActiveParticles(SingularityLinkBlockEntity blockEntity, float animationTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.level instanceof ClientLevel level)) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        long gameTime = level.getGameTime();
        long key = pos.asLong();
        Long lastTick = LAST_PARTICLE_TICKS.get(key);
        if (lastTick != null && lastTick == gameTime) {
            return;
        }
        LAST_PARTICLE_TICKS.put(key, gameTime);

        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        for (int index = 0; index < 3; index++) {
            double angle = animationTime * 0.25D + (Math.PI * 2.0D / 3.0D) * index;
            double radius = 0.16D + 0.015D * Math.sin(animationTime * 0.18D + index);
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(animationTime * 0.16D + index * 0.9D) * 0.08D;
            double z = centerZ + Math.sin(angle) * radius;
            double velocityX = (centerX - x) * 0.08D;
            double velocityY = 0.005D + 0.01D * Math.cos(animationTime * 0.2D + index);
            double velocityZ = (centerZ - z) * 0.08D;

            level.addParticle(new DustParticleOptions(ACTIVE_GLOW, 0.8F), x, y, z, velocityX, velocityY, velocityZ);
        }

        if ((gameTime + key) % 3L == 0L) {
            double x = centerX + (level.random.nextDouble() - 0.5D) * 0.22D;
            double y = centerY + (level.random.nextDouble() - 0.5D) * 0.18D;
            double z = centerZ + (level.random.nextDouble() - 0.5D) * 0.22D;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    private static void renderStructureOverlay(BlockPos centerPos, SingularityLinkBlockEntity.StructureAxes axes, Vec3 cameraPos,
                                               Matrix4f matrix, VertexConsumer consumer, long gameTime) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int horizontal = -1; horizontal <= 1; horizontal++) {
            for (int vertical = -1; vertical <= 1; vertical++) {
                BlockPos blockPos = centerPos.relative(axes.horizontalAxis(), horizontal).relative(axes.verticalAxis(), vertical);
                minX = Math.min(minX, (float) (blockPos.getX() - cameraPos.x));
                minY = Math.min(minY, (float) (blockPos.getY() - cameraPos.y));
                minZ = Math.min(minZ, (float) (blockPos.getZ() - cameraPos.z));
                maxX = Math.max(maxX, (float) (blockPos.getX() + 1.0D - cameraPos.x));
                maxY = Math.max(maxY, (float) (blockPos.getY() + 1.0D - cameraPos.y));
                maxZ = Math.max(maxZ, (float) (blockPos.getZ() + 1.0D - cameraPos.z));
            }
        }

        renderOutlinedBounds(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, gameTime + centerPos.asLong());
    }

    public static void renderOutlinedBounds(VertexConsumer consumer, Matrix4f matrix,
                                            float minX, float minY, float minZ,
                                            float maxX, float maxY, float maxZ,
                                            long animationSeed) {
        renderOutlinedBounds(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, animationSeed, OUTLINE_COLOR, CORNER_COLOR);
    }

    public static void renderOutlinedBounds(VertexConsumer consumer, Matrix4f matrix,
                                            float minX, float minY, float minZ,
                                            float maxX, float maxY, float maxZ,
                                            long animationSeed, Vector3f outlineColor, Vector3f cornerColor) {
        float wave = 0.72F + 0.28F * Mth.sin(animationSeed * 0.18F);
        float edgeAlpha = 0.18F + 0.14F * wave;
        drawEdgeX(consumer, matrix, minX, minY, minZ, maxX, edgeAlpha, outlineColor);
        drawEdgeX(consumer, matrix, minX, minY, maxZ, maxX, edgeAlpha, outlineColor);
        drawEdgeX(consumer, matrix, minX, maxY, minZ, maxX, edgeAlpha, outlineColor);
        drawEdgeX(consumer, matrix, minX, maxY, maxZ, maxX, edgeAlpha, outlineColor);

        drawEdgeY(consumer, matrix, minX, minY, minZ, maxY, edgeAlpha, outlineColor);
        drawEdgeY(consumer, matrix, maxX, minY, minZ, maxY, edgeAlpha, outlineColor);
        drawEdgeY(consumer, matrix, minX, minY, maxZ, maxY, edgeAlpha, outlineColor);
        drawEdgeY(consumer, matrix, maxX, minY, maxZ, maxY, edgeAlpha, outlineColor);

        drawEdgeZ(consumer, matrix, minX, minY, minZ, maxZ, edgeAlpha, outlineColor);
        drawEdgeZ(consumer, matrix, maxX, minY, minZ, maxZ, edgeAlpha, outlineColor);
        drawEdgeZ(consumer, matrix, minX, maxY, minZ, maxZ, edgeAlpha, outlineColor);
        drawEdgeZ(consumer, matrix, maxX, maxY, minZ, maxZ, edgeAlpha, outlineColor);

        drawCornerCube(consumer, matrix, new Vector3f(minX, minY, minZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(maxX, minY, minZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(minX, minY, maxZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(maxX, minY, maxZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(minX, maxY, minZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(maxX, maxY, minZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(minX, maxY, maxZ), edgeAlpha * 1.8F, cornerColor);
        drawCornerCube(consumer, matrix, new Vector3f(maxX, maxY, maxZ), edgeAlpha * 1.8F, cornerColor);
    }

    private static void drawEdgeX(VertexConsumer consumer, Matrix4f matrix, float minX, float y, float z, float maxX, float alpha, Vector3f color) {
        drawBox(consumer, matrix, minX, y - EDGE_HALF_WIDTH, z - EDGE_HALF_WIDTH, maxX, y + EDGE_HALF_WIDTH, z + EDGE_HALF_WIDTH,
                color.x(), color.y(), color.z(), alpha);
    }

    private static void drawEdgeY(VertexConsumer consumer, Matrix4f matrix, float x, float minY, float z, float maxY, float alpha, Vector3f color) {
        drawBox(consumer, matrix, x - EDGE_HALF_WIDTH, minY, z - EDGE_HALF_WIDTH, x + EDGE_HALF_WIDTH, maxY, z + EDGE_HALF_WIDTH,
                color.x(), color.y(), color.z(), alpha);
    }

    private static void drawEdgeZ(VertexConsumer consumer, Matrix4f matrix, float x, float y, float minZ, float maxZ, float alpha, Vector3f color) {
        drawBox(consumer, matrix, x - EDGE_HALF_WIDTH, y - EDGE_HALF_WIDTH, minZ, x + EDGE_HALF_WIDTH, y + EDGE_HALF_WIDTH, maxZ,
                color.x(), color.y(), color.z(), alpha);
    }

    private static void drawCornerCube(VertexConsumer consumer, Matrix4f matrix, Vector3f center, float alpha, Vector3f color) {
        drawBox(consumer, matrix,
                center.x() - CORNER_HALF_SIZE, center.y() - CORNER_HALF_SIZE, center.z() - CORNER_HALF_SIZE,
                center.x() + CORNER_HALF_SIZE, center.y() + CORNER_HALF_SIZE, center.z() + CORNER_HALF_SIZE,
                color.x(), color.y(), color.z(), alpha);
    }

    private static void drawBox(VertexConsumer consumer, Matrix4f matrix,
                                float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ,
                                float red, float green, float blue, float alpha) {
        addQuad(matrix, consumer,
                new Vector3f(minX, minY, minZ), new Vector3f(minX, maxY, minZ), new Vector3f(maxX, maxY, minZ), new Vector3f(maxX, minY, minZ),
                red, green, blue, alpha);
        addQuad(matrix, consumer,
                new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(minX, maxY, maxZ), new Vector3f(minX, minY, maxZ),
                red, green, blue, alpha);
        addQuad(matrix, consumer,
                new Vector3f(minX, minY, maxZ), new Vector3f(minX, maxY, maxZ), new Vector3f(minX, maxY, minZ), new Vector3f(minX, minY, minZ),
                red, green, blue, alpha * 0.92F);
        addQuad(matrix, consumer,
                new Vector3f(maxX, minY, minZ), new Vector3f(maxX, maxY, minZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(maxX, minY, maxZ),
                red, green, blue, alpha * 0.92F);
        addQuad(matrix, consumer,
                new Vector3f(minX, maxY, minZ), new Vector3f(minX, maxY, maxZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(maxX, maxY, minZ),
                red, green, blue, alpha * 0.78F);
        addQuad(matrix, consumer,
                new Vector3f(minX, minY, maxZ), new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ), new Vector3f(maxX, minY, maxZ),
                red, green, blue, alpha * 0.78F);
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer consumer, Vector3f first, Vector3f second, Vector3f third, Vector3f fourth,
                                float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, first.x(), first.y(), first.z()).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, second.x(), second.y(), second.z()).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, third.x(), third.y(), third.z()).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, fourth.x(), fourth.y(), fourth.z()).setColor(red, green, blue, alpha);
    }

    private static Vector3f blockCorner(BlockPos pos, Vec3 cameraPos) {
        return new Vector3f(
                (float) (pos.getX() - cameraPos.x),
                (float) (pos.getY() - cameraPos.y),
                (float) (pos.getZ() - cameraPos.z)
        );
    }

    private static Vector3f directionVector(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static void applyPlaneFacingRotation(PoseStack poseStack, Vector3f planeNormal) {
        if (Math.abs(planeNormal.y()) > 0.9F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            return;
        }
        if (Math.abs(planeNormal.x()) > 0.9F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }
}
