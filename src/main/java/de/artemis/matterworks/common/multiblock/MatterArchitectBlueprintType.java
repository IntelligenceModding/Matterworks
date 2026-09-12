package de.artemis.matterworks.common.multiblock;

import net.minecraft.network.chat.Component;

public enum MatterArchitectBlueprintType {
    MATTER_BATTERY(
            0,
            "matter_battery",
            Component.translatable("screen.matterworks.matter_architect.blueprint.matter_battery")
    );

    private static final MatterArchitectBlueprintType[] BY_ID = values();

    private final int id;
    private final String serializedName;
    private final Component displayName;

    MatterArchitectBlueprintType(int id, String serializedName, Component displayName) {
        this.id = id;
        this.serializedName = serializedName;
        this.displayName = displayName;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return serializedName;
    }

    public Component displayName() {
        return displayName;
    }

    public static MatterArchitectBlueprintType fromId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : MATTER_BATTERY;
    }

    public static MatterArchitectBlueprintType bySerializedName(String serializedName) {
        for (MatterArchitectBlueprintType type : values()) {
            if (type.serializedName.equals(serializedName)) {
                return type;
            }
        }
        return MATTER_BATTERY;
    }
}
