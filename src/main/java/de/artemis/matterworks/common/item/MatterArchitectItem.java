package de.artemis.matterworks.common.item;

import de.artemis.matterworks.client.render.MatterBatteryPreviewState;
import de.artemis.matterworks.common.menu.MatterArchitectMenu;
import de.artemis.matterworks.common.multiblock.MatterArchitectBlueprintType;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MatterArchitectItem extends Item {
    private static final String TAG_CORNER_A = "corner_a";
    private static final String TAG_CORNER_B = "corner_b";
    private static final String TAG_FRONT = "front";
    private static final String TAG_LAYER = "layer";
    private static final String TAG_LOCKED = "locked";
    private static final String TAG_BLUEPRINT_TYPE = "blueprint_type";

    public MatterArchitectItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos().immutable();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            clearSelection(stack);
            player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.cleared"), true);
            return InteractionResult.SUCCESS;
        }

        MatterArchitectBlueprintType blueprintType = getBlueprintType(stack);
        if (blueprintType == null) {
            openBlueprintMenu(player, context.getHand(), stack);
            return InteractionResult.SUCCESS;
        }

        if (!hasCornerA(stack)) {
            setCornerA(stack, clickedPos);
            clearCornerB(stack);
            setLocked(stack, false);
            player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.corner_a", blueprintType.displayName()), true);
            return InteractionResult.SUCCESS;
        }

        if (!hasCornerB(stack)) {
            return setSecondCorner(stack, clickedPos, player.getDirection(), player)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        return tryLockSelection(stack, player);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            clearSelection(stack);
            player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.cleared"), true);
            return InteractionResultHolder.success(stack);
        }

        if (getBlueprintType(stack) == null || !hasCornerA(stack)) {
            openBlueprintMenu(player, usedHand, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        if (hasCornerB(stack) && !isLocked(stack)) {
            InteractionResult lockResult = tryLockSelection(stack, player);
            return lockResult.consumesAction() ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        MatterArchitectBlueprintType blueprintType = getBlueprintType(stack);
        if (blueprintType == null) {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.no_blueprint").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.open_menu").withStyle(ChatFormatting.AQUA));
            return;
        }

        tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.blueprint", blueprintType.displayName()).withStyle(ChatFormatting.AQUA));
        if (!hasCornerA(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.set_first_corner").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.change_blueprint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        BlockPos cornerA = getCornerA(stack);
        BlockPos cornerB = getCornerB(stack);
        tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.corner_a", formatPos(cornerA)).withStyle(ChatFormatting.GRAY));
        if (cornerB != null) {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.corner_b", formatPos(cornerB)).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.size", getWidth(stack), getHeight(stack), getDepth(stack)).withStyle(ChatFormatting.DARK_GRAY));
            if (isLocked(stack)) {
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.locked").withStyle(ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.layer_scroll").withStyle(ChatFormatting.DARK_GRAY));
            } else if (hasValidBatterySelection(stack)) {
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.lock_preview").withStyle(ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.move_scroll").withStyle(ChatFormatting.DARK_GRAY));
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.resize_scroll").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.invalid_size").withStyle(ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.move_scroll").withStyle(ChatFormatting.DARK_GRAY));
                tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.resize_scroll").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.second_corner_preview").withStyle(ChatFormatting.AQUA));
            tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.set_air_corner").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.matterworks.matter_architect.clear").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static boolean hasValidBatterySelection(ItemStack stack) {
        if (getBlueprintType(stack) != MatterArchitectBlueprintType.MATTER_BATTERY || !hasCornerA(stack) || !hasCornerB(stack)) {
            return false;
        }
        return MatterBatteryMultiblockLayout.isValidSize(getWidth(stack), getHeight(stack), getDepth(stack));
    }

    public static boolean hasCornerA(ItemStack stack) {
        return getCustomDataTag(stack).contains(TAG_CORNER_A);
    }

    public static boolean hasCornerB(ItemStack stack) {
        return getCustomDataTag(stack).contains(TAG_CORNER_B);
    }

    public static boolean isLocked(ItemStack stack) {
        return getCustomDataTag(stack).getBoolean(TAG_LOCKED);
    }

    public static boolean setSecondCorner(ItemStack stack, BlockPos pos, Direction front, @Nullable Player player) {
        MatterArchitectBlueprintType blueprintType = getBlueprintType(stack);
        if (blueprintType != MatterArchitectBlueprintType.MATTER_BATTERY || !hasCornerA(stack) || hasCornerB(stack)) {
            return false;
        }

        setCornerB(stack, pos.immutable());
        setFront(stack, front);
        setLocked(stack, false);
        if (player != null) {
            if (hasValidBatterySelection(stack)) {
                player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.ready", blueprintType.displayName(), getWidth(stack), getHeight(stack), getDepth(stack)), true);
            } else {
                player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.invalid_size"), true);
            }
        }
        return true;
    }

    public static BlockPos getMinCorner(ItemStack stack) {
        BlockPos a = getCornerA(stack);
        BlockPos b = getCornerB(stack);
        if (a == null || b == null) {
            return BlockPos.ZERO;
        }
        return new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );
    }

    public static BlockPos getMaxCorner(ItemStack stack) {
        BlockPos a = getCornerA(stack);
        BlockPos b = getCornerB(stack);
        if (a == null || b == null) {
            return BlockPos.ZERO;
        }
        return new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );
    }

    public static Direction getFront(ItemStack stack) {
        Direction front = Direction.byName(getCustomDataTag(stack).getString(TAG_FRONT));
        return front != null && front.getAxis().isHorizontal() ? front : Direction.NORTH;
    }

    public static @Nullable MatterArchitectBlueprintType getBlueprintType(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        if (tag.contains(TAG_BLUEPRINT_TYPE)) {
            return MatterArchitectBlueprintType.bySerializedName(tag.getString(TAG_BLUEPRINT_TYPE));
        }
        return tag.contains(TAG_CORNER_A) ? MatterArchitectBlueprintType.MATTER_BATTERY : null;
    }

    public static void setBlueprintType(ItemStack stack, MatterArchitectBlueprintType blueprintType) {
        clearSelection(stack);
        CompoundTag tag = getCustomDataTag(stack);
        tag.putString(TAG_BLUEPRINT_TYPE, blueprintType.serializedName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getSelectedLayer(ItemStack stack) {
        return Math.max(0, Math.min(MatterBatteryMultiblockLayout.MAX_SIZE - 1, getCustomDataTag(stack).getInt(TAG_LAYER)));
    }

    public static void syncPreviewState(ItemStack stack, @Nullable BlockPos hoveredCorner, Direction lookDirection) {
        if (getBlueprintType(stack) != MatterArchitectBlueprintType.MATTER_BATTERY || !hasCornerA(stack)) {
            if (MatterBatteryPreviewState.isToolDriven()) {
                MatterBatteryPreviewState.clear();
            }
            return;
        }

        BlockPos cornerA = getCornerA(stack);
        BlockPos cornerB = hasCornerB(stack) ? getCornerB(stack) : hoveredCorner;
        if (cornerA == null || cornerB == null) {
            if (MatterBatteryPreviewState.isToolDriven()) {
                MatterBatteryPreviewState.clear();
            }
            return;
        }

        BlockPos minCorner = minCorner(cornerA, cornerB);
        BlockPos maxCorner = maxCorner(cornerA, cornerB);
        Direction front = hasCornerB(stack) ? getFront(stack) : getHorizontal(lookDirection);
        int layer = MatterBatteryPreviewState.isToolDriven()
                ? MatterBatteryPreviewState.getSelectedLayer()
                : getSelectedLayer(stack);
        MatterBatteryPreviewState.enable(
                minCorner,
                maxCorner,
                front,
                isLocked(stack),
                layer,
                true
        );
    }

    public static void shiftSelection(ItemStack stack, int dx, int dy, int dz) {
        if (getBlueprintType(stack) != MatterArchitectBlueprintType.MATTER_BATTERY || !hasCornerA(stack) || !hasCornerB(stack)) {
            return;
        }
        BlockPos offset = new BlockPos(dx, dy, dz);
        setCornerA(stack, getCornerA(stack).offset(offset));
        setCornerB(stack, getCornerB(stack).offset(offset));
    }

    public static boolean resizeSelection(ItemStack stack, Direction direction, int amount) {
        if (getBlueprintType(stack) != MatterArchitectBlueprintType.MATTER_BATTERY || !hasCornerA(stack) || !hasCornerB(stack) || amount == 0) {
            return false;
        }

        BlockPos min = getMinCorner(stack);
        BlockPos max = getMaxCorner(stack);
        int step = Integer.signum(amount);
        boolean changed = false;
        for (int index = 0; index < Math.abs(amount); index++) {
            ResizeBounds next = resizeOne(min, max, direction, step);
            if (next == null) {
                break;
            }
            min = next.min();
            max = next.max();
            changed = true;
        }

        if (!changed) {
            return false;
        }

        setCornerA(stack, min);
        setCornerB(stack, max);
        return true;
    }

    public static boolean matchesLockedSelection(ItemStack stack, BlockPos origin, Direction front, int width, int height, int depth) {
        if (getBlueprintType(stack) != MatterArchitectBlueprintType.MATTER_BATTERY || !hasCornerA(stack) || !hasCornerB(stack) || !isLocked(stack)) {
            return false;
        }
        BlockPos min = getMinCorner(stack);
        BlockPos max = getMaxCorner(stack);
        Direction stackFront = getFront(stack);
        return stackFront == getHorizontal(front)
                && MatterBatteryMultiblockLayout.getOrigin(min, max, stackFront).equals(origin)
                && getWidth(stack) == width
                && getHeight(stack) == height
                && getDepth(stack) == depth;
    }

    private static BlockPos getCornerA(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_CORNER_A) ? BlockPos.of(tag.getLong(TAG_CORNER_A)) : null;
    }

    private static BlockPos getCornerB(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.contains(TAG_CORNER_B) ? BlockPos.of(tag.getLong(TAG_CORNER_B)) : null;
    }

    private static void setCornerA(ItemStack stack, BlockPos pos) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putLong(TAG_CORNER_A, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void setCornerB(ItemStack stack, BlockPos pos) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putLong(TAG_CORNER_B, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void clearCornerB(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.remove(TAG_CORNER_B);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void clearSelection(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.remove(TAG_CORNER_A);
        tag.remove(TAG_CORNER_B);
        tag.remove(TAG_FRONT);
        tag.remove(TAG_LAYER);
        tag.remove(TAG_LOCKED);
        tag.remove(TAG_BLUEPRINT_TYPE);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void setFront(ItemStack stack, Direction front) {
        CompoundTag tag = getCustomDataTag(stack);
        Direction horizontal = getHorizontal(front);
        tag.putString(TAG_FRONT, horizontal.getName());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void setSelectedLayer(ItemStack stack, int layer) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putInt(TAG_LAYER, Math.max(0, Math.min(MatterBatteryMultiblockLayout.MAX_SIZE - 1, layer)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void setLocked(ItemStack stack, boolean locked) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putBoolean(TAG_LOCKED, locked);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static int getWidth(ItemStack stack) {
        BlockPos min = getMinCorner(stack);
        BlockPos max = getMaxCorner(stack);
        Direction front = hasCornerB(stack) ? getFront(stack) : Direction.NORTH;
        return MatterBatteryMultiblockLayout.getWidth(min, max, front);
    }

    private static int getHeight(ItemStack stack) {
        BlockPos min = getMinCorner(stack);
        BlockPos max = getMaxCorner(stack);
        return max.getY() - min.getY() + 1;
    }

    private static int getDepth(ItemStack stack) {
        BlockPos min = getMinCorner(stack);
        BlockPos max = getMaxCorner(stack);
        Direction front = hasCornerB(stack) ? getFront(stack) : Direction.NORTH;
        return MatterBatteryMultiblockLayout.getDepth(min, max, front);
    }

    private static Direction getHorizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }

    private static BlockPos minCorner(BlockPos a, BlockPos b) {
        return new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );
    }

    private static BlockPos maxCorner(BlockPos a, BlockPos b) {
        return new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );
    }

    private static String formatPos(BlockPos pos) {
        if (pos == null) {
            return "-";
        }
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static InteractionResult tryLockSelection(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (!hasValidBatterySelection(stack)) {
            player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.invalid_size"), true);
            return InteractionResult.FAIL;
        }
        setLocked(stack, true);
        player.displayClientMessage(Component.translatable("message.matterworks.matter_architect.locked"), true);
        return InteractionResult.SUCCESS;
    }

    private static void openBlueprintMenu(Player player, InteractionHand hand, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new MatterArchitectMenu(containerId, inventory, hand),
                stack.getHoverName()
        );
        serverPlayer.openMenu(provider, buffer -> buffer.writeVarInt(hand.ordinal()));
    }

    private static @Nullable ResizeBounds resizeOne(BlockPos min, BlockPos max, Direction direction, int step) {
        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();
        int maxY = max.getY();
        int maxZ = max.getZ();

        switch (direction) {
            case EAST -> maxX += step;
            case WEST -> minX -= step;
            case UP -> maxY += step;
            case DOWN -> minY -= step;
            case SOUTH -> maxZ += step;
            case NORTH -> minZ -= step;
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        if (!MatterBatteryMultiblockLayout.isValidSize(width, height, depth)) {
            return null;
        }

        return new ResizeBounds(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private record ResizeBounds(BlockPos min, BlockPos max) {
    }
}
