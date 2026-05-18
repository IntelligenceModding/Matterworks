package de.artemis.matterworks.common.multiblock;

public record MultiblockRequirement(
        MultiblockRole role,
        MultiblockPredicate predicate,
        String description,
        boolean optional
) {
    public MultiblockRequirement {
        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }
        if (predicate == null) {
            throw new IllegalArgumentException("predicate cannot be null");
        }
        if (description == null || description.isBlank()) {
            description = role.name().toLowerCase();
        }
    }

    public static MultiblockRequirement controller(MultiblockPredicate predicate, String description) {
        return new MultiblockRequirement(MultiblockRole.CONTROLLER, predicate, description, false);
    }

    public static MultiblockRequirement casing(MultiblockPredicate predicate, String description) {
        return new MultiblockRequirement(MultiblockRole.CASING, predicate, description, false);
    }

    public static MultiblockRequirement frame(MultiblockPredicate predicate, String description) {
        return new MultiblockRequirement(MultiblockRole.FRAME, predicate, description, false);
    }

    public static MultiblockRequirement port(MultiblockPredicate predicate, String description) {
        return new MultiblockRequirement(MultiblockRole.PORT, predicate, description, false);
    }

    public static MultiblockRequirement internal(MultiblockPredicate predicate, String description) {
        return new MultiblockRequirement(MultiblockRole.INTERNAL, predicate, description, false);
    }

    public static MultiblockRequirement air() {
        return new MultiblockRequirement(MultiblockRole.INTERNAL, MultiblockPredicate.air(), "air", false);
    }

    public static MultiblockRequirement optional(MultiblockRole role, MultiblockPredicate predicate, String description) {
        return new MultiblockRequirement(role, predicate, description, true);
    }
}
