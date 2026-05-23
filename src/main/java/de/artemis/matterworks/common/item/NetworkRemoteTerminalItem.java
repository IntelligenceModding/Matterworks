package de.artemis.matterworks.common.item;

import de.artemis.matterworks.common.blockentity.MatterNetworkControllerBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterNetworkMonitorBlockEntity;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class NetworkRemoteTerminalItem extends Item {
    private static final String TAG_LINKED_POS = "linked_pos";
    private static final String TAG_LINKED_DIMENSION = "linked_dimension";
    private static final String TAG_LINKED_NAME = "linked_name";
    private static final String TAG_LINKED_TYPE = "linked_type";
    private static final String TAG_LINKED_BLOCK_ID = "linked_block_id";

    public NetworkRemoteTerminalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack getLinkedBlockIcon(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        if (tag.contains(TAG_LINKED_BLOCK_ID)) {
            ResourceLocation location = ResourceLocation.tryParse(tag.getString(TAG_LINKED_BLOCK_ID));
            if (location != null && BuiltInRegistries.BLOCK.containsKey(location)) {
                ItemStack icon = BuiltInRegistries.BLOCK.get(location).asItem().getDefaultInstance();
                if (!icon.isEmpty()) {
                    return icon;
                }
            }
        }
        LinkedTargetType targetType = getLinkedTargetType(stack);
        if (targetType == LinkedTargetType.CONTROLLER) {
            return ModBlocks.MATTER_NETWORK_CONTROLLER.get().asItem().getDefaultInstance();
        }
        if (targetType == LinkedTargetType.MONITOR) {
            return ModBlocks.MATTER_NETWORK_MONITOR.get().asItem().getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        LinkedTargetType targetType = LinkedTargetType.fromBlockEntity(context.getLevel().getBlockEntity(context.getClickedPos()));
        if (targetType == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        setLinkedTarget(
                stack,
                context.getClickedPos(),
                context.getLevel().dimension().location(),
                BuiltInRegistries.BLOCK.getKey(context.getLevel().getBlockState(context.getClickedPos()).getBlock()),
                targetType.getDisplayName(context.getLevel().getBlockEntity(context.getClickedPos())),
                targetType
        );
        playLinkSound(context);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(
                    Component.translatable(
                            "message.matterworks.network_remote_terminal.linked",
                            targetType.displayName(),
                            getStoredTargetName(stack)
                    ),
                    true
            );
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.pick(8.0D, 0.0F, false).getType() == HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (player.isShiftKeyDown()) {
            clearLinkedTarget(stack);
            player.displayClientMessage(Component.translatable("message.matterworks.network_remote_terminal.cleared"), true);
            return InteractionResultHolder.success(stack);
        }

        LinkedTargetType targetType = getLinkedTargetType(stack);
        BlockPos linkedPos = getLinkedPos(stack);
        ResourceLocation linkedDimension = getLinkedDimensionId(stack);
        if (targetType == null || linkedPos == null || linkedDimension == null) {
            playRejectSound(level, player.blockPosition());
            player.displayClientMessage(Component.translatable("message.matterworks.network_remote_terminal.unlinked"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!level.dimension().location().equals(linkedDimension)) {
            playRejectSound(level, player.blockPosition());
            player.displayClientMessage(
                    Component.translatable("message.matterworks.network_remote_terminal.wrong_dimension", getStoredTargetName(stack)),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        BlockEntity blockEntity = level.getBlockEntity(linkedPos);
        if (targetType == LinkedTargetType.CONTROLLER && blockEntity instanceof MatterNetworkControllerBlockEntity controller) {
            if (player instanceof ServerPlayer serverPlayer) {
                controller.openRemoteMenu(serverPlayer);
            }
            return InteractionResultHolder.success(stack);
        }
        if (targetType == LinkedTargetType.MONITOR && blockEntity instanceof MatterNetworkMonitorBlockEntity monitor) {
            if (player instanceof ServerPlayer serverPlayer) {
                monitor.openRemoteMenu(serverPlayer);
            }
            return InteractionResultHolder.success(stack);
        }

        player.displayClientMessage(
                Component.translatable("message.matterworks.network_remote_terminal.missing_target", getStoredTargetName(stack)),
                true
        );
        playRejectSound(level, player.blockPosition());
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasLinkedTarget(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        LinkedTargetType targetType = getLinkedTargetType(stack);
        if (targetType == null || !hasLinkedTarget(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_remote_terminal.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(
                    Component.translatable(
                            "tooltip.matterworks.network_remote_terminal.linked",
                            targetType.displayName(),
                            getStoredTargetName(stack)
                    ).withStyle(ChatFormatting.GRAY)
            );
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_remote_terminal.open_hint").withStyle(ChatFormatting.DARK_GRAY));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.network_remote_terminal.clear_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void setLinkedTarget(ItemStack stack, BlockPos pos, ResourceLocation dimensionId, ResourceLocation blockId, Component targetName, LinkedTargetType targetType) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putLong(TAG_LINKED_POS, pos.asLong());
        tag.putString(TAG_LINKED_DIMENSION, dimensionId.toString());
        tag.putString(TAG_LINKED_BLOCK_ID, blockId.toString());
        tag.putString(TAG_LINKED_NAME, targetName.getString());
        tag.putString(TAG_LINKED_TYPE, targetType.id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearLinkedTarget(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.remove(TAG_LINKED_POS);
        tag.remove(TAG_LINKED_DIMENSION);
        tag.remove(TAG_LINKED_BLOCK_ID);
        tag.remove(TAG_LINKED_NAME);
        tag.remove(TAG_LINKED_TYPE);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean hasLinkedTarget(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_LINKED_POS) && tag.contains(TAG_LINKED_DIMENSION) && tag.contains(TAG_LINKED_TYPE);
    }

    private static BlockPos getLinkedPos(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_LINKED_POS) ? BlockPos.of(tag.getLong(TAG_LINKED_POS)) : null;
    }

    private static ResourceLocation getLinkedDimensionId(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_LINKED_DIMENSION) ? ResourceLocation.tryParse(tag.getString(TAG_LINKED_DIMENSION)) : null;
    }

    private static LinkedTargetType getLinkedTargetType(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_LINKED_TYPE) ? LinkedTargetType.byId(tag.getString(TAG_LINKED_TYPE)) : null;
    }

    private static Component getStoredTargetName(ItemStack stack) {
        String storedName = getCustomDataTag(stack).getString(TAG_LINKED_NAME);
        return storedName.isEmpty()
                ? Component.translatable("tooltip.matterworks.network_remote_terminal.unknown")
                : Component.literal(storedName);
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static void playLinkSound(UseOnContext context) {
        context.getLevel().playSound(
                null,
                context.getClickedPos(),
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.PLAYERS,
                0.35F,
                1.1F
        );
    }

    private static void playRejectSound(Level level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS,
                0.35F,
                1.1F
        );
    }

    private enum LinkedTargetType {
        CONTROLLER("controller", "item.matterworks.network_remote_terminal.target.controller"),
        MONITOR("monitor", "item.matterworks.network_remote_terminal.target.monitor");

        private final String id;
        private final String translationKey;

        LinkedTargetType(String id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        private Component displayName() {
            return Component.translatable(translationKey);
        }

        private Component getDisplayName(BlockEntity blockEntity) {
            return blockEntity instanceof MatterNetworkControllerBlockEntity controller
                    ? controller.getDisplayName()
                    : blockEntity instanceof MatterNetworkMonitorBlockEntity monitor
                    ? monitor.getDisplayName()
                    : Component.translatable("tooltip.matterworks.network_remote_terminal.unknown");
        }

        private static LinkedTargetType fromBlockEntity(BlockEntity blockEntity) {
            if (blockEntity instanceof MatterNetworkControllerBlockEntity) {
                return CONTROLLER;
            }
            if (blockEntity instanceof MatterNetworkMonitorBlockEntity) {
                return MONITOR;
            }
            return null;
        }

        private static LinkedTargetType byId(String id) {
            for (LinkedTargetType value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return null;
        }
    }
}
