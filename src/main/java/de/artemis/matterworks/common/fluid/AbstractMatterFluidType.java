package de.artemis.matterworks.common.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import de.artemis.matterworks.Matterworks;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;

public abstract class AbstractMatterFluidType extends FluidType {
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "block/fluid/fluid_overlay");

    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final int tintColor;
    private final Vector3f fogColor;
    private final float fogStart;
    private final float fogDistanceScale;
    private final float fogDistanceLimit;

    protected AbstractMatterFluidType(
            Properties properties,
            ResourceLocation stillTexture,
            ResourceLocation flowingTexture,
            int tintColor,
            Vector3f fogColor,
            float fogStart,
            float fogDistanceScale,
            float fogDistanceLimit
    ) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
        this.fogStart = fogStart;
        this.fogDistanceScale = fogDistanceScale;
        this.fogDistanceLimit = fogDistanceLimit;
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return tintColor;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(fogColor);
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                RenderSystem.setShaderFogStart(fogStart);
                RenderSystem.setShaderFogEnd(Math.max(fogStart + 4.0F, Math.min(Math.min(renderDistance, farDistance) * fogDistanceScale, fogDistanceLimit)));
                RenderSystem.setShaderFogShape(shape);
            }
        });
    }
}
