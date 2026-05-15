package de.artemis.matterworks.common.datagen;

import com.google.gson.JsonObject;
import de.artemis.matterworks.Matterworks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModModelProvider implements DataProvider {
    private static final String CUTOUT = "minecraft:cutout";

    private final PackOutput.PathProvider blockstatesPathProvider;
    private final PackOutput.PathProvider blockModelPathProvider;
    private final PackOutput.PathProvider itemModelPathProvider;

    public ModModelProvider(PackOutput output) {
        this.blockstatesPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.blockModelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/block");
        this.itemModelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        ModDatagenEntries.ALL_PAIRS.forEach(pair -> {
            futures.add(saveSimpleBlockstate(output, pair.baseModelName(), pair.baseModelName()));
            futures.add(saveSimpleBlockstate(output, pair.glowingModelName(), pair.glowingModelName()));
            futures.add(saveBlockItemDefinition(output, pair.baseModelName(), pair.baseModelName() + "_inventory"));
            futures.add(saveBlockItemDefinition(output, pair.glowingModelName(), pair.glowingModelName() + "_inventory"));
        });

        ModDatagenEntries.PILLAR_PAIRS.forEach(pair -> {
            futures.add(saveSimpleBlockstate(output, pair.baseModelName(), pair.baseModelName()));
            futures.add(saveSimpleBlockstate(output, pair.glowingModelName(), pair.glowingModelName()));
            futures.add(saveBlockItemDefinition(output, pair.baseModelName(), pair.baseModelName() + "_inventory"));
            futures.add(saveBlockItemDefinition(output, pair.glowingModelName(), pair.glowingModelName() + "_inventory"));
        });

        ModDatagenEntries.DOORS.forEach(door -> {
            String name = door.getId().getPath();
            futures.addAll(saveFlatItem(output, name));
            futures.add(saveDoorBlockstate(output, name));
        });

        ModDatagenEntries.TRAPDOORS.forEach(trapdoor -> {
            String name = trapdoor.getId().getPath();
            futures.add(saveBlockItemDefinition(output, name, name + "_bottom"));
            futures.add(saveTrapdoorBlockstate(output, name));
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public @NotNull String getName() {
        return "Model Definitions: " + Matterworks.MOD_ID;
    }

    private List<CompletableFuture<?>> saveFlatItem(CachedOutput output, String itemName) {
        return List.of(saveFlatItemModel(output, itemName, "minecraft:item/generated"));
    }

    @SuppressWarnings("unused")
    private List<CompletableFuture<?>> saveHandheldItem(CachedOutput output, String itemName) {
        return List.of(saveFlatItemModel(output, itemName, "minecraft:item/handheld"));
    }

    private CompletableFuture<?> saveFlatItemModel(CachedOutput output, String itemName, String parent) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent);

        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", modPath("item/" + itemName));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, itemModelPathProvider.json(id(itemName)));
    }

    private CompletableFuture<?> saveSimpleBlockstate(CachedOutput output, String blockName, String modelName) {
        JsonObject json = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modPath("block/" + modelName));
        variants.add("", variant);
        json.add("variants", variants);
        return DataProvider.saveStable(output, json, blockstatesPathProvider.json(id(blockName)));
    }

    private CompletableFuture<?> saveDoorBlockstate(CachedOutput output, String blockName) {
        JsonObject json = new JsonObject();
        json.add("variants", new JsonObject());
        return DataProvider.saveStable(output, json, blockstatesPathProvider.json(id(blockName)));
    }

    private CompletableFuture<?> saveTrapdoorBlockstate(CachedOutput output, String blockName) {
        JsonObject json = new JsonObject();
        json.add("variants", new JsonObject());
        return DataProvider.saveStable(output, json, blockstatesPathProvider.json(id(blockName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveCubeInventoryModel(CachedOutput output, String modelName, String texturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");

        JsonObject textures = new JsonObject();
        textures.addProperty("all", modPath("block/" + texturePath));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveCubeColumnModel(CachedOutput output, String modelName, String sideTexturePath, String endTexturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_column");

        JsonObject textures = new JsonObject();
        textures.addProperty("side", modPath("block/" + sideTexturePath));
        textures.addProperty("end", modPath("block/" + endTexturePath));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveGlowingInventoryModel(CachedOutput output, String modelName, String baseTexturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", modPath("block/glowing_block_inventory_overlay"));

        JsonObject textures = new JsonObject();
        textures.addProperty("base", modPath("block/" + baseTexturePath));
        textures.addProperty("overlay", modPath("block/glowing_block_inventory_overlay"));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveGlowingPillarInventoryModel(CachedOutput output, String modelName, String sideTexturePath, String endTexturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", modPath("block/glowing_column_inventory_overlay"));

        JsonObject textures = new JsonObject();
        textures.addProperty("base_side", modPath("block/" + sideTexturePath));
        textures.addProperty("base_end", modPath("block/" + endTexturePath));
        textures.addProperty("overlay", modPath("block/glowing_block_inventory_overlay"));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveCutoutCubeInventoryModel(CachedOutput output, String modelName, String texturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");
        json.addProperty("render_type", CUTOUT);

        JsonObject textures = new JsonObject();
        textures.addProperty("all", modPath("block/" + texturePath));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveCutoutGlowingInventoryModel(CachedOutput output, String modelName, String texturePath) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", modPath("block/glowing_block_inventory_overlay"));
        json.addProperty("render_type", CUTOUT);

        JsonObject textures = new JsonObject();
        textures.addProperty("base", modPath("block/" + texturePath));
        textures.addProperty("overlay", modPath("block/glowing_block_inventory_overlay"));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveDoorModel(CachedOutput output, String modelName, String parent, String textureBase) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent);
        json.addProperty("render_type", CUTOUT);

        JsonObject textures = new JsonObject();
        textures.addProperty("bottom", modPath("block/" + textureBase + "_bottom"));
        textures.addProperty("top", modPath("block/" + textureBase + "_top"));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<?> saveTrapdoorModel(CachedOutput output, String modelName, String parent, String textureBase) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent);
        json.addProperty("render_type", CUTOUT);

        JsonObject textures = new JsonObject();
        textures.addProperty("texture", modPath("block/" + textureBase));
        json.add("textures", textures);

        return DataProvider.saveStable(output, json, blockModelPathProvider.json(id(modelName)));
    }

    private CompletableFuture<?> saveBlockItemDefinition(CachedOutput output, String itemName, String blockModelName) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", modPath("block/" + blockModelName));
        return DataProvider.saveStable(output, json, itemModelPathProvider.json(id(itemName)));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Matterworks.MOD_ID, path);
    }

    private static String modPath(String path) {
        return Matterworks.MOD_ID + ":" + path;
    }
}
