package it.polimi.ingsw.client.view.tui;

import java.util.List;

/**
 * Something a player can type, and what it does.
 *
 * <p>The aliases and the description used to be written separately — the words a handler
 * matched on in {@code TextInterface}, and the form {@link Help} printed, declared in different
 * files with nothing tying them together. A verb could be accepted and undocumented, or
 * documented and unreachable, and neither shows up until somebody types the wrong thing and is
 * told it is not something they can do.
 *
 * <p>So a verb carries both, and {@code VocabularyTest} reads the declaration and the sources
 * that match it and compares the two sets in both directions.
 *
 * <p><b>Why the handlers still match on literals.</b> A verb that the handlers called directly
 * — {@code Help.RETURN_TO_PILE.matches(typed)} — would make divergence impossible rather than
 * merely detectable, which is the better property. It would also replace thirty readable words
 * with thirty constant references inside a terminal that is finished and working, for no
 * behavioural gain. The check is the proportionate answer, and it is only worth anything
 * because it runs both ways: a word matched and undeclared, and a word declared and matched by
 * nothing, both fail it.
 *
 * @param aliases every word that means this, first one preferred
 * @param form    how it reads in the help, arguments included
 * @param meaning what it does, in a few words
 */
record Verb(List<String> aliases, String form, String meaning) {

    /**
     * Validates the verb and takes a defensive copy.
     *
     * @throws IllegalArgumentException if it has no aliases, since a verb nothing matches is
     *                                  a line of help pointing at a command that does not exist
     */
    Verb {
        aliases = List.copyOf(aliases);
        if (aliases.isEmpty()) {
            throw new IllegalArgumentException(form + " has no word that means it");
        }
    }

    /**
     * Declares a verb.
     *
     * @param form    how it reads in the help
     * @param meaning what it does
     * @param aliases the words that mean it
     * @return the verb
     */
    static Verb of(String form, String meaning, String... aliases) {
        return new Verb(List.of(aliases), form, meaning);
    }

    /**
     * Returns this verb as one line of help.
     *
     * @return the line, padded so a column of them lines up
     */
    String rendered() {
        return "  " + String.format("%-26s", form) + meaning;
    }
}
