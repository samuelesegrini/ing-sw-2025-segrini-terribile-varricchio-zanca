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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /**
     * A {@code typed.is("a", "b")} call, with its literals.
     *
     * <p>Any character but a quote. An earlier version asked for {@code [a-z]+}, which meant
     * {@code typed.is("help", "?")} matched nothing at all and neither word was ever seen —
     * so a whole call went unscanned and the comparison below passed by not looking.
     */
    private static final Pattern MATCHED =
            Pattern.compile("typed\\.is\\(\\s*((?:\"[^\"]+\"\\s*,?\\s*)+)\\)");

    private static final Pattern LITERAL = Pattern.compile("\"([^\"]+)\"");

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

        @Test
        @DisplayName("and it sees words that are not letters, which it once did not")
        void theScanSeesSymbols() {
            // typed.is("help", "?") was invisible to the first version of the pattern, and it
            // is the call that would have failed the reverse check below.
            assertTrue(accepted().contains("?"),
                    "a verb spelled with a symbol is still a verb");
        }
    }

    @Nested
    @DisplayName("the other direction")
    class WhatIsDocumented {

        @Test
        @DisplayName("every word the help declares is one the terminal actually accepts")
        void nothingIsDocumentedAndUnreachable() {
            List<String> unreachable = new ArrayList<>(new TreeSet<>(declared()));
            unreachable.removeAll(accepted());

            // The worse of the two failures: a player reads the help, types the word, and is
            // told it is not something they can do.
            assertEquals(List.of(), unreachable,
                    "these words appear in a help line and nothing matches them");
        }
    }

    @Nested
    @DisplayName("the words the help declares")
    class WhatIsDeclared {

        @Test
        @DisplayName("cannot be declared without a word that means them")
        void averbWithoutAWordIsRefused() {
            // The previous version of this walked the declarations asserting none was empty,
            // which the compact constructor already makes impossible — it could not fail.
            assertThrows(IllegalArgumentException.class,
                    () -> Verb.of("mystery", "does something"));
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
