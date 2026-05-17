package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.fluid.MatterSludgeFluidType;
import de.artemis.matterworks.common.fluid.MatterFluid;
import de.artemis.matterworks.common.fluid.RefinedMatterFluidType;
import de.artemis.matterworks.common.fluid.RawMatterFluidType;
import de.artemis.matterworks.common.fluid.UnstableMatterFluidType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Matterworks.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Matterworks.MOD_ID);

    public static final DeferredHolder<FluidType, RawMatterFluidType> RAW_MATTER_TYPE =
            FLUID_TYPES.register("raw_matter", () -> new RawMatterFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.raw_matter")
                    .density(1325)
                    .temperature(295)
                    .viscosity(2400)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.MUD_HIT)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.SAND_PLACE)));

    public static final DeferredHolder<FluidType, RefinedMatterFluidType> REFINED_MATTER_TYPE =
            FLUID_TYPES.register("refined_matter", () -> new RefinedMatterFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.refined_matter")
                    .density(980)
                    .temperature(290)
                    .viscosity(900)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.AMETHYST_BLOCK_CHIME)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.ENCHANTMENT_TABLE_USE)));

    public static final DeferredHolder<FluidType, UnstableMatterFluidType> UNSTABLE_MATTER_TYPE =
            FLUID_TYPES.register("unstable_matter", () -> new UnstableMatterFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.unstable_matter")
                    .density(1080)
                    .temperature(305)
                    .viscosity(850)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.FIRE_EXTINGUISH)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.LAVA_POP)));

    public static final DeferredHolder<FluidType, MatterSludgeFluidType> MATTER_SLUDGE_TYPE =
            FLUID_TYPES.register("matter_sludge", () -> new MatterSludgeFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.matter_sludge")
                    .density(1450)
                    .temperature(300)
                    .viscosity(3200)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.MUD_HIT)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.MUD_PLACE)));

    public static final DeferredHolder<Fluid, MatterFluid.Source> RAW_MATTER =
            FLUIDS.register("raw_matter", () -> new MatterFluid.Source(rawMatterProperties(), ModParticles.DRIPPING_RAW_MATTER));

    public static final DeferredHolder<Fluid, MatterFluid.Flowing> FLOWING_RAW_MATTER =
            FLUIDS.register("flowing_raw_matter", () -> new MatterFluid.Flowing(rawMatterProperties(), ModParticles.DRIPPING_RAW_MATTER));

    public static final DeferredHolder<Fluid, MatterFluid.Source> REFINED_MATTER =
            FLUIDS.register("refined_matter", () -> new MatterFluid.Source(refinedMatterProperties(), () -> ParticleTypes.DRIPPING_OBSIDIAN_TEAR));

    public static final DeferredHolder<Fluid, MatterFluid.Flowing> FLOWING_REFINED_MATTER =
            FLUIDS.register("flowing_refined_matter", () -> new MatterFluid.Flowing(refinedMatterProperties(), () -> ParticleTypes.DRIPPING_OBSIDIAN_TEAR));

    public static final DeferredHolder<Fluid, MatterFluid.Source> UNSTABLE_MATTER =
            FLUIDS.register("unstable_matter", () -> new MatterFluid.Source(unstableMatterProperties(), () -> ParticleTypes.DRIPPING_LAVA));

    public static final DeferredHolder<Fluid, MatterFluid.Flowing> FLOWING_UNSTABLE_MATTER =
            FLUIDS.register("flowing_unstable_matter", () -> new MatterFluid.Flowing(unstableMatterProperties(), () -> ParticleTypes.DRIPPING_LAVA));

    public static final DeferredHolder<Fluid, MatterFluid.Source> MATTER_SLUDGE =
            FLUIDS.register("matter_sludge", () -> new MatterFluid.Source(matterSludgeProperties(), () -> ParticleTypes.DRIPPING_HONEY));

    public static final DeferredHolder<Fluid, MatterFluid.Flowing> FLOWING_MATTER_SLUDGE =
            FLUIDS.register("flowing_matter_sludge", () -> new MatterFluid.Flowing(matterSludgeProperties(), () -> ParticleTypes.DRIPPING_HONEY));

    private ModFluids() {
    }

    private static BaseFlowingFluid.Properties rawMatterProperties() {
        return new BaseFlowingFluid.Properties(RAW_MATTER_TYPE, RAW_MATTER, FLOWING_RAW_MATTER)
                .bucket(ModItems.RAW_MATTER_BUCKET)
                .block(ModBlocks.RAW_MATTER_BLOCK)
                .tickRate(28)
                .slopeFindDistance(1)
                .levelDecreasePerBlock(3);
    }

    private static BaseFlowingFluid.Properties refinedMatterProperties() {
        return new BaseFlowingFluid.Properties(REFINED_MATTER_TYPE, REFINED_MATTER, FLOWING_REFINED_MATTER)
                .bucket(ModItems.REFINED_MATTER_BUCKET)
                .block(ModBlocks.REFINED_MATTER_BLOCK)
                .tickRate(8)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    private static BaseFlowingFluid.Properties unstableMatterProperties() {
        return new BaseFlowingFluid.Properties(UNSTABLE_MATTER_TYPE, UNSTABLE_MATTER, FLOWING_UNSTABLE_MATTER)
                .bucket(ModItems.UNSTABLE_MATTER_BUCKET)
                .block(ModBlocks.UNSTABLE_MATTER_BLOCK)
                .tickRate(6)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1);
    }

    private static BaseFlowingFluid.Properties matterSludgeProperties() {
        return new BaseFlowingFluid.Properties(MATTER_SLUDGE_TYPE, MATTER_SLUDGE, FLOWING_MATTER_SLUDGE)
                .bucket(ModItems.MATTER_SLUDGE_BUCKET)
                .block(ModBlocks.MATTER_SLUDGE_BLOCK)
                .tickRate(36)
                .slopeFindDistance(1)
                .levelDecreasePerBlock(3);
    }

    public static boolean isRawMatter(Fluid fluid) {
        return fluid.isSame(RAW_MATTER.get()) || fluid.isSame(FLOWING_RAW_MATTER.get());
    }

    public static boolean isRawMatter(FluidStack fluidStack) {
        return !fluidStack.isEmpty() && isRawMatter(fluidStack.getFluid());
    }

    public static boolean isUnstableMatter(Fluid fluid) {
        return fluid.isSame(UNSTABLE_MATTER.get()) || fluid.isSame(FLOWING_UNSTABLE_MATTER.get());
    }

    public static boolean isUnstableMatter(FluidStack fluidStack) {
        return !fluidStack.isEmpty() && isUnstableMatter(fluidStack.getFluid());
    }

    public static boolean isRefinedMatter(Fluid fluid) {
        return fluid.isSame(REFINED_MATTER.get()) || fluid.isSame(FLOWING_REFINED_MATTER.get());
    }

    public static boolean isRefinedMatter(FluidStack fluidStack) {
        return !fluidStack.isEmpty() && isRefinedMatter(fluidStack.getFluid());
    }

    public static boolean isMatterSludge(Fluid fluid) {
        return fluid.isSame(MATTER_SLUDGE.get()) || fluid.isSame(FLOWING_MATTER_SLUDGE.get());
    }

    public static boolean isMatterSludge(FluidStack fluidStack) {
        return !fluidStack.isEmpty() && isMatterSludge(fluidStack.getFluid());
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
