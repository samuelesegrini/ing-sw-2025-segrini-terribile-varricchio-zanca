package it.polimi.ingsw.client.state;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Event;
import it.polimi.ingsw.common.protocol.GameEvent;
import it.polimi.ingsw.common.protocol.LobbyEvent;
import it.polimi.ingsw.common.protocol.view.GameSummary;
import it.polimi.ingsw.common.protocol.view.GameView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Everything a client knows, which is the last thing it was told and nothing else.
 *
 * <p>There is no game state here. There is a {@link GameView}, which is a copy of what the
 * server said was true a moment ago, and there is nothing computed from it that could later
 * disagree with it. Every question a screen has is either answered by that view or is a
 * question for the server.
 *
 * <p>That is the protocol's promise being taken literally rather than trusted. If a client can
 * be written this way then <em>"a client that ignores every narration event and reads only the
 * state is still correct"</em> is true; and if it could not, the protocol would be wrong and it
 * would be better to find out here than in a demonstration.
 *
 * <p>Narration is kept anyway, because a game told purely in board states is unreadable. It is
 * a log to print, never something to derive from — throw all of it away and the next state
 * still draws the same board.
 *
 * <p>Written from a transport's thread and read from whichever thread is drawing, so
 * everything here is behind one lock. The values handed out are immutable records, so nothing
 * escapes it.
 */
public final class ClientState {

    /** How much narration to keep. Enough to fill a screen twice over. */
    private static final int LOG_LENGTH = 200;

    private final Deque<Event> narration = new ArrayDeque<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private String nickname;
    private PlayerColor colour;
    private String gameId;
    private GameView game;
    private List<GameSummary> openGames = List.of();
    private String lastRefusal;
    private boolean connected = true;

    /**
     * Takes in one event.
     *
     * <p>Everything that is not recognised is still logged. A client that quietly dropped
     * events it did not understand would be a client that got quieter as the protocol grew.
     *
     * @param event what arrived
     */
    public synchronized void apply(Event event) {
        switch (event) {
            case LobbyEvent.LoggedIn loggedIn -> nickname = loggedIn.nickname();
            case LobbyEvent.GamesListed listed -> openGames = listed.games();
            case LobbyEvent.JoinedGame joined -> {
                gameId = joined.gameId();
                colour = joined.colour();
            }
            case GameEvent.StateChanged changed -> {
                game = changed.state();
                lastRefusal = null;
            }
            case GameEvent.Rejected rejected -> lastRefusal = rejected.reason();
            default -> {
                // Narration, and nothing to record beyond having heard it.
            }
        }
        remember(event);
        listeners.forEach(Runnable::run);
    }

    /**
     * Records that the connection has gone.
     *
     * @param reason what to show, for somebody who was in the middle of something
     */
    public synchronized void disconnected(String reason) {
        connected = false;
        lastRefusal = reason;
        listeners.forEach(Runnable::run);
    }

    private void remember(Event event) {
        narration.addLast(event);
        while (narration.size() > LOG_LENGTH) {
            narration.removeFirst();
        }
    }

    /**
     * Asks to be told when anything changes.
     *
     * <p>Called on the transport's thread, so a listener that draws a screen should hand the
     * work to whichever thread owns the terminal rather than doing it here.
     *
     * @param listener what to run
     */
    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * Returns the nickname the server accepted.
     *
     * @return the name, or empty before logging in
     */
    public synchronized Optional<String> nickname() {
        return Optional.ofNullable(nickname);
    }

    /**
     * Returns which player this client is.
     *
     * @return the colour, or empty before taking a seat
     */
    public synchronized Optional<PlayerColor> colour() {
        return Optional.ofNullable(colour);
    }

    /**
     * Returns which game this client is in.
     *
     * @return the game's identifier, or empty before taking a seat
     */
    public synchronized Optional<String> gameId() {
        return Optional.ofNullable(gameId);
    }

    /**
     * Returns the last picture the server sent.
     *
     * @return everything this client is allowed to know, or empty before it is told anything
     */
    public synchronized Optional<GameView> game() {
        return Optional.ofNullable(game);
    }

    /**
     * Returns the games somebody could join, as of the last listing.
     *
     * @return the open games
     */
    public synchronized List<GameSummary> openGames() {
        return openGames;
    }

    /**
     * Returns why the last command was refused.
     *
     * <p>Cleared by the next state, because a refusal is about something that did not happen
     * and stops being interesting the moment something does.
     *
     * @return the reason, or empty when nothing has been refused since
     */
    public synchronized Optional<String> lastRefusal() {
        return Optional.ofNullable(lastRefusal);
    }

    /**
     * Tells whether this client is still connected.
     *
     * @return {@code true} until the connection goes
     */
    public synchronized boolean isConnected() {
        return connected;
    }

    /**
     * Returns everything this client has been told, oldest first.
     *
     * @return the narration, capped at the last {@value #LOG_LENGTH} events
     */
    public synchronized List<Event> narration() {
        return List.copyOf(narration);
    }

    /**
     * Returns the narration since a point, for a screen that has already shown the rest.
     *
     * @param alreadySeen how many events have been shown
     * @return everything after those
     */
    public synchronized List<Event> narrationAfter(int alreadySeen) {
        List<Event> all = new ArrayList<>(narration);
        return alreadySeen >= all.size() ? List.of() : List.copyOf(all.subList(alreadySeen, all.size()));
    }
}
