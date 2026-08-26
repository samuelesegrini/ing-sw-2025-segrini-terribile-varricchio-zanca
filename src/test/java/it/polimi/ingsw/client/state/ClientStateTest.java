package it.polimi.ingsw.client.state;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.GamePhase;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.FlightEvent;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.protocol.Messages;
import it.polimi.ingsw.common.protocol.view.GameSummary;
import it.polimi.ingsw.common.protocol.view.GameView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a client remembers what it was told and invents nothing.
 *
 * <p>The failure this is really guarding against is a client that starts keeping its own
 * running total of something — credits, whose turn it is, how many tiles are left — and drifts.
 * There is one test at the bottom that says so directly: throw away every narration event and
 * the picture is unchanged.
 */
class ClientStateTest {

    private final ClientState state = new ClientState();

    @Test
    @DisplayName("a client knows nothing until it is told")
    void beforeAnythingHappens() {
        assertTrue(state.nickname().isEmpty());
        assertTrue(state.colour().isEmpty());
        assertTrue(state.gameId().isEmpty());
        assertTrue(state.game().isEmpty());
        assertTrue(state.lastRefusal().isEmpty());
        assertTrue(state.lostConnection().isEmpty());
        assertEquals(List.of(), state.openGames());
        assertEquals(List.of(), state.narration());
        assertTrue(state.isConnected());
    }

    @Test
    @DisplayName("logging in, taking a seat, and being told the picture")
    void gettingToATable() {
        state.apply(new LobbyEvent.LoggedIn("samuele"));
        state.apply(new LobbyEvent.JoinedGame("game-1", PlayerColor.RED));
        state.apply(new GameEvent.StateChanged(Messages.state()));

        assertEquals("samuele", state.nickname().orElseThrow());
        assertEquals(PlayerColor.RED, state.colour().orElseThrow());
        assertEquals("game-1", state.gameId().orElseThrow());
        assertEquals(GamePhase.FLIGHT, state.game().orElseThrow().phase());
    }

    @Test
    @DisplayName("the newest picture replaces the last one rather than being merged into it")
    void statesReplace() {
        state.apply(new GameEvent.StateChanged(Messages.state()));
        GameView later = new GameView("game-1", GameLevel.LEVEL_II, GamePhase.SCORING,
                PlayerColor.RED, List.of(), null, null, null, null);

        state.apply(new GameEvent.StateChanged(later));

        assertEquals(later, state.game().orElseThrow());
        assertTrue(state.game().orElseThrow().flightIfAny().isEmpty(),
                "nothing survives from the old picture into the new one");
    }

    @Test
    @DisplayName("a refusal appears once in the narration, which is where a screen prints it from")
    void refusalsAreSaidOnce() {
        state.apply(new GameEvent.Rejected("Weld", "there is nothing waiting to be welded"));

        assertEquals("there is nothing waiting to be welded", state.lastRefusal().orElseThrow());
        assertEquals(1, state.narration().size());
        assertEquals(0, state.narrationAfter(1).size(),
                "a stream each event appears in exactly once is what 'say this once' wants");
    }

    @Test
    @DisplayName("a refusal is forgotten when something happens")
    void refusalsAreTransient() {
        state.apply(new GameEvent.Rejected("Weld", "there is nothing waiting to be welded"));

        state.apply(new GameEvent.StateChanged(Messages.state()));

        assertTrue(state.lastRefusal().isEmpty(),
                "a refusal is about something that did not happen, and stops being interesting "
                        + "the moment something does");
    }

    @Test
    @DisplayName("the games on offer are the last ones listed")
    void listings() {
        state.apply(new LobbyEvent.GamesListed(List.of(
                new GameSummary("game-1", GameLevel.LEVEL_II, 4, List.of("samuele")))));

        assertEquals(1, state.openGames().size());
        assertTrue(state.openGames().get(0).hasRoom());
    }

    @Test
    @DisplayName("everything is logged, including what is not acted on")
    void narrationIsKept() {
        state.apply(new FlightEvent.DiceRolled(7));
        state.apply(new FlightEvent.CardResolved());

        assertEquals(2, state.narration().size(),
                "a client that dropped events it did not understand would get quieter as the "
                        + "protocol grew");
        assertEquals(1, state.narrationAfter(1).size());
        assertEquals(List.of(), state.narrationAfter(9));
    }

    @Test
    @DisplayName("the log does not grow for ever")
    void theLogIsBounded() {
        for (int event = 0; event < 500; event++) {
            state.apply(new FlightEvent.DiceRolled(7));
        }

        assertEquals(200, state.narration().size());
    }

    @Test
    @DisplayName("a listener hears about every change")
    void listeners() {
        AtomicInteger changes = new AtomicInteger();
        state.onChange(changes::incrementAndGet);

        state.apply(new LobbyEvent.LoggedIn("samuele"));
        state.apply(new FlightEvent.DiceRolled(7));
        state.disconnected("the other end hung up");

        assertEquals(3, changes.get());
    }

    @Test
    @DisplayName("a lost connection is remembered, with what to tell the player")
    void losingTheConnection() {
        state.disconnected("the other end hung up");

        assertFalse(state.isConnected());
        assertEquals("the other end hung up", state.lostConnection().orElseThrow());
        assertTrue(state.lastRefusal().isEmpty(),
                "a lost connection is not a refused command");
    }

    @Test
    @DisplayName("throwing away every narration event changes nothing about the picture")
    void narrationIsNotLoadBearing() {
        // The protocol's central claim, checked from the client's side. If this ever fails,
        // some screen has started deriving from the log and will drift the first time a client
        // misses a message.
        ClientState told = new ClientState();
        ClientState toldLess = new ClientState();

        told.apply(new FlightEvent.CardRevealed(
                new it.polimi.ingsw.common.game.AdventureCardIdentity("pirates_lvl2",
                        it.polimi.ingsw.common.game.AdventureCardType.PIRATES,
                        it.polimi.ingsw.common.game.CardLevel.LEVEL_II, false)));
        told.apply(new FlightEvent.DiceRolled(7));
        told.apply(new GameEvent.StateChanged(Messages.state()));

        toldLess.apply(new GameEvent.StateChanged(Messages.state()));

        assertEquals(told.game(), toldLess.game(),
                "a client that heard only the state should have the same board as one that "
                        + "heard everything");
    }
}
