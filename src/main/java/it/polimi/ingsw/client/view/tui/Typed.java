package it.polimi.ingsw.client.view.tui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * One line somebody typed, taken apart.
 *
 * <p>A verb and its arguments, with the fiddly parts done once rather than at every call site:
 * extra spaces, a missing argument, a word where a number belongs. A screen asks for what it
 * needs and is told plainly when it is not there.
 *
 * @param verb what they want, in lower case
 * @param words everything after it
 */
public record Typed(String verb, List<String> words) {

    /** Nothing at all, which is what a bare Enter means. */
    public static final Typed NOTHING = new Typed("", List.of());

    /**
     * Takes a defensive copy.
     */
    public Typed {
        words = List.copyOf(words);
    }

    /**
     * Takes a line apart.
     *
     * @param line what was typed, possibly with stray spaces or nothing at all
     * @return the verb and its arguments
     */
    public static Typed of(String line) {
        if (line == null || line.isBlank()) {
            return NOTHING;
        }
        String[] parts = line.strip().split("\\s+");
        return new Typed(parts[0].toLowerCase(Locale.ROOT),
                List.of(parts).subList(1, parts.length));
    }

    /**
     * Tells whether nothing was typed.
     *
     * @return {@code true} for a blank line
     */
    public boolean isBlank() {
        return verb.isEmpty();
    }

    /**
     * Tells whether this is one of a set of words.
     *
     * @param verbs the words to match, in lower case
     * @return {@code true} if the verb is one of them
     */
    public boolean is(String... verbs) {
        for (String candidate : verbs) {
            if (verb.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns one argument.
     *
     * @param index which, counting from zero
     * @return the word, or empty if there was no such argument
     */
    public Optional<String> word(int index) {
        return index < words.size() ? Optional.of(words.get(index)) : Optional.empty();
    }

    /**
     * Returns one argument as a number.
     *
     * @param index which, counting from zero
     * @return the number, or empty if it is missing or is not one
     */
    public OptionalInt number(int index) {
        Optional<String> word = word(index);
        if (word.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(word.get()));
        } catch (NumberFormatException notANumber) {
            return OptionalInt.empty();
        }
    }

    /**
     * Returns everything after the verb, joined back together.
     *
     * <p>For the arguments that are prose rather than parameters — a nickname with a space in
     * it, say.
     *
     * @return the rest of the line
     */
    public String rest() {
        return String.join(" ", words);
    }
}
