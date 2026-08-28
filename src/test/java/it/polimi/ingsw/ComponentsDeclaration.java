package it.polimi.ingsw;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the "components involved" clause out of one source file.
 *
 * <p>Separated from {@link TestDocumentationTest} so that the reading can be tested against
 * strings rather than against whatever the tree happens to contain. Both of the holes this
 * class exists to close got through the first version precisely because the only inputs it
 * ever saw were files that already passed: a top-level class with no Javadoc slipped by on a
 * {@code @Nested} class's clause, and a clause naming nothing passed on a {@code @link}
 * further down the comment.
 *
 * <p>Not a test class — it holds no test — so the rule it implements does not apply to it.
 */
final class ComponentsDeclaration {

    /** The form the suite uses, and the only one recognised. */
    private static final String CLAUSE = "Components involved:";

    /**
     * The Javadoc immediately before a type declared at the left margin.
     *
     * <p>Anchored to column zero, which is what makes it the <em>top-level</em> type: a
     * {@code @Nested} class is indented, so its comment can no longer stand in for a file that
     * has none of its own.
     */
    private static final Pattern TOP_LEVEL = Pattern.compile(
            "(/\\*\\*(?:[^*]|\\*(?!/))*\\*/)\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*"
                    + "^(?:public\\s+|final\\s+|abstract\\s+|sealed\\s+)*"
                    + "(?:class|interface|enum|record)\\s+\\w+",
            Pattern.DOTALL | Pattern.MULTILINE);

    private static final Pattern LINK = Pattern.compile("\\{@link\\s+([\\w.]+)");

    private static final Pattern IMPORT =
            Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+);", Pattern.MULTILINE);

    private static final Pattern PACKAGE =
            Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE);

    private ComponentsDeclaration() {
    }

    /**
     * Returns the Javadoc of the file's top-level type.
     *
     * @param source the whole file
     * @return the comment, or empty when the top-level type has none
     */
    static Optional<String> topLevelJavadoc(String source) {
        Matcher found = TOP_LEVEL.matcher(source);
        return found.find() ? Optional.of(found.group(1)) : Optional.empty();
    }

    /**
     * Returns the components a file names, which is empty when it names none.
     *
     * <p>Only what is linked <em>inside the clause</em> counts. Anything further down the
     * comment belongs to a different sentence, and letting it count is how
     * {@code Components involved: none worth naming.} passed.
     *
     * @param source the whole file
     * @return the linked names, in the order they appear
     */
    static List<String> namedBy(String source) {
        Optional<String> javadoc = topLevelJavadoc(source);
        if (javadoc.isEmpty()) {
            return List.of();
        }
        int at = javadoc.get().indexOf(CLAUSE);
        if (at < 0) {
            return List.of();
        }
        String clause = javadoc.get().substring(at + CLAUSE.length());
        // One clause, ending where the paragraph does.
        int ends = clause.indexOf("<p>");
        Matcher links = LINK.matcher(ends < 0 ? clause : clause.substring(0, ends));
        List<String> named = new ArrayList<>();
        while (links.find()) {
            named.add(links.group(1));
        }
        return List.copyOf(named);
    }

    /**
     * Tells whether a linked name is one Javadoc could actually resolve from this file.
     *
     * <p>The point of asking for {@code @link} rather than a bare word was that the name
     * would be checked. The first version checked it against every file stem in the project,
     * which is not the same thing at all: {@code GameController} named from a file that never
     * imports it passed, and Javadoc would not have resolved it. So this resolves the way
     * Javadoc does — same package, explicit import, or written out in full.
     *
     * @param link   the name as written
     * @param source the file it was written in
     * @return {@code true} when the link would resolve
     */
    static boolean resolves(String link, String source) {
        if (link.contains(".")) {
            // Written out in full: the JDK's own types, or something of ours that exists.
            return link.startsWith("java.") || link.startsWith("javax.")
                    || Known.TYPES.contains(link) || Known.PACKAGES.contains(link);
        }
        // Same package resolves without an import, and a test shares its package name with
        // the code it tests — which is how ShipTest names Ship without importing it. The
        // package is the one the file declares, not the folder it sits in, because main and
        // test are two trees with one set of package names between them.
        if (Known.TYPES.contains(packageOf(source) + "." + link)) {
            return true;
        }
        return importsOf(source).stream()
                .anyMatch(imported -> imported.endsWith("." + link) || imported.equals(link));
    }

    /** Returns the package a file declares itself in. */
    static String packageOf(String source) {
        Matcher found = PACKAGE.matcher(source);
        return found.find() ? found.group(1) : "";
    }

    private static Set<String> importsOf(String source) {
        Matcher found = IMPORT.matcher(source);
        Set<String> imports = new LinkedHashSet<>();
        while (found.find()) {
            imports.add(found.group(1));
        }
        return imports;
    }

    /** Everything this project declares, read once. */
    static final class Known {

        /** Fully qualified type names. */
        static final Set<String> TYPES = SourceTree.typeNames();

        /** Packages that document themselves, and so can be linked. */
        static final Set<String> PACKAGES = SourceTree.documentedPackages();

        private Known() {
        }
    }
}
