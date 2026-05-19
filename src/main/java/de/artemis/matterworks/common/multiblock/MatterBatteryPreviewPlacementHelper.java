package de.artemis.matterworks.common.multiblock;

import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
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

    public static boolean placeFromInventory(Player player, BlockPos controllerPos, BlockPos targetPos, InteractionHand hand) {
        if (!(player.level().getBlockEntity(controllerPos) instanceof MatterBatteryCoreBlockEntity controller)) {
            return false;
        }

        Direction front = controller.getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? controller.getBlockState().getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        BlockPos origin = controller.getMultiblockPartState().isFormed()
                ? controller.getMultiblockPartState().getOriginPos()
                : MultiblockTransforms.controllerToOrigin(controllerPos, front, MatterBatteryMultiblockDefinition.INSTANCE.getPattern().getControllerOffset());
        BlockPos localPos = MultiblockTransforms.worldToLocal(origin, front, targetPos);
        if (!MultiblockTransforms.localToWorld(origin, front, localPos).equals(targetPos)) {
            return false;
        }

        MultiblockRequirement requirement = MatterBatteryMultiblockDefinition.INSTANCE.getPattern().getRequirements().get(localPos);
        if (requirement == null) {
            return false;
        }

        BlockState currentState = player.level().getBlockState(targetPos);
        if (matchesRequirement(requirement, currentState) || (!currentState.isAir() && !currentState.canBeReplaced())) {
            return false;
        }

        InventoryBlockSelection selection = selectPlacement(player, hand, requirement);
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
        return true;
    }

    public static boolean canPlaceFromInventory(Player player, InteractionHand hand, MultiblockRequirement requirement) {
        return selectPlacement(player, hand, requirement) != null;
    }

    public static boolean matchesRequirement(MultiblockRequirement requirement, BlockState state) {
        return switch (requirement.description()) {
            case MatterBatteryMultiblockDefinition.DESC_CONTROLLER -> state.is(ModBlocks.MATTER_BATTERY_CORE.get());
            case MatterBatteryMultiblockDefinition.DESC_FRAME -> MatterBatteryMultiblockDefinition.matchesFrameState(state);
            case MatterBatteryMultiblockDefinition.DESC_CASING -> MatterBatteryMultiblockDefinition.matchesShellFaceState(state);
            case MatterBatteryMultiblockDefinition.DESC_CELL -> state.is(ModBlocks.MATTER_CAPACITOR_CELL.get());
            default -> false;
        };
    }

    public static @Nullable InventoryBlockSelection selectPlacement(Player player, InteractionHand hand, MultiblockRequirement requirement) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (isValidPlacementStack(heldStack, requirement)) {
            return new InventoryBlockSelection(resolveBlock(heldStack), heldStack);
        }

        if (player.getAbilities().instabuild) {
            List<Block> candidates = getCandidateBlocks(requirement);
            if (!candidates.isEmpty()) {
                return new InventoryBlockSelection(candidates.get(0), ItemStack.EMPTY);
            }
        }

        Inventory inventory = player.getInventory();
        if (hand == InteractionHand.MAIN_HAND) {
            ItemStack selectedStack = inventory.getSelected();
            if (!selectedStack.isEmpty() && isValidPlacementStack(selectedStack, requirement)) {
                return new InventoryBlockSelection(resolveBlock(selectedStack), selectedStack);
            }
        }

        for (Block candidate : getCandidateBlocks(requirement)) {
            ItemStack matchingStack = findMatchingStack(inventory.items, candidate);
            if (matchingStack != null) {
                return new InventoryBlockSelection(candidate, matchingStack);
            }
        }

        for (Block candidate : getCandidateBlocks(requirement)) {
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

    private static boolean isValidPlacementStack(ItemStack stack, MultiblockRequirement requirement) {
        Block block = resolveBlock(stack);
        return block != null && getCandidateBlocks(requirement).contains(block);
    }

    private static @Nullable Block resolveBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
    }

    private static List<Block> getCandidateBlocks(MultiblockRequirement requirement) {
        return switch (requirement.description()) {
            case MatterBatteryMultiblockDefinition.DESC_CONTROLLER -> List.of(ModBlocks.MATTER_BATTERY_CORE.get());
            case MatterBatteryMultiblockDefinition.DESC_FRAME -> List.of(ModBlocks.MULTIBLOCK_FRAME.get());
            case MatterBatteryMultiblockDefinition.DESC_CASING -> List.of(ModBlocks.MULTIBLOCK_CASING.get());
            case MatterBatteryMultiblockDefinition.DESC_CELL -> List.of(ModBlocks.MATTER_CAPACITOR_CELL.get());
            default -> List.of();
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
