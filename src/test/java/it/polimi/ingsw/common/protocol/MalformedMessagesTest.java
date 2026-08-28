package it.polimi.ingsw.common.protocol;

import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.common.game.ShipAttributes;
import it.polimi.ingsw.common.game.ValidationReport;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.GameSummary;
import it.polimi.ingsw.common.protocol.view.GameView;
import it.polimi.ingsw.common.protocol.view.PlayerView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a message which does not make sense cannot be built.
 *
 * <p>These guards are the first thing a hostile or broken client meets, and they are worth
 * having in the record rather than in the controller. A {@code CreateGame} for seven players
 * or a {@code KeepPiece} that keeps nothing is not a rule violation to be reported back
 * politely — it is a message that should never have existed, and refusing it at construction
 * means no later code has to wonder.
 *
 * <p>They also matter on the receiving end. A record is rebuilt through its canonical
 * constructor when it is deserialized, so these run again on everything that arrives over a
 * socket or an RMI connection. The guard a client skipped, the server applies.
 *
 * <p>Components involved: {@link Command}, {@link Event}, {@link Envelope}.
 */
class MalformedMessagesTest {

    private static final Position CELL = new Position(2, 3);

    private static void refuses(Class<? extends Throwable> expected, Executable message) {
        assertThrows(expected, message::run);
    }

    /** A message construction that is expected to fail. */
    private interface Executable {

        /**
         * Builds the message.
         */
        void run();
    }

    @Nested
    @DisplayName("commands")
    class Commands {

        @Test
        @DisplayName("a player cannot be nameless")
        void namelessPlayer() {
            refuses(IllegalArgumentException.class, () -> new LobbyCommand.Login("  "));
            refuses(IllegalArgumentException.class, () -> new LobbyCommand.Login(null));
        }

        @Test
        @DisplayName("a game seats two to four, and the record says so before the lobby does")
        void impossibleTableSizes() {
            refuses(IllegalArgumentException.class,
                    () -> new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 1));
            refuses(IllegalArgumentException.class,
                    () -> new LobbyCommand.CreateGame(GameLevel.LEVEL_II, 5));
            refuses(NullPointerException.class, () -> new LobbyCommand.CreateGame(null, 2));
        }

        @Test
        @DisplayName("joining, taking and scouting all need something to name")
        void nothingToName() {
            refuses(IllegalArgumentException.class, () -> new LobbyCommand.JoinGame(" "));
            refuses(IllegalArgumentException.class, () -> new BuildingCommand.TakeFaceUp(""));
            refuses(IllegalArgumentException.class, () -> new BuildingCommand.TakeReserved(null));
            refuses(IllegalArgumentException.class, () -> new BuildingCommand.ScoutPile(-1));
        }

        @Test
        @DisplayName("a placement needs both a cell and a rotation")
        void halfAPlacement() {
            refuses(NullPointerException.class,
                    () -> new BuildingCommand.PlaceInHand(null, Rotation.NONE));
            refuses(NullPointerException.class,
                    () -> new BuildingCommand.PlaceInHand(CELL, null));
            refuses(NullPointerException.class,
                    () -> new BuildingCommand.AdjustPlacement(null, Rotation.NONE));
            refuses(NullPointerException.class,
                    () -> new BuildingCommand.AdjustPlacement(CELL, null));
        }

