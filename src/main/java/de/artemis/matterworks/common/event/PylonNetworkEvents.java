package de.artemis.matterworks.common.event;

import de.artemis.matterworks.common.blockentity.MultiblockPortBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.blockentity.MatterPylonBlockEntity;
import de.artemis.matterworks.common.block.MultiblockPortBlock;
import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockHelper;
import de.artemis.matterworks.common.multiblock.MultiblockRole;
import de.artemis.matterworks.common.multiblock.MultiblockStructureRegistry;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;
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
        boolean placingFrame = event.getItemStack().is(ModBlocks.MULTIBLOCK_FRAME.get().asItem());
        BlockState currentState = event.getLevel().getBlockState(event.getPos());
        boolean dyeingPort = currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                && net.minecraft.world.item.DyeColor.getColor(event.getItemStack()) != null;
        boolean batteryInteractionBlock = isBatteryInteractionBlock(currentState);
        boolean activeBatteryMember = batteryInteractionBlock
                && MatterBatteryMultiblockHelper.isKnownFormedBatteryMember(event.getLevel(), event.getPos());
        if (event.getUseOnContext().isSecondaryUseActive()) {
            return;
        }
        if (!NetworkDataCardItem.isHeldBy(event.getPlayer())
                && !dyeingPort
                && !placingPort
                && !placingGlass
                && !placingCasing
                && !placingFrame
                && activeBatteryMember) {
            event.cancelWithResult(ItemInteractionResult.SUCCESS);
            if (!event.getLevel().isClientSide()) {
                openBatteryInteraction((ServerPlayer) event.getPlayer(), (ServerLevel) event.getLevel(), event.getPos(), currentState);
            }
            return;
        }

        if (batteryInteractionBlock && !activeBatteryMember) {
            return;
        }

        if (isSameBatteryBlockPlacement(event.getItemStack(), currentState) && canPlaceAgainstClickedFace(event)) {
            return;
        }

        if (isSameBatteryBlockPlacement(event.getItemStack(), currentState)) {
            event.cancelWithResult(ItemInteractionResult.FAIL);
            return;
        }

        if (!placingPort && !placingGlass && !placingCasing && !placingFrame) {
            return;
        }

        if (!canReplaceShellBlock(event.getLevel(), event.getPos(), currentState, placingPort, placingGlass, placingCasing, placingFrame)) {
            if (canPlaceAgainstClickedFace(event)) {
                return;
            }
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
        net.minecraft.world.item.DyeColor placedPortColor = placingPort ? MultiblockPortBlockEntity.getPortColor(event.getItemStack()) : null;
        BlockState replacementState = getReplacementState(placingPort, placingGlass, placingCasing, placingFrame, event.getItemStack());
        net.minecraft.world.item.DyeColor replacedPortColor = currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                && level.getBlockEntity(event.getPos()) instanceof MultiblockPortBlockEntity portBlockEntity
                ? portBlockEntity.getPortColor()
                : null;
        if (!level.setBlock(event.getPos(), replacementState, 3)) {
            return;
        }
        if (placedPortColor != null && level.getBlockEntity(event.getPos()) instanceof MultiblockPortBlockEntity portBlockEntity) {
            portBlockEntity.setPortColor(placedPortColor);
        }
        playShellReplacementEffects(level, event.getPos(), currentState, replacementState, player);

        if (!player.getAbilities().instabuild) {
            event.getItemStack().shrink(1);
            ItemStack returnedBlock = new ItemStack(currentState.getBlock());
            if (replacedPortColor != null) {
                MultiblockPortBlockEntity.applyColorToStack(returnedBlock, replacedPortColor);
            }
            if (!player.getInventory().add(returnedBlock)) {
                player.drop(returnedBlock, false, false);
            }
            player.getInventory().setChanged();
        }
    }

    private static boolean canReplaceShellBlock(Level level, BlockPos pos, BlockState currentState, boolean placingPort, boolean placingGlass, boolean placingCasing, boolean placingFrame) {
        if (!MatterBatteryMultiblockHelper.isKnownFormedBatteryMember(level, pos)) {
            return false;
        }

        if (placingPort) {
            if (MatterBatteryMultiblockHelper.wouldExceedPortLimitForKnownMember(level, pos, ModBlocks.MULTIBLOCK_PORT.get().defaultBlockState())) {
                return false;
            }
            return currentState.is(ModBlocks.MULTIBLOCK_FRAME.get())
                    || currentState.is(ModBlocks.MULTIBLOCK_CASING.get())
                    || currentState.is(ModBlocks.MULTIBLOCK_GLASS.get());
        }

        if (placingGlass) {
            if (currentState.is(ModBlocks.MULTIBLOCK_CASING.get())) {
                return true;
            }
            if (currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                    && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity
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
                    && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity
                    && portBlockEntity.getMultiblockPartState().isFormed()) {
                return portBlockEntity.getMultiblockPartState().getRole() == MultiblockRole.CASING;
            }
        }
        if (placingFrame) {
            return currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                    && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity
                    && portBlockEntity.getMultiblockPartState().isFormed()
                    && portBlockEntity.getMultiblockPartState().getRole() == MultiblockRole.FRAME;
        }
        return false;
    }

    private static BlockState getReplacementState(boolean placingPort, boolean placingGlass, boolean placingCasing, boolean placingFrame, ItemStack stack) {
        if (placingPort) {
            return ModBlocks.MULTIBLOCK_PORT.get().defaultBlockState()
                    .setValue(MultiblockPortBlock.COLOR, MultiblockPortBlockEntity.getPortColor(stack));
        }
        if (placingGlass) {
            return ModBlocks.MULTIBLOCK_GLASS.get().defaultBlockState();
        }
        if (placingCasing) {
            return ModBlocks.MULTIBLOCK_CASING.get().defaultBlockState();
        }
        if (placingFrame) {
            return ModBlocks.MULTIBLOCK_FRAME.get().defaultBlockState();
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

    private static boolean isSameBatteryBlockPlacement(ItemStack stack, BlockState currentState) {
        return stack.is(ModBlocks.MULTIBLOCK_FRAME.get().asItem()) && currentState.is(ModBlocks.MULTIBLOCK_FRAME.get())
                || stack.is(ModBlocks.MULTIBLOCK_CASING.get().asItem()) && currentState.is(ModBlocks.MULTIBLOCK_CASING.get())
                || stack.is(ModBlocks.MULTIBLOCK_PORT.get().asItem()) && currentState.is(ModBlocks.MULTIBLOCK_PORT.get())
                || stack.is(ModBlocks.MULTIBLOCK_GLASS.get().asItem()) && currentState.is(ModBlocks.MULTIBLOCK_GLASS.get())
                || stack.is(ModBlocks.MATTER_BATTERY_CORE.get().asItem()) && currentState.is(ModBlocks.MATTER_BATTERY_CORE.get())
                || stack.is(ModBlocks.MATTER_CAPACITOR_CELL.get().asItem()) && currentState.is(ModBlocks.MATTER_CAPACITOR_CELL.get());
    }

    private static boolean canPlaceAgainstClickedFace(UseItemOnBlockEvent event) {
        BlockPos targetPos = event.getPos().relative(event.getFace());
        return event.getLevel().isLoaded(targetPos) && event.getLevel().getBlockState(targetPos).canBeReplaced();
    }

    private static void openBatteryInteraction(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.MATTER_BATTERY_CORE.get())) {
            return;
        }

        if (state.is(ModBlocks.MULTIBLOCK_PORT.get())
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity) {
            if (portBlockEntity.isFunctionalPort()) {
                portBlockEntity.openMatterNetworkMenu(player);
            }
            return;
        }

        MatterBatteryMultiblockHelper.tryOpenBatteryMenu(level, pos, player);
    }

    private static void playShellReplacementEffects(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, ServerPlayer player) {
        SoundType oldSoundType = oldState.getSoundType(level, pos, player);
        level.playSound(null, pos, oldSoundType.getBreakSound(), SoundSource.BLOCKS, (oldSoundType.getVolume() + 1.0F) / 2.0F, oldSoundType.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, oldState),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                24,
                0.35D,
                0.35D,
                0.35D,
                0.05D
        );

        SoundType newSoundType = newState.getSoundType(level, pos, player);
        level.playSound(null, pos, newSoundType.getPlaceSound(), SoundSource.BLOCKS, (newSoundType.getVolume() + 1.0F) / 2.0F, newSoundType.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, newState),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                16,
                0.32D,
                0.32D,
                0.32D,
                0.04D
        );
    }
}
