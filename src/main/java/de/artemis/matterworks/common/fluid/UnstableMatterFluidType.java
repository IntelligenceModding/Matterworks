package de.artemis.matterworks.common.fluid;

import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

public class UnstableMatterFluidType extends AbstractMatterFluidType {
    private static final int TINT_COLOR = 0xFFFFD08E;
    private static final Vector3f FOG_COLOR = new Vector3f(0.84F, 0.31F, 0.08F);

    public UnstableMatterFluidType(Properties properties) {
        super(properties, TINT_COLOR, FOG_COLOR, -0.5F, 0.036F, 2.4F);
    }
}