        @Test
        @DisplayName("keeping nothing is not one of the choices a broken ship offers")
        void keepingNothing() {
            refuses(IllegalArgumentException.class,
                    () -> new PreparationCommand.KeepPiece(Set.of()));
            refuses(NullPointerException.class,
                    () -> new PreparationCommand.RemoveComponent(null));
            refuses(NullPointerException.class,
                    () -> new PreparationCommand.BoardCrew(null, AlienColor.BROWN));
            refuses(NullPointerException.class, () -> new FlightCommand.Answer(null));
        }
    }

    @Nested
    @DisplayName("events")
    class Events {

        @Test
        @DisplayName("an event about a player names one")
        void anonymousEvents() {
            refuses(IllegalArgumentException.class, () -> new LobbyEvent.LoggedIn(" "));
            refuses(IllegalArgumentException.class, () -> new LobbyEvent.PlayerLeft(null));
            refuses(NullPointerException.class, () -> new LobbyEvent.JoinedGame(null, PlayerColor.RED));
            refuses(NullPointerException.class, () -> new LobbyEvent.JoinedGame("g", null));
            refuses(NullPointerException.class, () -> new LobbyEvent.PlayerEntered("a", null));
            refuses(NullPointerException.class, () -> new LobbyEvent.PlayerEntered(null, PlayerColor.RED));
            refuses(NullPointerException.class, () -> new GameEvent.ConnectionChanged(null, true));
        }

        @Test
        @DisplayName("a refusal says what was refused and why")
        void silentRefusal() {
            refuses(IllegalArgumentException.class, () -> new GameEvent.Rejected("Weld", " "));
            refuses(IllegalArgumentException.class, () -> new GameEvent.Rejected("", "because"));
            refuses(NullPointerException.class, () -> new GameEvent.StateChanged(null));
            refuses(NullPointerException.class, () -> new GameEvent.PhaseBegan(null));
        }

        @Test
        @DisplayName("two dice cannot come up one, or thirteen")
        void impossibleRolls() {
            refuses(IllegalArgumentException.class, () -> new FlightEvent.DiceRolled(1));
            refuses(IllegalArgumentException.class, () -> new FlightEvent.DiceRolled(13));
            assertEquals(2, new FlightEvent.DiceRolled(2).total());
            assertEquals(12, new FlightEvent.DiceRolled(12).total());
        }

        @Test
        @DisplayName("a flight event without its subject is refused")
        void incompleteFlightEvents() {
            refuses(NullPointerException.class, () -> new FlightEvent.CardRevealed(null));
            refuses(NullPointerException.class, () -> new FlightEvent.Awaiting(null));
            refuses(NullPointerException.class, () -> new FlightEvent.ShipMoved(null, 1, 2));
            refuses(NullPointerException.class,
                    () -> new FlightEvent.ThreatResolved(null, null, null));
            refuses(NullPointerException.class, () -> new FlightEvent.ShipRetired(null, "gave up"));
            refuses(IllegalArgumentException.class,
                    () -> new FlightEvent.ShipRetired(PlayerColor.RED, " "));
        }
    }

    @Nested
    @DisplayName("views")
    class Views {

        private final TileView tile = new TileView("cabin_UUUU", ComponentKind.CABIN,
                Rotation.NONE, Map.of());

        @Test
        @DisplayName("a tile view needs its identity")
        void anonymousTile() {
            refuses(NullPointerException.class,
                    () -> new TileView(null, ComponentKind.CABIN, Rotation.NONE, Map.of()));
            refuses(NullPointerException.class,
                    () -> new TileView("id", null, Rotation.NONE, Map.of()));
            refuses(NullPointerException.class,
                    () -> new TileView("id", ComponentKind.CABIN, null, Map.of()));
        }

        @Test
        @DisplayName("people and an alien never share a cabin")
        void mixedCabin() {
            refuses(IllegalArgumentException.class,
                    () -> new CellView(tile, 0, List.of(), 2, AlienColor.PURPLE));
        }

        @Test
        @DisplayName("a cell cannot hold a negative amount of anything")
        void negativeCell() {
            refuses(IllegalArgumentException.class,
                    () -> new CellView(tile, -1, List.of(), 0, null));
            refuses(IllegalArgumentException.class,
                    () -> new CellView(tile, 0, List.of(), -1, null));
            refuses(NullPointerException.class, () -> new CellView(null, 0, List.of(), 0, null));
        }

        @Test
        @DisplayName("a ship cannot have components welded outside its own outline")
        void componentsOffTheBoard() {
            refuses(IllegalArgumentException.class, () -> new ShipView(5, 7, 5, 4, Set.of(),
                    Map.of(CELL, new CellView(tile, 0, List.of(), 2, null)),
                    List.of(), List.of(), 0, new ShipAttributes(0, 0, 0), ValidationReport.legal()));
        }

        @Test
        @DisplayName("a ship board has a positive size and a non-negative history")
        void impossibleShipBoards() {
            refuses(IllegalArgumentException.class, () -> ship(0, 7, 0));
            refuses(IllegalArgumentException.class, () -> ship(5, 0, 0));
            refuses(IllegalArgumentException.class, () -> ship(5, 7, -1));
            refuses(NullPointerException.class, () -> new ShipView(5, 7, 5, 4, Set.of(), Map.of(),
                    List.of(), List.of(), 0, null, ValidationReport.legal()));
            refuses(NullPointerException.class, () -> new ShipView(5, 7, 5, 4, Set.of(), Map.of(),
                    List.of(), List.of(), 0, new ShipAttributes(0, 0, 0), null));
        }

        @Test
        @DisplayName("a player view needs a nickname, a colour and a ship")
        void incompletePlayer() {
            refuses(NullPointerException.class,
                    () -> new PlayerView(null, PlayerColor.RED, true, false, 0, ship(5, 7, 0)));
            refuses(NullPointerException.class,
                    () -> new PlayerView("a", null, true, false, 0, ship(5, 7, 0)));
            refuses(NullPointerException.class,
                    () -> new PlayerView("a", PlayerColor.RED, true, false, 0, null));
        }

        @Test
        @DisplayName("the shipyard cannot hold a negative amount of anything")
        void negativeShipyard() {
            refuses(IllegalArgumentException.class, () -> shipyard(-1, 0, 0));
            refuses(IllegalArgumentException.class, () -> shipyard(0, -1, 0));
            refuses(IllegalArgumentException.class, () -> shipyard(0, 0, -1));
        }

        @Test
        @DisplayName("a route has a length and a deck a size")
        void impossibleRoutes() {
            refuses(IllegalArgumentException.class,
                    () -> new FlightView(0, Map.of(), List.of(), null, 0));
            refuses(IllegalArgumentException.class,
                    () -> new FlightView(24, Map.of(), List.of(), null, -1));
        }

        @Test
        @DisplayName("a game view names its game, its level, its phase and its reader")
        void incompleteGameView() {
            refuses(NullPointerException.class, () -> game(null, GameLevel.LEVEL_II,
                    GamePhase.LOBBY, PlayerColor.RED));
            refuses(NullPointerException.class, () -> game("g", null,
                    GamePhase.LOBBY, PlayerColor.RED));
            refuses(NullPointerException.class, () -> game("g", GameLevel.LEVEL_II,
                    null, PlayerColor.RED));
            refuses(NullPointerException.class, () -> game("g", GameLevel.LEVEL_II,
                    GamePhase.LOBBY, null));
        }

        @Test
        @DisplayName("a listed game seats two to four, and says whether there is room")
        void lobbyListing() {
            refuses(IllegalArgumentException.class,
                    () -> new GameSummary("g", GameLevel.LEVEL_II, 1, List.of()));
            refuses(IllegalArgumentException.class,
                    () -> new GameSummary("g", GameLevel.LEVEL_II, 5, List.of()));
            refuses(NullPointerException.class,
                    () -> new GameSummary(null, GameLevel.LEVEL_II, 2, List.of()));
            refuses(NullPointerException.class,
                    () -> new GameSummary("g", null, 2, List.of()));

            assertTrue(new GameSummary("g", GameLevel.LEVEL_II, 3, List.of("a")).hasRoom());
            assertTrue(!new GameSummary("g", GameLevel.LEVEL_II, 2, List.of("a", "b")).hasRoom());
        }

        private ShipView ship(int rows, int columns, int lost) {
            return new ShipView(rows, columns, 5, 4, Set.of(), Map.of(), List.of(), List.of(), lost,
                    new ShipAttributes(0, 0, 0), ValidationReport.legal());
        }

        private BuildingView shipyard(int faceDown, int hourglassSpaces, long seconds) {
            return new BuildingView(faceDown, List.of(), null, null, List.of(), null,
                    hourglassSpaces, seconds, Set.of(), List.of());
        }

        private GameView game(String id, GameLevel level, GamePhase phase, PlayerColor you) {
            return new GameView(id, level, phase, you, List.of(), null, null, null, null);
        }
    }
}
