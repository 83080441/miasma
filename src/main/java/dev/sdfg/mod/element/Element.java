package dev.sdfg.mod.element;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.resources.Identifier;

/**
 * Catalog of elemental identities for the mod.
 * Stable string ids and numeric codes (1–8); see {@code plan/ELEMENT_README.md}.
 */
public enum Element {
    FIRE("fire", 1, 0xFF8C1A),
    WATER("water", 2, 0x3D9BE8),
    EARTH("earth", 3, 0x8B5E3C),
    WIND("wind", 4, 0xE6D3C4),
    LIGHT("light", 5, 0xFFF4A3),
    DARKNESS("darkness", 6, 0x6B3A9A),
    MIASMA("miasma", 7, 0x7A6488),
    AETHER("aether", 8, 0x22E7FF);

    private final String id;
    private final int number;
    /** Particle RGB (no alpha). See {@code plan/LIQUID_README.md}. */
    private final int color;
    private final Identifier iconTexture;

    Element(String id, int number, int color) {
        this.id = id;
        this.number = number;
        this.color = color;
        this.iconTexture = Identifier.fromNamespaceAndPath(ExampleMod.MODID, "textures/gui/element/" + id + ".png");
    }

    public String id() {
        return this.id;
    }

    /** Stable numeric code: fire=1 … aether=8. */
    public int number() {
        return this.number;
    }

    /** Mote color as {@code 0xRRGGBB}, matching the element icon. */
    public int color() {
        return this.color;
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
