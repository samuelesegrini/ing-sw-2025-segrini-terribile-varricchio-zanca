package it.polimi.ingsw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Asserts that every test says what it tests and what it touches.
 *
 * <p>{@code requirements.pdf} § 3, among the things that carry marks: <em>"Il nome e i commenti
 * di ogni test dovranno chiaramente specificare le funzionalità testate e i componenti
 * coinvolti."</em> Two things, and the second was missing from more than half the suite — 44 of
 * 93 classes named the components they involve, and the rest described only the behaviour.
 *
 * <p>Prose cannot hold that. This is the same trick {@link LayeringTest} plays on the
 * architecture: read the sources and turn the rule into an assertion, so that the next test
 * class written without the line fails rather than quietly joining the other half.
 *
 * <p>It cannot judge whether the components named are the <em>right</em> ones. It can make
 * their absence impossible to miss, and it can check that the names are real: every
 * {@code @link} in the declaration has to resolve to a type this project actually has.
 * {@code javadoc:javadoc} would catch that for the main sources and does not run over the test
 * tree, so the check is made here instead rather than claimed and not made.
 *
 * <p>Components involved: the test tree itself.
 *
 * <p>Components involved: every test class in the tree, and {@link LayeringTest}, whose trick this borrows.
 */
class TestDocumentationTest {

    private static final Path TESTS = Path.of("src", "test", "java");

    /** The form the suite already uses, in the 44 classes that had it first. */
    private static final Pattern NAMES_ITS_COMPONENTS =
            Pattern.compile("Components involved:[^*]*(\\*[^/][^*]*)*?\\{@link", Pattern.DOTALL);

    /** A class Javadoc: the last block comment before the type declaration. */
    private static final Pattern CLASS_JAVADOC = Pattern.compile(
            "(/\\*\\*(?:[^*]|\\*(?!/))*\\*/)\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                    + "(?:public\\s+|final\\s+|abstract\\s+)*class\\s+\\w+", Pattern.DOTALL);

    /** Every type this project has, main and test, plus the packages that can be linked. */
    private static final java.util.Set<String> KNOWN_TYPES = knownTypes();

    @Test
    @DisplayName("every test class names the components it involves")
    void everyTestClassNamesItsComponents() {
        List<String> silent = testClasses()
                .filter(source -> !namesItsComponents(source))
                .map(source -> TESTS.relativize(source).toString())
                .sorted()
                .toList();

        assertEquals(List.of(), silent,
                "these test classes do not say which components they involve, which "
                        + "requirements.pdf § 3 asks for by name");
    }

    @Test
    @DisplayName("and names ones that exist, since nothing else checks these links")
    void everyNamedComponentIsReal() {
        java.util.Map<String, List<String>> invented = new java.util.TreeMap<>();
        testClasses().forEach(source -> {
            List<String> missing = componentsNamedBy(source).stream()
                    .filter(named -> !isReal(named))
                    .toList();
            if (!missing.isEmpty()) {
                invented.put(TESTS.relativize(source).toString(), missing);
            }
        });

        assertEquals(java.util.Map.of(), invented,
                "these declarations link to something this project does not have");
    }

    private static boolean isReal(String named) {
        String simple = named.substring(named.lastIndexOf('.') + 1);
        return KNOWN_TYPES.contains(named) || KNOWN_TYPES.contains(simple);
    }

    /** The names linked from one class's components declaration. */
    private static List<String> componentsNamedBy(Path source) {
        Matcher javadoc = CLASS_JAVADOC.matcher(read(source));
        if (!javadoc.find()) {
            return List.of();
        }
        String doc = javadoc.group(1);
        int at = doc.indexOf("Components involved:");
        if (at < 0) {
            return List.of();
        }
        Matcher links = Pattern.compile("\\{@link\\s+([\\w.]+)").matcher(doc.substring(at));
        List<String> named = new java.util.ArrayList<>();
        while (links.find()) {
            named.add(links.group(1));
        }
        return named;
    }

    private static java.util.Set<String> knownTypes() {
        java.util.Set<String> known = new java.util.HashSet<>();
        for (Path root : List.of(Path.of("src", "main", "java"), TESTS)) {
            try (Stream<Path> everything = Files.walk(root)) {
                everything.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> {
                            String file = path.getFileName().toString();
                            String stem = file.substring(0, file.length() - ".java".length());
                            if (stem.equals("package-info")) {
                                // A package is linkable exactly when it documents itself.
                                known.add(root.relativize(path.getParent()).toString()
                                        .replace(java.io.File.separatorChar, '.'));
                            } else {
                                known.add(stem);
                            }
                        });
            } catch (IOException unreadable) {
                throw new UncheckedIOException("cannot read " + root, unreadable);
            }
        }
        return java.util.Set.copyOf(known);
    }

    /**
     * Returns the files that are test classes.
     *
     * <p>By whether they hold a test rather than by what they are called. Fixtures like
     * {@code Ships} and {@code Messages} are not tests and have nothing to declare; abstract
     * contracts like {@code ChannelContract} hold tests and do, which a name-based rule would
     * have got backwards in both directions.
     */
    private static Stream<Path> testClasses() {
        try (Stream<Path> everything = Files.walk(TESTS)) {
            return everything.filter(path -> path.toString().endsWith(".java"))
                    .filter(TestDocumentationTest::holdsATest)
                    .toList()
                    .stream();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + TESTS, unreadable);
        }
    }

    private static boolean holdsATest(Path source) {
        String text = read(source);
        return text.contains("@Test") || text.contains("@ParameterizedTest");
    }

    private static boolean namesItsComponents(Path source) {
        Matcher javadoc = CLASS_JAVADOC.matcher(read(source));
        return javadoc.find() && NAMES_ITS_COMPONENTS.matcher(javadoc.group(1)).find();
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + source, unreadable);
        }
    }
}
