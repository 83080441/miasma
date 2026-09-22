package dev.sdfg.mod.entity;

/**
 * Visual subtype of {@link WarpEntity}. Client-only effect changes; same entity type.
 */
public enum WarpSubtype {
    /** Static convex warp (no animation). */
    STATIC("static"),
    /** Animated whirlpool: spins and pulls the scene toward the center. */
    VORTEX("vortex"),
    /** Two small vortex nodes orbiting each other inside the same 1×1 block. */
    BINARY("binary"),
    /** Irregular living blob that mutates its silhouette inside the block. */
    AMOEBA("amoeba"),
    /** Virus body with spikes that extend and retract. */
    VIRUS("virus");

    private final String id;

    WarpSubtype(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public static WarpSubtype byId(String id) {
        if (id == null || id.isEmpty()) {
            return STATIC;
        }
        for (WarpSubtype subtype : values()) {
            if (subtype.id.equalsIgnoreCase(id)) {
                return subtype;
            }
        }
        return STATIC;
    }

    public static WarpSubtype byOrdinalSafe(int ordinal) {
        WarpSubtype[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return STATIC;
        }
        return values[ordinal];
    }
}
