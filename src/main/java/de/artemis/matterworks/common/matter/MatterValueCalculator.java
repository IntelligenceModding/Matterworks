package de.artemis.matterworks.common.matter;

import de.artemis.matterworks.common.recipe.FluidMachineRecipe;
import de.artemis.matterworks.common.registry.ModRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class MatterValueCalculator {
    private static final int DEFAULT_SOURCE_CEILING = 64 * MatterValueManager.MATTER_PER_DUST;
    private static final int MAX_STABILIZATION_PASSES = 8192;
    private static final int MAX_REFLECTION_DEPTH = 5;

    private MatterValueCalculator() {
    }

    static CalculationResult calculate(Level level, Map<ResourceLocation, Integer> overrides, MatterRuleConfig rules) {
        Map<Item, Integer> values = createInitialValues(overrides, rules);
        List<ValueRule> valueRules = collectValueRules(level, rules);
        Map<Item, String> errorReasons = new LinkedHashMap<>();

        stabilizeValues(values, valueRules, errorReasons);
        enforceRecipeSafety(values, valueRules, errorReasons);

        Map<ResourceLocation, Integer> generatedValues = new LinkedHashMap<>();
        Map<ResourceLocation, MatterValueError> errors = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString()))
                .forEach(entry -> {
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(entry.getKey());
                    if (itemId == null || isConfigured(itemId, rules.valueBlacklist())) {
                        return;
                    }
                    if (entry.getValue() <= 0) {
                        errors.put(itemId, MatterValueError.zero(errorReasons.getOrDefault(entry.getKey(), "zero_value")));
                        return;
                    }
                    generatedValues.put(itemId, entry.getValue());
                });

        rules.valueBlacklist().stream()
                .sorted()
                .forEach(itemId -> errors.put(itemId, MatterValueError.zero("value_blacklisted")));
        return new CalculationResult(generatedValues, errors);
    }

    private static Map<Item, Integer> createInitialValues(Map<ResourceLocation, Integer> overrides, MatterRuleConfig rules) {
        Map<Item, Integer> values = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (item == Items.AIR || itemId == null || isConfigured(itemId, rules.valueBlacklist())) {
                continue;
            }

            int ceiling = overrides.getOrDefault(itemId, DEFAULT_SOURCE_CEILING);
            values.put(item, Math.max(0, ceiling));
        }
        return values;
    }

    private static List<ValueRule> collectValueRules(Level level, MatterRuleConfig rules) {
        List<ValueRule> rulesOut = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.isSpecial()) {
                continue;
            }

            ExtractedRecipe extractedRecipe = extractRecipe(recipe, level, rules);
            addExtractedRecipeRules(extractedRecipe, rulesOut);
        }

        collectMatterMachineRules(level, ModRecipeTypes.GRAVITIC_CONDENSING.get(), rulesOut, rules);
        collectMatterMachineRules(level, ModRecipeTypes.MATTER_SEPARATING.get(), rulesOut, rules);
        return rulesOut;
    }

    private static ExtractedRecipe extractRecipe(Recipe<?> recipe, Level level, MatterRuleConfig rules) {
        ExtractedRecipe extractedRecipe = new ExtractedRecipe();

        ItemStack vanillaOutput = recipe.getResultItem(level.registryAccess()).copy();
        addOutputStack(extractedRecipe, vanillaOutput, rules);

        for (Ingredient ingredient : recipe.getIngredients()) {
            addIngredient(extractedRecipe, ingredient, 1, rules, false);
        }

        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        reflectRecipeValue(recipe, "", extractedRecipe, rules, visited, 0, false);
        return extractedRecipe;
    }

    private static void reflectRecipeValue(
            Object value,
            String nameHint,
            ExtractedRecipe extractedRecipe,
            MatterRuleConfig rules,
            IdentityHashMap<Object, Boolean> visited,
            int depth,
            boolean broadContext
    ) {
        if (value == null || depth > MAX_REFLECTION_DEPTH) {
            return;
        }

        if (value instanceof ItemStack stack) {
            if (isOutputName(nameHint)) {
                addOutputStack(extractedRecipe, stack, rules);
            } else if (isInputName(nameHint) || broadContext) {
                addInputStack(extractedRecipe, stack, rules);
            }
            return;
        }
        if (value instanceof Ingredient ingredient) {
            if (isInputName(nameHint) || broadContext) {
                addIngredient(extractedRecipe, ingredient, 1, rules, true);
            }
            return;
        }
        if (value instanceof SizedIngredient sizedIngredient) {
            if (isInputName(nameHint) || broadContext) {
                addSizedIngredient(extractedRecipe, sizedIngredient, rules);
            }
            return;
        }
        if (value instanceof FluidStack || value instanceof FluidIngredient || value instanceof SizedFluidIngredient) {
            if (isInputName(nameHint) || broadContext) {
                extractedRecipe.reflectedInputs.add(IngredientChoice.free());
            }
            return;
        }
        if (value instanceof Optional<?> optional) {
            optional.ifPresent(nested -> reflectRecipeValue(nested, nameHint, extractedRecipe, rules, visited, depth + 1, broadContext));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                reflectRecipeValue(entry.getValue(), nameHint, extractedRecipe, rules, visited, depth + 1, broadContext);
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object nested : iterable) {
                reflectRecipeValue(nested, nameHint, extractedRecipe, rules, visited, depth + 1, broadContext);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                reflectRecipeValue(Array.get(value, index), nameHint, extractedRecipe, rules, visited, depth + 1, broadContext);
            }
            return;
        }
        if (!shouldInspectObject(value, broadContext) || visited.put(value, Boolean.TRUE) != null) {
            return;
        }

        inspectFields(value, extractedRecipe, rules, visited, depth, broadContext);
    }

    private static void inspectFields(
            Object value,
            ExtractedRecipe extractedRecipe,
            MatterRuleConfig rules,
            IdentityHashMap<Object, Boolean> visited,
            int depth,
            boolean broadContext
    ) {
        Class<?> type = value.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                String fieldName = field.getName();
                boolean relevant = broadContext || isRecipeDataName(fieldName);
                if (!relevant) {
                    continue;
                }

                try {
                    if (!field.trySetAccessible()) {
                        continue;
                    }
                    reflectRecipeValue(field.get(value), fieldName, extractedRecipe, rules, visited, depth + 1, broadContext || isRecipeDataName(fieldName));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Some mod recipe fields are deliberately guarded; ignore those and keep scanning supported data.
                }
            }
            type = type.getSuperclass();
        }
    }

    private static boolean shouldInspectObject(Object value, boolean broadContext) {
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.isEnum() || value instanceof Number || value instanceof CharSequence || value instanceof Boolean) {
            return false;
        }

        Package typePackage = type.getPackage();
        String packageName = typePackage == null ? "" : typePackage.getName();
        return broadContext
                || packageName.startsWith("de.artemis.matterworks")
                || (!packageName.startsWith("java.")
                && !packageName.startsWith("javax.")
                && !packageName.startsWith("com.mojang.")
                && !packageName.startsWith("org.slf4j."));
    }

    private static boolean isRecipeDataName(String name) {
        return isInputName(name) || isOutputName(name);
    }

    private static boolean isInputName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("input")
                || normalized.contains("ingredient")
                || normalized.contains("catalyst")
                || normalized.contains("consume")
                || normalized.contains("requirement");
    }

    private static boolean isOutputName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("output")
                || normalized.contains("result")
                || normalized.contains("produce");
    }

    private static void addExtractedRecipeRules(ExtractedRecipe recipe, List<ValueRule> target) {
        if (recipe.outputs.isEmpty()) {
            return;
        }

        int totalOutputCount = recipe.outputs.stream()
                .mapToInt(ItemAmount::count)
                .sum();
        if (totalOutputCount <= 0) {
            return;
        }

        List<IngredientChoice> recipeInputs = recipe.inputs();
        List<IngredientChoice> inputs = recipeInputs.isEmpty()
                ? List.of(IngredientChoice.free())
                : List.copyOf(recipeInputs);
        for (ItemAmount output : recipe.outputs) {
            target.add(new MultiOutputIngredientValueRule(output.item(), totalOutputCount, inputs));
        }
    }

    private static void addOutputStack(ExtractedRecipe recipe, ItemStack stack, MatterRuleConfig rules) {
        if (stack.isEmpty() || stack.getItem() == Items.AIR || stack.getCount() <= 0) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || isConfigured(itemId, rules.valueBlacklist())) {
            return;
        }
        addUniqueItemAmount(recipe.outputs, new ItemAmount(stack.getItem(), stack.getCount()));
    }

    private static void addInputStack(ExtractedRecipe recipe, ItemStack stack, MatterRuleConfig rules) {
        if (stack.isEmpty() || stack.getItem() == Items.AIR || stack.getCount() <= 0) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || isConfigured(itemId, rules.valueBlacklist())) {
            return;
        }
        recipe.reflectedInputs.add(new IngredientChoice(List.of(new ItemAmount(stack.getItem(), stack.getCount())), false));
    }

    private static void addIngredient(ExtractedRecipe recipe, Ingredient ingredient, int count, MatterRuleConfig rules, boolean reflected) {
        if (ingredient.isEmpty()) {
            return;
        }

        List<ItemAmount> options = new ArrayList<>();
        for (ItemStack option : ingredient.getItems()) {
            if (option.isEmpty() || option.getItem() == Items.AIR) {
                continue;
            }
            ResourceLocation optionId = BuiltInRegistries.ITEM.getKey(option.getItem());
            if (optionId == null || isConfigured(optionId, rules.valueBlacklist())) {
                continue;
            }
            addUniqueItemAmount(options, new ItemAmount(option.getItem(), Math.max(1, option.getCount()) * Math.max(1, count)));
        }

        if (!options.isEmpty()) {
            recipe.addInput(new IngredientChoice(List.copyOf(options), false), reflected);
        }
    }

    private static void addSizedIngredient(ExtractedRecipe recipe, SizedIngredient sizedIngredient, MatterRuleConfig rules) {
        List<ItemAmount> options = new ArrayList<>();
        for (ItemStack option : sizedIngredient.getItems()) {
            if (option.isEmpty() || option.getItem() == Items.AIR) {
                continue;
            }
            ResourceLocation optionId = BuiltInRegistries.ITEM.getKey(option.getItem());
            if (optionId == null || isConfigured(optionId, rules.valueBlacklist())) {
                continue;
            }
            addUniqueItemAmount(options, new ItemAmount(option.getItem(), Math.max(1, option.getCount())));
        }

        if (!options.isEmpty()) {
            recipe.reflectedInputs.add(new IngredientChoice(List.copyOf(options), false));
        }
    }

    private static void addUniqueItemAmount(Collection<ItemAmount> itemAmounts, ItemAmount itemAmount) {
        if (!itemAmounts.contains(itemAmount)) {
            itemAmounts.add(itemAmount);
        }
    }

    private static void collectMatterMachineRules(
            Level level,
            net.minecraft.world.item.crafting.RecipeType<FluidMachineRecipe> recipeType,
            List<ValueRule> target,
            MatterRuleConfig rules
    ) {
        for (RecipeHolder<FluidMachineRecipe> holder : level.getRecipeManager().getAllRecipesFor(recipeType)) {
            FluidMachineRecipe recipe = holder.value();
            ItemStack result = recipe.itemResult();
            if (result.isEmpty() || result.getItem() == Items.AIR || result.getCount() <= 0) {
                continue;
            }

            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (resultId == null || isConfigured(resultId, rules.valueBlacklist())) {
                continue;
            }

            target.add(new FixedBudgetValueRule(result.getItem(), result.getCount(), recipe.inputAmount()));
        }
    }

    private static void stabilizeValues(Map<Item, Integer> values, List<ValueRule> valueRules, Map<Item, String> errorReasons) {
        for (int pass = 0; pass < MAX_STABILIZATION_PASSES; pass++) {
            boolean changed = false;
            for (ValueRule valueRule : valueRules) {
                if (applyRecipeSafetyClamp(values, valueRule)) {
                    changed = true;
                }
            }

            if (!changed) {
                return;
            }
        }

        for (ValueRule valueRule : valueRules) {
            SafetyResult safetyResult = valueRule.safetyResult(values);
            if (safetyResult.canEvaluate()
                    && safetyResult.safeUnitValue() < values.getOrDefault(valueRule.outputItem(), Integer.MAX_VALUE)) {
                values.put(valueRule.outputItem(), 0);
                errorReasons.put(valueRule.outputItem(), "stabilization_pass_limit");
            }
        }
    }

    private static void enforceRecipeSafety(Map<Item, Integer> values, List<ValueRule> valueRules, Map<Item, String> errorReasons) {
        for (int pass = 0; pass < MAX_STABILIZATION_PASSES; pass++) {
            boolean changed = false;
            for (ValueRule valueRule : valueRules) {
                if (applyRecipeSafetyClamp(values, valueRule)) {
                    changed = true;
                }
            }

            if (!changed) {
                return;
            }
        }

        for (ValueRule valueRule : valueRules) {
            SafetyResult safetyResult = valueRule.safetyResult(values);
            if (safetyResult.canEvaluate()
                    && safetyResult.safeUnitValue() < values.getOrDefault(valueRule.outputItem(), Integer.MAX_VALUE)) {
                values.put(valueRule.outputItem(), 0);
                errorReasons.put(valueRule.outputItem(), "recipe_safety_pass_limit");
            }
        }
    }

    private static boolean applyRecipeSafetyClamp(Map<Item, Integer> values, ValueRule valueRule) {
        SafetyResult safetyResult = valueRule.safetyResult(values);
        if (!safetyResult.canEvaluate()) {
            return false;
        }

        Item outputItem = valueRule.outputItem();
        Integer current = values.get(outputItem);
        if (current != null && safetyResult.safeUnitValue() < current) {
            values.put(outputItem, safetyResult.safeUnitValue());
            return true;
        }
        return false;
    }

    private static int divideOutputValue(long inputValue, int outputCount) {
        if (inputValue <= 0L) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, inputValue / Math.max(1, outputCount)));
    }

    private static boolean isConfigured(ResourceLocation itemId, Set<ResourceLocation> configuredIds) {
        return configuredIds.contains(itemId);
    }

    private interface ValueRule {
        Item outputItem();

        SafetyResult safetyResult(Map<Item, Integer> values);
    }

    private record MultiOutputIngredientValueRule(Item outputItem, int totalOutputCount, List<IngredientChoice> inputs) implements ValueRule {
        @Override
        public SafetyResult safetyResult(Map<Item, Integer> values) {
            long inputValue = 0L;
            for (IngredientChoice input : inputs) {
                long cheapestOption = input.zeroCost() ? 0L : Long.MAX_VALUE;
                for (ItemAmount option : input.options()) {
                    Integer optionValue = values.get(option.item());
                    if (optionValue != null) {
                        cheapestOption = Math.min(cheapestOption, (long) optionValue * option.count());
                    }
                }
                if (cheapestOption == Long.MAX_VALUE) {
                    return SafetyResult.missingInputs();
                }

                inputValue += cheapestOption;
                if (inputValue > Integer.MAX_VALUE) {
                    inputValue = Integer.MAX_VALUE;
                }
            }
            return SafetyResult.safe(divideOutputValue(inputValue, totalOutputCount));
        }
    }

    private record FixedBudgetValueRule(Item outputItem, int outputCount, int inputBudget) implements ValueRule {
        @Override
        public SafetyResult safetyResult(Map<Item, Integer> values) {
            return SafetyResult.safe(divideOutputValue(Math.max(0, inputBudget), outputCount));
        }
    }

    private record SafetyResult(boolean canEvaluate, int safeUnitValue) {
        private static SafetyResult safe(int safeUnitValue) {
            return new SafetyResult(true, Math.max(0, safeUnitValue));
        }

        private static SafetyResult missingInputs() {
            return new SafetyResult(false, 0);
        }
    }

    private record IngredientChoice(List<ItemAmount> options, boolean zeroCost) {
        private static IngredientChoice free() {
            return new IngredientChoice(List.of(), true);
        }
    }

    private record ItemAmount(Item item, int count) {
    }

    record CalculationResult(Map<ResourceLocation, Integer> values, Map<ResourceLocation, MatterValueError> errors) {
    }

    private static final class ExtractedRecipe {
        private final List<ItemAmount> outputs = new ArrayList<>();
        private final List<IngredientChoice> apiInputs = new ArrayList<>();
        private final List<IngredientChoice> reflectedInputs = new ArrayList<>();

        private void addInput(IngredientChoice input, boolean reflected) {
            if (reflected) {
                reflectedInputs.add(input);
            } else {
                apiInputs.add(input);
            }
        }

        private List<IngredientChoice> inputs() {
            return reflectedInputs.isEmpty() ? apiInputs : reflectedInputs;
        }
    }
}
