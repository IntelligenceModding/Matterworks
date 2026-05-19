package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockDefinition;
import de.artemis.matterworks.common.multiblock.MatterBatteryPreviewPlacementHelper;
import de.artemis.matterworks.common.multiblock.MultiblockPattern;
import de.artemis.matterworks.common.multiblock.MultiblockRequirement;
import de.artemis.matterworks.common.multiblock.MultiblockTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Optional;

public final class MatterBatteryPreviewRenderer {
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

        BlockEntity blockEntity = minecraft.level.getBlockEntity(MatterBatteryPreviewState.getControllerPos());
        if (!(blockEntity instanceof MatterBatteryCoreBlockEntity controller)) {
            MatterBatteryPreviewState.clear();
            return;
        }

        Map<BlockPos, MultiblockRequirement> requirements = MatterBatteryMultiblockDefinition.INSTANCE.getPattern().getRequirements();
        int selectedLayer = MatterBatteryPreviewState.getSelectedLayer();
        Matrix4f matrix = poseStack.last().pose();

        for (var entry : requirements.entrySet()) {
            BlockPos localPos = entry.getKey();
            if (localPos.getY() != selectedLayer) {
                continue;
            }

            MultiblockRequirement requirement = entry.getValue();
            BlockPos worldPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
            BlockState actualState = minecraft.level.getBlockState(worldPos);
            boolean matches = matchesRequirement(requirement, actualState);

            ResourceLocation texture = getPreviewTexture(requirement, actualState);
            VertexConsumer texturedConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
            drawTexturedCube(texturedConsumer, matrix, cameraPos, worldPos, matches ? 0.18F : 0.52F, 0.005F);

            float[] color = getRoleColor(requirement);
            VertexConsumer overlayConsumer = buffer.getBuffer(RenderType.lightning());
            drawCube(overlayConsumer, matrix, cameraPos, worldPos, color[0], color[1], color[2], matches ? 0.05F : 0.16F, 0.010F);
        }
    }

    public static String getRequirementLabel(BlockPos localPos) {
        MultiblockRequirement requirement = MatterBatteryMultiblockDefinition.INSTANCE.getPattern().getRequirements().get(localPos);
        if (requirement == null) {
            return "";
        }

        return switch (requirement.description()) {
            case MatterBatteryMultiblockDefinition.DESC_CONTROLLER -> "Matter Battery Core";
            case MatterBatteryMultiblockDefinition.DESC_FRAME -> "Multiblock Frame";
            case MatterBatteryMultiblockDefinition.DESC_CASING -> "Multiblock Casing";
            case MatterBatteryMultiblockDefinition.DESC_CELL -> "Matter Capacitor Cell";
            default -> requirement.description();
        };
    }

    public static String getPlacementLabel(BlockState state) {
        return state.isAir() ? "Air" : state.getBlock().getName().getString();
    }

    public static BlockPos findPreviewLocalPos(BlockPos worldPos) {
        MultiblockPattern pattern = MatterBatteryMultiblockDefinition.INSTANCE.getPattern();
        for (BlockPos localPos : pattern.getRequirements().keySet()) {
            BlockPos previewPos = MultiblockTransforms.localToWorld(MatterBatteryPreviewState.getOriginPos(), MatterBatteryPreviewState.getFront(), localPos);
            if (previewPos.equals(worldPos) && localPos.getY() == MatterBatteryPreviewState.getSelectedLayer()) {
                return localPos;
            }
        }
        return null;
    }

    public static @Nullable PreviewHit findTargetedPreviewHit(Minecraft minecraft) {
        if (!MatterBatteryPreviewState.isActive() || minecraft.player == null || minecraft.level == null) {
            return null;
        }

        Vec3 start = minecraft.player.getEyePosition();
        Vec3 end = start.add(minecraft.player.getViewVector(1.0F).scale(8.0D));
        double bestDistance = Double.MAX_VALUE;
        PreviewHit bestHit = null;

        for (var entry : MatterBatteryMultiblockDefinition.INSTANCE.getPattern().getRequirements().entrySet()) {
            BlockPos localPos = entry.getKey();
            if (localPos.getY() != MatterBatteryPreviewState.getSelectedLayer()) {
                continue;
            }

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
            bestHit = new PreviewHit(localPos, worldPos, entry.getValue(), actualState);
        }

        return bestHit;
    }

    public static boolean matchesRequirement(MultiblockRequirement requirement, BlockState state) {
        return MatterBatteryPreviewPlacementHelper.matchesRequirement(requirement, state);
    }

    private static float[] getRoleColor(MultiblockRequirement requirement) {
        if (MatterBatteryMultiblockDefinition.DESC_CONTROLLER.equals(requirement.description())) {
            return new float[]{0.72F, 0.38F, 1.0F};
        }
        if (MatterBatteryMultiblockDefinition.DESC_FRAME.equals(requirement.description())) {
            return new float[]{0.93F, 0.72F, 0.28F};
        }
        if (MatterBatteryMultiblockDefinition.DESC_CASING.equals(requirement.description())) {
            return new float[]{0.26F, 0.74F, 0.96F};
        }
        if (MatterBatteryMultiblockDefinition.DESC_CELL.equals(requirement.description())) {
            return new float[]{0.44F, 0.96F, 0.58F};
        }
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    private static ResourceLocation getPreviewTexture(MultiblockRequirement requirement, BlockState actualState) {
        if (actualState.is(de.artemis.matterworks.common.registry.ModBlocks.MULTIBLOCK_PORT.get())) {
            return PORT_TEXTURE;
        }
        if (actualState.is(de.artemis.matterworks.common.registry.ModBlocks.MULTIBLOCK_GLASS.get())) {
            return GLASS_TEXTURE;
        }
        return switch (requirement.description()) {
            case MatterBatteryMultiblockDefinition.DESC_CONTROLLER -> CORE_TEXTURE;
            case MatterBatteryMultiblockDefinition.DESC_FRAME -> FRAME_TEXTURE;
            case MatterBatteryMultiblockDefinition.DESC_CELL -> CAPACITOR_TEXTURE;
            default -> CASING_TEXTURE;
        };
    }

    private static void drawCube(VertexConsumer consumer, Matrix4f matrix, Vec3 cameraPos, BlockPos pos, float red, float green, float blue, float alpha, float expansion) {
        float minX = (float) (pos.getX() - cameraPos.x) - expansion;
        float minY = (float) (pos.getY() - cameraPos.y) - expansion;
        float minZ = (float) (pos.getZ() - cameraPos.z) - expansion;
        float maxX = minX + 1.0F + (expansion * 2.0F);
        float maxY = minY + 1.0F + (expansion * 2.0F);
        float maxZ = minZ + 1.0F + (expansion * 2.0F);

        addFace(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addFace(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addFace(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, red, green, blue, alpha * 0.75F);
        addFace(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha * 0.75F);
        addFace(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha * 0.6F);
        addFace(consumer, matrix, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha * 0.6F);
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
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(normalX, normalY, normalZ);
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

    public record PreviewHit(BlockPos localPos, BlockPos worldPos, MultiblockRequirement requirement, BlockState actualState) {
    }
}
