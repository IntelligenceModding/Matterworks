package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.Matterworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class SingularityLinkSavedData extends SavedData {
    private static final String DATA_NAME = Matterworks.MOD_ID + "_singularity_links";
    private static final Factory<SingularityLinkSavedData> FACTORY =
            new Factory<>(SingularityLinkSavedData::new, SingularityLinkSavedData::load, null);

    private final Map<BlockPos, Entry> entries = new HashMap<>();

    public static SingularityLinkSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static SingularityLinkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SingularityLinkSavedData data = new SingularityLinkSavedData();
        ListTag entriesTag = tag.getList("entries", Tag.TAG_COMPOUND);
        for (Tag entryTag : entriesTag) {
            if (!(entryTag instanceof CompoundTag compoundTag)) {
                continue;
            }
            BlockPos pos = new BlockPos(compoundTag.getInt("x"), compoundTag.getInt("y"), compoundTag.getInt("z"));
            data.entries.put(pos, new Entry(
                    pos,
                    compoundTag.getInt("code"),
                    compoundTag.getBoolean("formed"),
                    compoundTag.getBoolean("has_singularity"),
                    compoundTag.contains("connected_x") ? new BlockPos(compoundTag.getInt("connected_x"), compoundTag.getInt("connected_y"), compoundTag.getInt("connected_z")) : null
            ));
        }
        return data;
    }

    public void update(BlockPos pos, int code, boolean formed, boolean hasSingularity) {
        Entry existing = entries.get(pos);
        BlockPos connectedTo = existing == null ? null : existing.connectedTo();
        entries.put(pos.immutable(), new Entry(pos.immutable(), code, formed, hasSingularity, connectedTo));
        setDirty();
    }

    public void remove(BlockPos pos) {
        Entry removed = entries.remove(pos);
        if (removed != null) {
            if (removed.connectedTo() != null) {
                Entry partner = entries.get(removed.connectedTo());
                if (partner != null && pos.equals(partner.connectedTo())) {
                    entries.put(removed.connectedTo(), partner.withConnectedTo(null));
                }
            }
            setDirty();
        }
    }

    public @Nullable BlockPos getConnectedPartner(BlockPos pos) {
        Entry self = entries.get(pos);
        if (self == null || self.connectedTo() == null) {
            return null;
        }
        Entry partner = entries.get(self.connectedTo());
        if (partner == null || !pos.equals(partner.connectedTo())) {
            return null;
        }
        return partner.pos();
    }

    public BlockPos findUniquePartner(BlockPos pos, int code) {
        Entry self = entries.get(pos);
        BlockPos partnerPos = getConnectedPartner(pos);
        if (self == null || partnerPos == null) {
            return null;
        }
        Entry partner = entries.get(partnerPos);
        if (!self.isReady() || !partner.isReady() || self.code() != code || partner.code() != code) {
            return null;
        }
        return partner.pos();
    }

    public ConnectAvailability getConnectAvailability(BlockPos pos, int code) {
        Entry self = entries.get(pos);
        if (self == null || !self.isReady() || self.code() != code) {
            return ConnectAvailability.UNAVAILABLE;
        }
        if (getConnectedPartner(pos) != null) {
            return ConnectAvailability.LINKED;
        }

        int freeCandidateCount = 0;
        boolean hasOccupiedCandidate = false;
        for (Entry entry : entries.values()) {
            if (entry.pos().equals(pos) || !entry.isReady() || entry.code() != code) {
                continue;
            }
            if (getConnectedPartner(entry.pos()) != null) {
                hasOccupiedCandidate = true;
                continue;
            }
            freeCandidateCount++;
        }

        if (freeCandidateCount == 1) {
            return ConnectAvailability.AVAILABLE;
        }
        if (freeCandidateCount > 1) {
            return ConnectAvailability.MULTIPLE_MATCHES;
        }
        return hasOccupiedCandidate ? ConnectAvailability.PAIR_OCCUPIED : ConnectAvailability.NO_MATCH;
    }

    public BlockPos findUniqueConnectCandidate(BlockPos pos, int code) {
        if (getConnectAvailability(pos, code) != ConnectAvailability.AVAILABLE) {
            return null;
        }
        BlockPos candidate = null;
        for (Entry entry : entries.values()) {
            if (entry.pos().equals(pos) || !entry.isReady() || entry.code() != code || getConnectedPartner(entry.pos()) != null) {
                continue;
            }
            if (candidate != null) {
                return null;
            }
            candidate = entry.pos();
        }
        return candidate;
    }

    public boolean connect(BlockPos first, BlockPos second) {
        Entry firstEntry = entries.get(first);
        Entry secondEntry = entries.get(second);
        if (firstEntry == null || secondEntry == null) {
            return false;
        }
        entries.put(first, firstEntry.withConnectedTo(second));
        entries.put(second, secondEntry.withConnectedTo(first));
        setDirty();
        return true;
    }

    public boolean disconnect(BlockPos pos) {
        Entry entry = entries.get(pos);
        if (entry == null || entry.connectedTo() == null) {
            return false;
        }
        BlockPos partnerPos = entry.connectedTo();
        entries.put(pos, entry.withConnectedTo(null));
        Entry partner = entries.get(partnerPos);
        if (partner != null && pos.equals(partner.connectedTo())) {
            entries.put(partnerPos, partner.withConnectedTo(null));
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entriesTag = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("x", entry.pos().getX());
            entryTag.putInt("y", entry.pos().getY());
            entryTag.putInt("z", entry.pos().getZ());
            entryTag.putInt("code", entry.code());
            entryTag.putBoolean("formed", entry.formed());
            entryTag.putBoolean("has_singularity", entry.hasSingularity());
            if (entry.connectedTo() != null) {
                entryTag.putInt("connected_x", entry.connectedTo().getX());
                entryTag.putInt("connected_y", entry.connectedTo().getY());
                entryTag.putInt("connected_z", entry.connectedTo().getZ());
            }
            entriesTag.add(entryTag);
        }
        tag.put("entries", entriesTag);
        return tag;
    }

    private record Entry(BlockPos pos, int code, boolean formed, boolean hasSingularity, BlockPos connectedTo) {
        private boolean isReady() {
            return formed && hasSingularity;
        }

        private Entry withConnectedTo(BlockPos newConnectedTo) {
            return new Entry(pos, code, formed, hasSingularity, newConnectedTo == null ? null : newConnectedTo.immutable());
        }
    }

    public enum ConnectAvailability {
        UNAVAILABLE,
        NO_MATCH,
        AVAILABLE,
        LINKED,
        PAIR_OCCUPIED,
        MULTIPLE_MATCHES
    }
}
