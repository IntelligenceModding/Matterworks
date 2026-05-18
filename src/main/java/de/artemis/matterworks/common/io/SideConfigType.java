package de.artemis.matterworks.common.io;

public enum SideConfigType {
    ITEMS("Items"),
    FLUIDS("Fluids"),
    ENERGY("Energy");

    private final String label;

    SideConfigType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
