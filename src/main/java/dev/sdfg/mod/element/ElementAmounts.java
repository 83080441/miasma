package dev.sdfg.mod.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.sdfg.mod.ExampleMod;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Per-element integer amounts (e.g. fire=1, water=2). Zero means absent;
 * when present, each amount is clamped to {@link #MIN_PRESENT}–{@link #MAX_AMOUNT} (1–100).
 * Indexing uses {@link Element#number()} (1–8).
 */
public final class ElementAmounts {
    /** Absent / cleared. */
    public static final int MIN_AMOUNT = 0;
    /** Minimum when an element is present. */
    public static final int MIN_PRESENT = 1;
    /** Cap per element. */
    public static final int MAX_AMOUNT = 100;

    /**
     * Datapack JSON shape: {@code { "elements": { "fire": 40, "light": 15 } }}.
     */
    public static final Codec<ElementAmounts> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("elements", Map.of())
                    .forGetter(ElementAmounts::toIdMap)
    ).apply(instance, map -> fromIdMap(map, true)));

    private static final int SLOT_COUNT = Element.values().length;

    private final int[] amounts;

    private ElementAmounts(int[] amounts) {
        if (amounts.length != SLOT_COUNT) {
            throw new IllegalArgumentException("ElementAmounts requires " + SLOT_COUNT + " slots");
        }
        this.amounts = amounts;
    }

    public static ElementAmounts empty() {
        return new ElementAmounts(new int[SLOT_COUNT]);
    }

    public static ElementAmounts of(Element element, int amount) {
        ElementAmounts result = empty();
        result.set(element, amount);
        return result;
    }

    public static ElementAmounts copyOf(ElementAmounts other) {
        return other == null ? empty() : new ElementAmounts(other.amounts.clone());
    }

    /**
     * Builds amounts from element id → quantity. Unknown ids are skipped (and logged if {@code logUnknown}).
     * Values ≤ 0 are ignored; positive values are clamped to {@link #MIN_PRESENT}–{@link #MAX_AMOUNT}.
     */
    public static ElementAmounts fromIdMap(Map<String, Integer> map, boolean logUnknown) {
        ElementAmounts result = empty();
        if (map == null || map.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            Element element = Element.byId(entry.getKey());
            if (element == null) {
                if (logUnknown) {
                    ExampleMod.LOGGER.warn("Unknown element id '{}' in element datapack entry; ignoring", entry.getKey());
                }
                continue;
            }
            Integer raw = entry.getValue();
            if (raw == null || raw <= 0) {
                continue;
            }
            result.set(element, raw);
        }
        return result;
    }

    /** Sparse map of present element ids → amounts (for codecs / JSON). */
    public Map<String, Integer> toIdMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Element element : Element.values()) {
            int amount = get(element);
            if (amount > 0) {
                map.put(element.id(), amount);
            }
        }
        return map;
    }

    /**
     * Deterministic random mix for Warp nodes: 1–3 distinct elements,
     * each with amount {@link #MIN_PRESENT}–{@link #MAX_AMOUNT} (1–100).
     */
    public static ElementAmounts randomForWarp(RandomSource random) {
        ElementAmounts result = empty();
        List<Element> pool = new ArrayList<>(List.of(Element.values()));
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(pool, i, j);
        }
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            result.set(pool.get(i), MIN_PRESENT + random.nextInt(MAX_AMOUNT));
        }
        return result;
    }

    public int get(Element element) {
        if (element == null) {
            return 0;
        }
        return this.amounts[element.number() - 1];
    }

    public void set(Element element, int amount) {
        if (element == null) {
            return;
        }
        this.amounts[element.number() - 1] = Mth.clamp(amount, MIN_AMOUNT, MAX_AMOUNT);
    }

    public void add(Element element, int delta) {
        if (element == null || delta == 0) {
            return;
        }
        set(element, get(element) + delta);
    }

    public boolean isEmpty() {
        for (int amount : this.amounts) {
            if (amount > 0) {
                return false;
            }
        }
        return true;
    }

    /** Non-zero entries as an unmodifiable map. */
    public Map<Element, Integer> nonZero() {
        EnumMap<Element, Integer> map = new EnumMap<>(Element.class);
        for (Element element : Element.values()) {
            int amount = get(element);
            if (amount > 0) {
                map.put(element, amount);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public Set<Element> present() {
        return nonZero().keySet();
    }

    /**
     * Element with the highest amount; ties broken by lower {@link Element#number()}.
     */
    public Optional<Element> primary() {
        Element best = null;
        int bestAmount = 0;
        for (Element element : Element.values()) {
            int amount = get(element);
            if (amount > bestAmount) {
                best = element;
                bestAmount = amount;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Compact sync form: {@code 1:3,2:1} (number:amount), empty if none. */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (Element element : Element.values()) {
            int amount = get(element);
            if (amount <= 0) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(element.number()).append(':').append(amount);
        }
        return sb.toString();
    }

    public static ElementAmounts decode(String encoded) {
        ElementAmounts result = empty();
        if (encoded == null || encoded.isEmpty()) {
            return result;
        }
        for (String part : encoded.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon >= trimmed.length() - 1) {
                continue;
            }
            try {
                int number = Integer.parseInt(trimmed.substring(0, colon).trim());
                int amount = Integer.parseInt(trimmed.substring(colon + 1).trim());
                Element element = Element.byNumber(number);
                if (element != null) {
                    result.set(element, amount);
                }
            } catch (NumberFormatException ignored) {
                // skip malformed segment
            }
        }
        return result;
    }

    public void write(ValueOutput output) {
        String encoded = encode();
        if (!encoded.isEmpty()) {
            output.putString("Elements", encoded);
        }
    }

    public static ElementAmounts read(ValueInput input) {
        return decode(input.getStringOr("Elements", ""));
    }

    /** HUD / debug: {@code fire 1, water 2}. */
    public String formatDisplay() {
        Map<Element, Integer> map = nonZero();
        if (map.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Element, Integer> entry : map.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(entry.getKey().id()).append(' ').append(entry.getValue());
        }
        return sb.toString();
    }
}
