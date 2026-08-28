package it.polimi.ingsw.server.lobby;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.controller.GameController;
import it.polimi.ingsw.server.model.game.Game;
import it.polimi.ingsw.server.model.game.Seat;
import it.polimi.ingsw.server.persistence.GameSnapshot;
import it.polimi.ingsw.server.persistence.Snapshots;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * Where a game comes into being, and the only place it does.
 *
 * <p>There are two ways a game starts: a table fills, or a server that was stopped starts
 * again. They are the same four steps — settle a shuffle, build the aggregate, wire up
 * something that writes it down, hand back a controller running it — and they used to be
 * written out twice in {@link Lobby}, fifty lines apart, from different sources, with nothing
 * checking that they agreed.
 *
 * <p><b>Why that mattered rather than merely being untidy.</b> Both defects M14 turned up
 * landed here, because there was nowhere else for them to land: a shutdown that deleted every
 * snapshot on the way out, and a snapshot that would not replay being read again at every
 * startup. Neither is about a lobby, a table or a nickname. They are about what it means for a
 * game to be kept, which is this module's whole subject.
 *
 * <p>The desk above no longer knows that games have seeds, that snapshots have a format, or
 * that a {@link GameSnapshot.Seated} exists. It knows who is at which table.
 *
 * <p><b>Not thread-safe, and it does not need to be.</b> {@link #recoverAll} is called once,
 * from the lobby's constructor, before anything can connect — a server that let somebody log in
 * while games were still being put back could tell them their name was free and then find it
 * was not. Everything else is reached on the lobby's single worker.
 */
public final class GameArchive {

    private static final Logger LOG = LoggerFactory.getLogger(GameArchive.class);

    private final GameData data;
    private final RandomGenerator random;
    private final InstantSource clock;
    private final Duration soloTimeout;
    private final Snapshots snapshots;

    /**
     * Opens the archive a server keeps its games in.
     *
     * @param settings the catalogue, the shuffle, the clock, how long a game waits for its
     *                 last player, and where games are written down
     */
    public GameArchive(ServerSettings settings) {
        this.data = settings.data();
        this.random = settings.random();
        this.clock = settings.clock();
        this.soloTimeout = settings.soloTimeout();
        this.snapshots = settings.snapshots();
    }

    /**
     * Deals a new game and hands back something running it.
     *
     * <p>The tiles are shuffled and the cards dealt here, at the last possible moment. Doing it
     * when a table was created would mean shuffling a hundred and fifty tiles for every player
     * who joined and changed their mind.
     *
     * <p>It is written down before it is returned — a game is recoverable from the moment it is
     * dealt, not from the moment somebody first does something in it.
     *
     * @param id    what to call it
     * @param level which rules
     * @param seats who is playing, in seating order
     * @return a controller running it
     */
    public GameController deal(String id, GameLevel level, List<Seat> seats) {
        // A seed of its own, drawn from the server's shuffle. A game that shared that
        // generator could not be written down: reproducing it would mean reproducing every
        // other game that had drawn from it since.
        long seed = random.nextLong();
        Game game = Game.create(id, level, seats, data, new Random(seed), clock, soloTimeout);
        LOG.debug("{} dealt, {} seats at {}", id, seats.size(), level);
        return new GameController(game, keeping(id, level, seats, seed));
    }

    /**
     * Picks up every game that was running when the server stopped.
     *
     * <p>They come back with nobody attached: the seats are held under the old nicknames, and
     * logging in with one puts that player back at their table. That is the same path a player
     * takes after their own connection drops, which is why there is not a second one.
     *
     * <p>A game that will not replay does not stop the rest coming back, and is moved out of
     * the way rather than deleted: the file is the one artifact that would explain why, and a
     * server reading this directory at every startup would otherwise report the same dead game
     * for ever.
     *
     * @return a controller for each game that came back, in no particular order
     */
    public List<GameController> recoverAll() {
        List<GameController> back = new ArrayList<>();
        for (GameSnapshot kept : snapshots.loadAll()) {
            try {
                Game game = Game.restore(kept, data, clock, soloTimeout);
                back.add(new GameController(game,
                        keeping(kept.gameId(), kept.level(), game.seats(), kept.seed())));
                LOG.info("{} picked up again, {} commands replayed", kept.gameId(),
                        kept.accepted().size());
            } catch (RuntimeException broken) {
                LOG.warn("could not put {} back, setting it aside: {}", kept.gameId(),
                        broken.getMessage());
                snapshots.setAside(kept.gameId());
            }
        }
        return List.copyOf(back);
    }

    /**
     * Forgets a game, because it has finished.
     *
     * <p>One left behind would come back from the dead the next time the server started.
     *
     * @param gameId which game
     */
    public void forget(String gameId) {
        snapshots.delete(gameId);
    }

    /**
     * Returns something that writes a game down whenever it reaches a point worth keeping.
     *
     * <p>The recipe rather than the state: what a game is called, its rules, its seats and its
     * shuffle, plus everything it has accepted. A game is a deterministic function of those,
     * so replaying them lands on the same state. Assembling it is the reason this module
     * exists — it is the part that was written twice.
     */
    private Consumer<Game> keeping(String id, GameLevel level, List<Seat> seats, long seed) {
        List<GameSnapshot.Seated> written = seats.stream()
                .map(seat -> new GameSnapshot.Seated(seat.nickname(), seat.colour()))
                .toList();
        return game -> {
            snapshots.save(new GameSnapshot(GameSnapshot.FORMAT, id, level, written,
                    seed, game.history()));
            LOG.debug("{} written down, {} commands", id, game.history().size());
        };
    }
}
