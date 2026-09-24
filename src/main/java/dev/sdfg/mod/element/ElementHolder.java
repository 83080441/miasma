package dev.sdfg.mod.element;

import java.util.Optional;
import java.util.Set;

/**
 * Minimal association API: mod content can declare which {@link Element}(s) it carries.
 * No side effects — combat, tags, and vanilla mapping come later.
 */
@FunctionalInterface
public interface ElementHolder {
    /**
     * Primary element, or empty if this content has no elemental affinity.
     */
    Optional<Element> getElement();

    /**
     * Same as {@link #getElement()} for single-affinity content.
     * Override when a piece of content has several elements.
     */
    default Optional<Element> getPrimaryElement() {
        return getElement();
    }

    /**
     * All elements with a positive amount on this content. Default: from {@link #getElementAmounts()}.
     */
    default Set<Element> getElements() {
        return getElementAmounts().present();
    }

    /**
     * Numeric amounts per element. Default: primary at 1, or empty.
     */
    default ElementAmounts getElementAmounts() {
        return getPrimaryElement()
                .map(e -> ElementAmounts.of(e, 1))
                .orElseGet(ElementAmounts::empty);
    }

    /** Content with no elemental affinity. */
    static ElementHolder none() {
        return Optional::empty;
    }

    /** Content with a single primary element (amount 1). */
    static ElementHolder of(Element element) {
        return of(element, 1);
    }

    /** Content with a single element and amount. */
    static ElementHolder of(Element element, int amount) {
        if (element == null || amount <= 0) {
            return none();
        }
        ElementAmounts amounts = ElementAmounts.of(element, amount);
        return new ElementHolder() {
            @Override
            public Optional<Element> getElement() {
                return Optional.of(element);
            }

            @Override
            public ElementAmounts getElementAmounts() {
                return ElementAmounts.copyOf(amounts);
            }
        };
    }

    static ElementHolder of(ElementAmounts amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return none();
        }
        ElementAmounts copy = ElementAmounts.copyOf(amounts);
        return new ElementHolder() {
            @Override
            public Optional<Element> getElement() {
                return copy.primary();
            }

            @Override
            public ElementAmounts getElementAmounts() {
                return ElementAmounts.copyOf(copy);
            }
        };
    }
}
