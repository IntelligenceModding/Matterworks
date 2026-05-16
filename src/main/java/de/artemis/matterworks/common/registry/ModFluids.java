package de.artemis.matterworks.common.registry;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.fluid.MatterSludgeFluidType;
import de.artemis.matterworks.common.fluid.RefinedMatterFluidType;
import de.artemis.matterworks.common.fluid.RawMatterFluidType;
import de.artemis.matterworks.common.fluid.UnstableMatterFluidType;
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
                    .density(1200)
                    .temperature(295)
                    .viscosity(1800)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<FluidType, RefinedMatterFluidType> REFINED_MATTER_TYPE =
            FLUID_TYPES.register("refined_matter", () -> new RefinedMatterFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.refined_matter")
                    .density(1100)
                    .temperature(290)
                    .viscosity(1400)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<FluidType, UnstableMatterFluidType> UNSTABLE_MATTER_TYPE =
            FLUID_TYPES.register("unstable_matter", () -> new UnstableMatterFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.unstable_matter")
                    .density(1250)
                    .temperature(305)
                    .viscosity(2200)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<FluidType, MatterSludgeFluidType> MATTER_SLUDGE_TYPE =
            FLUID_TYPES.register("matter_sludge", () -> new MatterSludgeFluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.matterworks.matter_sludge")
                    .density(1350)
                    .temperature(300)
                    .viscosity(2600)
                    .canDrown(false)
                    .canExtinguish(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> RAW_MATTER =
            FLUIDS.register("raw_matter", () -> new BaseFlowingFluid.Source(rawMatterProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_RAW_MATTER =
            FLUIDS.register("flowing_raw_matter", () -> new BaseFlowingFluid.Flowing(rawMatterProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> REFINED_MATTER =
            FLUIDS.register("refined_matter", () -> new BaseFlowingFluid.Source(refinedMatterProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_REFINED_MATTER =
            FLUIDS.register("flowing_refined_matter", () -> new BaseFlowingFluid.Flowing(refinedMatterProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> UNSTABLE_MATTER =
            FLUIDS.register("unstable_matter", () -> new BaseFlowingFluid.Source(unstableMatterProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_UNSTABLE_MATTER =
            FLUIDS.register("flowing_unstable_matter", () -> new BaseFlowingFluid.Flowing(unstableMatterProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> MATTER_SLUDGE =
            FLUIDS.register("matter_sludge", () -> new BaseFlowingFluid.Source(matterSludgeProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_MATTER_SLUDGE =
            FLUIDS.register("flowing_matter_sludge", () -> new BaseFlowingFluid.Flowing(matterSludgeProperties()));

    private ModFluids() {
    }

    private static BaseFlowingFluid.Properties rawMatterProperties() {
        return new BaseFlowingFluid.Properties(RAW_MATTER_TYPE, RAW_MATTER, FLOWING_RAW_MATTER)
                .bucket(ModItems.RAW_MATTER_BUCKET)
                .block(ModBlocks.RAW_MATTER_BLOCK)
                .tickRate(20)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }

    private static BaseFlowingFluid.Properties refinedMatterProperties() {
        return new BaseFlowingFluid.Properties(REFINED_MATTER_TYPE, REFINED_MATTER, FLOWING_REFINED_MATTER)
                .bucket(ModItems.REFINED_MATTER)
                .block(ModBlocks.REFINED_MATTER_BLOCK)
                .tickRate(18)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }

    private static BaseFlowingFluid.Properties unstableMatterProperties() {
        return new BaseFlowingFluid.Properties(UNSTABLE_MATTER_TYPE, UNSTABLE_MATTER, FLOWING_UNSTABLE_MATTER)
                .bucket(ModItems.UNSTABLE_MATTER)
                .block(ModBlocks.UNSTABLE_MATTER_BLOCK)
                .tickRate(24)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }

    private static BaseFlowingFluid.Properties matterSludgeProperties() {
        return new BaseFlowingFluid.Properties(MATTER_SLUDGE_TYPE, MATTER_SLUDGE, FLOWING_MATTER_SLUDGE)
                .bucket(ModItems.MATTER_SLUDGE)
                .block(ModBlocks.MATTER_SLUDGE_BLOCK)
                .tickRate(28)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
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
