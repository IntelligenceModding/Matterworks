package de.artemis.matterworks.client.jei;

import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.blockentity.MatterAnalyzerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterConstructorBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterRecyclerBlockEntity;
import de.artemis.matterworks.common.blockentity.PowerCrystalChargerBlockEntity;
import de.artemis.matterworks.common.blockentity.EnergyCellBlockEntity;
import de.artemis.matterworks.common.blockentity.FluidTankBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterStorageBarrelBlockEntity;
import de.artemis.matterworks.common.matter.MatterValueManager;
import de.artemis.matterworks.common.matter.MatterValueResult;
import de.artemis.matterworks.common.recipe.FluidMachineRecipe;
import de.artemis.matterworks.common.recipe.MatterFilterCopyRecipe;
import de.artemis.matterworks.common.registry.ModBlocks;
import de.artemis.matterworks.common.registry.ModFluids;
import de.artemis.matterworks.common.registry.ModItems;
import de.artemis.matterworks.common.registry.ModRecipeTypes;
import de.artemis.matterworks.common.template.EncodedTemplateData;
import de.artemis.matterworks.common.template.TemplateAnalysisManager;
import de.artemis.matterworks.common.upgrade.PowerCrystalData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@JeiPlugin
public class MatterworksJeiPlugin implements IModPlugin {
    public static final Supplier<RecipeType<RecipeHolder<FluidMachineRecipe>>> GRAVITIC_CONDENSING =
            RecipeType.createFromDeferredVanilla(ModRecipeTypes.GRAVITIC_CONDENSING);
    public static final Supplier<RecipeType<RecipeHolder<FluidMachineRecipe>>> MATTER_SEPARATING =
            RecipeType.createFromDeferredVanilla(ModRecipeTypes.MATTER_SEPARATING);
    public static final Supplier<RecipeType<RecipeHolder<FluidMachineRecipe>>> MATTER_STABILIZING =
            RecipeType.createFromDeferredVanilla(ModRecipeTypes.MATTER_STABILIZING);
    public static final RecipeType<CombustionFuelJeiRecipe> COMBUSTION_GENERATING =
            RecipeType.create(Matterworks.MOD_ID, "combustion_generating", CombustionFuelJeiRecipe.class);
    public static final RecipeType<MatterRecyclerJeiRecipe> MATTER_RECYCLING =
            RecipeType.create(Matterworks.MOD_ID, "matter_recycling", MatterRecyclerJeiRecipe.class);
    public static final RecipeType<MatterAnalyzerJeiRecipe> MATTER_ANALYZING =
            RecipeType.create(Matterworks.MOD_ID, "matter_analyzing", MatterAnalyzerJeiRecipe.class);
    public static final RecipeType<MatterConstructorJeiRecipe> MATTER_CONSTRUCTING =
            RecipeType.create(Matterworks.MOD_ID, "matter_constructing", MatterConstructorJeiRecipe.class);
    public static final RecipeType<PowerCrystalChargingJeiRecipe> POWER_CRYSTAL_CHARGING =
            RecipeType.create(Matterworks.MOD_ID, "power_crystal_charging", PowerCrystalChargingJeiRecipe.class);
    public static final RecipeType<MachineOperationJeiRecipe> MACHINE_OPERATIONS =
            RecipeType.create(Matterworks.MOD_ID, "machine_operations", MachineOperationJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CombustionGeneratorRecipeCategory(COMBUSTION_GENERATING, guiHelper),
                new MatterRecyclerRecipeCategory(MATTER_RECYCLING, guiHelper),
                new MatterAnalyzerRecipeCategory(MATTER_ANALYZING, guiHelper),
                new MatterConstructorRecipeCategory(MATTER_CONSTRUCTING, guiHelper),
                new PowerCrystalChargerRecipeCategory(POWER_CRYSTAL_CHARGING, guiHelper),
                new MachineOperationRecipeCategory(MACHINE_OPERATIONS, guiHelper),
                new FluidMachineRecipeCategory(GRAVITIC_CONDENSING.get(), ModBlocks.GRAVITIC_CONDENSER, guiHelper),
                new FluidMachineRecipeCategory(MATTER_SEPARATING.get(), ModBlocks.MATTER_SEPARATOR, guiHelper),
                new FluidMachineRecipeCategory(MATTER_STABILIZING.get(), ModBlocks.MATTER_STABILIZER, guiHelper)
        );
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(MatterFilterCopyRecipe.class, new MatterFilterCopyCategoryExtension());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        registration.addRecipes(COMBUSTION_GENERATING, createFuelRecipes());
        registration.addRecipes(MATTER_RECYCLING, createRecyclerRecipes(level));
        registration.addRecipes(MATTER_ANALYZING, createAnalyzerRecipes(level));
        registration.addRecipes(MATTER_CONSTRUCTING, createConstructorRecipes(level));
        registration.addRecipes(POWER_CRYSTAL_CHARGING, createChargingRecipes());
        registration.addRecipes(MACHINE_OPERATIONS, createMachineOperationRecipes());

