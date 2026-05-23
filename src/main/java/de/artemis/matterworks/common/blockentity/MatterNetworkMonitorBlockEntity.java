package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.common.menu.MatterNetworkMonitorMenu;
import de.artemis.matterworks.common.registry.ModBlockEntities;
import de.artemis.matterworks.common.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.Set;

public class MatterNetworkMonitorBlockEntity extends MatterPylonBlockEntity {
    private static final int REFRESH_INTERVAL = 4;
    private static final int HISTORY_SAMPLE_INTERVAL = 4;
    private static final int HISTORY_LENGTH = 120;

    private ChannelTotals[] channelTotals = createEmptyTotals();
    private final HistorySample[][] channelHistory = createEmptyHistory();
    private int historySize;

    public MatterNetworkMonitorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MATTER_NETWORK_MONITOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterNetworkMonitorBlockEntity blockEntity) {
        blockEntity.serverTick();
    }

    public ChannelTotals getChannelTotals(int channel) {
        return channel >= 0 && channel < channelTotals.length ? channelTotals[channel] : ChannelTotals.EMPTY;
    }

    public int getHistorySize() {
        return historySize;
    }

    public int getHistoryCapacity() {
        return HISTORY_LENGTH;
    }

    public HistorySample getHistorySample(int channel, int index) {
        if (channel < 0 || channel >= channelHistory.length || index < 0 || index >= historySize) {
            return HistorySample.EMPTY;
        }
        return channelHistory[channel][index];
    }

    public boolean isMonitorMenuStillValid(Player player, boolean remoteAccess) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return remoteAccess || player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    public void openRemoteMenu(ServerPlayer player) {
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) -> new MatterNetworkMonitorMenu(containerId, inventory, this, true),
                        getDisplayName()
                ),
                buffer -> {
                    buffer.writeBlockPos(worldPosition);
                    buffer.writeBoolean(true);
                }
        );
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % REFRESH_INTERVAL == 0L) {
            refreshMonitorData(serverLevel.getGameTime(), false);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(ModBlocks.MATTER_NETWORK_MONITOR.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!player.level().isClientSide() && level instanceof ServerLevel serverLevel) {
            refreshMonitorData(serverLevel.getGameTime(), true);
        }
        return new MatterNetworkMonitorMenu(containerId, playerInventory, this, false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeMonitorTag(tag);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeMonitorTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readMonitorTag(tag);
    }

    private void refreshMonitorData(long gameTime, boolean forceSample) {
        if (level == null) {
            return;
        }

        Set<BlockPos> members = collectConnectedNodePositions();
        int[] sourceTotals = new int[CHANNEL_COUNT];
        int[] sinkTotals = new int[CHANNEL_COUNT];
        int[] transitTotals = new int[CHANNEL_COUNT];
        int[] activeNodes = new int[CHANNEL_COUNT];

        for (BlockPos pos : members) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof MatterPylonBlockEntity node)) {
                continue;
            }

            for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
                int source = node.getTransferRoleAmount(channel, TransferDisplayRole.SOURCE);
                int sink = node.getTransferRoleAmount(channel, TransferDisplayRole.SINK);
                int transit = node.getTransferRoleAmount(channel, TransferDisplayRole.TRANSIT);
                sourceTotals[channel] += source;
                sinkTotals[channel] += sink;
                transitTotals[channel] += transit;
                if (source > 0 || sink > 0 || transit > 0) {
                    activeNodes[channel]++;
                }
            }
        }

        ChannelTotals[] totals = new ChannelTotals[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            totals[channel] = new ChannelTotals(sourceTotals[channel], sinkTotals[channel], transitTotals[channel], activeNodes[channel]);
        }

        boolean totalsChanged = !channelTotalsEqual(channelTotals, totals);
        channelTotals = totals;

        boolean shouldSample = forceSample || gameTime % HISTORY_SAMPLE_INTERVAL == 0L;
        boolean historyChanged = shouldSample && appendHistorySamples(totals);

        if (totalsChanged || historyChanged) {
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    private boolean appendHistorySamples(ChannelTotals[] totals) {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            HistorySample sample = totals[channel].toHistorySample();
            if (historySize < HISTORY_LENGTH) {
                channelHistory[channel][historySize] = sample;
            } else {
                System.arraycopy(channelHistory[channel], 1, channelHistory[channel], 0, HISTORY_LENGTH - 1);
                channelHistory[channel][HISTORY_LENGTH - 1] = sample;
            }
        }
        if (historySize < HISTORY_LENGTH) {
            historySize++;
        }
        return true;
    }

    private void writeMonitorTag(CompoundTag tag) {
        tag.putInt("history_size", historySize);

        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            ChannelTotals totals = channelTotals[channel];
            CompoundTag totalsTag = new CompoundTag();
            totalsTag.putInt("source", totals.sourceAmount());
            totalsTag.putInt("sink", totals.sinkAmount());
            totalsTag.putInt("transit", totals.transitAmount());
            totalsTag.putInt("active_nodes", totals.activeNodes());
            tag.put("monitor_totals_" + channel, totalsTag);

            ListTag historyTag = new ListTag();
            for (int index = 0; index < historySize; index++) {
                HistorySample sample = channelHistory[channel][index];
                CompoundTag sampleTag = new CompoundTag();
                sampleTag.putInt("source", sample.sourceAmount());
                sampleTag.putInt("sink", sample.sinkAmount());
                sampleTag.putInt("transit", sample.transitAmount());
                sampleTag.putInt("active_nodes", sample.activeNodes());
                historyTag.add(sampleTag);
            }
            tag.put("monitor_history_" + channel, historyTag);
        }
    }

    private void readMonitorTag(CompoundTag tag) {
        ChannelTotals[] totals = new ChannelTotals[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            CompoundTag totalsTag = tag.getCompound("monitor_totals_" + channel);
            totals[channel] = totalsTag.isEmpty()
                    ? ChannelTotals.EMPTY
                    : new ChannelTotals(
                    totalsTag.getInt("source"),
                    totalsTag.getInt("sink"),
                    totalsTag.getInt("transit"),
                    totalsTag.getInt("active_nodes")
            );
        }
        channelTotals = totals;

        clearHistory();
        int loadedHistorySize = 0;
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            ListTag historyTag = tag.getList("monitor_history_" + channel, Tag.TAG_COMPOUND);
            int channelSize = Math.min(HISTORY_LENGTH, historyTag.size());
            loadedHistorySize = Math.max(loadedHistorySize, channelSize);
            for (int index = 0; index < channelSize; index++) {
                CompoundTag sampleTag = historyTag.getCompound(index);
                channelHistory[channel][index] = new HistorySample(
                        sampleTag.getInt("source"),
                        sampleTag.getInt("sink"),
                        sampleTag.getInt("transit"),
                        sampleTag.getInt("active_nodes")
                );
            }
        }
        historySize = Math.min(HISTORY_LENGTH, Math.max(tag.getInt("history_size"), loadedHistorySize));
    }

    private void clearHistory() {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            for (int index = 0; index < HISTORY_LENGTH; index++) {
                channelHistory[channel][index] = HistorySample.EMPTY;
            }
        }
        historySize = 0;
    }

    private static ChannelTotals[] createEmptyTotals() {
        ChannelTotals[] totals = new ChannelTotals[CHANNEL_COUNT];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            totals[channel] = ChannelTotals.EMPTY;
        }
        return totals;
    }

    private static HistorySample[][] createEmptyHistory() {
        HistorySample[][] history = new HistorySample[CHANNEL_COUNT][HISTORY_LENGTH];
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            for (int index = 0; index < HISTORY_LENGTH; index++) {
                history[channel][index] = HistorySample.EMPTY;
            }
        }
        return history;
    }

    private static boolean channelTotalsEqual(ChannelTotals[] first, ChannelTotals[] second) {
        if (first.length != second.length) {
            return false;
        }
        for (int index = 0; index < first.length; index++) {
            if (!Objects.equals(first[index], second[index])) {
                return false;
            }
        }
        return true;
    }

    public record HistorySample(int sourceAmount, int sinkAmount, int transitAmount, int activeNodes) {
        public static final HistorySample EMPTY = new HistorySample(0, 0, 0, 0);

        public int totalAmount() {
            return sourceAmount + sinkAmount + transitAmount;
        }
    }

    public record ChannelTotals(int sourceAmount, int sinkAmount, int transitAmount, int activeNodes) {
        public static final ChannelTotals EMPTY = new ChannelTotals(0, 0, 0, 0);

        public int totalAmount() {
            return sourceAmount + sinkAmount + transitAmount;
        }

        public HistorySample toHistorySample() {
            return new HistorySample(sourceAmount, sinkAmount, transitAmount, activeNodes);
        }
    }
}
