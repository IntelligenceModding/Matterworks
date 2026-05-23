package de.artemis.matterworks.common.blockentity;

import de.artemis.matterworks.Matterworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

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
                    compoundTag.getBoolean("has_singularity")
            ));
        }
        return data;
    }

    public void update(BlockPos pos, int code, boolean formed, boolean hasSingularity) {
        entries.put(pos.immutable(), new Entry(pos.immutable(), code, formed, hasSingularity));
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos) != null) {
            setDirty();
        }
    }

    public BlockPos findUniquePartner(BlockPos pos, int code) {
        BlockPos candidate = null;
        for (Entry entry : entries.values()) {
            if (entry.pos().equals(pos) || !entry.isReady() || entry.code() != code) {
                continue;
            }
            if (candidate != null) {
                return null;
            }
            candidate = entry.pos();
        }
        return candidate;
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
            entriesTag.add(entryTag);
        }
        tag.put("entries", entriesTag);
        return tag;
    }

    private record Entry(BlockPos pos, int code, boolean formed, boolean hasSingularity) {
        private boolean isReady() {
            return formed && hasSingularity;
        }
    }
}
