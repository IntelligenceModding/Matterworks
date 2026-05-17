package de.artemis.matterworks.common.fluid;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.function.Supplier;

public abstract class MatterFluid extends BaseFlowingFluid {
    private final Supplier<? extends ParticleOptions> dripParticle;

    protected MatterFluid(Properties properties, Supplier<? extends ParticleOptions> dripParticle) {
        super(properties);
        this.dripParticle = dripParticle;
    }

    @Override
    protected ParticleOptions getDripParticle() {
        return dripParticle.get();
    }

    public static class Flowing extends MatterFluid {
        public Flowing(Properties properties, Supplier<? extends ParticleOptions> dripParticle) {
            super(properties, dripParticle);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<net.minecraft.world.level.material.Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends MatterFluid {
        public Source(Properties properties, Supplier<? extends ParticleOptions> dripParticle) {
            super(properties, dripParticle);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
