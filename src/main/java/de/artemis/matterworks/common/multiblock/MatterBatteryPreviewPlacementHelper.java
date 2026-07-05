package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MatterBatteryPreviewPlacementHelper {
    private MatterBatteryPreviewPlacementHelper() {
    }

    public static boolean placeFromInventory(Player player, BlockPos origin, Direction front, int width, int height, int depth, BlockPos targetPos, InteractionHand hand) {
        BlockPos localPos = MultiblockTransforms.worldToLocal(origin, front, targetPos);
        if (!MultiblockTransforms.localToWorld(origin, front, localPos).equals(targetPos)) {
            return false;
        }

        if (localPos.getX() < 0 || localPos.getX() >= width
                || localPos.getY() < 0 || localPos.getY() >= height
                || localPos.getZ() < 0 || localPos.getZ() >= depth) {
            return false;
        }
        MultiblockRole role = MatterBatteryMultiblockLayout.getRole(localPos, width, height, depth);

        BlockState currentState = player.level().getBlockState(targetPos);
        if (matchesRequirement(role, currentState) || (!currentState.isAir() && !currentState.canBeReplaced())) {
            return false;
        }

        InventoryBlockSelection selection = selectPlacement(player, hand, role);
        if (selection == null) {
            return false;
        }

        BlockState placedState = createPlacementState(selection.block(), front);
        if (!player.level().setBlock(targetPos, placedState, 3)) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            selection.stack().shrink(1);
            player.getInventory().setChanged();
        }

        SoundType soundType = placedState.getSoundType();
        player.level().playSound(null, targetPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        player.level().gameEvent(player, GameEvent.BLOCK_PLACE, targetPos);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            MatterBatteryMultiblockHelper.tryAssembleAtOrigin(serverLevel, origin, front, width, height, depth, null, false);
        }
        return true;
    }

    public static boolean canPlaceFromInventory(Player player, InteractionHand hand, MultiblockRole role) {
        return selectPlacement(player, hand, role) != null;
    }

    public static boolean matchesRequirement(MultiblockRole role, BlockState state) {
        return switch (role) {
            case CONTROLLER -> state.is(ModBlocks.MATTER_BATTERY_CORE.get());
            case FRAME -> MatterBatteryMultiblockDefinition.matchesFrameState(state);
            case CASING, PORT -> MatterBatteryMultiblockDefinition.matchesShellFaceState(state);
            case INTERNAL -> state.is(ModBlocks.MATTER_CAPACITOR_CELL.get());
        };
    }

    public static @Nullable InventoryBlockSelection selectPlacement(Player player, InteractionHand hand, MultiblockRole role) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (isValidPlacementStack(heldStack, role)) {
            return new InventoryBlockSelection(resolveBlock(heldStack), heldStack);
        }

        if (player.getAbilities().instabuild) {
            List<Block> candidates = getCandidateBlocks(role);
            if (!candidates.isEmpty()) {
                return new InventoryBlockSelection(candidates.get(0), ItemStack.EMPTY);
            }
        }

        Inventory inventory = player.getInventory();
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack selectedStack = inventory.getSelected();
            if (!selectedStack.isEmpty() && isValidPlacementStack(selectedStack, role)) {
                return new InventoryBlockSelection(resolveBlock(selectedStack), selectedStack);
            }
        }

        for (Block candidate : getCandidateBlocks(role)) {
            ItemStack matchingStack = findMatchingStack(inventory.items, candidate);
            if (matchingStack != null) {
                return new InventoryBlockSelection(candidate, matchingStack);
            }
        }

        for (Block candidate : getCandidateBlocks(role)) {
            ItemStack matchingStack = findMatchingStack(inventory.offhand, candidate);
            if (matchingStack != null) {
                return new InventoryBlockSelection(candidate, matchingStack);
            }
        }

        return null;
    }

    private static @Nullable ItemStack findMatchingStack(List<ItemStack> stacks, Block candidate) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            Block stackBlock = resolveBlock(stack);
            if (stackBlock == candidate) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isValidPlacementStack(ItemStack stack, MultiblockRole role) {
        Block block = resolveBlock(stack);
        return block != null && getCandidateBlocks(role).contains(block);
    }

    private static @Nullable Block resolveBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
    }

    private static List<Block> getCandidateBlocks(MultiblockRole role) {
        return switch (role) {
            case CONTROLLER -> List.of(ModBlocks.MATTER_BATTERY_CORE.get());
            case FRAME -> List.of(ModBlocks.MULTIBLOCK_FRAME.get());
            case CASING, PORT -> List.of(ModBlocks.MULTIBLOCK_CASING.get(), ModBlocks.MULTIBLOCK_GLASS.get(), ModBlocks.MULTIBLOCK_PORT.get());
            case INTERNAL -> List.of(ModBlocks.MATTER_CAPACITOR_CELL.get());
        };
    }

    private static BlockState createPlacementState(Block block, Direction front) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, front);
        }
        return state;
    }

    public record InventoryBlockSelection(Block block, ItemStack stack) {
    }
}
