package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.controller.GameController;
import it.polimi.ingsw.server.model.game.Games;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where everybody is, asked directly.
 *
 * <p>These used to be four maps inside {@code Lobby}, and the only way to ask them anything was
 * to hold a whole conversation over a channel with a desk in front of them. So the invariants
 * that mattered most were never asserted at all — they were sentences in comments, and the
 * tests that happened to depend on them did so by accident, several layers up.
 *
 * <p>Two of them are the point of this suite: that a seat outlives the connection that left it,
 * which is the whole of how somebody comes back; and that whether anybody is left at a game is
 * answered from these books rather than by asking the game, which would race with the game's
 * own queue.
 *
 * <p>A {@code Connection} here is built with nothing in it. The roster only ever treats one as
 * an identity — something to key a table by and hand back — and giving it a real channel would
 * be furnishing a thing the module never looks inside.
 *
 * <p>Components involved: {@link Roster}, {@link Connection}, {@link PendingGame}.
 */
class RosterTest {

    private final Roster roster = new Roster();
    private final List<GameController> running = new ArrayList<>();

    @AfterEach
    void closeWhatWasOpened() {
        running.forEach(GameController::close);
        running.clear();
    }

    private static Connection somebody() {
        return new Connection(null, null);
    }

    private static PendingGame aTableFor(int seats) {
        return new PendingGame("game-1", GameLevel.LEVEL_II, seats);
    }

    /** A real controller, because the roster asks one for its seats. */
    private GameController aGame() {
        GameController controller = new GameController(Games.levelTwo());
        running.add(controller);
        return controller;
    }

    @Nested
    @DisplayName("names")
    class Names {

        @Test
        @DisplayName("a name is free until somebody takes it, and free again once they go")
        void namesAreTakenAndGivenBack() {
            Connection who = somebody();
            assertFalse(roster.isTaken("samuele"));

            roster.name("samuele", who);
            assertTrue(roster.isTaken("samuele"));

            roster.release("samuele", who);
            assertFalse(roster.isTaken("samuele"));
        }

        @Test
        @DisplayName("and a late notice from a connection already replaced releases nobody")
        void aLateNoticeDoesNotReleaseSomebodyElse() {
            Connection first = somebody();
            Connection second = somebody();
            roster.name("samuele", first);
            roster.name("samuele", second);

            roster.release("samuele", first);

            // The first connection dropping after the second took the name is exactly the
            // reconnection case, and releasing here would take the name off the player who
            // just came back.
            assertTrue(roster.isTaken("samuele"));
        }
    }

    @Nested
    @DisplayName("tables")
    class Tables {

        @Test
        @DisplayName("a connection's table is remembered, not searched for")
        void aTableIsFoundByIndex() {
            PendingGame table = aTableFor(4);
            Connection who = somebody();
            roster.open(table);

            roster.join(table, who);

            assertEquals(table, roster.tableOf(who).orElseThrow());
            assertEquals(List.of("game-1"), roster.tablesWaiting());
        }

        @Test
        @DisplayName("leaving the last seat forgets the table")
        void anEmptyTableIsForgotten() {
            PendingGame table = aTableFor(4);
            Connection who = somebody();
            roster.open(table);
            roster.join(table, who);

            assertTrue(roster.leave(table, who));

            assertTrue(roster.tableOf(who).isEmpty());
            assertEquals(List.of(), roster.tablesWaiting());
        }

        @Test
        @DisplayName("closing one takes everybody at it off the books")
        void closingATableClearsItsPlayers() {
            PendingGame table = aTableFor(4);
            Connection one = somebody();
            Connection two = somebody();
            roster.open(table);
            roster.join(table, one);
            roster.join(table, two);

            roster.close(table);

            assertEquals(List.of(), roster.tablesWaiting());
            assertTrue(roster.tableOf(one).isEmpty());
            assertTrue(roster.tableOf(two).isEmpty());
        }

        @Test
        @DisplayName("only tables with room are worth offering")
        void aFullTableIsNotOffered() {
            PendingGame table = aTableFor(2);
            roster.open(table);
            roster.join(table, somebody());
            assertEquals(1, roster.withRoom().size());

            roster.join(table, somebody());

            assertEquals(List.of(), roster.withRoom());
        }
    }

    @Nested
    @DisplayName("the two rules that used to be comments")
    class TheRules {

        @Test
        @DisplayName("a seat outlives the connection that left it")
        void aSeatOutlivesItsConnection() {
            GameController game = aGame();
            Connection who = somebody();
            String nickname = game.seats().get(0).nickname();
            roster.name(nickname, who);
            roster.started(game);

            roster.release(nickname, who);

            // The whole of how somebody comes back: the name is free, and the seat is still
            // held under it. Releasing the seat too would make reconnection impossible and
            // would need a second path to put it back.
            assertFalse(roster.isTaken(nickname));
            assertSame(game, roster.seatOf(nickname).orElseThrow().controller());
        }

        @Test
        @DisplayName("whether anybody is left is answered from here, not by asking the game")
        void emptinessIsAnsweredFromTheBooks() {
            GameController game = aGame();
            List<Connection> players = new ArrayList<>();
            game.seats().forEach(seat -> {
                Connection who = somebody();
                players.add(who);
                roster.name(seat.nickname(), who);
            });
            roster.started(game);

            roster.release(game.seats().get(0).nickname(), players.get(0));
            assertTrue(roster.anybodyLeftAt(game), "one of them is still connected");

            roster.release(game.seats().get(1).nickname(), players.get(1));

            // Asking the game would race with its own queue: the second player can hang up
            // before the game has heard about the first, and the table would look occupied
            // when nobody is in it.
            assertFalse(roster.anybodyLeftAt(game));
        }
    }

    @Nested
    @DisplayName("games")
    class Games_ {

        @Test
        @DisplayName("a game and its seats become known together")
        void startingRegistersBoth() {
            GameController game = aGame();

            roster.started(game);

            assertEquals(List.of(game.game().id()), roster.gamesRunning());
            game.seats().forEach(seat ->
                    assertTrue(roster.seatOf(seat.nickname()).isPresent()));
        }

        @Test
        @DisplayName("and a closing desk forgets both, not just the games")
        void forgettingEverythingLeavesNoSeatBehind() {
            GameController game = aGame();
            roster.started(game);

            roster.forgetEverything();

            // started() promises a game and its seats become known together. Clearing only the
            // games left seats pointing at a game the roster had never heard of — exactly the
            // state that promise says nothing should be able to observe.
            assertEquals(List.of(), roster.gamesRunning());
            game.seats().forEach(seat ->
                    assertTrue(roster.seatOf(seat.nickname()).isEmpty()));
        }

        @Test
        @DisplayName("and are forgotten together")
        void reclaimingRemovesBoth() {
            GameController game = aGame();
            roster.started(game);

            roster.reclaim(game);

            assertEquals(List.of(), roster.gamesRunning());
            game.seats().forEach(seat ->
                    assertTrue(roster.seatOf(seat.nickname()).isEmpty()));
        }
    }
}
