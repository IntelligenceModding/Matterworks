package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.io.SideAccessMode;
import de.artemis.matterworks.common.io.SideConfigType;
import de.artemis.matterworks.common.io.SideConfigurableBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Locale;

public class NetworkDataCardItem extends Item {
    private static final String TAG_BLOCK_ID = "stored_block_id";
    private static final String TAG_BLOCK_NAME = "stored_block_name";
    private static final String TAG_CONFIG = "stored_config";
    private static final String TAG_SIDE_CONFIG = "side_config";
    private static final String TAG_PYLON_CONFIG = "pylon_config";
    private static final String TAG_CHANNELS = "channels";
    private static final String TAG_COLORS = "colors";
    private static final String TAG_FILTER_INVENTORY = "filter_inventory";
    private static final String TAG_FILTER_CARD_DATA = "filter_card_data";
    private static final String TAG_FILTER_CARD_FLUID = "filter_card_fluid";

    public NetworkDataCardItem(Properties properties) {
        super(properties);
    }

    public static boolean isHeldBy(Player player) {
        return player != null
                && (player.getMainHandItem().getItem() instanceof NetworkDataCardItem
                || player.getOffhandItem().getItem() instanceof NetworkDataCardItem);
    }

    public static ItemStack getStoredBlockIcon(ItemStack stack) {
        String blockId = getStoredBlockId(stack);
        if (blockId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation location = ResourceLocation.tryParse(blockId);
        if (location == null || !BuiltInRegistries.BLOCK.containsKey(location)) {
            return ItemStack.EMPTY;
        }
        ItemStack icon = BuiltInRegistries.BLOCK.get(location).asItem().getDefaultInstance();
        return icon.isEmpty() ? ItemStack.EMPTY : icon;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }
        if (!isSupportedTarget(blockEntity)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        String targetBlockId = BuiltInRegistries.BLOCK.getKey(context.getLevel().getBlockState(context.getClickedPos()).getBlock()).toString();

        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            copyConfiguration(stack, blockEntity, targetBlockId, context.getLevel().registryAccess());
            playFeedbackSound(context, 0.95F);
            context.getPlayer().displayClientMessage(Component.translatable("message.matterworks.network_data_card.copied", getStoredBlockName(stack)), true);
            return InteractionResult.SUCCESS;
        }

        if (!hasStoredConfiguration(stack)) {
            copyConfiguration(stack, blockEntity, targetBlockId, context.getLevel().registryAccess());
            playFeedbackSound(context, 0.95F);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.matterworks.network_data_card.copied", getStoredBlockName(stack)), true);
            }
            return InteractionResult.SUCCESS;
        }

