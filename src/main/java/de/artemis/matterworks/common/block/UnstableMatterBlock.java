package de.artemis.matterworks.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import de.artemis.matterworks.common.registry.ModItems;
import org.joml.Vector3f;

public class UnstableMatterBlock extends AbstractMatterFluidBlock {
    private static final Vec3 MOVEMENT_DRAG = new Vec3(0.90D, 0.88D, 0.90D);
    private static final Vector3f UNSTABLE_TINT = new Vector3f(1.0F, 0.48F, 0.14F);
    private static final Vector3f UNSTABLE_HOT_TINT = new Vector3f(1.0F, 0.86F, 0.22F);

    public UnstableMatterBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties, MOVEMENT_DRAG);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof ItemEntity itemEntity && transformItemEntity((ServerLevel) level, itemEntity)) {
            return;
        }

        super.entityInside(state, level, pos, entity);
    }

    @Override
    protected void applyEntityMotion(Level level, BlockPos pos, FluidState fluidState, Entity entity) {
        Vec3 flow = fluidState.getFlow(level, pos);
        Vec3 motion = entity.getDeltaMovement();
        Vec3 surged = motion.add(flow.x * 0.040D, flow.horizontalDistanceSqr() > 1.0E-4D ? 0.006D : 0.0D, flow.z * 0.040D);
        if (entity.tickCount % 8 == 0) {
            double driftX = (level.random.nextDouble() - 0.5D) * 0.04D;
            double driftY = level.random.nextDouble() * 0.03D;
            double driftZ = (level.random.nextDouble() - 0.5D) * 0.04D;
            surged = surged.add(driftX, driftY, driftZ);
        }
        entity.setDeltaMovement(surged);
    }

    @Override
    protected void spawnAmbientEffects(Level level, BlockPos pos, FluidState fluidState, SurfaceSample sample, RandomSource random) {
        double x = sample.x();
        double y = sample.y();
        double z = sample.z();

        if (random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(UNSTABLE_TINT, 0.8F), x, y, z, 0.0D, 0.012D, 0.0D);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(new DustParticleOptions(UNSTABLE_HOT_TINT, 0.65F), x, y, z, 0.0D, 0.014D, 0.0D);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.015D, 0.0D);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.015D, 0.0D);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.008D, 0.0D);
        }
        if (random.nextInt(16) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D, 0.01D, 0.0D);
        }
        if (random.nextInt(18) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.08F + random.nextFloat() * 0.05F, 0.6F + random.nextFloat() * 0.18F, false);
        }
        if (random.nextInt(28) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.07F + random.nextFloat() * 0.05F, 1.35F + random.nextFloat() * 0.25F, false);
        }
    }

    private boolean transformItemEntity(ServerLevel level, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty() || stack.is(ModItems.ENTROPIC_MATTER.get())) {
            return false;
        }

        ItemStack transformedStack = new ItemStack(ModItems.ENTROPIC_MATTER.get(), stack.getCount());
        ItemEntity transformedEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), transformedStack);
        transformedEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.45D));
        transformedEntity.setPickUpDelay(10);
        transformedEntity.setDefaultPickUpDelay();
        level.addFreshEntity(transformedEntity);

        Vec3 center = itemEntity.position().add(0.0D, 0.08D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 8, 0.16D, 0.08D, 0.16D, 0.01D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 6, 0.14D, 0.06D, 0.14D, 0.02D);
        level.sendParticles(new DustParticleOptions(UNSTABLE_HOT_TINT, 0.7F), center.x, center.y, center.z, 10, 0.18D, 0.08D, 0.18D, 0.01D);
        level.playSound(null, itemEntity.blockPosition(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.22F, 1.2F);
        level.playSound(null, itemEntity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.16F, 1.45F);

        itemEntity.discard();
        return true;
    }
}
