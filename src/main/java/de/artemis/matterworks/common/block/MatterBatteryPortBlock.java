package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MatterBatteryPortBlockEntity;
import de.artemis.matterworks.common.multiblock.MatterBatteryMultiblockHelper;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MatterBatteryPortBlock extends AbstractBatteryMultiblockEntityBlock {
    public static final MapCodec<MatterBatteryPortBlock> CODEC = simpleCodec(MatterBatteryPortBlock::new);

    public MatterBatteryPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterBatteryPortBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MatterBatteryPortBlockEntity portBlockEntity) {
            if (player.isShiftKeyDown()) {
                portBlockEntity.handleNetworkLinkUse(player);
            } else if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                if (!MatterBatteryMultiblockHelper.tryOpenBatteryMenu(serverLevel, pos, player)) {
                    portBlockEntity.openMatterNetworkMenu(player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterBatteryPortBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MULTIBLOCK_PORT.get(), MatterBatteryPortBlockEntity::tick);
    }
}
