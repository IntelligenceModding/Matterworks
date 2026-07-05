package de.artemis.matterworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockLayout;
import de.artemis.matterworks.common.multiblock.MultiblockPartState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

public class MatterBatteryCoreBlockEntityRenderer implements BlockEntityRenderer<MatterBatteryCoreBlockEntity> {
    private static final ResourceLocation ENERGY_TEXTURE = ResourceLocation.withDefaultNamespace("block/lava_still");
    private static final float RED = 0xE2 / 255.0F;
    private static final float GREEN = 0x3D / 255.0F;
    private static final float BLUE = 0x2D / 255.0F;
    private static final float OUTER_ALPHA = 0.94F;
    private static final float INNER_ALPHA = 0.52F;

    public MatterBatteryCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MatterBatteryCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!blockEntity.isFormed() || blockEntity.getDisplayedEnergyStored() <= 0 || blockEntity.getDisplayedEnergyCapacity() <= 0) {
            return;
        }

        MultiblockPartState state = blockEntity.getMultiblockPartState();
        if (!state.isController(blockEntity.getBlockPos())) {
            return;
        }

        MatterBatteryMultiblockLayout.WorldBounds bounds = MatterBatteryMultiblockLayout.getWorldBounds(
                state.getOriginPos(), state.getFront(), state.getWidth(), state.getHeight(), state.getDepth()
        );
        int innerSizeX = bounds.sizeX() - 2;
        int innerSizeY = bounds.sizeY() - 2;
        int innerSizeZ = bounds.sizeZ() - 2;
        if (innerSizeX <= 0 || innerSizeY <= 0 || innerSizeZ <= 0) {
            return;
        }

        float fillRatio = Mth.clamp(blockEntity.getDisplayedEnergyStored() / (float) blockEntity.getDisplayedEnergyCapacity(), 0.0F, 1.0F);
        if (fillRatio <= 0.0F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ENERGY_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        Matrix4f matrix = poseStack.last().pose();

        float minX = (bounds.minPos().getX() + 1.01F) - blockEntity.getBlockPos().getX();
        float minY = (bounds.minPos().getY() + 1.01F) - blockEntity.getBlockPos().getY();
        float minZ = (bounds.minPos().getZ() + 1.01F) - blockEntity.getBlockPos().getZ();
        float maxX = minX + innerSizeX - 0.02F;
        float maxZ = minZ + innerSizeZ - 0.02F;
        float filledHeight = Math.max(0.04F, (innerSizeY - 0.02F) * fillRatio);
        float maxY = minY + Math.min(innerSizeY - 0.02F, filledHeight);

        renderVolume(consumer, matrix, sprite, minX, minY, minZ, maxX, maxY, maxZ, RED, GREEN, BLUE, OUTER_ALPHA);

        float inset = 0.18F;
        if ((maxX - minX) > inset * 2.0F && (maxY - minY) > inset * 2.0F && (maxZ - minZ) > inset * 2.0F) {
            renderVolume(
                    consumer,
                    matrix,
                    sprite,
                    minX + inset,
                    minY + inset,
                    minZ + inset,
                    maxX - inset,
                    maxY - inset * 0.4F,
                    maxZ - inset,
                    RED * 0.92F,
                    GREEN * 0.84F,
                    BLUE * 0.84F,
                    INNER_ALPHA
            );
        }
    }

    private static void renderVolume(VertexConsumer consumer, Matrix4f matrix, TextureAtlasSprite sprite,
                                     float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     float red, float green, float blue, float alpha) {
        addTexturedFace(consumer, matrix, sprite, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addTexturedFace(consumer, matrix, sprite, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addTexturedFace(consumer, matrix, sprite, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, red, green, blue, alpha);
        addTexturedFace(consumer, matrix, sprite, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addFlatFace(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, Math.min(1.0F, alpha + 0.04F));
        addTexturedFace(consumer, matrix, sprite, minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
    }

    private static void addTexturedFace(VertexConsumer consumer, Matrix4f matrix, TextureAtlasSprite sprite,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float x4, float y4, float z4,
                                        float red, float green, float blue, float alpha) {
        addTexturedVertex(consumer, matrix, sprite, x1, y1, z1, sprite.getU0(), sprite.getV1(), red, green, blue, alpha);
        addTexturedVertex(consumer, matrix, sprite, x2, y2, z2, sprite.getU0(), sprite.getV0(), red, green, blue, alpha);
        addTexturedVertex(consumer, matrix, sprite, x3, y3, z3, sprite.getU1(), sprite.getV0(), red, green, blue, alpha);
        addTexturedVertex(consumer, matrix, sprite, x4, y4, z4, sprite.getU1(), sprite.getV1(), red, green, blue, alpha);
    }

    private static void addFlatFace(VertexConsumer consumer, Matrix4f matrix,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4,
                                    float red, float green, float blue, float alpha) {
        addFlatVertex(consumer, matrix, x1, y1, z1, red, green, blue, alpha);
        addFlatVertex(consumer, matrix, x2, y2, z2, red, green, blue, alpha);
        addFlatVertex(consumer, matrix, x3, y3, z3, red, green, blue, alpha);
        addFlatVertex(consumer, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    private static void addTexturedVertex(VertexConsumer consumer, Matrix4f matrix, TextureAtlasSprite sprite,
                                          float x, float y, float z, float u, float v,
                                          float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static void addFlatVertex(VertexConsumer consumer, Matrix4f matrix,
                                      float x, float y, float z,
                                      float red, float green, float blue, float alpha) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
