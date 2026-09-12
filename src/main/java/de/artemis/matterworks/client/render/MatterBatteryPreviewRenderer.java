package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockLayout;
import de.artemis.matterworks.common.multiblock.MatterBatteryPreviewPlacementHelper;
import de.artemis.matterworks.common.multiblock.MultiblockRole;
import de.artemis.matterworks.common.multiblock.MultiblockTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Optional;

public final class MatterBatteryPreviewRenderer {
    private static final Vector3f INVALID_OUTLINE_COLOR = new Vector3f(0.96F, 0.28F, 0.28F);
    private static final Vector3f INVALID_CORNER_COLOR = new Vector3f(1.0F, 0.58F, 0.58F);
    private static final Vector3f WRONG_BLOCK_OUTLINE_COLOR = new Vector3f(0.96F, 0.24F, 0.24F);
    private static final Vector3f WRONG_BLOCK_CORNER_COLOR = new Vector3f(1.0F, 0.50F, 0.50F);
    private static final ResourceLocation CORE_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/obsidian.png");
    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
    private static final ResourceLocation CASING_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone_bricks.png");
    private static final ResourceLocation PORT_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/copper_block.png");
    private static final ResourceLocation GLASS_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/glass.png");
    private static final ResourceLocation CAPACITOR_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/sea_lantern.png");

    private MatterBatteryPreviewRenderer() {
    }

    public static void renderPreviewOverlay(PoseStack poseStack, MultiBufferSource buffer, Vec3 cameraPos) {
        if (!MatterBatteryPreviewState.isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            MatterBatteryPreviewState.clear();
            return;
        }

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        float minX = (float) (MatterBatteryPreviewState.getMinPos().getX() - cameraPos.x);
        float minY = (float) (MatterBatteryPreviewState.getMinPos().getY() - cameraPos.y);
        float minZ = (float) (MatterBatteryPreviewState.getMinPos().getZ() - cameraPos.z);
        float maxX = (float) (MatterBatteryPreviewState.getMaxPos().getX() + 1 - cameraPos.x);
        float maxY = (float) (MatterBatteryPreviewState.getMaxPos().getY() + 1 - cameraPos.y);
        float maxZ = (float) (MatterBatteryPreviewState.getMaxPos().getZ() + 1 - cameraPos.z);

        if (!MatterBatteryPreviewState.isValid()) {
            SingularityLinkBlockEntityRenderer.renderOutlinedBounds(
                    consumer,
                    matrix,
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ,
                    MatterBatteryPreviewState.getMinPos().asLong(),
                    INVALID_OUTLINE_COLOR,
                    INVALID_CORNER_COLOR
            );
            return;
        }

        SingularityLinkBlockEntityRenderer.renderOutlinedBounds(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, MatterBatteryPreviewState.getMinPos().asLong());
        if (!MatterBatteryPreviewState.isLocked()) {
            return;
        }
        renderLayerHighlights(minecraft, buffer, matrix, cameraPos);
    }

