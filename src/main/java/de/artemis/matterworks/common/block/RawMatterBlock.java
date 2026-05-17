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

public class RawMatterBlock extends AbstractMatterFluidBlock {
    private static final Vec3 MOVEMENT_DRAG = new Vec3(0.88D, 0.84D, 0.88D);
    private static final Vector3f RAW_TINT = new Vector3f(0.89F, 0.91F, 0.93F);

    public RawMatterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties, MOVEMENT_DRAG);
    }

    @Override
    protected void applyEntityMotion(Level level, BlockPos pos, FluidState fluidState, Entity entity) {
        Vec3 motion = entity.getDeltaMovement();
        Vec3 flow = fluidState.getFlow(level, pos);
        double sink = motion.y > -0.04D ? -0.008D : 0.0D;
        entity.setDeltaMovement(
                motion.x * 0.93D + flow.x * 0.010D,
                motion.y + sink,
                motion.z * 0.93D + flow.z * 0.010D
        );
    }

    @Override
    protected void spawnAmbientEffects(Level level, BlockPos pos, FluidState fluidState, SurfaceSample sample, RandomSource random) {
        double x = sample.x();
        double y = sample.y();
        double z = sample.z();

        if (random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(RAW_TINT, 0.75F), x, y, z, 0.0D, 0.012D, 0.0D);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0D, 0.006D, 0.0D);
        }
        if (random.nextInt(20) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.10F + random.nextFloat() * 0.05F, 0.55F + random.nextFloat() * 0.15F, false);
        }
        if (random.nextInt(32) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.MUD_HIT, SoundSource.BLOCKS, 0.12F + random.nextFloat() * 0.06F, 0.65F + random.nextFloat() * 0.15F, false);
        }
    }
}
