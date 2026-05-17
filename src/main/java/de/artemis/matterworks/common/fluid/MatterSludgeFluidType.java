package de.artemis.matterworks.common.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public class MatterSludgeFluidType extends AbstractMatterFluidType {
    private static final ResourceLocation STILL_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "block/fluid/matter_sludge_still");
    private static final ResourceLocation FLOWING_TEXTURE = ResourceLocation.fromNamespaceAndPath("matterworks", "block/fluid/matter_sludge_flow");
    private static final int TINT_COLOR = 0xFFB28A63;
    private static final Vector3f FOG_COLOR = new Vector3f(0.30F, 0.22F, 0.16F);

    public MatterSludgeFluidType(Properties properties) {
        super(properties, STILL_TEXTURE, FLOWING_TEXTURE, TINT_COLOR, FOG_COLOR, -0.5F, 0.032F, 2.0F);
    }
}
