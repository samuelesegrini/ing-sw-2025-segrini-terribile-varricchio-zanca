package it.polimi.ingsw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reading behind {@link TestDocumentationTest}, checked against strings.
 *
 * <p>Its first version was only ever run over files that already passed, and two ways of
 * satisfying it without satisfying it went unnoticed until somebody reviewed the change. Both
 * are pinned here as literal sources, which is the only way to hold a rule that is otherwise
 * only as strong as whatever the tree happens to contain.
 *
 * <p>Components involved: {@link ComponentsDeclaration}.
 */
class ComponentsDeclarationTest {

    @Nested
    @DisplayName("which Javadoc the clause has to be in")
    class WhereItLooks {

        @Test
        @DisplayName("a nested class's clause does not stand in for the file's own")
        void aNestedClassCannotSpeakForTheFile() {
            String source = """
                    package it.polimi.ingsw;

                    class Undocumented {

                        /**
                         * A nested class that does say.
                         *
                         * <p>Components involved: {@link Undocumented}.
                         */
                        class Inner {
                        }
                    }
                    """;

            // The first version searched the whole file for any class Javadoc and took the
            // first it found, so a file with nothing at the top passed on this comment.
            assertEquals(List.of(), ComponentsDeclaration.namedBy(source));
        }

        @Test
        @DisplayName("and the top-level class's own clause does")
        void theTopLevelClauseCounts() {
            String source = """
                    package it.polimi.ingsw;

                    /**
                     * Documented.
                     *
                     * <p>Components involved: {@link SourceTree}.
                     */
                    class Documented {
                    }
                    """;

            assertEquals(List.of("SourceTree"), ComponentsDeclaration.namedBy(source));
        }
    }

    @Nested
    @DisplayName("what counts as naming something")
    class WhatCounts {

        @Test
        @DisplayName("a link in a later paragraph does not rescue a clause that names nothing")
        void aLaterLinkDoesNotCount() {
            String source = """
                    package it.polimi.ingsw;

                    /**
                     * Something.
                     *
                     * <p>Components involved: none worth naming.
                     *
                     * <p>See {@link SourceTree} for the walk.
                     */
                    class Evasive {
                    }
                    """;

            // The first version asked only whether a link appeared somewhere after the words,
            // which this satisfies while saying nothing at all.
            assertEquals(List.of(), ComponentsDeclaration.namedBy(source));
        }

        @Test
        @DisplayName("and a file with no clause at all names nothing")
        void noClauseNamesNothing() {
            String source = """
                    package it.polimi.ingsw;

                    /**
                     * Says what it does and not what it touches.
                     */
                    class Silent {
                    }
                    """;

            assertEquals(List.of(), ComponentsDeclaration.namedBy(source));
        }
    }

    @Nested
    @DisplayName("whether a name would resolve")
    class Resolving {

        private static final String IN_THE_SHIP_PACKAGE = """
                package it.polimi.ingsw.server.model.ship;

                import it.polimi.ingsw.common.game.Position;
                """;

        @Test
        @DisplayName("a type in the file's own package needs no import")
        void samePackageResolves() {
            // How ShipGridTest names ShipGrid: the test shares the package name with the code
            // it tests, across two source trees.
            assertTrue(ComponentsDeclaration.resolves("ShipGrid", IN_THE_SHIP_PACKAGE));
        }

        @Test
        @DisplayName("an imported type resolves")
        void importedResolves() {
            assertTrue(ComponentsDeclaration.resolves("Position", IN_THE_SHIP_PACKAGE));
        }

        @Test
        @DisplayName("a type that is neither is refused, which is the whole point")
        void anUnimportedStrangerIsRefused() {
            // The first version compared the simple name against every file stem in the
            // project, so this passed and Javadoc would still not have resolved it.
            assertFalse(ComponentsDeclaration.resolves("GameController", IN_THE_SHIP_PACKAGE));
        }

        @Test
        @DisplayName("writing it out in full always resolves")
        void qualifiedResolves() {
            assertTrue(ComponentsDeclaration.resolves(
                    "it.polimi.ingsw.server.controller.GameController", IN_THE_SHIP_PACKAGE));
        }

        @Test
        @DisplayName("and so does the JDK, which is a component like any other")
        void theJdkResolves() {
            // Rejected outright by the first version, which knew only this project's files.
            assertTrue(ComponentsDeclaration.resolves("java.util.List", IN_THE_SHIP_PACKAGE));
        }

        @Test
        @DisplayName("but a name nothing declares does not")
        void anInventedNameIsRefused() {
            assertFalse(ComponentsDeclaration.resolves(
                    "it.polimi.ingsw.NotAThing", IN_THE_SHIP_PACKAGE));
        }
    }
}
