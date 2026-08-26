package it.polimi.ingsw.server.model.game;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Random;

/**
 * Games to run tests against, built out of the real tiles and the real cards.
 *
 * <p>Real data rather than a fixture, because the aggregate's job is to hold the actual game
 * together and a pool of eight identical tiles would not exercise that. The seed is fixed, so
 * the shuffling is the same every run.
 */
public final class Games {

    /** The same shuffle every time, so a failing test fails the same way twice. */
    private static final long SEED = 20260826L;

    private static final GameData DATA = GameDataLoader.loadBundled();

    private Games() {
    }

    /**
     * A clock a test can push forward.
     *
     * <p>The hourglass is the only thing in this game that happens without anybody doing
     * anything, and waiting ninety real seconds to see it is not a test.
     */
    public static final class Hand implements InstantSource {

        private Instant now = Instant.parse("2026-08-26T10:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        /**
         * Moves time forward.
         *
         * @param by how far
         */
        public void pass(Duration by) {
            now = now.plus(by);
        }
    }

    /**
     * Returns a two-player level II game, just opened.
     *
     * @return a game in its building phase
     */
    public static Game levelTwo() {
        return levelTwo(new Hand());
    }

    /**
     * Returns a two-player level II game with a clock the caller can push forward.
     *
     * @param clock where the hourglass reads the time
     * @return a game in its building phase
     */
    public static Game levelTwo(InstantSource clock) {
        return Game.create("game-1", GameLevel.LEVEL_II,
                List.of(new Seat("samuele", PlayerColor.RED), new Seat("chiara", PlayerColor.BLUE)),
                DATA, new Random(SEED), clock);
    }

    /**
     * Returns a two-player test flight, just opened.
     *
     * @return a game in its building phase
     */
    public static Game testFlight() {
        return Game.create("game-2", GameLevel.TEST_FLIGHT,
                List.of(new Seat("samuele", PlayerColor.RED), new Seat("chiara", PlayerColor.BLUE)),
                DATA, new Random(SEED), new Hand());
    }
}
