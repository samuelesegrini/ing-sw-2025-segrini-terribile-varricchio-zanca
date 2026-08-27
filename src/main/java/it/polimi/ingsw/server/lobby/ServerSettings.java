package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.game.Game;
import it.polimi.ingsw.server.persistence.SnapshotStore;
import it.polimi.ingsw.server.persistence.Snapshots;

import java.nio.file.Path;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Random;
import java.util.random.RandomGenerator;

/**
 * How a server is set up, as one value rather than six positional arguments.
 *
 * <p>Before this there were four {@code Lobby} constructors, the widest taking six parameters
 * and ending in a nullable {@link Path} whose {@code null} silently meant "keep no games", and
 * three {@code Server.start} overloads none of which could reach it. That last part was not a
 * tidiness problem: it meant the shipped server could not persist anything at all, which is
 * the whole of #157.
 *
 * <p><b>Refined, not constructed.</b> Every setting has a default and every change to one is a
 * named method returning a new value:
 *
 * <pre>{@code
 * ServerSettings.defaults().keeping(Path.of("games"))
 * }</pre>
 *
 * <p>That shape is deliberate. A caller decides one thing at a time and can be asked one thing
 * at a time, so a launcher that interviewed an operator before starting the server could build
 * this up answer by answer and show it back to them before committing. Nothing here does any
 * of that, and nothing here reads a command line or touches a disk — it is a value, and the
 * places that parse arguments and open directories stay where they are.
 *
 * <p><b>{@code defaults()} keeps nothing.</b> The default is the one a forgetful caller should
 * get: a test that never thinks about persistence does not write files into whatever directory
 * it happened to run in, and does not pick up the games another test left behind. The server
 * that <em>should</em> keep games says so, in one readable line, in {@code ServerMain}.
 *
 * @param data            the tiles, cards and boards games are made of
 * @param random          where the shuffling and the dice come from
 * @param clock           where hourglasses read the time
 * @param onDisconnection what a dropped connection does to a game in progress
 * @param soloTimeout     how long a game with one player left waits before awarding them the win
 * @param snapshots       where games are written down, {@link Snapshots#NONE} to keep none
 */
public record ServerSettings(GameData data,
                             RandomGenerator random,
                             InstantSource clock,
                             DisconnectionPolicy onDisconnection,
                             Duration soloTimeout,
                             Snapshots snapshots) {

    /**
     * Validates the settings.
     *
     * @throws NullPointerException     if anything is missing — including the snapshots, which
     *                                  is why "keep nothing" is {@link Snapshots#NONE} and not
     *                                  {@code null}
     * @throws IllegalArgumentException if the solo timeout is not positive
     */
    public ServerSettings {
        if (data == null || random == null || clock == null || onDisconnection == null
                || soloTimeout == null || snapshots == null) {
            throw new NullPointerException("a server needs all of its settings");
        }
        if (soloTimeout.isNegative() || soloTimeout.isZero()) {
            throw new IllegalArgumentException(
                    "a game cannot wait " + soloTimeout + " for its last player");
        }
    }

    /**
     * Returns the settings a server has before anybody asks for anything else.
     *
     * <p>The bundled catalogue, an unseeded shuffle, the wall clock, games that carry on when
     * somebody drops, and <b>no games kept</b>.
     *
     * @return the starting point every other setting is refined from
     */
    public static ServerSettings defaults() {
        return new ServerSettings(GameDataLoader.loadBundled(), new Random(),
                InstantSource.system(), DisconnectionPolicy.GAME_CARRIES_ON,
                Game.DEFAULT_SOLO_TIMEOUT, Snapshots.NONE);
    }

    /**
     * Returns these settings, keeping games in a directory.
     *
     * @param directory where to write them
     * @return settings whose games survive the server stopping
     */
    public ServerSettings keeping(Path directory) {
        return keeping(new SnapshotStore(directory));
    }

    /**
     * Returns these settings, keeping games somewhere given.
     *
     * <p>For a test that wants to watch what is written without writing it.
     *
     * @param where the store
     * @return settings that keep their games there
     */
    public ServerSettings keeping(Snapshots where) {
        return new ServerSettings(data, random, clock, onDisconnection, soloTimeout, where);
    }

    /**
     * Returns these settings, built from a given catalogue.
     *
     * @param catalogue the tiles, cards and boards
     * @return settings using it
     */
    public ServerSettings dealtFrom(GameData catalogue) {
        return new ServerSettings(catalogue, random, clock, onDisconnection, soloTimeout, snapshots);
    }

    /**
     * Returns these settings, shuffled from a given source.
     *
     * <p>A seeded generator is what makes a game reproducible, and so what makes a failing
     * flight something a test can replay.
     *
     * @param source where the shuffling and the dice come from
     * @return settings using it
     */
    public ServerSettings shuffledBy(RandomGenerator source) {
        return new ServerSettings(data, source, clock, onDisconnection, soloTimeout, snapshots);
    }

    /**
     * Returns these settings, reading the time from a given clock.
     *
     * @param source where hourglasses and timeouts read the time
     * @return settings using it
     */
    public ServerSettings timedBy(InstantSource source) {
        return new ServerSettings(data, random, source, onDisconnection, soloTimeout, snapshots);
    }

    /**
     * Returns these settings, with a stated answer to somebody dropping out.
     *
     * @param policy what a dropped connection does to a game in progress
     * @return settings using it
     */
    public ServerSettings whenSomebodyDrops(DisconnectionPolicy policy) {
        return new ServerSettings(data, random, clock, policy, soloTimeout, snapshots);
    }

    /**
     * Returns these settings, waiting a stated time for a game's last player.
     *
     * @param howLong how long a game with one player left waits
     * @return settings using it
     */
    public ServerSettings waiting(Duration howLong) {
        return new ServerSettings(data, random, clock, onDisconnection, howLong, snapshots);
    }
}
