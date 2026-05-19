package de.artemis.matterworks.common.event;

import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockHelper;
import de.artemis.matterworks.common.multiblock.MultiblockRole;
import de.artemis.matterworks.common.multiblock.MultiblockStructureRegistry;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@EventBusSubscriber
public final class PylonNetworkEvents {
    private PylonNetworkEvents() {
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            MatterPylonBlockEntity.clearServerLevelState(serverLevel);
            MultiblockStructureRegistry.clearServerLevelState(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MatterPylonBlockEntity.clearAllSharedState();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MatterPylonBlockEntity.clearAllSharedState();
    }

    @SubscribeEvent
    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK
                || event.getPlayer() == null) {
            return;
        }

        boolean placingPort = event.getItemStack().is(ModBlocks.MULTIBLOCK_PORT.get().asItem());
        boolean placingGlass = event.getItemStack().is(ModBlocks.MULTIBLOCK_GLASS.get().asItem());
        boolean placingCasing = event.getItemStack().is(ModBlocks.MULTIBLOCK_CASING.get().asItem());
        if (!placingPort && !placingGlass && !placingCasing) {
            return;
        }

        BlockState currentState = event.getLevel().getBlockState(event.getPos());
        if (!canReplaceShellBlock(event.getLevel(), event.getPos(), currentState, placingPort, placingGlass, placingCasing)) {
            if (isBatteryInteractionBlock(currentState)) {
                event.cancelWithResult(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
                if (!event.getLevel().isClientSide()) {
                    openBatteryInteraction((ServerPlayer) event.getPlayer(), (ServerLevel) event.getLevel(), event.getPos(), currentState);
                }
            }
            return;
        }

        event.cancelWithResult(ItemInteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getPlayer();
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockState replacementState = getReplacementState(placingPort, placingGlass, placingCasing);
        if (!level.setBlock(event.getPos(), replacementState, 3)) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
            ItemStack returnedBlock = new ItemStack(currentState.getBlock());
            if (!player.getInventory().add(returnedBlock)) {
                player.drop(returnedBlock, false, false);
            }
            player.getInventory().setChanged();
        }
    }

    private static boolean canReplaceShellBlock(Level level, BlockPos pos, BlockState currentState, boolean placingPort, boolean placingGlass, boolean placingCasing) {
        if (placingPort) {
            return currentState.is(ModBlocks.MULTIBLOCK_FRAME.get())
                    || currentState.is(ModBlocks.MULTIBLOCK_CASING.get())
                    || currentState.is(ModBlocks.MULTIBLOCK_GLASS.get());
        }

        if (placingGlass) {
            if (currentState.is(ModBlocks.MULTIBLOCK_CASING.get())) {
                return true;
            }
            if (currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                    && level.getBlockEntity(pos) instanceof MatterBatteryPortBlockEntity portBlockEntity
                    && portBlockEntity.getMultiblockPartState().isFormed()) {
                return portBlockEntity.getMultiblockPartState().getRole() == MultiblockRole.CASING;
            }
            return false;
        }

        if (placingCasing) {
            if (currentState.is(ModBlocks.MULTIBLOCK_GLASS.get())) {
                return true;
            }
            if (currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                    && level.getBlockEntity(pos) instanceof MatterBatteryPortBlockEntity portBlockEntity
                    && portBlockEntity.getMultiblockPartState().isFormed()) {
                return portBlockEntity.getMultiblockPartState().getRole() == MultiblockRole.CASING;
            }
        }
        return false;
    }

    private static BlockState getReplacementState(boolean placingPort, boolean placingGlass, boolean placingCasing) {
        if (placingPort) {
            return ModBlocks.MULTIBLOCK_PORT.get().defaultBlockState();
        }
        if (placingGlass) {
            return ModBlocks.MULTIBLOCK_GLASS.get().defaultBlockState();
        }
        if (placingCasing) {
            return ModBlocks.MULTIBLOCK_CASING.get().defaultBlockState();
        }
        throw new IllegalStateException("Unsupported shell replacement item");
    }

    private static boolean isBatteryInteractionBlock(BlockState state) {
        return state.is(ModBlocks.MULTIBLOCK_FRAME.get())
                || state.is(ModBlocks.MULTIBLOCK_CASING.get())
                || state.is(ModBlocks.MULTIBLOCK_PORT.get())
                || state.is(ModBlocks.MULTIBLOCK_GLASS.get())
                || state.is(ModBlocks.MATTER_BATTERY_CORE.get())
                || state.is(ModBlocks.MATTER_CAPACITOR_CELL.get());
    }

    private static void openBatteryInteraction(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.MATTER_BATTERY_CORE.get())
                && level.getBlockEntity(pos) instanceof MatterBatteryCoreBlockEntity controller) {
            controller.tryAssemble(null, false);
            player.openMenu((MenuProvider) controller, pos);
            return;
        }

        MatterBatteryMultiblockHelper.tryOpenBatteryMenu(level, pos, player);
    }
}
