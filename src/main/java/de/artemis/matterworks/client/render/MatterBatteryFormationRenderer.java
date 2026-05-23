package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MatterBatteryFormationRenderer {
    private static final Vector3f OUTLINE_COLOR = new Vector3f(0.28F, 0.90F, 0.98F);
    private static final Vector3f CORE_COLOR = new Vector3f(0.82F, 0.45F, 1.0F);
    private static final float EDGE_HALF_WIDTH = 0.035F;

    private MatterBatteryFormationRenderer() {
    }

    public static void renderFormationOverlay(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos, long gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            MatterBatteryFormationOverlayState.clear();
            return;
        }

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        for (MatterBatteryFormationOverlayState.FormationPulse pulse : MatterBatteryFormationOverlayState.getActivePulses(gameTime)) {
            renderPulse(matrix, consumer, cameraPos, pulse, gameTime);
        }
    }

    public static void tickParticles() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            MatterBatteryFormationOverlayState.clear();
            return;
        }

        long gameTime = level.getGameTime();
        if ((gameTime & 1L) != 0L) {
            return;
        }

        for (MatterBatteryFormationOverlayState.FormationPulse pulse : MatterBatteryFormationOverlayState.getActivePulses(gameTime)) {
            spawnOutlineParticles(level, pulse, gameTime);
        }
    }

    private static void renderPulse(Matrix4f matrix, VertexConsumer consumer, Vec3 cameraPos,
                                    MatterBatteryFormationOverlayState.FormationPulse pulse, long gameTime) {
        float minX = (float) (pulse.originPos().getX() - cameraPos.x);
        float minY = (float) (pulse.originPos().getY() - cameraPos.y);
        float minZ = (float) (pulse.originPos().getZ() - cameraPos.z);
        float maxX = minX + MatterBatteryMultiblockDefinition.STRUCTURE_SIZE;
        float maxY = minY + MatterBatteryMultiblockDefinition.STRUCTURE_SIZE;
        float maxZ = minZ + MatterBatteryMultiblockDefinition.STRUCTURE_SIZE;

        long remainingTicks = Math.max(0L, pulse.expireTick() - gameTime);
        float fade = Math.min(1.0F, remainingTicks / (float) Math.max(1, pulse.durationTicks()));
        float pulseWave = 0.70F + 0.30F * (float) Math.sin((gameTime + pulse.originPos().asLong()) * 0.35D);
        float edgeAlpha = 0.12F + (0.24F * fade * pulseWave);
        float faceAlpha = 0.03F + (0.06F * fade * pulseWave);

        drawBox(consumer, matrix, minX - EDGE_HALF_WIDTH, minY - EDGE_HALF_WIDTH, minZ - EDGE_HALF_WIDTH,
                maxX + EDGE_HALF_WIDTH, maxY + EDGE_HALF_WIDTH, maxZ + EDGE_HALF_WIDTH,
                OUTLINE_COLOR.x(), OUTLINE_COLOR.y(), OUTLINE_COLOR.z(), faceAlpha);

        drawEdgeX(consumer, matrix, minX, minY, minZ, maxX, edgeAlpha);
        drawEdgeX(consumer, matrix, minX, minY, maxZ, maxX, edgeAlpha);
        drawEdgeX(consumer, matrix, minX, maxY, minZ, maxX, edgeAlpha);
        drawEdgeX(consumer, matrix, minX, maxY, maxZ, maxX, edgeAlpha);

        drawEdgeY(consumer, matrix, minX, minY, minZ, maxY, edgeAlpha);
        drawEdgeY(consumer, matrix, maxX, minY, minZ, maxY, edgeAlpha);
        drawEdgeY(consumer, matrix, minX, minY, maxZ, maxY, edgeAlpha);
        drawEdgeY(consumer, matrix, maxX, minY, maxZ, maxY, edgeAlpha);

        drawEdgeZ(consumer, matrix, minX, minY, minZ, maxZ, edgeAlpha);
        drawEdgeZ(consumer, matrix, maxX, minY, minZ, maxZ, edgeAlpha);
        drawEdgeZ(consumer, matrix, minX, maxY, minZ, maxZ, edgeAlpha);
        drawEdgeZ(consumer, matrix, maxX, maxY, minZ, maxZ, edgeAlpha);

        float cornerAlpha = Math.min(0.50F, edgeAlpha * 1.8F);
        drawCornerCube(consumer, matrix, minX, minY, minZ, cornerAlpha);
        drawCornerCube(consumer, matrix, maxX, minY, minZ, cornerAlpha);
        drawCornerCube(consumer, matrix, minX, minY, maxZ, cornerAlpha);
        drawCornerCube(consumer, matrix, maxX, minY, maxZ, cornerAlpha);
        drawCornerCube(consumer, matrix, minX, maxY, minZ, cornerAlpha);
        drawCornerCube(consumer, matrix, maxX, maxY, minZ, cornerAlpha);
        drawCornerCube(consumer, matrix, minX, maxY, maxZ, cornerAlpha);
        drawCornerCube(consumer, matrix, maxX, maxY, maxZ, cornerAlpha);
    }

    private static void spawnOutlineParticles(ClientLevel level, MatterBatteryFormationOverlayState.FormationPulse pulse, long gameTime) {
        double minX = pulse.originPos().getX();
        double minY = pulse.originPos().getY();
        double minZ = pulse.originPos().getZ();
        double maxX = minX + MatterBatteryMultiblockDefinition.STRUCTURE_SIZE;
        double maxY = minY + MatterBatteryMultiblockDefinition.STRUCTURE_SIZE;
        double maxZ = minZ + MatterBatteryMultiblockDefinition.STRUCTURE_SIZE;

        for (int i = 0; i < 5; i++) {
            double t = level.random.nextDouble();
            int edge = level.random.nextInt(12);
            Vec3 edgePos = sampleEdge(minX, minY, minZ, maxX, maxY, maxZ, edge, t);
            double driftX = (level.random.nextDouble() - 0.5D) * 0.02D;
            double driftY = 0.005D + (level.random.nextDouble() * 0.015D);
            double driftZ = (level.random.nextDouble() - 0.5D) * 0.02D;
            level.addParticle(new DustParticleOptions(OUTLINE_COLOR, 0.85F), edgePos.x, edgePos.y, edgePos.z, driftX, driftY, driftZ);
            if (((gameTime + i) & 3L) == 0L) {
                level.addParticle(ParticleTypes.END_ROD, edgePos.x, edgePos.y, edgePos.z, driftX * 0.35D, driftY * 0.5D, driftZ * 0.35D);
            }
            if (level.random.nextFloat() < 0.30F) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, edgePos.x, edgePos.y, edgePos.z, driftX * 0.6D, driftY * 0.6D, driftZ * 0.6D);
            }
        }
    }

    private static Vec3 sampleEdge(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int edgeIndex, double t) {
        return switch (edgeIndex) {
            case 0 -> new Vec3(lerp(minX, maxX, t), minY, minZ);
            case 1 -> new Vec3(lerp(minX, maxX, t), minY, maxZ);
            case 2 -> new Vec3(lerp(minX, maxX, t), maxY, minZ);
            case 3 -> new Vec3(lerp(minX, maxX, t), maxY, maxZ);
            case 4 -> new Vec3(minX, lerp(minY, maxY, t), minZ);
            case 5 -> new Vec3(maxX, lerp(minY, maxY, t), minZ);
            case 6 -> new Vec3(minX, lerp(minY, maxY, t), maxZ);
            case 7 -> new Vec3(maxX, lerp(minY, maxY, t), maxZ);
            case 8 -> new Vec3(minX, minY, lerp(minZ, maxZ, t));
            case 9 -> new Vec3(maxX, minY, lerp(minZ, maxZ, t));
            case 10 -> new Vec3(minX, maxY, lerp(minZ, maxZ, t));
            default -> new Vec3(maxX, maxY, lerp(minZ, maxZ, t));
        };
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static void drawEdgeX(VertexConsumer consumer, Matrix4f matrix, float minX, float y, float z, float maxX, float alpha) {
        drawBox(consumer, matrix, minX, y - EDGE_HALF_WIDTH, z - EDGE_HALF_WIDTH, maxX, y + EDGE_HALF_WIDTH, z + EDGE_HALF_WIDTH,
                OUTLINE_COLOR.x(), OUTLINE_COLOR.y(), OUTLINE_COLOR.z(), alpha);
    }

    private static void drawEdgeY(VertexConsumer consumer, Matrix4f matrix, float x, float minY, float z, float maxY, float alpha) {
        drawBox(consumer, matrix, x - EDGE_HALF_WIDTH, minY, z - EDGE_HALF_WIDTH, x + EDGE_HALF_WIDTH, maxY, z + EDGE_HALF_WIDTH,
                OUTLINE_COLOR.x(), OUTLINE_COLOR.y(), OUTLINE_COLOR.z(), alpha);
    }

    private static void drawEdgeZ(VertexConsumer consumer, Matrix4f matrix, float x, float y, float minZ, float maxZ, float alpha) {
        drawBox(consumer, matrix, x - EDGE_HALF_WIDTH, y - EDGE_HALF_WIDTH, minZ, x + EDGE_HALF_WIDTH, y + EDGE_HALF_WIDTH, maxZ,
                OUTLINE_COLOR.x(), OUTLINE_COLOR.y(), OUTLINE_COLOR.z(), alpha);
    }

    private static void drawCornerCube(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float alpha) {
        float halfSize = 0.08F;
        drawBox(consumer, matrix, x - halfSize, y - halfSize, z - halfSize, x + halfSize, y + halfSize, z + halfSize,
                CORE_COLOR.x(), CORE_COLOR.y(), CORE_COLOR.z(), alpha);
    }

    private static void drawBox(VertexConsumer consumer, Matrix4f matrix,
                                float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ,
                                float red, float green, float blue, float alpha) {
        addFace(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addFace(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addFace(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, red, green, blue, alpha * 0.92F);
        addFace(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha * 0.92F);
        addFace(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha * 0.78F);
        addFace(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha * 0.78F);
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
