package de.artemis.matterworks.common.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public class RawMatterFluidType extends AbstractMatterFluidType {
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "block/fluid/raw_matter_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "block/fluid/raw_matter_flow");
    private static final int TINT_COLOR = 0xFFF4F6F7;
    private static final Vector3f FOG_COLOR = new Vector3f(0.82F, 0.85F, 0.88F);

    public RawMatterFluidType(Properties properties) {
        super(properties, STILL_TEXTURE, FLOWING_TEXTURE, TINT_COLOR, FOG_COLOR, -1.0F, 0.056F, 4.0F);
    }
}
