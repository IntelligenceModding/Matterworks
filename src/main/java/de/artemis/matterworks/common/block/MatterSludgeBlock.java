package de.artemis.matterworks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import de.artemis.matterworks.common.registry.ModMobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

public class MatterSludgeBlock extends LiquidBlock {
    private static final Vec3 MOVEMENT_DRAG = new Vec3(0.82D, 0.72D, 0.82D);

    public MatterSludgeBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.makeStuckInBlock(state, MOVEMENT_DRAG);

        if (!level.isClientSide() && entity.tickCount % 12 == 0 && entity.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    level.random.nextInt(4) == 0 ? SoundEvents.SLIME_BLOCK_STEP : SoundEvents.MUD_STEP,
                    SoundSource.BLOCKS,
                    0.18F + level.random.nextFloat() * 0.08F,
                    0.7F + level.random.nextFloat() * 0.18F
            );
        }

        if (!level.isClientSide() && entity instanceof LivingEntity livingEntity && !entity.isInvulnerable()) {
            int fluidAmount = level.getFluidState(pos).getAmount();
            int amplifier = fluidAmount >= 7 ? 2 : fluidAmount >= 4 ? 1 : 0;
            livingEntity.addEffect(new MobEffectInstance(ModMobEffects.MOLECULAR_DISPLACEMENT, 80, amplifier, true, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        FluidState fluidState = level.getFluidState(pos);
        SurfaceSample sample = getSurfaceSample(level, pos, fluidState, random);
        double x = sample.x();
        double y = sample.y();
        double z = sample.z();

        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.015D, 0.0D);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.ASH, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextInt(9) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0.0D, 0.008D, 0.0D);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0.0D, 0.02D, 0.0D);
        }
        if (random.nextInt(18) == 0) {
            level.playLocalSound(
                    x,
                    y,
                    z,
                    SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS,
                    0.12F + random.nextFloat() * 0.08F,
                    0.6F + random.nextFloat() * 0.25F,
                    false
            );
        }
        if (random.nextInt(24) == 0) {
            level.playLocalSound(
                    x,
                    y,
                    z,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.08F + random.nextFloat() * 0.05F,
                    1.5F + random.nextFloat() * 0.3F,
                    false
            );
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(
                    x,
                    y,
                    z,
                    SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS,
                    0.06F + random.nextFloat() * 0.05F,
                    0.55F + random.nextFloat() * 0.18F,
                    false
            );
        }
    }

    private SurfaceSample getSurfaceSample(Level level, BlockPos pos, FluidState fluidState, RandomSource random) {
        if (!level.getFluidState(pos.above()).is(fluidState.getType())) {
            float surfaceHeight = Math.min(0.98F, fluidState.getHeight(level, pos));
            Vec3 flow = fluidState.getFlow(level, pos);
            double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
            double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
            if (flow.horizontalDistanceSqr() > 1.0E-4D) {
                x += Math.max(-0.22D, Math.min(0.22D, flow.x * 0.22D));
                z += Math.max(-0.22D, Math.min(0.22D, flow.z * 0.22D));
            }
            double y = pos.getY() + surfaceHeight + 0.02D + random.nextDouble() * 0.02D;
            return new SurfaceSample(x, y, z);
        }

        Direction face = getExposedSide(level, pos, fluidState, random);
        double inset = 0.03D;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.18D + random.nextDouble() * 0.64D;
        double z = pos.getZ() + 0.5D;
        switch (face) {
            case NORTH -> {
                x += (random.nextDouble() - 0.5D) * 0.7D;
                z = pos.getZ() + inset;
            }
            case SOUTH -> {
                x += (random.nextDouble() - 0.5D) * 0.7D;
                z = pos.getZ() + 1.0D - inset;
            }
            case WEST -> {
                x = pos.getX() + inset;
                z += (random.nextDouble() - 0.5D) * 0.7D;
            }
            default -> {
                x = pos.getX() + 1.0D - inset;
                z += (random.nextDouble() - 0.5D) * 0.7D;
            }
        }
        return new SurfaceSample(x, y, z);
    }

    private Direction getExposedSide(Level level, BlockPos pos, FluidState fluidState, RandomSource random) {
        Vec3 flow = fluidState.getFlow(level, pos);
        if (Math.abs(flow.x) > Math.abs(flow.z) && Math.abs(flow.x) > 1.0E-4D) {
            Direction preferred = flow.x > 0.0D ? Direction.EAST : Direction.WEST;
            if (!level.getFluidState(pos.relative(preferred)).is(fluidState.getType())) {
                return preferred;
            }
        } else if (Math.abs(flow.z) > 1.0E-4D) {
            Direction preferred = flow.z > 0.0D ? Direction.SOUTH : Direction.NORTH;
            if (!level.getFluidState(pos.relative(preferred)).is(fluidState.getType())) {
                return preferred;
            }
        }

        Direction[] sides = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction side : sides) {
            if (!level.getFluidState(pos.relative(side)).is(fluidState.getType())) {
                return side;
            }
        }

        return sides[random.nextInt(sides.length)];
    }

    private record SurfaceSample(double x, double y, double z) {
    }

}
