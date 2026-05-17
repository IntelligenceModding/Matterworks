package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.block.HardenedSludgeBlock;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MatterSludgeOverflowHelper {
    private static final int MB_PER_FLUID_UNIT = FluidType.BUCKET_VOLUME / 8;
    private static final int MAX_FALL_DISTANCE = 3;

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
                int currentUnits = getSludgeUnits(level.getBlockState(vent.targetPos()));
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
            capacityUnits += 8 - getSludgeUnits(level.getBlockState(vent.targetPos()));
        }
        return capacityUnits;
    }

    private static List<VentTarget> collectVentTargets(Level level, BlockPos machinePos) {
        List<VentTarget> vents = new ArrayList<>();
        Set<BlockPos> seenTargets = new HashSet<>();
        addVentIfValid(vents, level, machinePos, Direction.NORTH, seenTargets);
        addVentIfValid(vents, level, machinePos, Direction.SOUTH, seenTargets);
        addVentIfValid(vents, level, machinePos, Direction.WEST, seenTargets);
        addVentIfValid(vents, level, machinePos, Direction.EAST, seenTargets);
        addVentIfValid(vents, level, machinePos, Direction.UP, seenTargets);
        addVentIfValid(vents, level, machinePos, Direction.DOWN, seenTargets);
        return vents;
    }

    private static void addVentIfValid(List<VentTarget> vents, Level level, BlockPos machinePos, Direction direction, Set<BlockPos> seenTargets) {
        BlockPos targetPos = resolveDepositPos(level, machinePos, direction);
        if (targetPos != null && seenTargets.add(targetPos)) {
            vents.add(new VentTarget(direction, targetPos));
        }
    }

    private static BlockPos resolveDepositPos(Level level, BlockPos machinePos, Direction direction) {
        BlockPos primaryPos = machinePos.relative(direction);
        if (canAcceptDeposit(level, primaryPos)) {
            return primaryPos;
        }

        if (direction.getAxis().isHorizontal()) {
            BlockPos fallingPos = primaryPos;
            for (int i = 0; i < MAX_FALL_DISTANCE; i++) {
                fallingPos = fallingPos.below();
                if (canAcceptDeposit(level, fallingPos)) {
                    return fallingPos;
                }
            }
        }

        return null;
    }

    private static boolean canAcceptDeposit(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.HARDENED_SLUDGE.get())) {
            return state.getValue(HardenedSludgeBlock.LAYERS) < 8;
        }
        if (!state.canBeReplaced()) {
            return false;
        }

        BlockState depositState = ModBlocks.HARDENED_SLUDGE.get().defaultBlockState();
        return depositState.canSurvive(level, pos);
    }

    private static int getSludgeUnits(BlockState state) {
        return state.is(ModBlocks.HARDENED_SLUDGE.get())
                ? Mth.clamp(state.getValue(HardenedSludgeBlock.LAYERS), 1, 8)
                : 0;
    }

    private static void setSludgeUnits(ServerLevel level, BlockPos targetPos, int units) {
        int clampedUnits = Mth.clamp(units, 1, 8);
        BlockState sludgeState = ModBlocks.HARDENED_SLUDGE.get()
                .defaultBlockState()
                .setValue(HardenedSludgeBlock.LAYERS, clampedUnits);
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

                level.sendParticles(ParticleTypes.POOF, baseX, baseY, baseZ, 0, motionX * 0.12D, Math.max(0.01D, motionY * 0.12D), motionZ * 0.12D, 0.0D);
                level.sendParticles(ParticleTypes.SMOKE, baseX, baseY, baseZ, 0, motionX * 0.18D, Math.max(0.01D, motionY * 0.18D), motionZ * 0.18D, 0.0D);
                level.sendParticles(ParticleTypes.ASH, baseX, baseY, baseZ, 0, motionX * 0.12D, Math.max(0.005D, motionY * 0.12D), motionZ * 0.12D, 0.0D);
            }

            level.sendParticles(ParticleTypes.SMOKE, baseX, baseY, baseZ, 1 + intensity, 0.05D, 0.04D, 0.05D, 0.01D);
            level.sendParticles(ParticleTypes.ASH, baseX, baseY, baseZ, 1 + intensity, 0.05D, 0.03D, 0.05D, 0.003D);
            level.sendParticles(ParticleTypes.WHITE_ASH, baseX, baseY, baseZ, Math.max(1, intensity / 2), 0.04D, 0.03D, 0.04D, 0.002D);
            level.sendParticles(ParticleTypes.POOF, baseX, baseY, baseZ, Math.max(1, intensity / 2), 0.03D, 0.02D, 0.03D, 0.0D);
            if (intensity > 1) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, baseX, baseY, baseZ, intensity - 1, 0.03D, 0.02D, 0.03D, 0.01D);
            }
        }

        level.playSound(null, machinePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.25F, 0.7F);
        level.playSound(null, machinePos, SoundEvents.MUD_HIT, SoundSource.BLOCKS, 0.26F, 0.65F);
        level.playSound(null, machinePos, SoundEvents.SLIME_BLOCK_STEP, SoundSource.BLOCKS, 0.12F, 0.72F);
        level.playSound(null, machinePos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.24F, 0.65F);
        level.playSound(null, machinePos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.14F, 1.5F);
        level.playSound(null, machinePos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.08F, 0.58F);
    }

    private record VentTarget(Direction direction, BlockPos targetPos) {
    }
}
