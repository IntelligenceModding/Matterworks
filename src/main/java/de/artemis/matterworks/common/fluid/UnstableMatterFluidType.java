package de.artemis.matterworks.common.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public class UnstableMatterFluidType extends AbstractMatterFluidType {
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "block/fluid/unstable_matter_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "block/fluid/unstable_matter_flow");
    private static final int TINT_COLOR = 0xFFFFD08E;
    private static final Vector3f FOG_COLOR = new Vector3f(0.84F, 0.31F, 0.08F);

    public UnstableMatterFluidType(Properties properties) {
        super(properties, STILL_TEXTURE, FLOWING_TEXTURE, TINT_COLOR, FOG_COLOR, -0.5F, 0.036F, 2.4F);
    }
}
