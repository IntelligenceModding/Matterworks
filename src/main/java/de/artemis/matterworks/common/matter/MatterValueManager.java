package de.artemis.matterworks.common.matter;

import com.mojang.logging.LogUtils;
import de.artemis.matterworks.Matterworks;
import de.artemis.matterworks.common.template.TemplateAnalysisManager;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MatterValueManager {
    public static final int MATTER_PER_DUST = 100;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LOCK = new Object();

    private static final Map<ResourceLocation, Integer> overrideValues = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Integer> generatedValues = new LinkedHashMap<>();
    private static final Set<UUID> debugSubscribers = new HashSet<>();
    private static final Set<UUID> recyclerDebugSubscribers = new HashSet<>();
    private static MatterRuleConfig ruleConfig = MatterRuleConfig.defaults();

    private static Path storageDirectory;
    private static boolean dirty;

    private MatterValueManager() {
    }

    public static void registerGameEvents() {
        NeoForge.EVENT_BUS.addListener(MatterValueManager::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(MatterValueManager::onServerStopping);
        NeoForge.EVENT_BUS.addListener(MatterValueManager::onRegisterCommands);
    }

    public static MatterValueResult evaluateRecycler(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return MatterValueResult.rejected("empty");
        }
        if (level == null) {
            return MatterValueResult.rejected("missing_level");
        }
        if (isRecyclerBlacklisted(stack)) {
            return MatterValueResult.rejected("blacklisted");
        }
        if (isRecyclerContainerItem(stack)) {
            return MatterValueResult.rejected("container_like");
        }
        if (hasRecyclerBlockedData(stack)) {
            return MatterValueResult.rejected("unsupported_data");
        }

        int matterValue = getMatterValue(stack, level);
        return matterValue > 0 ? MatterValueResult.allowed(matterValue) : MatterValueResult.rejected("zero_value");
    }

    public static int getMatterValue(ItemStack stack, Level level) {
        if (stack.isEmpty() || level == null || isValueBlacklisted(stack) || isValueContainerItem(stack)) {
            return 0;
        }

        int matterMillibuckets = resolveBaseValue(stack.getItem(), level);
        if (matterMillibuckets <= 0) {
            return 0;
        }

        if (stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int remainingDurability = maxDamage - stack.getDamageValue();
            if (remainingDurability <= 0) {
                return 0;
            }

            double durabilityRatio = (double) remainingDurability / (double) maxDamage;
            matterMillibuckets = (int) Math.floor(matterMillibuckets * durabilityRatio);
        }

        return Math.max(matterMillibuckets, 0);
    }

    public static int getMatterDustCost(ItemStack stack, Level level) {
        int matterValue = getMatterValue(stack, level);
        return matterValue <= 0 ? 0 : (int) Math.ceil(matterValue / (double) MATTER_PER_DUST);
    }

    public static boolean canConstruct(ItemStack stack, Level level) {
        return !stack.isEmpty()
                && !isPatternEncodingBlocked(stack)
                && !isValueContainerItem(stack)
                && getMatterValue(stack, level) > 0;
    }

    public static boolean isPatternEncodingBlocked(ItemStack stack) {
        return isConfigured(stack, ruleConfig.patternBlacklist());
    }

    private static boolean isValueBlacklisted(ItemStack stack) {
        return isConfigured(stack, ruleConfig.valueBlacklist());
    }

    private static boolean isRecyclerBlacklisted(ItemStack stack) {
        return isValueBlacklisted(stack) || isConfigured(stack, ruleConfig.recyclerBlacklist());
    }

    private static boolean isValueContainerItem(ItemStack stack) {
        return ruleConfig.rejectContainerItems() && hasPatchedComponent(stack, DataComponents.CONTAINER);
    }

    private static boolean isRecyclerContainerItem(ItemStack stack) {
        return ruleConfig.rejectContainerItems() && (
                hasPatchedComponent(stack, DataComponents.CONTAINER)
                || stack.is(Items.BUCKET)
                || stack.is(Items.WATER_BUCKET)
                || stack.is(Items.LAVA_BUCKET)
                || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.POWDER_SNOW_BUCKET)
                || stack.is(Items.SALMON_BUCKET)
                || stack.is(Items.COD_BUCKET)
                || stack.is(Items.PUFFERFISH_BUCKET)
                || stack.is(Items.TROPICAL_FISH_BUCKET)
                || stack.is(Items.AXOLOTL_BUCKET)
                || stack.is(Items.TADPOLE_BUCKET));
    }

    private static boolean hasRecyclerBlockedData(ItemStack stack) {
        return (ruleConfig.rejectCustomName() && hasPatchedComponent(stack, DataComponents.CUSTOM_NAME))
                || (ruleConfig.rejectLore() && hasPatchedComponent(stack, DataComponents.LORE))
                || (ruleConfig.rejectWritableBooks() && hasPatchedComponent(stack, DataComponents.WRITABLE_BOOK_CONTENT))
                || (ruleConfig.rejectWrittenBooks() && hasPatchedComponent(stack, DataComponents.WRITTEN_BOOK_CONTENT));
    }

    private static boolean hasPatchedComponent(ItemStack stack, net.minecraft.core.component.DataComponentType<?> componentType) {
        return stack.getComponentsPatch().get(componentType) != null;
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        Path configDirectory = FMLPaths.CONFIGDIR.get().resolve(Matterworks.MOD_ID);
        MatterValueStorage.LoadedValues loadedValues;
        try {
            loadedValues = MatterValueStorage.load(configDirectory);
        } catch (IOException exception) {
            LOGGER.error("Failed to load matter value files from {}", configDirectory, exception);
            synchronized (LOCK) {
                storageDirectory = configDirectory;
                overrideValues.clear();
                generatedValues.clear();
                debugSubscribers.clear();
                recyclerDebugSubscribers.clear();
                ruleConfig = MatterRuleConfig.defaults();
                dirty = false;
            }
            return;
        }

        synchronized (LOCK) {
            storageDirectory = configDirectory;
            overrideValues.clear();
            overrideValues.putAll(loadedValues.overrides());
            generatedValues.clear();
            generatedValues.putAll(loadedValues.generated());
            ruleConfig = loadedValues.rules();
            dirty = false;
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        saveGeneratedValues();
        synchronized (LOCK) {
            debugSubscribers.clear();
            recyclerDebugSubscribers.clear();
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("matterworksdebug")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("values")
                                .then(Commands.literal("on").executes(context -> {
                                    if (context.getSource().getPlayer() instanceof ServerPlayer player) {
                                        synchronized (LOCK) {
                                            debugSubscribers.add(player.getUUID());
                                        }
                                        context.getSource().sendSuccess(() -> Component.literal("Matter value debug enabled."), false);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("off").executes(context -> {
                                    if (context.getSource().getPlayer() instanceof ServerPlayer player) {
                                        synchronized (LOCK) {
                                            debugSubscribers.remove(player.getUUID());
                                        }
                                        context.getSource().sendSuccess(() -> Component.literal("Matter value debug disabled."), false);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("dump").executes(context -> {
                                    dumpKnownValuesToPlayer(context.getSource().getPlayerOrException());
                                    context.getSource().sendSuccess(() -> Component.literal("Dumped known matter values to chat."), false);
                                    return 1;
                                })))
                        .then(Commands.literal("recycler")
                                .then(Commands.literal("on").executes(context -> {
                                    if (context.getSource().getPlayer() instanceof ServerPlayer player) {
                                        synchronized (LOCK) {
                                            recyclerDebugSubscribers.add(player.getUUID());
                                        }
                                        context.getSource().sendSuccess(() -> Component.literal("Recycler debug enabled."), false);
                                    }
                                    return 1;
                                }))
                                .then(Commands.literal("off").executes(context -> {
                                    if (context.getSource().getPlayer() instanceof ServerPlayer player) {
                                        synchronized (LOCK) {
                                            recyclerDebugSubscribers.remove(player.getUUID());
                                        }
                                        context.getSource().sendSuccess(() -> Component.literal("Recycler debug disabled."), false);
                                    }
                                    return 1;
                                })))
        );
    }

    private static int resolveBaseValue(Item item, Level level) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        synchronized (LOCK) {
            Integer overrideValue = overrideValues.get(itemId);
            if (overrideValue != null) {
                return overrideValue;
            }

            Integer generatedValue = generatedValues.get(itemId);
            if (generatedValue != null) {
                return generatedValue;
            }
        }

        int computedValue = Math.max(MATTER_PER_DUST, TemplateAnalysisManager.getRequiredItemCount(new ItemStack(item), level) * MATTER_PER_DUST);
        cacheGeneratedValue(itemId, computedValue, level);
        return computedValue;
    }

    private static void cacheGeneratedValue(ResourceLocation itemId, int value) {
        cacheGeneratedValue(itemId, value, null);
    }

    private static void cacheGeneratedValue(ResourceLocation itemId, int value, Level level) {
        Path saveDirectory;
        boolean added = false;
        synchronized (LOCK) {
            if (overrideValues.containsKey(itemId) || generatedValues.containsKey(itemId)) {
                return;
            }

            generatedValues.put(itemId, value);
            dirty = true;
            saveDirectory = storageDirectory;
            added = true;
        }

        if (added) {
            debugValueChange(level, "added", itemId, value);
        }

        if (saveDirectory != null) {
            saveGeneratedValues();
        }
    }

    private static void saveGeneratedValues() {
        Path saveDirectory;
        Map<ResourceLocation, Integer> snapshot;
        synchronized (LOCK) {
            if (!dirty || storageDirectory == null) {
                return;
            }

            saveDirectory = storageDirectory;
            snapshot = new LinkedHashMap<>(generatedValues);
            dirty = false;
        }

        try {
            MatterValueStorage.saveGenerated(saveDirectory, snapshot);
        } catch (IOException exception) {
            synchronized (LOCK) {
                dirty = true;
            }
            LOGGER.error("Failed to save generated matter values to {}", saveDirectory, exception);
        }
    }

    public static void debugRecyclerResult(Level level, ItemStack inputStack, int outputMb) {
        if (level == null || level.isClientSide() || inputStack.isEmpty()) {
            return;
        }
        debugRecyclerEvent(level, Component.literal("[Matter Recycler] " + BuiltInRegistries.ITEM.getKey(inputStack.getItem()) + " -> " + outputMb + " mB"));
    }

    public static void debugRecyclerRejected(Level level, ItemStack inputStack, String reason) {
        if (level == null || level.isClientSide() || inputStack.isEmpty()) {
            return;
        }
        debugRecyclerEvent(level, Component.literal("[Matter Recycler] rejected " + BuiltInRegistries.ITEM.getKey(inputStack.getItem()) + " [" + reason + "]"));
    }

    public static void debugConstructorCost(Level level, ItemStack resultStack, int matterDustCost, int matterMillibuckets) {
    }

    private static void debugValueChange(Level level, String changeType, ResourceLocation itemId, int matterValue) {
        if (level == null || level.isClientSide()) {
            return;
        }
        debugEvent(level, Component.literal("[Matter Values] " + changeType + " " + itemId + " = " + matterValue + " mB"));
    }

    private static void debugEvent(Level level, Component message) {
        if (level.getServer() == null) {
            return;
        }

        Set<UUID> recipients;
        synchronized (LOCK) {
            if (debugSubscribers.isEmpty()) {
                return;
            }
            recipients = Set.copyOf(debugSubscribers);
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (recipients.contains(player.getUUID())) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static void debugRecyclerEvent(Level level, Component message) {
        if (level.getServer() == null) {
            return;
        }

        Set<UUID> recipients;
        synchronized (LOCK) {
            if (recyclerDebugSubscribers.isEmpty()) {
                return;
            }
            recipients = Set.copyOf(recyclerDebugSubscribers);
        }

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (recipients.contains(player.getUUID())) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static void dumpKnownValuesToPlayer(ServerPlayer player) {
        Map<ResourceLocation, Integer> mergedValues = new LinkedHashMap<>();
        synchronized (LOCK) {
            mergedValues.putAll(generatedValues);
            mergedValues.putAll(overrideValues);
        }

        if (mergedValues.isEmpty()) {
            player.sendSystemMessage(Component.literal("[Matter Values] No values loaded yet."));
            return;
        }

        player.sendSystemMessage(Component.literal("[Matter Values] Known values:"));
        mergedValues.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> player.sendSystemMessage(Component.literal(" - " + entry.getKey() + " = " + entry.getValue() + " mB")));
    }

    private static boolean isConfigured(ItemStack stack, Set<ResourceLocation> configuredIds) {
        return configuredIds.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
