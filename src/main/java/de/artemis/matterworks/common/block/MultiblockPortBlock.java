package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MultiblockPortBlockEntity;
import de.artemis.matterworks.common.item.NetworkDataCardItem;
import de.artemis.matterworks.common.network.SetMultiblockPortColorPayload;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiblockPortBlock extends AbstractBatteryMultiblockEntityBlock {
    public static final MapCodec<MultiblockPortBlock> CODEC = simpleCodec(MultiblockPortBlock::new);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public MultiblockPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    public MapCodec<MultiblockPortBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (NetworkDataCardItem.isHeldBy(player)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity)
                || !portBlockEntity.isFunctionalPort()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                portBlockEntity.handleNetworkLinkUse(player);
            } else {
                portBlockEntity.openMatterNetworkMenu(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity)
                || !portBlockEntity.isFunctionalPort()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        DyeColor dyeColor = DyeColor.getColor(stack);
        if (dyeColor != null) {
            if (!level.isClientSide() && portBlockEntity.setPortColor(dyeColor)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SetMultiblockPortColorPayload(pos, dyeColor.getId()));
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (NetworkDataCardItem.isHeldBy(player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                portBlockEntity.handleNetworkLinkUse(player);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            portBlockEntity.openMatterNetworkMenu(player);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(COLOR, MultiblockPortBlockEntity.getPortColor(context.getItemInHand()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof MultiblockPortBlockEntity portBlockEntity) {
            for (ItemStack drop : drops) {
                MultiblockPortBlockEntity.applyColorToStack(drop, portBlockEntity.getPortColor());
            }
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity) {
            MultiblockPortBlockEntity.applyColorToStack(stack, portBlockEntity.getPortColor());
        }
        return stack;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockPortBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MULTIBLOCK_PORT.get(), MultiblockPortBlockEntity::tick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity portBlockEntity) {
            portBlockEntity.releaseChunkLoadingTickets();
            portBlockEntity.unlinkAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
