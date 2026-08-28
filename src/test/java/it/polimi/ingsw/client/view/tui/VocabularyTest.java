package it.polimi.ingsw.client.view.tui;

import it.polimi.ingsw.common.game.GamePhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That what the terminal accepts and what it says it accepts are the same set of words.
 *
 * <p>They were written separately: the aliases a handler matched on, and the form {@link Help}
 * printed, in different files with nothing tying them together. A verb could be accepted and
 * undocumented — a command only somebody reading the source would ever find — or documented and
 * unreachable, which is worse, because a player types it and is told it is not something they
 * can do. Neither shows up in ordinary use.
 *
 * <p>So both are read here and compared. The declaration in {@code Help} carries the aliases
 * now, and this walks the sources that match them.
 *
 * <p>Components involved: {@link Verb}, {@link Help}, {@link TextInterface},
 * {@link FlightAnswers}.
 */
class VocabularyTest {

    /** The files that turn a typed word into something the game does. */
    private static final List<Path> MATCHERS = List.of(
            Path.of("src/main/java/it/polimi/ingsw/client/view/tui/TextInterface.java"),
            Path.of("src/main/java/it/polimi/ingsw/client/view/tui/FlightAnswers.java"));

    /** A `typed.is("a", "b")` call, with its literals. */
    private static final Pattern MATCHED =
            Pattern.compile("typed\\.is\\(\\s*((?:\"[a-z]+\"\\s*,?\\s*)+)\\)");

    private static final Pattern LITERAL = Pattern.compile("\"([a-z]+)\"");

    private static Set<String> declared() {
        Set<String> words = new HashSet<>(namesIn(Help.always()));
        words.addAll(namesIn(Help.inTheLobbyVerbs()));
        for (GamePhase phase : GamePhase.values()) {
            words.addAll(namesIn(Help.verbsDuring(phase)));
        }
        return words;
    }

    private static List<String> namesIn(List<Verb> verbs) {
        return verbs.stream().flatMap(verb -> verb.aliases().stream()).toList();
    }

    private static Set<String> accepted() {
        Set<String> words = new TreeSet<>();
        for (Path source : MATCHERS) {
            String text = read(source);
            Matcher call = MATCHED.matcher(text);
            while (call.find()) {
                Matcher literal = LITERAL.matcher(call.group(1));
                while (literal.find()) {
                    words.add(literal.group(1));
                }
            }
        }
        return words;
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + source, unreadable);
        }
    }

    @Nested
    @DisplayName("the words the terminal accepts")
    class WhatIsAccepted {

        @Test
        @DisplayName("are all words the help declares")
        void nothingIsAcceptedUndocumented() {
            List<String> undocumented = new ArrayList<>(accepted());
            undocumented.removeAll(declared());

            // A command that works and is not in the help is one only somebody reading the
            // source will ever find.
            assertEquals(List.of(), undocumented,
                    "these words are matched by the terminal and appear in no help line");
        }

        @Test
        @DisplayName("and the scan found some, so an empty comparison would prove nothing")
        void theScanActuallyFindsWords() {
            assertTrue(accepted().size() > 20,
                    "if this drops to nothing the comparison above passes vacuously");
        }
    }

    @Nested
    @DisplayName("the words the help declares")
    class WhatIsDeclared {

        @ParameterizedTest
        @EnumSource(GamePhase.class)
        @DisplayName("carry at least one word that means them, in every phase")
        void everyVerbHasAWord(GamePhase phase) {
            Help.verbsDuring(phase).forEach(verb -> assertFalse(verb.aliases().isEmpty()));
        }

        @ParameterizedTest
        @EnumSource(value = GamePhase.class, names = "FLIGHT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("do not shadow one another inside a phase")
        void noTwoVerbsShareAWordInOnePhase(GamePhase phase) {
            List<String> words = namesIn(Help.verbsDuring(phase));

            // Two verbs answering to the same word in the same phase means one of them is
            // unreachable, and which one depends on the order the handler happens to test in.
            assertEquals(new TreeSet<>(words).size(), words.size(),
                    "a word means two things in " + phase + ": " + words);
        }

        @Test
        @DisplayName("except in the flight, where the outstanding question picks between them")
        void theFlightIsAllowedToShadow() {
            List<String> words = namesIn(Help.verbsDuring(GamePhase.FLIGHT));

            // `take` accepts an offer and also takes a shot; `give` hands over crew and also
            // leaves the route. Neither is ambiguous in practice, because the flight is not one
            // context: TextInterface consults the outstanding prompt before anything else, and
            // FlightAnswers has a handler per kind of question. Asserted rather than left
            // implicit, so that the exclusion above reads as a decision and not an oversight.
            assertTrue(words.size() > new TreeSet<>(words).size(),
                    "if the flight stops sharing words, fold it back into the rule above");
        }
    }
}
