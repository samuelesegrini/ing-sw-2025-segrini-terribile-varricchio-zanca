package it.polimi.ingsw;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The source files themselves, for the two tests that read them.
 *
 * <p>{@link LayeringTest} reads imports to check the layers point one way; {@link
 * TestDocumentationTest} reads Javadoc to check each test says what it touches. Both had their
 * own walk, their own {@code .endsWith(".java")} and their own {@code UncheckedIOException},
 * which is one copy too many of something neither of them is about.
 *
 * <p>Not a test class — it holds no test.
 */
final class SourceTree {

    /** Where the implementation lives. */
    static final Path MAIN = Path.of("src", "main", "java");

    /** Where the tests live. */
    static final Path TESTS = Path.of("src", "test", "java");

    private SourceTree() {
    }

    /**
     * Returns every Java source file under a root.
     *
     * @param root where to look
     * @return the files, in no particular order
     */
    static List<Path> filesUnder(Path root) {
        try (Stream<Path> everything = Files.walk(root)) {
            return everything.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + root, unreadable);
        }
    }

    /**
     * Reads one source file.
     *
     * @param source which file
     * @return its whole text
     */
    static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + source, unreadable);
        }
    }

    /**
     * Returns the fully qualified name of every type this project declares.
     *
     * @return the names, main and test alike
     */
    static Set<String> typeNames() {
        Set<String> names = new HashSet<>();
        for (Path root : List.of(MAIN, TESTS)) {
            for (Path source : filesUnder(root)) {
                String stem = stemOf(source);
                if (!stem.equals("package-info")) {
                    names.add(packageOf(root, source) + "." + stem);
                }
            }
        }
        return Set.copyOf(names);
    }

    /**
     * Returns the packages that carry a {@code package-info}, and so can be linked.
     *
     * @return their names
     */
    static Set<String> documentedPackages() {
        Set<String> packages = new HashSet<>();
        for (Path root : List.of(MAIN, TESTS)) {
            for (Path source : filesUnder(root)) {
                if (stemOf(source).equals("package-info")) {
                    packages.add(packageOf(root, source));
                }
            }
        }
        return Set.copyOf(packages);
    }

    private static String stemOf(Path source) {
        String file = source.getFileName().toString();
        return file.substring(0, file.length() - ".java".length());
    }

    private static String packageOf(Path root, Path source) {
        return root.relativize(source.getParent()).toString()
                .replace(java.io.File.separatorChar, '.');
    }
}
