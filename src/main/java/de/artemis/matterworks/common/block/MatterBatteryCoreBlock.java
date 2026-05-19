package de.artemis.matterworks.common.block;

import com.mojang.serialization.MapCodec;
import de.artemis.matterworks.common.blockentity.MatterBatteryCoreBlockEntity;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MatterBatteryCoreBlock extends AbstractBatteryMultiblockHorizontalBlock {
    public static final MapCodec<MatterBatteryCoreBlock> CODEC = simpleCodec(MatterBatteryCoreBlock::new);

    public MatterBatteryCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MatterBatteryCoreBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MatterBatteryCoreBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                if (controller.isFormed()) {
                    controller.disassemble(player);
                } else {
                    controller.tryAssemble(player, true);
                }
            } else {
                controller.tryAssemble(null, false);
                player.openMenu((MenuProvider) controller, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MatterBatteryCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.MATTER_BATTERY_CORE.get(), MatterBatteryCoreBlockEntity::tick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MatterBatteryCoreBlockEntity controller) {
            int energyStored = controller.getDisplayedEnergyStored();
            int capacity = controller.getDisplayedEnergyCapacity();
            if (energyStored <= 0 || capacity <= 0) {
                return 0;
            }
            return Mth.floor((double) energyStored * 14.0D / capacity) + 1;
        }
        return 0;
    }
}
