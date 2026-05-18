package de.artemis.matterworks.common.io;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

public final class SideConfigurationData {
    private final Map<SideConfigType, SideAccessMode[]> modesByType = new EnumMap<>(SideConfigType.class);

    public SideConfigurationData(SideAccessMode itemDefault, SideAccessMode fluidDefault, SideAccessMode energyDefault) {
        modesByType.put(SideConfigType.ITEMS, createFilledModes(itemDefault));
        modesByType.put(SideConfigType.FLUIDS, createFilledModes(fluidDefault));
        modesByType.put(SideConfigType.ENERGY, createFilledModes(energyDefault));
    }

    public SideAccessMode get(SideConfigType type, Direction side) {
        return modesByType.get(type)[side.ordinal()];
    }

    public boolean set(SideConfigType type, Direction side, SideAccessMode mode) {
        SideAccessMode[] modes = modesByType.get(type);
        if (modes[side.ordinal()] == mode) {
            return false;
        }
        modes[side.ordinal()] = mode;
        return true;
    }

    public void writeToTag(CompoundTag tag) {
        CompoundTag configTag = new CompoundTag();
        for (SideConfigType type : SideConfigType.values()) {
            CompoundTag typeTag = new CompoundTag();
            SideAccessMode[] modes = modesByType.get(type);
            for (Direction side : Direction.values()) {
                typeTag.putString(side.getName(), modes[side.ordinal()].name().toLowerCase(java.util.Locale.ROOT));
            }
            configTag.put(type.name().toLowerCase(java.util.Locale.ROOT), typeTag);
        }
        tag.put("side_config", configTag);
    }

    public void readFromTag(CompoundTag tag, SideConfigModeSanitizer sanitizer) {
        CompoundTag configTag = tag.getCompound("side_config");
        for (SideConfigType type : SideConfigType.values()) {
            if (!configTag.contains(type.name().toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            CompoundTag typeTag = configTag.getCompound(type.name().toLowerCase(java.util.Locale.ROOT));
            for (Direction side : Direction.values()) {
                SideAccessMode parsed = parseMode(typeTag.getString(side.getName()));
                modesByType.get(type)[side.ordinal()] = sanitizer.sanitize(type, side, parsed);
            }
        }
    }

    private static SideAccessMode[] createFilledModes(SideAccessMode mode) {
        SideAccessMode[] values = new SideAccessMode[Direction.values().length];
        java.util.Arrays.fill(values, mode);
        return values;
    }

    private static SideAccessMode parseMode(String serializedMode) {
        return switch (serializedMode) {
            case "input" -> SideAccessMode.INPUT;
            case "output" -> SideAccessMode.OUTPUT;
            case "both" -> SideAccessMode.BOTH;
            default -> SideAccessMode.DISABLED;
        };
    }

    @FunctionalInterface
    public interface SideConfigModeSanitizer {
        SideAccessMode sanitize(SideConfigType type, Direction side, SideAccessMode requestedMode);
    }
}
