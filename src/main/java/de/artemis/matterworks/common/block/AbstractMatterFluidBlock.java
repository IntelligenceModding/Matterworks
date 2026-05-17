package de.artemis.matterworks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMatterFluidBlock extends LiquidBlock {
    private final Vec3 movementDrag;

    protected AbstractMatterFluidBlock(FlowingFluid fluid, Properties properties, Vec3 movementDrag) {
        super(fluid, properties);
        this.movementDrag = movementDrag;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.makeStuckInBlock(state, movementDrag);
        applyEntityMotion(level, pos, level.getFluidState(pos), entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        FluidState fluidState = level.getFluidState(pos);
        SurfaceSample sample = getSurfaceSample(level, pos, fluidState, random);
        spawnAmbientEffects(level, pos, fluidState, sample, random);
    }

    protected void applyEntityMotion(Level level, BlockPos pos, FluidState fluidState, Entity entity) {
    }

    protected abstract void spawnAmbientEffects(Level level, BlockPos pos, FluidState fluidState, SurfaceSample sample, RandomSource random);

    protected SurfaceSample getSurfaceSample(Level level, BlockPos pos, FluidState fluidState, RandomSource random) {
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

    protected record SurfaceSample(double x, double y, double z) {
    }
}
