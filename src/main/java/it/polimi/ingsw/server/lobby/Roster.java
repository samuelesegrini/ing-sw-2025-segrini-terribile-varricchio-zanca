package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.controller.GameController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where everybody is: anonymous, named, waiting at a table, or sitting in a game.
 *
 * <p>Four books used to be kept side by side in {@link Lobby} and stepped through by hand —
 * fourteen methods reading and writing them, three of those added after somebody first
 * suggested this. What held them together was prose in comments, which is the kind of
 * invariant that survives exactly as long as everybody remembers to read it.
 *
 * <p>Two of those comments were load-bearing enough to be worth restating as the rules of this
 * module rather than notes about it:
 *
 * <ul>
 *   <li><b>A seat outlives the connection that left it.</b> Releasing a nickname gives up the
 *       name and the table, and does <em>not</em> give up the seat — logging in with that name
 *       again is how somebody comes back, and it is the only way, so a second path would be a
 *       second thing to keep in step.</li>
 *   <li><b>Whether anybody is left at a game is answered from here, never by asking the game.</b>
 *       A game learns about a disconnection on its own queue, so asking it races with that: the
 *       second player can hang up before the game has heard about the first, and the table
 *       looks occupied when nobody is in it. Who is logged in is this module's own business and
 *       cannot be stale.</li>
 * </ul>
 *
 * <p><b>Not thread-safe, deliberately.</b> Every path in reaches it on the desk's single
 * worker, which is what makes plain maps correct here and a lock unnecessary — the same reason
 * the model beneath has none. That includes the two questions {@code Lobby} answers for anybody
 * who asks from outside: they are put on the queue and waited for rather than read where they
 * stand, because a plain map walked on one thread while another writes to it is the one thing
 * the single-worker arrangement exists to rule out.
 */
final class Roster {

    /** Where a nickname belongs once its game has started. */
    record Seated(GameController controller, PlayerColor colour) {
    }

    private final Map<String, Connection> named = new HashMap<>();
    private final Map<String, PendingGame> tables = new LinkedHashMap<>();
    private final Map<String, Seated> seats = new HashMap<>();
    private final Map<String, GameController> games = new LinkedHashMap<>();

    /**
     * Which table a connection is waiting at.
     *
     * <p>An index rather than a search. The question is asked on every disconnection and every
     * attempt to join a second table, and answering it by walking every open table and asking
     * each for its players is a scan over the whole building to find one person.
     *
     * <p>Keyed by identity: two connections are the same one or they are not, and a
     * {@code Connection} has no business defining equality for a map's benefit.
     */
    private final Map<Connection, PendingGame> waitingAt = new IdentityHashMap<>();

    // ------------------------------------------------------------------ names

    /**
     * Tells whether somebody is already using a nickname.
     *
     * @param nickname the name being claimed
     * @return {@code true} when a connection is currently under it
     */
    boolean isTaken(String nickname) {
        return named.containsKey(nickname);
    }

    /**
     * Writes a connection down under a nickname.
     *
     * @param nickname   what they are called
     * @param connection who they are
     */
    void name(String nickname, Connection connection) {
        named.put(nickname, connection);
    }

    /**
     * Gives up a nickname, and nothing else.
     *
     * <p>Not the seat: a game in progress holds it under this name until somebody logs in with
     * it again, which is the whole of how coming back works. Not the table either — leaving one
     * is something the others have to be told about, and who is told what is the desk's
     * business rather than this module's.
     *
     * @param nickname   the name being given up
     * @param connection the connection giving it up, so that a late notice from one the desk
     *                   has already moved on from does not release somebody else's name
     */
    void release(String nickname, Connection connection) {
        named.remove(nickname, connection);
    }

    // ------------------------------------------------------------------ tables

    /**
     * Opens a table for players to join.
     *
     * @param table the table
     */
    void open(PendingGame table) {
        tables.put(table.id(), table);
    }

    /**
     * Returns the table with an identifier, if it is still filling.
     *
     * @param id what it is called
     * @return the table, or empty when there is no such table waiting
     */
    Optional<PendingGame> table(String id) {
        return Optional.ofNullable(tables.get(id));
    }