        String storedBlockId = getStoredBlockId(stack);
        if (!targetBlockId.equals(storedBlockId)) {
            playRejectSound(context);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable("message.matterworks.network_data_card.mismatch", getStoredBlockName(stack)), true);
            }
            return InteractionResult.SUCCESS;
        }

        applyConfiguration(stack, blockEntity, context.getLevel().registryAccess());
        playFeedbackSound(context, 1.2F);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable("message.matterworks.network_data_card.applied", getStoredBlockName(stack)), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasStoredConfiguration(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (!hasStoredConfiguration(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_data_card.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_data_card.stored", getStoredBlockName(stack)).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_data_card.apply_hint").withStyle(ChatFormatting.DARK_GRAY));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_data_card.overwrite_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static boolean isSupportedTarget(BlockEntity blockEntity) {
        return blockEntity instanceof MatterPylonBlockEntity || blockEntity instanceof SideConfigurableBlockEntity;
    }

    private static boolean hasStoredConfiguration(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_BLOCK_ID) && tag.contains(TAG_CONFIG);
    }

    private static String getStoredBlockId(ItemStack stack) {
        return getCustomDataTag(stack).getString(TAG_BLOCK_ID);
    }

    private static Component getStoredBlockName(ItemStack stack) {
        String storedName = getCustomDataTag(stack).getString(TAG_BLOCK_NAME);
        return storedName.isEmpty() ? Component.translatable("tooltip.matterworks.network_data_card.unknown") : Component.literal(storedName);
    }

    private static void copyConfiguration(ItemStack stack, BlockEntity blockEntity, String blockId, HolderLookup.Provider registries) {
        CompoundTag stackTag = getCustomDataTag(stack);
        stackTag.putString(TAG_BLOCK_ID, blockId);
        stackTag.putString(TAG_BLOCK_NAME, blockEntity.getBlockState().getBlock().getName().getString());
        stackTag.put(TAG_CONFIG, buildConfigTag(blockEntity, registries));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(stackTag));
    }

    private static void applyConfiguration(ItemStack stack, BlockEntity blockEntity, HolderLookup.Provider registries) {
        CompoundTag stackTag = getCustomDataTag(stack);
        if (!stackTag.contains(TAG_CONFIG)) {
            return;
        }

        CompoundTag configTag = stackTag.getCompound(TAG_CONFIG);
        if (blockEntity instanceof SideConfigurableBlockEntity sideConfigurable && configTag.contains(TAG_SIDE_CONFIG)) {
            applySideConfig(sideConfigurable, configTag.getCompound(TAG_SIDE_CONFIG));
        }
        if (blockEntity instanceof MatterPylonBlockEntity pylonBlockEntity && configTag.contains(TAG_PYLON_CONFIG)) {
            applyPylonConfig(pylonBlockEntity, configTag.getCompound(TAG_PYLON_CONFIG), registries);
        }
    }

    private static CompoundTag buildConfigTag(BlockEntity blockEntity, HolderLookup.Provider registries) {
        CompoundTag configTag = new CompoundTag();
        if (blockEntity instanceof SideConfigurableBlockEntity sideConfigurable) {
            configTag.put(TAG_SIDE_CONFIG, buildSideConfigTag(sideConfigurable));
        }
        if (blockEntity instanceof MatterPylonBlockEntity pylonBlockEntity) {
            configTag.put(TAG_PYLON_CONFIG, buildPylonConfigTag(pylonBlockEntity, registries));
        }
        return configTag;
    }

    private static CompoundTag buildSideConfigTag(SideConfigurableBlockEntity sideConfigurable) {
        CompoundTag sideConfigTag = new CompoundTag();
        for (SideConfigType type : SideConfigType.values()) {
            if (!sideConfigurable.supportsSideConfigType(type)) {
                continue;
            }
            CompoundTag typeTag = new CompoundTag();
            for (Direction side : Direction.values()) {
                typeTag.putString(side.getName(), sideConfigurable.getSideAccessMode(type, side).name().toLowerCase(Locale.ROOT));
            }
            sideConfigTag.put(type.name().toLowerCase(Locale.ROOT), typeTag);
        }
        return sideConfigTag;
    }

    private static void applySideConfig(SideConfigurableBlockEntity sideConfigurable, CompoundTag sideConfigTag) {
        for (SideConfigType type : SideConfigType.values()) {
            String typeKey = type.name().toLowerCase(Locale.ROOT);
            if (!sideConfigTag.contains(typeKey) || !sideConfigurable.supportsSideConfigType(type)) {
                continue;
            }
            CompoundTag typeTag = sideConfigTag.getCompound(typeKey);
            for (Direction side : Direction.values()) {
                if (!typeTag.contains(side.getName())) {
                    continue;
                }
                sideConfigurable.setSideAccessMode(type, side, parseSideAccessMode(typeTag.getString(side.getName())));
            }
        }
    }

    private static CompoundTag buildPylonConfigTag(MatterPylonBlockEntity pylonBlockEntity, HolderLookup.Provider registries) {
        CompoundTag pylonConfigTag = new CompoundTag();

        CompoundTag channelsTag = new CompoundTag();
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            CompoundTag channelTag = new CompoundTag();
            channelTag.putString("mode", pylonBlockEntity.getMode(channel).name());
            channelTag.putInt("id", pylonBlockEntity.getPylonId(channel));
            channelsTag.put(Integer.toString(channel), channelTag);
        }
        pylonConfigTag.put(TAG_CHANNELS, channelsTag);

        CompoundTag colorsTag = new CompoundTag();
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            CompoundTag channelColorsTag = new CompoundTag();
            for (int index = 0; index < MatterPylonBlockEntity.NETWORK_COLOR_CODE_PARTS; index++) {
                channelColorsTag.putInt(Integer.toString(index), pylonBlockEntity.getNetworkColor(channel, index).getId());
            }
            colorsTag.put(Integer.toString(channel), channelColorsTag);
        }
        pylonConfigTag.put(TAG_COLORS, colorsTag);

        if (pylonBlockEntity.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_ITEMS)
                || pylonBlockEntity.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_FLUIDS)) {
            pylonConfigTag.put(TAG_FILTER_INVENTORY, buildFilterConfigTag(pylonBlockEntity));
        }

        return pylonConfigTag;
    }

    private static void applyPylonConfig(MatterPylonBlockEntity pylonBlockEntity, CompoundTag pylonConfigTag, HolderLookup.Provider registries) {
        CompoundTag channelsTag = pylonConfigTag.getCompound(TAG_CHANNELS);
        for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
            String channelKey = Integer.toString(channel);
            if (!channelsTag.contains(channelKey)) {
                continue;
            }
            CompoundTag channelTag = channelsTag.getCompound(channelKey);
            pylonBlockEntity.setMode(channel, parsePylonMode(channelTag.getString("mode")));
            pylonBlockEntity.setPylonId(channel, channelTag.getInt("id"));
        }

        CompoundTag colorsTag = pylonConfigTag.getCompound(TAG_COLORS);
        if (containsLegacyColorConfig(colorsTag)) {
            for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
                for (int index = 0; index < MatterPylonBlockEntity.NETWORK_COLOR_CODE_PARTS; index++) {
                    String colorKey = Integer.toString(index);
                    if (colorsTag.contains(colorKey)) {
                        pylonBlockEntity.setNetworkColor(channel, index, net.minecraft.world.item.DyeColor.byId(colorsTag.getInt(colorKey)));
                    }
                }
            }
        } else {
            for (int channel = 0; channel < MatterPylonBlockEntity.CHANNEL_COUNT; channel++) {
                String channelKey = Integer.toString(channel);
                if (!colorsTag.contains(channelKey, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    continue;
                }
                CompoundTag channelColorsTag = colorsTag.getCompound(channelKey);
                for (int index = 0; index < MatterPylonBlockEntity.NETWORK_COLOR_CODE_PARTS; index++) {
                    String colorKey = Integer.toString(index);
                    if (channelColorsTag.contains(colorKey)) {
                        pylonBlockEntity.setNetworkColor(channel, index, net.minecraft.world.item.DyeColor.byId(channelColorsTag.getInt(colorKey)));
                    }
                }
            }
        }

        if (pylonConfigTag.contains(TAG_FILTER_INVENTORY)
                && (pylonBlockEntity.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_ITEMS)
                || pylonBlockEntity.supportsFilterChannel(MatterPylonBlockEntity.CHANNEL_FLUIDS))) {
            applyFilterConfig(pylonBlockEntity, pylonConfigTag.getCompound(TAG_FILTER_INVENTORY), registries);
            pylonBlockEntity.setChanged();
            if (pylonBlockEntity.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(pylonBlockEntity.getBlockPos(), pylonBlockEntity.getBlockState(), pylonBlockEntity.getBlockState(), 3);
            }
        }
    }

    private static SideAccessMode parseSideAccessMode(String serializedMode) {
        return switch (serializedMode) {
            case "input" -> SideAccessMode.INPUT;
            case "output" -> SideAccessMode.OUTPUT;
            case "both" -> SideAccessMode.BOTH;
            default -> SideAccessMode.DISABLED;
        };
    }

    private static de.artemis.matterworks.common.transport.PylonMode parsePylonMode(String serializedMode) {
        return switch (serializedMode) {
            case "EXPORT" -> de.artemis.matterworks.common.transport.PylonMode.EXPORT;
            case "IMPORT" -> de.artemis.matterworks.common.transport.PylonMode.IMPORT;
            case "IMPORT_EXPORT" -> de.artemis.matterworks.common.transport.PylonMode.IMPORT_EXPORT;
            default -> de.artemis.matterworks.common.transport.PylonMode.DISABLED;
        };
    }

    private static boolean containsLegacyColorConfig(CompoundTag colorsTag) {
        for (int index = 0; index < MatterPylonBlockEntity.NETWORK_COLOR_CODE_PARTS; index++) {
            if (colorsTag.contains(Integer.toString(index))) {
                return true;
            }
        }
        return false;
    }

    private static CompoundTag buildFilterConfigTag(MatterPylonBlockEntity pylonBlockEntity) {
        CompoundTag filterConfigTag = new CompoundTag();
        for (int slot = 0; slot < MatterPylonBlockEntity.FILTER_SLOT_COUNT; slot++) {
            ItemStack filterStack = pylonBlockEntity.getFilterHandler().getStackInSlot(slot);
            if (filterStack.isEmpty()) {
                continue;
            }

            CompoundTag slotTag = new CompoundTag();
            slotTag.putBoolean(TAG_FILTER_CARD_FLUID, filterStack.getItem() == de.artemis.matterworks.common.registry.ModItems.MATTER_FLUID_FILTER.get());
            CompoundTag cardData = filterStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!cardData.isEmpty()) {
                slotTag.put(TAG_FILTER_CARD_DATA, cardData);
            }
            filterConfigTag.put(Integer.toString(slot), slotTag);
        }
        return filterConfigTag;
    }

    private static void applyFilterConfig(MatterPylonBlockEntity pylonBlockEntity, CompoundTag filterConfigTag, HolderLookup.Provider registries) {
        if (looksLikeLegacyFilterInventory(filterConfigTag)) {
            pylonBlockEntity.getFilterHandler().deserializeNBT(registries, filterConfigTag);
            return;
        }

        for (int slot = 0; slot < MatterPylonBlockEntity.FILTER_SLOT_COUNT; slot++) {
            String slotKey = Integer.toString(slot);
            if (!filterConfigTag.contains(slotKey, Tag.TAG_COMPOUND)) {
                pylonBlockEntity.getFilterHandler().setStackInSlot(slot, ItemStack.EMPTY);
                continue;
            }

            CompoundTag slotTag = filterConfigTag.getCompound(slotKey);
            boolean fluidCard = slotTag.getBoolean(TAG_FILTER_CARD_FLUID);
            ItemStack filterStack = new ItemStack(fluidCard
                    ? de.artemis.matterworks.common.registry.ModItems.MATTER_FLUID_FILTER.get()
                    : de.artemis.matterworks.common.registry.ModItems.MATTER_ITEM_FILTER.get());
            if (slotTag.contains(TAG_FILTER_CARD_DATA, Tag.TAG_COMPOUND)) {
                CustomData.set(DataComponents.CUSTOM_DATA, filterStack, slotTag.getCompound(TAG_FILTER_CARD_DATA));
            }
            pylonBlockEntity.getFilterHandler().setStackInSlot(slot, filterStack);
        }
    }

    private static boolean looksLikeLegacyFilterInventory(CompoundTag filterConfigTag) {
        return filterConfigTag.contains("Items", Tag.TAG_LIST) || filterConfigTag.contains("Size", Tag.TAG_INT);
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static void playFeedbackSound(UseOnContext context, float pitch) {
        context.getLevel().playSound(
                null,
                context.getClickedPos(),
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.PLAYERS,
                0.35F,
                pitch
        );
    }

    private static void playRejectSound(UseOnContext context) {
        context.getLevel().playSound(
                null,
                context.getClickedPos(),
                SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS,
                0.35F,
                1.1F
        );
    }
}
