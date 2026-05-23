package de.artemis.matterworks.common.fluid;

import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public class RefinedMatterFluidType extends AbstractMatterFluidType {
    private static final int TINT_COLOR = 0xFFF1DEFF;
    private static final Vector3f FOG_COLOR = new Vector3f(0.53F, 0.27F, 0.78F);

    public RefinedMatterFluidType(Properties properties) {
        super(properties, TINT_COLOR, FOG_COLOR, -1.0F, 0.064F, 4.8F);
    }
}
