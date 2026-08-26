package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.board.DeckComposition;
import it.polimi.ingsw.server.model.board.FlightBoardSpec;
import it.polimi.ingsw.server.model.board.RewardTable;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks how a finishing player gets their place on the route.
 *
 * <p>The two levels genuinely differ, and the difference is easy to miss because the
 * manual states each rule exactly once, sixty pages apart. A test flight hands spaces out
 * in finishing order (p.8); a level II flight lets the player pick any free one (p.17).
 * Implementing only the second would silently give test flight players a choice the rules
 * never offered them.
 *
 * <p>The other rule with teeth: spaces numbered above the player count are not in play at
 * all. In a two-player game only spaces 1 and 2 exist, however many the board prints.
 *
 * <p>Components involved: {@link StartSpaces}, {@link StartSpacePolicy}, {@link FlightBoardSpec}.
 */
class StartSpacesTest {

    /** The level II flight board: four start spaces at route offsets 6, 3, 1 and 0. */
    private static FlightBoardSpec levelTwoBoard() {
        return new FlightBoardSpec(24, List.of(6, 3, 1, 0), 3,
                new RewardTable(List.of(8, 6, 4, 2), 4, 1,
                        Map.of(GoodColor.RED, 4, GoodColor.YELLOW, 3,
                                GoodColor.GREEN, 2, GoodColor.BLUE, 1)),
                new DeckComposition(4, Map.of(CardLevel.LEVEL_I, 1, CardLevel.LEVEL_II, 2), false));
    }

    private static StartSpaces spaces(int playerCount, StartSpacePolicy policy) {
        return new StartSpaces(levelTwoBoard(), playerCount, policy);
    }

    @Nested
    @DisplayName("in a test flight, spaces go out in finishing order")
    class InFinishingOrder {

        @Test
        @DisplayName("the first to finish takes space 1, the second space 2, and so on")
        void spacesGoOutInOrder() {
            StartSpaces spaces = spaces(3, StartSpacePolicy.IN_FINISHING_ORDER);

            assertEquals(1, spaces.claim(PlayerColor.RED, OptionalInt.empty()));
            assertEquals(2, spaces.claim(PlayerColor.BLUE, OptionalInt.empty()));
            assertEquals(3, spaces.claim(PlayerColor.GREEN, OptionalInt.empty()));
        }

        @Test
        @DisplayName("choosing is refused, because this board offers no choice")
        void choosing_isRefused() {
            StartSpaces spaces = spaces(4, StartSpacePolicy.IN_FINISHING_ORDER);

            assertThrows(IllegalArgumentException.class,
                    () -> spaces.claim(PlayerColor.RED, OptionalInt.of(3)));
        }

        @Test
        @DisplayName("the space taken maps onto the route offset printed on the board")
        void spaceMapsOntoARouteOffset() {
            StartSpaces spaces = spaces(4, StartSpacePolicy.IN_FINISHING_ORDER);
            spaces.claim(PlayerColor.RED, OptionalInt.empty());
            spaces.claim(PlayerColor.BLUE, OptionalInt.empty());

            assertEquals(OptionalInt.of(6), spaces.routePositionOf(PlayerColor.RED));
            assertEquals(OptionalInt.of(3), spaces.routePositionOf(PlayerColor.BLUE));
            assertEquals(OptionalInt.empty(), spaces.routePositionOf(PlayerColor.GREEN));
        }
    }

    @Nested
    @DisplayName("in a level II flight, the player picks")
    class ChosenByPlayer {

        @Test
        @DisplayName("a player may take a space behind one that is still free")
        void playerMayTakeASpaceBehindAFreeOne() {
            StartSpaces spaces = spaces(4, StartSpacePolicy.CHOSEN_BY_PLAYER);

            assertEquals(3, spaces.claim(PlayerColor.RED, OptionalInt.of(3)));
            assertEquals(List.of(1, 2, 4), spaces.free());
        }

        @Test
        @DisplayName("a space someone has already taken is refused")
        void takenSpace_isRefused() {
            StartSpaces spaces = spaces(4, StartSpacePolicy.CHOSEN_BY_PLAYER);
            spaces.claim(PlayerColor.RED, OptionalInt.of(1));

            assertThrows(IllegalArgumentException.class,
                    () -> spaces.claim(PlayerColor.BLUE, OptionalInt.of(1)));
        }

        @Test
        @DisplayName("a player who declines to choose takes the best space left")
        void decliningToChoose_takesTheBestFreeSpace() {
            StartSpaces spaces = spaces(4, StartSpacePolicy.CHOSEN_BY_PLAYER);
            spaces.claim(PlayerColor.RED, OptionalInt.of(1));

            assertEquals(2, spaces.claim(PlayerColor.BLUE, OptionalInt.empty()));
        }
    }

    @Nested
    @DisplayName("spaces above the player count are not in play")
    class SpacesInPlay {

        @Test
        @DisplayName("a two-player game has only spaces 1 and 2, whatever the board prints")
        void twoPlayerGame_hasTwoSpaces() {
            StartSpaces spaces = spaces(2, StartSpacePolicy.CHOSEN_BY_PLAYER);

            assertEquals(List.of(1, 2), spaces.free());
            assertThrows(IllegalArgumentException.class,
                    () -> spaces.claim(PlayerColor.RED, OptionalInt.of(3)));
        }

        @Test
        @DisplayName("once every space in play is taken there is nothing left to claim")
        void spacesRunOut() {
            StartSpaces spaces = spaces(2, StartSpacePolicy.IN_FINISHING_ORDER);
            spaces.claim(PlayerColor.RED, OptionalInt.empty());
            spaces.claim(PlayerColor.BLUE, OptionalInt.empty());

            assertEquals(List.of(), spaces.free());
            assertThrows(IllegalStateException.class,
                    () -> spaces.claim(PlayerColor.GREEN, OptionalInt.empty()));
        }

        @Test
        @DisplayName("a game too big or too small for the board is refused outright")
        void impossiblePlayerCount_isRefused() {
            assertThrows(IllegalArgumentException.class,
                    () -> new StartSpaces(levelTwoBoard(), 1, StartSpacePolicy.CHOSEN_BY_PLAYER));
            assertThrows(IllegalArgumentException.class,
                    () -> new StartSpaces(levelTwoBoard(), 5, StartSpacePolicy.CHOSEN_BY_PLAYER));
        }
    }

    @Test
    @DisplayName("a player takes exactly one space, and asking twice is refused")
    void playerTakesOneSpaceOnly() {
        StartSpaces spaces = spaces(4, StartSpacePolicy.CHOSEN_BY_PLAYER);
        spaces.claim(PlayerColor.RED, OptionalInt.of(2));

        assertThrows(IllegalStateException.class,
                () -> spaces.claim(PlayerColor.RED, OptionalInt.of(3)));
        assertEquals(OptionalInt.of(2), spaces.spaceOf(PlayerColor.RED));
        assertEquals(Optional.of(PlayerColor.RED), spaces.occupantOf(2));
        assertEquals(Optional.empty(), spaces.occupantOf(1));
        assertEquals(1, spaces.claimedCount());
    }
}
