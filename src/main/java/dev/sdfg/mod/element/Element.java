package dev.sdfg.mod.element;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.resources.Identifier;

/**
 * Catalog of elemental identities for the mod.
 * Stable string ids and numeric codes (1–8); see {@code plan/ELEMENT_README.md}.
 */
public enum Element {
    FIRE("fire", 1),
    WATER("water", 2),
    EARTH("earth", 3),
    WIND("wind", 4),
    LIGHT("light", 5),
    DARKNESS("darkness", 6),
    MIASMA("miasma", 7),
    AETHER("aether", 8);

    private final String id;
    private final int number;
    private final Identifier iconTexture;

    Element(String id, int number) {
        this.id = id;
        this.number = number;
        this.iconTexture = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/element/" + id + ".png");
    }

    public String id() {
        return this.id;
    }

    /** Stable numeric code: fire=1 … aether=8. */
    public int number() {
        return this.number;
    }

    /** 16×16 HUD icon under {@code assets/sdfg/textures/gui/element/<id>.png}. */
    public Identifier iconTexture() {
        return this.iconTexture;
    }

    /**
     * Resolves by string id (case-insensitive). Unknown or empty → {@code null}.
     */
    public static Element byId(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (Element element : values()) {
            if (element.id.equalsIgnoreCase(id)) {
                return element;
            }
        }
        return null;
    }

    /** Resolves by numeric code 1–8. Out of range → {@code null}. */
    public static Element byNumber(int number) {
        for (Element element : values()) {
            if (element.number == number) {
                return element;
            }
        }
        return null;
    }

    public static Element byOrdinalSafe(int ordinal) {
        Element[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return null;
        }
        return values[ordinal];
    }
}