    public static String getRequirementLabel(BlockPos localPos) {
        MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, MatterBatteryPreviewState.getWidth(), MatterBatteryPreviewState.getHeight(), MatterBatteryPreviewState.getDepth());
        return switch (role) {
            case CONTROLLER -> "Battery Core";
            case FRAME -> "Multiblock Frame";
            case CASING, PORT -> "Multiblock Casing";
            case INTERNAL -> "Matter Capacitor Cell";
        };
    }

    public static String getPlacementLabel(BlockState state) {
        return state.isAir() ? "Air" : state.getBlock().getName().getString();
    }

    public static @Nullable PreviewHit findTargetedPreviewHit(Minecraft minecraft) {
        if (!MatterBatteryPreviewState.isActive() || !MatterBatteryPreviewState.isValid() || !MatterBatteryPreviewState.isLocked() || minecraft.player == null || minecraft.level == null) {
            return null;
        }

        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getViewVector(1.0F).scale(8.0D));
        double bestDistance = Double.MAX_VALUE;
        PreviewHit bestHit = null;

        int selectedLayer = MatterBatteryPreviewState.getSelectedLayer();
        for (int z = 0; z < MatterBatteryPreviewState.getDepth(); z++) {
            for (int x = 0; x < MatterBatteryPreviewState.getWidth(); x++) {
                BlockPos localPos = new BlockPos(x, selectedLayer, z);
                BlockPos worldPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
                Optional<Vec3> intersection = new AABB(worldPos).inflate(0.01D).clip(start, end);
                if (intersection.isEmpty()) {
                    continue;
                }

                double distance = start.distanceToSqr(intersection.get());
                if (distance >= bestDistance) {
                    continue;
                }

                BlockState actualState = minecraft.level.getBlockState(worldPos);
                bestDistance = distance;
                bestHit = new PreviewHit(localPos, worldPos, MatterBatteryMultiblockLayout.getRole(localPos, MatterBatteryPreviewState.getWidth(), MatterBatteryPreviewState.getHeight(), MatterBatteryPreviewState.getDepth()), actualState);
            }
        }

        return bestHit;
    }

    public static boolean isSelectedLayerComplete(Minecraft minecraft) {
        if (!MatterBatteryPreviewState.isActive() || !MatterBatteryPreviewState.isValid() || !MatterBatteryPreviewState.isLocked() || minecraft.level == null) {
            return false;
        }

        int selectedLayer = MatterBatteryPreviewState.getSelectedLayer();
        for (int z = 0; z < MatterBatteryPreviewState.getDepth(); z++) {
            for (int x = 0; x < MatterBatteryPreviewState.getWidth(); x++) {
                BlockPos localPos = new BlockPos(x, selectedLayer, z);
                BlockPos worldPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
                BlockState actualState = minecraft.level.getBlockState(worldPos);
                MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, MatterBatteryPreviewState.getWidth(), MatterBatteryPreviewState.getHeight(), MatterBatteryPreviewState.getDepth());
                if (!MatterBatteryPreviewPlacementHelper.matchesRequirement(role, actualState)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isPreviewComplete(Minecraft minecraft) {
        if (!MatterBatteryPreviewState.isActive() || !MatterBatteryPreviewState.isValid() || !MatterBatteryPreviewState.isLocked() || minecraft.level == null) {
            return false;
        }

        for (int y = 0; y < MatterBatteryPreviewState.getHeight(); y++) {
            for (int z = 0; z < MatterBatteryPreviewState.getDepth(); z++) {
                for (int x = 0; x < MatterBatteryPreviewState.getWidth(); x++) {
                    BlockPos localPos = new BlockPos(x, y, z);
                    BlockPos worldPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
                    BlockState actualState = minecraft.level.getBlockState(worldPos);
                    MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, MatterBatteryPreviewState.getWidth(), MatterBatteryPreviewState.getHeight(), MatterBatteryPreviewState.getDepth());
                    if (!MatterBatteryPreviewPlacementHelper.matchesRequirement(role, actualState)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static void renderLayerHighlights(Minecraft minecraft, MultiBufferSource buffer, Matrix4f matrix, Vec3 cameraPos) {
        int selectedLayer = MatterBatteryPreviewState.getSelectedLayer();
        for (int z = 0; z < MatterBatteryPreviewState.getDepth(); z++) {
            for (int x = 0; x < MatterBatteryPreviewState.getWidth(); x++) {
                BlockPos localPos = new BlockPos(x, selectedLayer, z);
                MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, MatterBatteryPreviewState.getWidth(), MatterBatteryPreviewState.getHeight(), MatterBatteryPreviewState.getDepth());
                BlockPos worldPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
                BlockState actualState = minecraft.level.getBlockState(worldPos);
                boolean matches = MatterBatteryPreviewPlacementHelper.matchesRequirement(role, actualState);
                if (matches) {
                    continue;
                }

                if (isObstructingWrongBlock(actualState)) {
                    VertexConsumer solidConsumer = buffer.getBuffer(RenderType.lightning());
                    drawBrightRedObstruction(solidConsumer, matrix, cameraPos, worldPos);
                    continue;
                }

                ResourceLocation texture = getPreviewTexture(role, actualState);
                VertexConsumer texturedConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
                drawTexturedCube(texturedConsumer, matrix, cameraPos, worldPos, 0.52F, 0.005F);
            }
        }

        VertexConsumer outlineConsumer = buffer.getBuffer(RenderType.lightning());
        for (int z = 0; z < MatterBatteryPreviewState.getDepth(); z++) {
            for (int x = 0; x < MatterBatteryPreviewState.getWidth(); x++) {
                BlockPos localPos = new BlockPos(x, selectedLayer, z);
                MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, MatterBatteryPreviewState.getWidth(), MatterBatteryPreviewState.getHeight(), MatterBatteryPreviewState.getDepth());
                BlockPos worldPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
                BlockState actualState = minecraft.level.getBlockState(worldPos);
                boolean matches = MatterBatteryPreviewPlacementHelper.matchesRequirement(role, actualState);
                if (matches) {
                    continue;
                }

                if (isObstructingWrongBlock(actualState)) {
                    float minX = (float) (worldPos.getX() - cameraPos.x) + 0.08F;
                    float minY = (float) (worldPos.getY() - cameraPos.y) + 0.08F;
                    float minZ = (float) (worldPos.getZ() - cameraPos.z) + 0.08F;
                    float maxX = (float) (worldPos.getX() + 1 - cameraPos.x) - 0.08F;
                    float maxY = (float) (worldPos.getY() + 1 - cameraPos.y) - 0.08F;
                    float maxZ = (float) (worldPos.getZ() + 1 - cameraPos.z) - 0.08F;
                    SingularityLinkBlockEntityRenderer.renderOutlinedBounds(
                            outlineConsumer,
                            matrix,
                            minX,
                            minY,
                            minZ,
                            maxX,
                            maxY,
                            maxZ,
                            worldPos.asLong() + selectedLayer,
                            INVALID_OUTLINE_COLOR,
                            INVALID_CORNER_COLOR
                    );
                    continue;
                }

                float minX = (float) (worldPos.getX() - cameraPos.x) + 0.14F;
                float minY = (float) (worldPos.getY() - cameraPos.y) + 0.14F;
                float minZ = (float) (worldPos.getZ() - cameraPos.z) + 0.14F;
                float maxX = (float) (worldPos.getX() + 1 - cameraPos.x) - 0.14F;
                float maxY = (float) (worldPos.getY() + 1 - cameraPos.y) - 0.14F;
                float maxZ = (float) (worldPos.getZ() + 1 - cameraPos.z) - 0.14F;
                float[] color = getWrongBlockColor();
                SingularityLinkBlockEntityRenderer.renderOutlinedBounds(
                        outlineConsumer,
                        matrix,
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ,
                        worldPos.asLong() + selectedLayer,
                        WRONG_BLOCK_OUTLINE_COLOR,
                        WRONG_BLOCK_CORNER_COLOR
                );
                drawSolidBox(outlineConsumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, color[0], color[1], color[2], 0.11F);
            }
        }
    }

    private static float[] getWrongBlockColor() {
        return new float[]{0.96F, 0.24F, 0.24F};
    }

    private static boolean isObstructingWrongBlock(BlockState state) {
        return !state.isAir() && !state.canBeReplaced();
    }

    private static void drawBrightRedObstruction(VertexConsumer consumer, Matrix4f matrix, Vec3 cameraPos, BlockPos pos) {
        float minX = (float) (pos.getX() - cameraPos.x) + 0.02F;
        float minY = (float) (pos.getY() - cameraPos.y) + 0.02F;
        float minZ = (float) (pos.getZ() - cameraPos.z) + 0.02F;
        float maxX = (float) (pos.getX() + 1 - cameraPos.x) - 0.02F;
        float maxY = (float) (pos.getY() + 1 - cameraPos.y) - 0.02F;
        float maxZ = (float) (pos.getZ() + 1 - cameraPos.z) - 0.02F;
        drawSolidBox(consumer, matrix, minX, minY, minZ, maxX, maxY, maxZ, 1.0F, 0.14F, 0.14F, 0.34F);
    }

    private static ResourceLocation getPreviewTexture(MultiblockRole role, BlockState actualState) {
        if (actualState.is(de.artemis.matterworks.common.registry.ModBlocks.MULTIBLOCK_PORT.get())) {
            return PORT_TEXTURE;
        }
        if (actualState.is(de.artemis.matterworks.common.registry.ModBlocks.MULTIBLOCK_GLASS.get())) {
            return GLASS_TEXTURE;
        }
        return switch (role) {
            case CONTROLLER -> CORE_TEXTURE;
            case FRAME -> FRAME_TEXTURE;
            case INTERNAL -> CAPACITOR_TEXTURE;
            case CASING, PORT -> CASING_TEXTURE;
        };
    }

    private static void drawSolidBox(VertexConsumer consumer, Matrix4f matrix,
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

    private static void drawTexturedCube(VertexConsumer consumer, Matrix4f matrix, Vec3 cameraPos, BlockPos pos, float alpha, float expansion) {
        float minX = (float) (pos.getX() - cameraPos.x) - expansion;
        float minY = (float) (pos.getY() - cameraPos.y) - expansion;
        float minZ = (float) (pos.getZ() - cameraPos.z) - expansion;
        float maxX = minX + 1.0F + (expansion * 2.0F);
        float maxY = minY + 1.0F + (expansion * 2.0F);
        float maxZ = minZ + 1.0F + (expansion * 2.0F);

        addTexturedFace(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, alpha, 0.0F, 0.0F, -1.0F);
        addTexturedFace(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, alpha, 0.0F, 0.0F, 1.0F);
        addTexturedFace(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, alpha, -1.0F, 0.0F, 0.0F);
        addTexturedFace(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, alpha, 1.0F, 0.0F, 0.0F);
        addTexturedFace(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, alpha, 0.0F, 1.0F, 0.0F);
        addTexturedFace(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, alpha, 0.0F, -1.0F, 0.0F);
    }

    private static void addTexturedFace(VertexConsumer consumer, Matrix4f matrix,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float x4, float y4, float z4,
                                        float alpha, float normalX, float normalY, float normalZ) {
        addTexturedVertex(consumer, matrix, x1, y1, z1, 0.0F, 1.0F, alpha, normalX, normalY, normalZ);
        addTexturedVertex(consumer, matrix, x2, y2, z2, 0.0F, 0.0F, alpha, normalX, normalY, normalZ);
        addTexturedVertex(consumer, matrix, x3, y3, z3, 1.0F, 0.0F, alpha, normalX, normalY, normalZ);
        addTexturedVertex(consumer, matrix, x4, y4, z4, 1.0F, 1.0F, alpha, normalX, normalY, normalZ);
    }

    private static void addTexturedVertex(VertexConsumer consumer, Matrix4f matrix,
                                          float x, float y, float z,
                                          float u, float v, float alpha,
                                          float normalX, float normalY, float normalZ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(u, v)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                .setNormal(normalX, normalY, normalZ);
    }

    public record PreviewHit(BlockPos localPos, BlockPos worldPos, MultiblockRole role, BlockState actualState) {
    }
}
