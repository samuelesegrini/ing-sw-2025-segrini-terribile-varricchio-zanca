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
 * <p>So a verb carries both. What matches it and what explains it are the same object, and
 * accepting a word the help does not mention now takes deliberate effort rather than
 * forgetfulness.
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
     * Tells whether a typed line is this verb.
     *
     * @param typed what somebody wrote
     * @return {@code true} when its first word is one of these aliases
     */
    boolean matches(Typed typed) {
        return typed.is(aliases.toArray(String[]::new));
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
