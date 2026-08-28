package it.polimi.ingsw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Asserts that the layers depend on each other in the one direction that was agreed.
 *
 * <p>Architecture § 2 says {@code common} is shared by both sides and holds no game state
 * and no authority. That is worth nothing as prose: the first time somebody reaches into
 * {@code server.model} from a protocol message to save writing a projection, the client
 * quietly starts depending on the whole server and nobody notices until the jars are
 * built.
 *
 * <p>So the rule is a test. It reads the import lines of every source file and checks the
 * three arrows the design allows:
 *
 * <ul>
 *   <li>{@code common} imports neither side.</li>
 *   <li>{@code server} may import {@code common}, never {@code client}.</li>
 *   <li>{@code client} may import {@code common}, never {@code server}.</li>
 * </ul>
 *
 * <p>Reading imports is a blunt instrument — it cannot see a fully qualified name written
 * inline — but a fully qualified {@code it.polimi.ingsw.server.…} inside a client file is
 * not something that happens by accident, and the compiler catches the rest.
 *
 * <p>Components involved: every type under {@link it.polimi.ingsw.common}, {@link
 * it.polimi.ingsw.server} and {@link it.polimi.ingsw.client}.
 */
class LayeringTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final String ROOT = "it.polimi.ingsw.";

    private static final Pattern REFERENCE =
            Pattern.compile("^\\s*import\\s+(?:static\\s+)?(it\\.polimi\\.ingsw\\.[\\w.]+)",
                    Pattern.MULTILINE);

    @Test
    @DisplayName("common depends on neither side")
    void commonIsBelowBothSides() {
        assertNoReferences("common", Set.of("server", "client"));
    }

    @Test
    @DisplayName("the server never reaches into the client")
    void serverIgnoresTheClient() {
        assertNoReferences("server", Set.of("client"));
    }

    @Test
    @DisplayName("the client never reaches into the server")
    void clientIgnoresTheServer() {
        assertNoReferences("client", Set.of("server"));
    }

    private void assertNoReferences(String layer, Set<String> forbidden) {
        Map<String, List<String>> offences = new TreeMap<>();
        for (Path source : sourcesIn(layer)) {
            List<String> bad = referencesOf(source).stream()
                    .filter(imported -> forbidden.contains(layerOf(imported)))
                    .sorted()
                    .toList();
            if (!bad.isEmpty()) {
                offences.put(SOURCES.relativize(source).toString(), bad);
            }
        }
        assertEquals(Map.of(), offences,
                layer + " may not depend on " + forbidden + ", but these files do");
    }

    private static List<Path> sourcesIn(String layer) {
        Path root = SOURCES.resolve(ROOT.replace('.', '/')).resolve(layer);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> tree = Files.walk(root)) {
            return tree.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException problem) {
            throw new UncheckedIOException("cannot read the " + layer + " sources", problem);
        }
    }

    private static List<String> referencesOf(Path source) {
        String text;
        try {
            text = Files.readString(source);
        } catch (IOException problem) {
            throw new UncheckedIOException("cannot read " + source, problem);
        }
        Matcher matcher = REFERENCE.matcher(text);
        return matcher.results().map(result -> result.group(1)).toList();
    }

    private static String layerOf(String qualifiedName) {
        String rest = qualifiedName.substring(ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
