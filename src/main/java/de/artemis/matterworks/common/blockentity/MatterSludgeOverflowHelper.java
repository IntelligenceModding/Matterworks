package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MatterSludgeOverflowHelper {
    private static final int MB_PER_FLUID_UNIT = FluidType.BUCKET_VOLUME / 8;
    private static final Vector3f SLUDGE_SPRAY_COLOR = new Vector3f(0.49F, 0.39F, 0.28F);

    private MatterSludgeOverflowHelper() {
    }

    public static boolean canAcceptOutput(Level level, BlockPos machinePos, FluidTank tank, int outputMb, int bufferedOverflowMb) {
        int overflowMb = Math.max(0, outputMb - tank.getSpace());
        int totalOverflowMb = bufferedOverflowMb + overflowMb;
        return totalOverflowMb <= 0 || getTotalVentCapacityUnits(level, machinePos) * MB_PER_FLUID_UNIT >= totalOverflowMb;
    }

    public static int fillTankAndBufferOverflow(FluidTank tank, int outputMb, int bufferedOverflowMb) {
        int insertedMb = tank.fill(new FluidStack(ModFluids.MATTER_SLUDGE.get(), outputMb), IFluidHandler.FluidAction.EXECUTE);
        return bufferedOverflowMb + Math.max(0, outputMb - insertedMb);
    }

    public static int spillBufferedOverflow(ServerLevel level, BlockPos machinePos, int bufferedOverflowMb) {
        int remainingUnits = bufferedOverflowMb / MB_PER_FLUID_UNIT;
        int remainderMb = bufferedOverflowMb % MB_PER_FLUID_UNIT;
        if (remainingUnits <= 0) {
            return remainderMb;
        }

        List<VentTarget> vents = collectVentTargets(level, machinePos);
        if (vents.isEmpty()) {
            return bufferedOverflowMb;
        }

        Map<Direction, Integer> usedDirections = new EnumMap<>(Direction.class);
        while (remainingUnits > 0) {
            boolean placedThisPass = false;

            for (VentTarget vent : vents) {
                int currentUnits = getSludgeUnits(level.getFluidState(vent.targetPos()));
                if (currentUnits >= 8) {
                    continue;
                }

                setSludgeUnits(level, vent.targetPos(), currentUnits + 1);
                usedDirections.merge(vent.direction(), 1, Integer::sum);
                remainingUnits--;
                placedThisPass = true;

                if (remainingUnits <= 0) {
                    break;
                }
            }

            if (!placedThisPass) {
                break;
            }
        }

        if (!usedDirections.isEmpty()) {
            spawnSprayEffects(level, machinePos, usedDirections);
        }

        return remainingUnits * MB_PER_FLUID_UNIT + remainderMb;
    }

    private static int getTotalVentCapacityUnits(Level level, BlockPos machinePos) {
        int capacityUnits = 0;
        for (VentTarget vent : collectVentTargets(level, machinePos)) {
            capacityUnits += 8 - getSludgeUnits(level.getFluidState(vent.targetPos()));
        }
        return capacityUnits;
    }

    private static List<VentTarget> collectVentTargets(Level level, BlockPos machinePos) {
        List<VentTarget> vents = new ArrayList<>();
        addVentIfValid(vents, level, machinePos, Direction.NORTH);
        addVentIfValid(vents, level, machinePos, Direction.SOUTH);
        addVentIfValid(vents, level, machinePos, Direction.WEST);
        addVentIfValid(vents, level, machinePos, Direction.EAST);
        addVentIfValid(vents, level, machinePos, Direction.UP);
        addVentIfValid(vents, level, machinePos, Direction.DOWN);
        return vents;
    }

    private static void addVentIfValid(List<VentTarget> vents, Level level, BlockPos machinePos, Direction direction) {
        BlockPos targetPos = machinePos.relative(direction);
        BlockState targetState = level.getBlockState(targetPos);
        FluidState fluidState = level.getFluidState(targetPos);
        if (ModFluids.isMatterSludge(fluidState.getType()) || targetState.canBeReplaced()) {
            vents.add(new VentTarget(direction, targetPos));
        }
    }

    private static int getSludgeUnits(FluidState fluidState) {
        return ModFluids.isMatterSludge(fluidState.getType()) ? Mth.clamp(fluidState.getAmount(), 1, 8) : 0;
    }

    private static void setSludgeUnits(ServerLevel level, BlockPos targetPos, int units) {
        int clampedUnits = Mth.clamp(units, 1, 8);
        BlockState sludgeState = clampedUnits >= 8
                ? ModFluids.MATTER_SLUDGE.get().defaultFluidState().createLegacyBlock()
                : ModFluids.FLOWING_MATTER_SLUDGE.get().getFlowing(clampedUnits, false).createLegacyBlock();
        level.setBlock(targetPos, sludgeState, 3);
    }

    private static void spawnSprayEffects(ServerLevel level, BlockPos machinePos, Map<Direction, Integer> usedDirections) {
        for (Map.Entry<Direction, Integer> entry : usedDirections.entrySet()) {
            Direction direction = entry.getKey();
            int intensity = entry.getValue();
            double baseX = machinePos.getX() + 0.5D + direction.getStepX() * 0.54D;
            double baseY = machinePos.getY() + 0.5D + direction.getStepY() * 0.54D;
            double baseZ = machinePos.getZ() + 0.5D + direction.getStepZ() * 0.54D;

            for (int i = 0; i < 2 + intensity; i++) {
                double sideJitter = (level.random.nextDouble() - 0.5D) * 0.08D;
                double outwardSpeed = 0.07D + level.random.nextDouble() * 0.03D;
                double motionX = direction.getStepX() * outwardSpeed;
                double motionY = direction == Direction.UP
                        ? 0.10D + level.random.nextDouble() * 0.04D
                        : direction == Direction.DOWN
                        ? -0.07D - level.random.nextDouble() * 0.03D
                        : -0.025D - level.random.nextDouble() * 0.03D;
                double motionZ = direction.getStepZ() * outwardSpeed;

                if (direction.getAxis() != Direction.Axis.X) {
                    motionX += sideJitter;
                }
                if (direction.getAxis() != Direction.Axis.Z) {
                    motionZ += sideJitter;
                }

                level.sendParticles(ParticleTypes.SPLASH, baseX, baseY, baseZ, 0, motionX, motionY, motionZ, 1.0D);
                level.sendParticles(new DustParticleOptions(SLUDGE_SPRAY_COLOR, 0.9F), baseX, baseY, baseZ, 0, motionX * 0.28D, Math.max(0.01D, motionY * 0.22D), motionZ * 0.28D, 0.0D);
            }

            level.sendParticles(ParticleTypes.SMOKE, baseX, baseY, baseZ, 1 + intensity, 0.05D, 0.04D, 0.05D, 0.01D);
            level.sendParticles(ParticleTypes.BUBBLE, baseX, baseY, baseZ, 1 + intensity, 0.03D, 0.02D, 0.03D, 0.0D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, baseX, baseY, baseZ, intensity, 0.03D, 0.03D, 0.03D, 0.0D);
        }

        level.playSound(null, machinePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.25F, 0.7F);
        level.playSound(null, machinePos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.24F, 0.65F);
        level.playSound(null, machinePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.14F, 1.5F);
    }

    private record VentTarget(Direction direction, BlockPos targetPos) {
    }
}
