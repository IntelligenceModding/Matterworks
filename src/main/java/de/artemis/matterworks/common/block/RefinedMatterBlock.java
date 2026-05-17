package de.artemis.matterworks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class RefinedMatterBlock extends AbstractMatterFluidBlock {
    private static final Vec3 MOVEMENT_DRAG = new Vec3(0.95D, 0.94D, 0.95D);
    private static final Vector3f REFINED_TINT = new Vector3f(0.69F, 0.49F, 1.0F);

    public RefinedMatterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties, MOVEMENT_DRAG);
    }

    @Override
    protected void applyEntityMotion(Level level, BlockPos pos, FluidState fluidState, Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        Vec3 flow = fluidState.getFlow(level, pos);
        if (motion.y < 0.05D) {
            entity.setDeltaMovement(
                    motion.x * 0.98D + flow.x * 0.028D,
                    Math.min(0.08D, motion.y + 0.012D),
                    motion.z * 0.98D + flow.z * 0.028D
            );
        } else if (flow.horizontalDistanceSqr() > 1.0E-4D) {
            entity.setDeltaMovement(motion.x + flow.x * 0.018D, motion.y, motion.z + flow.z * 0.018D);
        }
    }

    @Override
    protected void spawnAmbientEffects(Level level, BlockPos pos, FluidState fluidState, SurfaceSample sample, RandomSource random) {
        double x = sample.x();
        double y = sample.y();
        double z = sample.z();

        if (random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(REFINED_TINT, 0.65F), x, y, z, 0.0D, 0.014D, 0.0D);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0.0D, 0.008D, 0.0D);
        }
        if (random.nextInt(22) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.10F + random.nextFloat() * 0.05F, 1.15F + random.nextFloat() * 0.2F, false);
        }
        if (random.nextInt(36) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.07F + random.nextFloat() * 0.05F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }
}
