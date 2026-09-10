package de.artemis.matterworks.common.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public abstract class AbstractMatterFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation OVERLAY_TEXTURE = ResourceLocation.withDefaultNamespace("block/water_overlay");

    private final int tintColor;
    private final Vector3f fogColor;
    private final float fogStart;
    private final float fogDistanceScale;
    private final float fogDistanceLimit;

    protected AbstractMatterFluidType(
            Properties properties,
            int tintColor,
            Vector3f fogColor,
            float fogStart,
            float fogDistanceScale,
            float fogDistanceLimit
    ) {
        super(properties);
        this.tintColor = tintColor;
        this.fogColor = new Vector3f(fogColor);
        this.fogStart = fogStart;
        this.fogDistanceScale = fogDistanceScale;
        this.fogDistanceLimit = fogDistanceLimit;
    }

    public ResourceLocation getStillTexture() {
        return STILL_TEXTURE;
    }

    public ResourceLocation getFlowingTexture() {
        return FLOWING_TEXTURE;
    }

    public ResourceLocation getOverlayTexture() {
        return OVERLAY_TEXTURE;
    }

    public int getTintColor() {
        return tintColor;
    }

    public Vector3f getFogColor() {
        return new Vector3f(fogColor);
    }

    public float getFogStart() {
        return fogStart;
    }

    public float getFogDistanceScale() {
        return fogDistanceScale;
    }

    public float getFogDistanceLimit() {
        return fogDistanceLimit;
    }
}
