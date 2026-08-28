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

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(ClientState.class);

    /** How much narration to keep. Enough to fill a screen twice over. */
    private static final int LOG_LENGTH = 200;

    private final Deque<Event> narration = new ArrayDeque<>();

    /**
     * How many events have ever arrived, including the ones {@link #LOG_LENGTH} has since
     * dropped.
     *
     * <p>The narration itself is a window; this is not. A screen that has printed part of the
     * story needs a mark that keeps meaning the same thing after the window has slid past it.
     */
    private long heard;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private String nickname;
    private PlayerColor colour;
    private String gameId;
    private GameView game;
    private List<GameSummary> openGames = List.of();
    private String lastRefusal;
    private String lostConnection;
    private boolean connected = true;

    /**
     * Builds a client that has been told nothing.
     */
    public ClientState() {
        // Everything here starts empty, which is what a client that has not connected knows.
    }

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
            case LobbyEvent.LoggedIn loggedIn -> {
                nickname = loggedIn.nickname();
                LOG.debug("logged in as {}", nickname);
            }
            case LobbyEvent.GamesListed listed -> {
                openGames = listed.games();
                LOG.debug("{} games waiting", openGames.size());
            }
            case LobbyEvent.JoinedGame joined -> {
                gameId = joined.gameId();
                colour = joined.colour();
                LOG.debug("seated at {} as {}", gameId, colour);
            }
            case GameEvent.StateChanged changed -> {
                game = changed.state();
                lastRefusal = null;
                // Summarised, never dumped: a whole projection in a log file is unreadable and
                // is also the one message that carries what the other players cannot see.
                LOG.debug("state: {} phase, {} players, waiting on {}", game.phase(),
                        game.players().size(),
                        game.pendingIfAny().map(prompt -> prompt.player().toString())
                                .orElse("nobody"));
            }
            case GameEvent.Rejected rejected -> {
                lastRefusal = rejected.reason();
                LOG.debug("refused {}: {}", rejected.command(), rejected.reason());
            }
            default ->
                // Narration, and nothing to record beyond having heard it. Traced all the
                // same: from a player's chair an event this state ignores looks exactly like
                // one that never arrived.
                    LOG.debug("narration: {}", event.getClass().getSimpleName());
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
        LOG.info("connection lost: {}", reason);
        connected = false;
        lostConnection = reason;
        listeners.forEach(Runnable::run);
    }

    private void remember(Event event) {
        narration.addLast(event);
        heard++;
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
     * and stops being interesting the moment something does. It is not cleared by being read:
     * this is a record of the last thing the client was told, and a reader that changed it
     * would make it something else.
     *
     * <p>A screen that wants to <em>show</em> a refusal once should print it from the narration
     * rather than from here, since the narration is already a stream each event appears in
     * exactly once.
     *
     * @return the reason, or empty when nothing has been refused since
     */
    public synchronized Optional<String> lastRefusal() {
        return Optional.ofNullable(lastRefusal);
    }

    /**
     * Returns why the connection went, if it has.
     *
     * <p>Not consumed, because it does not stop being true. A client whose server has gone is
     * in that state until it is restarted.
     *
     * @return the reason, or empty while the connection is up
     */
    public synchronized Optional<String> lostConnection() {
        return Optional.ofNullable(lostConnection);
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
     * Returns how many events have ever arrived.
     *
     * <p>Unlike {@code narration().size()} this keeps climbing once the log is full, so it can
     * be compared against itself to tell whether anything new has been said.
     *
     * @return the count of everything heard, dropped or not
     */
    public synchronized long heard() {
        return heard;
    }

    /**
     * Returns the narration since a point, for a screen that has already shown the rest.
     *
     * <p>The cursor counts events heard, not places in the log, so it goes on meaning the same
     * thing after the log has dropped its oldest. A screen that has been away longer than the
     * log is deep is given everything still kept rather than nothing.
     *
     * @param alreadySeen the {@link Unshown#cursor()} from last time, or 0 to start
     * @return what has not been shown, and the cursor to ask with next time
     */
    public synchronized Unshown narrationAfter(long alreadySeen) {
        List<Event> all = new ArrayList<>(narration);
        long dropped = heard - all.size();
        int from = (int) Math.min(all.size(), Math.max(0, alreadySeen - dropped));
        return new Unshown(List.copyOf(all.subList(from, all.size())), heard);
    }

    /**
     * The narration a screen has not drawn yet, with the mark to come back with.
     *
     * <p>The two travel together because reading and advancing have to be one step: taking the
     * cursor separately either loses the events that land in between or shows them twice.
     *
     * @param events what to draw now, oldest first
     * @param cursor what to pass to the next {@link #narrationAfter(long)}
     */
    public record Unshown(List<Event> events, long cursor) {
    }
}