        addMachineRecipes(registration, GRAVITIC_CONDENSING.get(), ModRecipeTypes.GRAVITIC_CONDENSING);
        addMachineRecipes(registration, MATTER_SEPARATING.get(), ModRecipeTypes.MATTER_SEPARATING);
        addMachineRecipes(registration, MATTER_STABILIZING.get(), ModRecipeTypes.MATTER_STABILIZING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.COMBUSTION_GENERATOR.get(), COMBUSTION_GENERATING, mezz.jei.api.constants.RecipeTypes.FUELING);
        registration.addRecipeCatalyst(ModBlocks.MATTER_RECYCLER.get(), MATTER_RECYCLING);
        registration.addRecipeCatalyst(ModBlocks.MATTER_ANALYZER.get(), MATTER_ANALYZING);
        registration.addRecipeCatalyst(ModBlocks.MATTER_CONSTRUCTOR.get(), MATTER_CONSTRUCTING);
        registration.addRecipeCatalyst(ModBlocks.POWER_CRYSTAL_CHARGER.get(), POWER_CRYSTAL_CHARGING);
        registration.addRecipeCatalyst(ModBlocks.GRAVITIC_CONDENSER.get(), GRAVITIC_CONDENSING.get());
        registration.addRecipeCatalyst(ModBlocks.MATTER_SEPARATOR.get(), MATTER_SEPARATING.get());
        registration.addRecipeCatalyst(ModBlocks.MATTER_STABILIZER.get(), MATTER_STABILIZING.get());
        registration.addRecipeCatalyst(ModBlocks.MATTER_ENERGY_CELL.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.FLUID_TANK.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.MATTER_STORAGE_BARREL.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.MATTER_BATTERY_CORE.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.MULTIBLOCK_PORT.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.MATTER_PYLON.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.SINGULARITY_LINK.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.CREATIVE_SOURCE.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModBlocks.CREATIVE_SINK.get(), MACHINE_OPERATIONS);
        registration.addRecipeCatalyst(ModItems.MATTER_ITEM_FILTER.get(), mezz.jei.api.constants.RecipeTypes.CRAFTING);
        registration.addRecipeCatalyst(ModItems.MATTER_FLUID_FILTER.get(), mezz.jei.api.constants.RecipeTypes.CRAFTING);
    }

    private static List<CombustionFuelJeiRecipe> createFuelRecipes() {
        return BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> !stack.isEmpty())
                .map(stack -> new CombustionFuelJeiRecipe(
                        stack,
                        Math.max(0, stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING)),
                        PowerCrystalEffectsAccess.COMBUSTION_GENERATOR_BASE_ENERGY_PER_TICK
                ))
                .filter(recipe -> recipe.burnTime() > 0)
                .toList();
    }

    private static List<MatterRecyclerJeiRecipe> createRecyclerRecipes(Level level) {
        if (level == null) {
            return List.of();
        }
        List<MatterRecyclerJeiRecipe> recipes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            MatterValueResult result = MatterValueManager.evaluateRecycler(stack, level);
            if (result.allowed()) {
                recipes.add(new MatterRecyclerJeiRecipe(
                        stack,
                        new FluidStack(ModFluids.RAW_MATTER.get(), result.matterMillibuckets()),
                        MatterRecyclerBlockEntity.PROCESS_TIME,
                        MatterRecyclerBlockEntity.ENERGY_PER_TICK
                ));
            }
        }
        return recipes;
    }

    private static List<MatterAnalyzerJeiRecipe> createAnalyzerRecipes(Level level) {
        if (level == null) {
            return List.of();
        }
        List<MatterAnalyzerJeiRecipe> recipes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()
                    || stack.is(ModItems.EMPTY_TEMPLATE.get())
                    || stack.is(ModItems.ENCODED_TEMPLATE.get())
                    || MatterValueManager.isPatternEncodingBlocked(stack)) {
                continue;
            }
            int required = TemplateAnalysisManager.getRequiredItemCount(stack, level);
            ItemStack output = EncodedTemplateData.createEncodedTemplate(item, required);
            EncodedTemplateData.setAnalysisProgress(output, required);
            recipes.add(new MatterAnalyzerJeiRecipe(
                    stack.copyWithCount(Math.min(stack.getMaxStackSize(), required)),
                    output,
                    required,
                    MatterAnalyzerBlockEntity.DATA_COUNT == 7 ? 100 : 100,
                    25
            ));
        }
        return recipes;
    }

    private static List<MatterConstructorJeiRecipe> createConstructorRecipes(Level level) {
        if (level == null) {
            return List.of();
        }
        List<MatterConstructorJeiRecipe> recipes = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack output = new ItemStack(item);
            if (!MatterValueManager.canConstruct(output, level)) {
                continue;
            }
            int required = TemplateAnalysisManager.getRequiredItemCount(output, level);
            ItemStack template = EncodedTemplateData.createEncodedTemplate(item, required);
            EncodedTemplateData.setAnalysisProgress(template, required);
            int matter = Math.max(1, MatterValueManager.getMatterValue(output, level));
            recipes.add(new MatterConstructorJeiRecipe(
                    template,
                    new FluidStack(ModFluids.REFINED_MATTER.get(), matter),
                    output,
                    new FluidStack(ModFluids.MATTER_SLUDGE.get(), Math.max(5, matter / 20)),
                    MatterConstructorBlockEntity.PROCESS_TIME,
                    MatterConstructorBlockEntity.ENERGY_PER_TICK
            ));
        }
        return recipes;
    }

    private static List<PowerCrystalChargingJeiRecipe> createChargingRecipes() {
        return List.of(
                createCrystalChargingRecipe(ModItems.CRIMSON_POWER_CRYSTAL.get()),
                createCrystalChargingRecipe(ModItems.AZURE_POWER_CRYSTAL.get()),
                createCrystalChargingRecipe(ModItems.VERDANT_POWER_CRYSTAL.get())
        );
    }

    private static PowerCrystalChargingJeiRecipe createCrystalChargingRecipe(Item item) {
        ItemStack input = new ItemStack(item);
        PowerCrystalData.setChargePercent(input, 99);
        return new PowerCrystalChargingJeiRecipe(
                input,
                new ItemStack(item),
                PowerCrystalChargerBlockEntity.TICKS_PER_PERCENT,
                PowerCrystalChargerBlockEntity.ENERGY_PER_TICK
        );
    }

    private static List<MachineOperationJeiRecipe> createMachineOperationRecipes() {
        ItemStack powerBank = new ItemStack(ModItems.MATTER_POWER_BANK.get());
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE);
        FluidStack waterBucket = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME);

        return List.of(
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.MATTER_ENERGY_CELL.get()),
                        List.of(powerBank),
                        List.of(),
                        List.of(powerBank),
                        List.of(),
                        List.of(
                                Component.literal("Stores and sends FE."),
                                Component.literal(EnergyCellBlockEntity.MAX_SLOT_TRANSFER_PER_TICK + " FE/t item transfer")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.FLUID_TANK.get()),
                        List.of(new ItemStack(Items.WATER_BUCKET)),
                        List.of(),
                        List.of(new ItemStack(Items.BUCKET)),
                        List.of(waterBucket),
                        List.of(
                                Component.literal("Drains fluid containers."),
                                Component.literal(FluidTankBlockEntity.MAX_SLOT_TRANSFER_PER_TICK + " mB/t item transfer")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.FLUID_TANK.get()),
                        List.of(new ItemStack(Items.BUCKET)),
                        List.of(waterBucket),
                        List.of(new ItemStack(Items.WATER_BUCKET)),
                        List.of(),
                        List.of(
                                Component.literal("Fills fluid containers."),
                                Component.literal(FluidTankBlockEntity.MAX_SIDE_TRANSFER_PER_TICK + " mB/t side transfer")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.MATTER_STORAGE_BARREL.get()),
                        List.of(cobblestone),
                        List.of(),
                        List.of(cobblestone),
                        List.of(),
                        List.of(
                                Component.literal("Stores network items."),
                                Component.literal(MatterStorageBarrelBlockEntity.SLOT_COUNT + " item slots")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.MATTER_BATTERY_CORE.get()),
                        List.of(powerBank, new ItemStack(ModBlocks.MULTIBLOCK_PORT.get())),
                        List.of(),
                        List.of(powerBank),
                        List.of(),
                        List.of(
                                Component.literal("Multiblock FE storage."),
                                Component.literal("Ports import/export FE.")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.MATTER_PYLON.get()),
                        List.of(cobblestone),
                        List.of(waterBucket),
                        List.of(cobblestone),
                        List.of(waterBucket),
                        List.of(
                                Component.literal("Routes network resources."),
                                Component.literal("Crystals boost transfer.")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.SINGULARITY_LINK.get()),
                        List.of(new ItemStack(ModItems.MATTER_SINGULARITY.get()), powerBank),
                        List.of(),
                        List.of(cobblestone),
                        List.of(),
                        List.of(
                                Component.literal("Long-distance transfer."),
                                Component.literal("80 FE per link segment")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.CREATIVE_SOURCE.get()),
                        List.of(),
                        List.of(),
                        List.of(cobblestone),
                        List.of(waterBucket),
                        List.of(
                                Component.literal("Produces test resources."),
                                Component.literal("60 FE/t")
                        )
                ),
                new MachineOperationJeiRecipe(
                        new ItemStack(ModBlocks.CREATIVE_SINK.get()),
                        List.of(cobblestone),
                        List.of(waterBucket),
                        List.of(),
                        List.of(),
                        List.of(
                                Component.literal("Consumes test resources."),
                                Component.literal("120 FE/t max input")
                        )
                )
        );
    }

    private static void addMachineRecipes(
            IRecipeRegistration registration,
            RecipeType<RecipeHolder<FluidMachineRecipe>> jeiRecipeType,
            Supplier<net.minecraft.world.item.crafting.RecipeType<FluidMachineRecipe>> minecraftRecipeType
    ) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        List<RecipeHolder<FluidMachineRecipe>> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(minecraftRecipeType.get());
        registration.addRecipes(jeiRecipeType, recipes);
    }

    private static final class PowerCrystalEffectsAccess {
        private static final int COMBUSTION_GENERATOR_BASE_ENERGY_PER_TICK = 40;
    }
}
