package de.artemis.matterworks.common.matter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class MatterValueStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String VALUES_KEY = "values";
    private static final String VALUE_BLACKLIST_KEY = "value_blacklist";
    private static final String RECYCLER_BLACKLIST_KEY = "recycler_blacklist";
    private static final String PATTERN_BLACKLIST_KEY = "pattern_blacklist";
    private static final String RECYCLER_REJECTIONS_KEY = "recycler_rejections";
    private static final String CONTAINER_ITEMS_KEY = "container_items";
    private static final String CUSTOM_NAME_KEY = "custom_name";
    private static final String LORE_KEY = "lore";
    private static final String WRITABLE_BOOKS_KEY = "writable_books";
    private static final String WRITTEN_BOOKS_KEY = "written_books";

    private MatterValueStorage() {
    }

    static LoadedValues load(Path baseDirectory) throws IOException {
        Files.createDirectories(baseDirectory);

        Path overridesPath = baseDirectory.resolve("matter_values.json");
        Path generatedPath = baseDirectory.resolve("generated_matter_values.json");
        Path rulesPath = baseDirectory.resolve("matter_rules.json");

        ensureFileExists(overridesPath);
        ensureFileExists(generatedPath);
        ensureRulesFileExists(rulesPath);

        MatterRuleConfig rules = readRules(rulesPath);
        if (shouldBackfillDefaultRules(rules)) {
            rules = MatterRuleConfig.defaults().mergedWith(rules);
            writeRules(rulesPath, rules);
        }

        return new LoadedValues(
                readValues(overridesPath),
                readValues(generatedPath),
                rules
        );
    }

    static void saveGenerated(Path baseDirectory, Map<ResourceLocation, Integer> values) throws IOException {
        Files.createDirectories(baseDirectory);
        writeValues(baseDirectory.resolve("generated_matter_values.json"), values);
    }

    private static Map<ResourceLocation, Integer> readValues(Path path) throws IOException {
        Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return values;
            }

            JsonObject rootObject = rootElement.getAsJsonObject();
            JsonObject valuesObject = rootObject.has(VALUES_KEY) && rootObject.get(VALUES_KEY).isJsonObject()
                    ? rootObject.getAsJsonObject(VALUES_KEY)
                    : rootObject;

            for (Map.Entry<String, JsonElement> entry : valuesObject.entrySet()) {
                if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                    continue;
                }

                ResourceLocation resourceLocation = ResourceLocation.tryParse(entry.getKey());
                if (resourceLocation == null) {
                    continue;
                }

                int value = entry.getValue().getAsInt();
                if (value < 0) {
                    continue;
                }

                values.put(resourceLocation, value);
            }
        }
        return values;
    }

    private static void writeValues(Path path, Map<ResourceLocation, Integer> values) throws IOException {
        JsonObject rootObject = new JsonObject();
        JsonObject valuesObject = new JsonObject();
        for (Map.Entry<String, Integer> entry : new TreeMap<>(flatten(values)).entrySet()) {
            valuesObject.addProperty(entry.getKey(), entry.getValue());
        }
        rootObject.add(VALUES_KEY, valuesObject);

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(rootObject, writer);
        }
    }

    private static Map<String, Integer> flatten(Map<ResourceLocation, Integer> values) {
        Map<String, Integer> flattened = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : values.entrySet()) {
            flattened.put(entry.getKey().toString(), entry.getValue());
        }
        return flattened;
    }

    private static void ensureFileExists(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }

        JsonObject rootObject = new JsonObject();
        rootObject.add(VALUES_KEY, new JsonObject());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(rootObject, writer);
        }
    }

    private static MatterRuleConfig readRules(Path path) throws IOException {
        MatterRuleConfig defaults = MatterRuleConfig.defaults();
        Set<ResourceLocation> valueBlacklist = new LinkedHashSet<>();
        Set<ResourceLocation> recyclerBlacklist = new LinkedHashSet<>();
        Set<ResourceLocation> patternBlacklist = new LinkedHashSet<>();
        boolean rejectContainerItems = defaults.rejectContainerItems();
        boolean rejectCustomName = defaults.rejectCustomName();
        boolean rejectLore = defaults.rejectLore();
        boolean rejectWritableBooks = defaults.rejectWritableBooks();
        boolean rejectWrittenBooks = defaults.rejectWrittenBooks();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                return defaults;
            }

            JsonObject rootObject = rootElement.getAsJsonObject();
            readIdArray(rootObject.get(VALUE_BLACKLIST_KEY), valueBlacklist);
            readIdArray(rootObject.get(RECYCLER_BLACKLIST_KEY), recyclerBlacklist);
            readIdArray(rootObject.get(PATTERN_BLACKLIST_KEY), patternBlacklist);

            if (rootObject.has(RECYCLER_REJECTIONS_KEY) && rootObject.get(RECYCLER_REJECTIONS_KEY).isJsonObject()) {
                JsonObject rejectionObject = rootObject.getAsJsonObject(RECYCLER_REJECTIONS_KEY);
                rejectContainerItems = getBooleanOrDefault(rejectionObject, CONTAINER_ITEMS_KEY, rejectContainerItems);
                rejectCustomName = getBooleanOrDefault(rejectionObject, CUSTOM_NAME_KEY, rejectCustomName);
                rejectLore = getBooleanOrDefault(rejectionObject, LORE_KEY, rejectLore);
                rejectWritableBooks = getBooleanOrDefault(rejectionObject, WRITABLE_BOOKS_KEY, rejectWritableBooks);
                rejectWrittenBooks = getBooleanOrDefault(rejectionObject, WRITTEN_BOOKS_KEY, rejectWrittenBooks);
            }
        }

        return new MatterRuleConfig(
                valueBlacklist,
                recyclerBlacklist,
                patternBlacklist,
                rejectContainerItems,
                rejectCustomName,
                rejectLore,
                rejectWritableBooks,
                rejectWrittenBooks
        );
    }

    private static void readIdArray(JsonElement element, Set<ResourceLocation> target) {
        if (element == null || !element.isJsonArray()) {
            return;
        }

        JsonArray array = element.getAsJsonArray();
        for (JsonElement valueElement : array) {
            if (!valueElement.isJsonPrimitive() || !valueElement.getAsJsonPrimitive().isString()) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(valueElement.getAsString());
            if (id != null) {
                target.add(id);
            }
        }
    }

    private static boolean getBooleanOrDefault(JsonObject object, String key, boolean defaultValue) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean()
                ? object.get(key).getAsBoolean()
                : defaultValue;
    }

    private static void ensureRulesFileExists(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }

        JsonObject rootObject = new JsonObject();
        rootObject.add(VALUE_BLACKLIST_KEY, new JsonArray());
        rootObject.add(RECYCLER_BLACKLIST_KEY, new JsonArray());
        rootObject.add(PATTERN_BLACKLIST_KEY, new JsonArray());

        JsonObject rejectionsObject = new JsonObject();
        rejectionsObject.addProperty(CONTAINER_ITEMS_KEY, true);
        rejectionsObject.addProperty(CUSTOM_NAME_KEY, true);
        rejectionsObject.addProperty(LORE_KEY, true);
        rejectionsObject.addProperty(WRITABLE_BOOKS_KEY, true);
        rejectionsObject.addProperty(WRITTEN_BOOKS_KEY, true);
        rootObject.add(RECYCLER_REJECTIONS_KEY, rejectionsObject);

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(rootObject, writer);
        }
    }

    private static void writeRules(Path path, MatterRuleConfig rules) throws IOException {
        JsonObject rootObject = new JsonObject();
        rootObject.add(VALUE_BLACKLIST_KEY, writeIdArray(rules.valueBlacklist()));
        rootObject.add(RECYCLER_BLACKLIST_KEY, writeIdArray(rules.recyclerBlacklist()));
        rootObject.add(PATTERN_BLACKLIST_KEY, writeIdArray(rules.patternBlacklist()));

        JsonObject rejectionsObject = new JsonObject();
        rejectionsObject.addProperty(CONTAINER_ITEMS_KEY, rules.rejectContainerItems());
        rejectionsObject.addProperty(CUSTOM_NAME_KEY, rules.rejectCustomName());
        rejectionsObject.addProperty(LORE_KEY, rules.rejectLore());
        rejectionsObject.addProperty(WRITABLE_BOOKS_KEY, rules.rejectWritableBooks());
        rejectionsObject.addProperty(WRITTEN_BOOKS_KEY, rules.rejectWrittenBooks());
        rootObject.add(RECYCLER_REJECTIONS_KEY, rejectionsObject);

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(rootObject, writer);
        }
    }

    private static JsonArray writeIdArray(Set<ResourceLocation> ids) {
        JsonArray array = new JsonArray();
        ids.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(array::add);
        return array;
    }

    private static boolean shouldBackfillDefaultRules(MatterRuleConfig rules) {
        return rules.valueBlacklist().isEmpty()
                && rules.recyclerBlacklist().isEmpty()
                && rules.patternBlacklist().isEmpty();
    }

    record LoadedValues(Map<ResourceLocation, Integer> overrides, Map<ResourceLocation, Integer> generated, MatterRuleConfig rules) {
    }
}
