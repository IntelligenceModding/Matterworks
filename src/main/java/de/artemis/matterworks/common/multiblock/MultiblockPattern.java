package de.artemis.matterworks.common.multiblock;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiblockPattern {
    private final int width;
    private final int height;
    private final int depth;
    private final BlockPos controllerOffset;
    private final Map<BlockPos, MultiblockRequirement> requirements;

    private MultiblockPattern(int width, int height, int depth, BlockPos controllerOffset, Map<BlockPos, MultiblockRequirement> requirements) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.controllerOffset = controllerOffset.immutable();
        this.requirements = Map.copyOf(requirements);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public BlockPos getControllerOffset() {
        return controllerOffset;
    }

    public Map<BlockPos, MultiblockRequirement> getRequirements() {
        return requirements;
    }

    public boolean containsLocalPos(BlockPos localPos) {
        return localPos.getX() >= 0 && localPos.getX() < width
                && localPos.getY() >= 0 && localPos.getY() < height
                && localPos.getZ() >= 0 && localPos.getZ() < depth;
    }

    public static Builder builder(BlockPos controllerOffset) {
        return new Builder(controllerOffset);
    }

    public static final class Builder {
        private final BlockPos controllerOffset;
        private final List<String[]> layers = new ArrayList<>();
        private final Map<Character, MultiblockRequirement> palette = new LinkedHashMap<>();
        private int width = -1;
        private int depth = -1;

        private Builder(BlockPos controllerOffset) {
            this.controllerOffset = controllerOffset.immutable();
            palette.put(' ', MultiblockRequirement.air());
        }

        public Builder aisle(String... rows) {
            if (rows == null || rows.length == 0) {
                throw new IllegalArgumentException("layer rows cannot be empty");
            }
            if (depth == -1) {
                depth = rows.length;
            } else if (depth != rows.length) {
                throw new IllegalArgumentException("all layers must have the same row count");
            }

            int layerWidth = rows[0].length();
            if (layerWidth == 0) {
                throw new IllegalArgumentException("layer width cannot be zero");
            }
            if (width == -1) {
                width = layerWidth;
            } else if (width != layerWidth) {
                throw new IllegalArgumentException("all layers must have the same width");
            }

            for (String row : rows) {
                if (row.length() != layerWidth) {
                    throw new IllegalArgumentException("all rows in a layer must have the same width");
                }
            }

            layers.add(rows.clone());
            return this;
        }

        public Builder where(char symbol, MultiblockRequirement requirement) {
            palette.put(symbol, requirement);
            return this;
        }

        public MultiblockPattern build() {
            if (layers.isEmpty()) {
                throw new IllegalStateException("pattern must contain at least one layer");
            }
            if (width <= 0 || depth <= 0) {
                throw new IllegalStateException("pattern dimensions are invalid");
            }

            Map<BlockPos, MultiblockRequirement> requirements = new LinkedHashMap<>();
            for (int y = 0; y < layers.size(); y++) {
                String[] layer = layers.get(y);
                for (int z = 0; z < layer.length; z++) {
                    String row = layer[z];
                    for (int x = 0; x < row.length(); x++) {
                        char symbol = row.charAt(x);
                        MultiblockRequirement requirement = palette.get(symbol);
                        if (requirement == null) {
                            throw new IllegalStateException("missing pattern mapping for symbol '" + symbol + "'");
                        }
                        requirements.put(new BlockPos(x, y, z), requirement);
                    }
                }
            }

            if (controllerOffset.getX() < 0 || controllerOffset.getX() >= width
                    || controllerOffset.getY() < 0 || controllerOffset.getY() >= layers.size()
                    || controllerOffset.getZ() < 0 || controllerOffset.getZ() >= depth) {
                throw new IllegalStateException("controller offset is outside the pattern bounds");
            }

            return new MultiblockPattern(width, layers.size(), depth, controllerOffset, requirements);
        }
    }
}
