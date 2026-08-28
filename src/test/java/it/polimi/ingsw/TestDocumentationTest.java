package it.polimi.ingsw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Asserts that every test says what it tests and what it touches.
 *
 * <p>Section 3 of the requirements asks that the name and comments of every test clearly
 * specify the functionality tested <em>and the components involved</em> — <em>"Il nome e i
 * commenti di ogni test dovranno chiaramente specificare le funzionalità testate e i
 * componenti coinvolti"</em>. Two things, and the second was missing from more than half the
 * suite: 44 classes of 93 named the components they involve, and the rest described only the
 * behaviour.
 *
 * <p>Prose cannot hold that. This is the same trick {@link LayeringTest} plays on the
 * architecture — read the sources and turn the rule into an assertion, so the next class
 * written without the line fails rather than quietly joining the other half.
 *
 * <p>It cannot judge whether the components named are the <em>right</em> ones. It can make
 * their absence impossible to miss, and it can check the names would resolve: same package,
 * explicit import, or written out in full, which is what Javadoc itself would accept. An
 * earlier version compared the simple name against every file stem in the project and called
 * that checking, which passed links Javadoc could never have resolved.
 *
 * <p>Components involved: {@link ComponentsDeclaration}, {@link SourceTree}, and every test
 * class in the tree.
 */
class TestDocumentationTest {

    @Test
    @DisplayName("every test class names the components it involves")
    void everyTestClassNamesItsComponents() {
        List<String> silent = new ArrayList<>();
        for (Path source : testClasses()) {
            if (ComponentsDeclaration.namedBy(SourceTree.read(source)).isEmpty()) {
                silent.add(SourceTree.TESTS.relativize(source).toString());
            }
        }
        silent.sort(String::compareTo);

        assertEquals(List.of(), silent,
                "these test classes do not say which components they involve, which section 3 "
                        + "of the requirements asks for by name");
    }

    @Test
    @DisplayName("and names ones Javadoc could resolve, since nothing else checks these links")
    void everyNamedComponentWouldResolve() {
        Map<String, List<String>> unresolvable = new TreeMap<>();
        for (Path source : testClasses()) {
            String text = SourceTree.read(source);
            List<String> missing = ComponentsDeclaration.namedBy(text).stream()
                    .filter(named -> !ComponentsDeclaration.resolves(named, text))
                    .toList();
            if (!missing.isEmpty()) {
                unresolvable.put(SourceTree.TESTS.relativize(source).toString(), missing);
            }
        }

        assertEquals(Map.of(), unresolvable,
                "these declarations link to something that cannot be resolved from the file "
                        + "they are written in — import it, or write it out in full");
    }

    /**
     * Returns the files that are test classes.
     *
     * <p>By whether they hold a test rather than by what they are called. Fixtures like
     * {@code Ships} and {@code Messages} are not tests and have nothing to declare; abstract
     * contracts like {@code ChannelContract} hold tests and do, which a name-based rule would
     * have got backwards in both directions.
     */
    private static List<Path> testClasses() {
        return SourceTree.filesUnder(SourceTree.TESTS).stream()
                .filter(source -> {
                    String text = SourceTree.read(source);
                    return text.contains("@Test") || text.contains("@ParameterizedTest");
                })
                .toList();
    }
}
