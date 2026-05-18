package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.debug.PylonDebugOverlayState;
import de.artemis.matterworks.common.debug.SideConfigDebugOverlayState;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import de.artemis.matterworks.common.menu.SideConfigOrientation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MatterPylonBlockEntityRenderer<T extends MatterPylonBlockEntity> implements BlockEntityRenderer<T> {
    private static final float LINK_RED = 0.72F;
    private static final float LINK_GREEN = 0.36F;
    private static final float LINK_BLUE = 0.96F;
    private static final float LINK_ALPHA = 0.85F;
    private static final float LINK_WIDTH = 0.032F;
    private static final float CHANNEL_LINK_ALPHA = 0.95F;
    private static final float CHANNEL_LINK_WIDTH = 0.018F;
    private static final float CHANNEL_LANE_RADIUS = 0.11F;
    private static final int LINK_CONTROL_SEGMENTS = 8;
    private static final int LINK_SUBDIVISIONS = 3;
    private static final int LINK_RESHAPE_INTERVAL = 4;
    private static final float LINK_PRIMARY_AMPLITUDE_FACTOR = 0.055F;
    private static final float LINK_SECONDARY_AMPLITUDE_FACTOR = 0.022F;
    private static final float LINK_MIN_PRIMARY_AMPLITUDE = 0.010F;
    private static final float LINK_MAX_PRIMARY_AMPLITUDE = 0.075F;

    public MatterPylonBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, getViewDistance())
                || blockEntity.getRenderBoundingBox().inflate(4.0D).contains(cameraPos);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!PylonDebugOverlayState.isEnabled()) {
            return;
        }

        Font font = Minecraft.getInstance().font;

        if (PylonDebugOverlayState.isEnabled()) {
            poseStack.pushPose();
            poseStack.translate(0.5D, 1.24D, 0.5D);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(0.025F, -0.025F, 0.025F);

            for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
                int transferAmount = blockEntity.getTransferDisplayAmount(channel);
                MatterPylonBlockEntity.TransferDisplayRole role = blockEntity.getTransferDisplayRole(channel);
                String text = formatChannelText(channel, role, transferAmount);
                int color = getChannelColor(channel, transferAmount > 0);
                float x = -font.width(text) / 2.0F;
                float y = channel * 10.0F;
                font.drawInBatch(text, x, y, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
            }
            String capText = formatCapText(blockEntity);
            font.drawInBatch(capText, -font.width(capText) / 2.0F, MatterPylonBlockEntity.CHANNEL_COUNT * 10.0F + 2.0F,
                    0xFFC5C7CC, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
            if (blockEntity.supportsUpgradeCrystals()) {
                String crystalText = formatCrystalText(blockEntity);
                font.drawInBatch(crystalText, -font.width(crystalText) / 2.0F, MatterPylonBlockEntity.CHANNEL_COUNT * 10.0F + 12.0F,
                        0xFFD8C9FF, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
            }
            poseStack.popPose();
        }
    }

    public static void renderMatterNetworkLinks(Iterable<MatterPylonBlockEntity> nodes, PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos, long gameTime) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (MatterPylonBlockEntity node : nodes) {
            BlockPos nodePos = node.getBlockPos();
            for (BlockPos linkedPos : node.getLinkedNodePositions()) {
                if (nodePos.asLong() >= linkedPos.asLong()) {
                    continue;
                }
                if (!(node.getLevel().getBlockEntity(linkedPos) instanceof MatterPylonBlockEntity other)
                        || !other.getLinkedNodePositions().contains(nodePos)) {
                    continue;
                }

                drawLightningBeam(matrix, consumer, nodePos, linkedPos, cameraPos, gameTime, LINK_RED, LINK_GREEN, LINK_BLUE, LINK_ALPHA, LINK_WIDTH, -1);
                int activeChannelMask = node.getActiveLinkChannelMask(linkedPos) | other.getActiveLinkChannelMask(nodePos);
                for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
                    if ((activeChannelMask & (1 << channel)) == 0) {
                        continue;
                    }
                    float[] color = getChannelBeamColor(channel);
                    drawLightningBeam(matrix, consumer, nodePos, linkedPos, cameraPos, gameTime, color[0], color[1], color[2], CHANNEL_LINK_ALPHA, CHANNEL_LINK_WIDTH, channel);
                }
            }
        }
    }

    public static void renderSideConfigOverlays(Iterable<BlockEntity> blockEntities, PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos) {
        if (!SideConfigDebugOverlayState.isEnabled()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        for (BlockEntity blockEntity : blockEntities) {
            if (!(blockEntity instanceof SideConfigurableBlockEntity configurable)) {
                continue;
            }

            BlockPos blockPos = blockEntity.getBlockPos();
            if (!Vec3.atCenterOf(blockPos).closerThan(cameraPos, 96.0D)) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(blockPos.getX() - cameraPos.x, blockPos.getY() - cameraPos.y, blockPos.getZ() - cameraPos.z);
            renderSideConfigOverlay(blockEntity.getBlockState(), configurable, poseStack, buffer, 0x00F000F0, font);
            poseStack.popPose();
        }
    }

    private static void drawLightningBeam(Matrix4f matrix, VertexConsumer consumer, BlockPos startPos, BlockPos endPos, Vec3 cameraPos, long gameTime,
                                          float red, float green, float blue, float alpha, float width, int channel) {
        Vector3f start = new Vector3f(
                (float) (startPos.getX() + 0.5D - cameraPos.x),
                (float) (startPos.getY() + 0.5D - cameraPos.y),
                (float) (startPos.getZ() + 0.5D - cameraPos.z)
        );
        Vector3f end = new Vector3f(
                (float) (endPos.getX() + 0.5D - cameraPos.x),
                (float) (endPos.getY() + 0.5D - cameraPos.y),
                (float) (endPos.getZ() + 0.5D - cameraPos.z)
        );

        Vector3f direction = new Vector3f(end).sub(start);
        float length = direction.length();
        if (length < 1.0E-4F) {
            return;
        }

        direction.normalize();
        Vector3f perpendicularA = direction.cross(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f());
        if (perpendicularA.lengthSquared() < 1.0E-4F) {
            perpendicularA = direction.cross(new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f());
        }
        perpendicularA.normalize();
        Vector3f perpendicularB = direction.cross(perpendicularA, new Vector3f()).normalize();
        if (channel >= 0) {
            float angle = (float) (channel * (Math.PI * 2.0D / MatterPylonBlockEntity.CHANNEL_COUNT));
            Vector3f laneOffset = new Vector3f(perpendicularA).mul((float) Math.cos(angle) * CHANNEL_LANE_RADIUS)
                    .add(new Vector3f(perpendicularB).mul((float) Math.sin(angle) * CHANNEL_LANE_RADIUS));
            start.add(laneOffset);
            end.add(laneOffset);
        }
        long phase = gameTime / LINK_RESHAPE_INTERVAL;
        float primaryAmplitude = Mth.clamp(length * LINK_PRIMARY_AMPLITUDE_FACTOR, LINK_MIN_PRIMARY_AMPLITUDE, LINK_MAX_PRIMARY_AMPLITUDE);
        float secondaryAmplitude = primaryAmplitude * (LINK_SECONDARY_AMPLITUDE_FACTOR / LINK_PRIMARY_AMPLITUDE_FACTOR);

        List<Vector3f> controlPoints = new ArrayList<>(LINK_CONTROL_SEGMENTS + 1);
        controlPoints.add(new Vector3f(start));
        long phaseSeed = orderedPairSeed(startPos, endPos) ^ (phase * 0x9E3779B97F4A7C15L);
        for (int segment = 1; segment < LINK_CONTROL_SEGMENTS; segment++) {
            float t = segment / (float) LINK_CONTROL_SEGMENTS;
            float taper = 1.0F - Math.abs(t - 0.5F) * 1.6F;
            taper = Mth.clamp(taper, 0.18F, 1.0F);
            float primaryOffset = signedNoise(phaseSeed + segment * 31L) * primaryAmplitude * taper;
            float secondaryOffset = signedNoise(phaseSeed + segment * 79L + 11L) * secondaryAmplitude * taper;

            Vector3f point = new Vector3f(
                    Mth.lerp(t, start.x(), end.x()),
                    Mth.lerp(t, start.y(), end.y()),
                    Mth.lerp(t, start.z(), end.z())
            );
            point.add(new Vector3f(perpendicularA).mul(primaryOffset));
            point.add(new Vector3f(perpendicularB).mul(secondaryOffset));
            controlPoints.add(point);
        }
        controlPoints.add(new Vector3f(end));

        List<Vector3f> points = smoothControlPoints(controlPoints);

        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f segmentStart = points.get(i);
            Vector3f segmentEnd = points.get(i + 1);
            Vector3f segmentDirection = new Vector3f(segmentEnd).sub(segmentStart);
            if (segmentDirection.lengthSquared() < 1.0E-5F) {
                continue;
            }
            segmentDirection.normalize();
            Vector3f axisA = segmentDirection.cross(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f());
            if (axisA.lengthSquared() < 1.0E-4F) {
                axisA = segmentDirection.cross(new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f());
            }
            axisA.normalize().mul(width);
            Vector3f axisB = segmentDirection.cross(new Vector3f(axisA).normalize(), new Vector3f()).normalize().mul(width);

            addBoltPrism(matrix, consumer, segmentStart, segmentEnd, axisA, axisB, red, green, blue, alpha);
        }
    }

    private static void addBoltPrism(Matrix4f matrix, VertexConsumer consumer, Vector3f start, Vector3f end, Vector3f axisA, Vector3f axisB,
                                     float red, float green, float blue, float alpha) {
        Vector3f startA = new Vector3f(start).add(axisA);
        Vector3f startB = new Vector3f(start).add(axisB);
        Vector3f startNegA = new Vector3f(start).sub(axisA);
        Vector3f startNegB = new Vector3f(start).sub(axisB);
        Vector3f endA = new Vector3f(end).add(axisA);
        Vector3f endB = new Vector3f(end).add(axisB);
        Vector3f endNegA = new Vector3f(end).sub(axisA);
        Vector3f endNegB = new Vector3f(end).sub(axisB);

        addQuad(matrix, consumer, startA, startB, endB, endA, red, green, blue, alpha);
        addQuad(matrix, consumer, startB, startNegA, endNegA, endB, red, green, blue, alpha);
        addQuad(matrix, consumer, startNegA, startNegB, endNegB, endNegA, red, green, blue, alpha);
        addQuad(matrix, consumer, startNegB, startA, endA, endNegB, red, green, blue, alpha);

        float coreAlpha = Math.min(1.0F, alpha + 0.10F);
        addQuad(matrix, consumer, startA, startNegA, endNegA, endA, red, green, blue, coreAlpha);
        addQuad(matrix, consumer, startB, startNegB, endNegB, endB, red, green, blue, coreAlpha);
    }

    private static void addQuad(Matrix4f matrix, VertexConsumer consumer, Vector3f first, Vector3f second, Vector3f third, Vector3f fourth,
                                float red, float green, float blue, float alpha) {
        addVertex(consumer, matrix, first, red, green, blue, alpha);
        addVertex(consumer, matrix, second, red, green, blue, alpha);
        addVertex(consumer, matrix, third, red, green, blue, alpha);
        addVertex(consumer, matrix, fourth, red, green, blue, alpha);
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, Vector3f point, float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, point.x(), point.y(), point.z()).setColor(red, green, blue, alpha);
    }

    private static float[] getChannelBeamColor(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> new float[]{0.33F, 0.82F, 0.45F};
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> new float[]{0.88F, 0.28F, 0.28F};
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> new float[]{0.26F, 0.58F, 0.95F};
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> new float[]{0.94F, 0.74F, 0.18F};
            default -> new float[]{1.0F, 1.0F, 1.0F};
        };
    }

    private static List<Vector3f> smoothControlPoints(List<Vector3f> controlPoints) {
        if (controlPoints.size() < 2) {
            return controlPoints;
        }

        List<Vector3f> smoothed = new ArrayList<>((controlPoints.size() - 1) * LINK_SUBDIVISIONS + 1);
        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Vector3f p0 = i > 0 ? controlPoints.get(i - 1) : controlPoints.get(i);
            Vector3f p1 = controlPoints.get(i);
            Vector3f p2 = controlPoints.get(i + 1);
            Vector3f p3 = i + 2 < controlPoints.size() ? controlPoints.get(i + 2) : p2;

            for (int step = 0; step < LINK_SUBDIVISIONS; step++) {
                float t = step / (float) LINK_SUBDIVISIONS;
                smoothed.add(catmullRom(p0, p1, p2, p3, t));
            }
        }
        smoothed.add(new Vector3f(controlPoints.get(controlPoints.size() - 1)));
        return smoothed;
    }

    private static Vector3f catmullRom(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return new Vector3f(
                0.5F * ((2.0F * p1.x()) + (-p0.x() + p2.x()) * t
                        + (2.0F * p0.x() - 5.0F * p1.x() + 4.0F * p2.x() - p3.x()) * t2
                        + (-p0.x() + 3.0F * p1.x() - 3.0F * p2.x() + p3.x()) * t3),
                0.5F * ((2.0F * p1.y()) + (-p0.y() + p2.y()) * t
                        + (2.0F * p0.y() - 5.0F * p1.y() + 4.0F * p2.y() - p3.y()) * t2
                        + (-p0.y() + 3.0F * p1.y() - 3.0F * p2.y() + p3.y()) * t3),
                0.5F * ((2.0F * p1.z()) + (-p0.z() + p2.z()) * t
                        + (2.0F * p0.z() - 5.0F * p1.z() + 4.0F * p2.z() - p3.z()) * t2
                        + (-p0.z() + 3.0F * p1.z() - 3.0F * p2.z() + p3.z()) * t3)
        );
    }

    private static long orderedPairSeed(BlockPos first, BlockPos second) {
        long a = first.asLong();
        long b = second.asLong();
        long low = Math.min(a, b);
        long high = Math.max(a, b);
        return mix64(low * 31L + high * 17L);
    }

    private static float signedNoise(long seed) {
        return ((mix64(seed) >>> 40) / (float) (1L << 24)) * 2.0F - 1.0F;
    }

    private static long mix64(long value) {
        value ^= (value >>> 33);
        value *= 0xff51afd7ed558ccdl;
        value ^= (value >>> 33);
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= (value >>> 33);
        return value;
    }

    private static String formatChannelText(int channel, MatterPylonBlockEntity.TransferDisplayRole role, int amount) {
        String prefix = switch (role) {
            case SOURCE -> "+";
            case SINK -> "-";
            case TRANSIT, IDLE -> "";
        };
        return prefix + amount + getUnitSuffix(channel);
    }

    private static String getUnitSuffix(int channel) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> "fe/t";
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> "i/t";
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> "mb/t";
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> "rs";
            default -> "/t";
        };
    }

    private static int getChannelColor(int channel, boolean active) {
        return switch (channel) {
            case MatterPylonBlockEntity.CHANNEL_ENERGY -> active ? 0xFF55D26A : 0xCC55D26A;
            case MatterPylonBlockEntity.CHANNEL_ITEMS -> active ? 0xFFE14B4B : 0xCCE14B4B;
            case MatterPylonBlockEntity.CHANNEL_FLUIDS -> active ? 0xFF4C86F5 : 0xCC4C86F5;
            case MatterPylonBlockEntity.CHANNEL_REDSTONE -> active ? 0xFFF0C34A : 0xCCF0C34A;
            default -> 0xFFC5C7CC;
        };
    }

    private static String formatCapText(MatterPylonBlockEntity blockEntity) {
        return "cap e" + blockEntity.getEffectiveTransferCap(MatterPylonBlockEntity.CHANNEL_ENERGY)
                + " i" + blockEntity.getEffectiveTransferCap(MatterPylonBlockEntity.CHANNEL_ITEMS)
                + " f" + blockEntity.getEffectiveTransferCap(MatterPylonBlockEntity.CHANNEL_FLUIDS);
    }

    private static String formatCrystalText(MatterPylonBlockEntity blockEntity) {
        return "cry r" + blockEntity.getActiveCrystalCount(MatterPylonBlockEntity.CHANNEL_ITEMS)
                + " g" + blockEntity.getActiveCrystalCount(MatterPylonBlockEntity.CHANNEL_ENERGY)
                + " b" + blockEntity.getActiveCrystalCount(MatterPylonBlockEntity.CHANNEL_FLUIDS)
                + " up " + blockEntity.getCrystalMaintenanceEnergyPerTick() + "fe/t";
    }

    private static void renderSideConfigOverlay(BlockState blockState, SideConfigurableBlockEntity configurable, PoseStack poseStack, MultiBufferSource buffer, int packedLight, Font font) {
        Direction frontFacing = SideConfigOrientation.resolveFrontFacing(blockState);
        for (Direction side : Direction.values()) {
            List<TextSegment> segments = buildSideConfigSegments(configurable, side);
            if (segments.isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            translateToSide(poseStack, side);
            poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            poseStack.scale(0.015F, -0.015F, 0.015F);

            String sideLabel = getRelativeSideLabel(side, frontFacing);
            float sideLabelX = -font.width(sideLabel) / 2.0F;
            font.drawInBatch(sideLabel, sideLabelX, -9.0F, 0xFFE0E3E8, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);

            float totalWidth = 0.0F;
            for (TextSegment segment : segments) {
                totalWidth += font.width(segment.text());
            }
            float x = -totalWidth / 2.0F;
            for (TextSegment segment : segments) {
                font.drawInBatch(segment.text(), x, 1.0F, segment.color(), false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, packedLight);
                x += font.width(segment.text());
            }
            poseStack.popPose();
        }
    }

    private static List<TextSegment> buildSideConfigSegments(SideConfigurableBlockEntity configurable, Direction side) {
        List<TextSegment> segments = new ArrayList<>();
        for (SideConfigType type : SideConfigType.values()) {
            if (!configurable.supportsSideConfigType(type)) {
                continue;
            }
            if (!segments.isEmpty()) {
                segments.add(new TextSegment("  ", 0xFFD0D4DB));
            }
            segments.add(new TextSegment(getTypeShortLabel(type) + "=", getTypeColor(type)));
            segments.add(new TextSegment(configurable.getSideAccessMode(type, side).getShortLabel(), 0xFFD0D4DB));
        }
        return segments;
    }

    private static void translateToSide(PoseStack poseStack, Direction side) {
        float verticalOffset = 0.18F;
        switch (side) {
            case DOWN -> poseStack.translate(0.5D, -0.05D, 0.5D);
            case UP -> poseStack.translate(0.5D, 1.05D, 0.5D);
            case NORTH -> poseStack.translate(0.5D, 0.5D + verticalOffset, -0.05D);
            case SOUTH -> poseStack.translate(0.5D, 0.5D + verticalOffset, 1.05D);
            case WEST -> poseStack.translate(-0.05D, 0.5D + verticalOffset, 0.5D);
            case EAST -> poseStack.translate(1.05D, 0.5D + verticalOffset, 0.5D);
        }
    }

    private static String getRelativeSideLabel(Direction side, Direction frontFacing) {
        if (side == Direction.UP) {
            return "Up";
        }
        if (side == Direction.DOWN) {
            return "Down";
        }
        if (side == frontFacing) {
            return "Front";
        }
        if (side == frontFacing.getOpposite()) {
            return "Back";
        }
        if (side == frontFacing.getCounterClockWise()) {
            return "Left";
        }
        if (side == frontFacing.getClockWise()) {
            return "Right";
        }
        return side.getName();
    }

    private static String getTypeShortLabel(SideConfigType type) {
        return switch (type) {
            case ITEMS -> "I";
            case FLUIDS -> "F";
            case ENERGY -> "E";
        };
    }

    private static int getTypeColor(SideConfigType type) {
        return switch (type) {
            case ITEMS -> 0xFFD9A441;
            case FLUIDS -> 0xFF55A8FF;
            case ENERGY -> 0xFFE35B47;
        };
    }

    private record TextSegment(String text, int color) {
    }
}