    /**
     * Returns the table a connection is waiting at.
     *
     * @param connection who is asking
     * @return their table, or empty when they are not at one
     */
    Optional<PendingGame> tableOf(Connection connection) {
        return Optional.ofNullable(waitingAt.get(connection));
    }

    /**
     * Seats a connection at a table.
     *
     * @param table the table
     * @param who   the connection
     * @return the colour they were given
     */
    PlayerColor join(PendingGame table, Connection who) {
        PlayerColor colour = table.seat(who);
        waitingAt.put(who, table);
        return colour;
    }

    /**
     * Takes a connection off a table, and forgets the table if that empties it.
     *
     * @param table the table
     * @param who   the connection
     * @return {@code true} if they were at it
     */
    boolean leave(PendingGame table, Connection who) {
        if (!table.remove(who)) {
            return false;
        }
        waitingAt.remove(who);
        forgetIfEmpty(table);
        return true;
    }

    /**
     * Closes a table without waiting for it to empty.
     *
     * <p>For a table that has become a game, and for one the baseline policy ends before it
     * ever does.
     *
     * @param table the table
     */
    void close(PendingGame table) {
        tables.remove(table.id());
        table.players().forEach(waitingAt::remove);
    }

    /**
     * Returns the tables somebody could still join.
     *
     * @return them, oldest first
     */
    List<PendingGame> withRoom() {
        return tables.values().stream().filter(PendingGame::hasRoom).toList();
    }

    /**
     * Returns the identifiers of every table still filling.
     *
     * @return them, oldest first
     */
    List<String> tablesWaiting() {
        return List.copyOf(tables.keySet());
    }

    // ------------------------------------------------------------------ games

    /**
     * Records a table having become a game.
     *
     * <p>One call rather than a put and a loop, because a game and its seats becoming known are
     * one fact: a game in the books with nobody seated in it, or a seat pointing at a game
     * nothing has heard of, are both states nothing should be able to observe.
     *
     * <p>Who sits where is read from the game itself rather than passed in. Both callers had
     * it to hand — one from the table that filled, one from the snapshot that was replayed —
     * and handing it over separately would have been two chances for it to disagree with the
     * seats the game actually dealt.
     *
     * @param controller what is running it
     */
    void started(GameController controller) {
        games.put(controller.game().id(), controller);
        controller.seats().forEach(seat ->
                seats.put(seat.nickname(), new Seated(controller, seat.colour())));
    }

    /**
     * Returns where a nickname is sitting.
     *
     * @param nickname who
     * @return their seat, or empty when they are not in a game
     */
    Optional<Seated> seatOf(String nickname) {
        return Optional.ofNullable(seats.get(nickname));
    }

    /**
     * Takes a game and every seat at it out of the books.
     *
     * @param controller the game
     */
    void reclaim(GameController controller) {
        controller.seats().forEach(seat -> seats.remove(seat.nickname()));
        games.remove(controller.game().id());
    }

    /**
     * Tells whether anybody at a game is still connected.
     *
     * <p>Answered from these books rather than by asking the game — see this class's own
     * documentation for why asking would race.
     *
     * @param controller the game
     * @return {@code true} while at least one of its nicknames is logged in
     */
    boolean anybodyLeftAt(GameController controller) {
        return controller.seats().stream().anyMatch(seat -> named.containsKey(seat.nickname()));
    }

    /**
     * Returns every running game.
     *
     * @return their controllers, oldest first
     */
    List<GameController> running() {
        return List.copyOf(new ArrayList<>(games.values()));
    }

    /**
     * Returns the identifiers of every running game.
     *
     * @return them, oldest first
     */
    List<String> gamesRunning() {
        return List.copyOf(games.keySet());
    }

    /**
     * Forgets every running game and every seat at one, for a desk that is closing.
     *
     * <p>Both, because {@link #started} promises they become known together and a seat pointing
     * at a game nothing has heard of is a state nothing should be able to observe. Clearing
     * only the games left exactly that.
     */
    void forgetEverything() {
        games.clear();
        seats.clear();
    }

    private void forgetIfEmpty(PendingGame table) {
        if (table.isEmpty()) {
            tables.remove(table.id());
        }
    }
}
