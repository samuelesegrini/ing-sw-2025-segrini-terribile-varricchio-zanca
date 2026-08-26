package it.polimi.ingsw.server.lobby;

/**
 * What happens to a game when somebody's connection goes away.
 *
 * <p>The requirements ask for two different things in two different places, and
 * {@code docs/specs/requirements.md} is explicit that both must stay reachable and that the
 * active one must be a decision rather than an accident. So it is one, here, rather than
 * whichever behaviour the code happened to grow.
 */
public enum DisconnectionPolicy {

    /**
     * The baseline of requirement L4: one player going away ends the game for everybody.
     *
     * <p>Blunt, and what the requirements ask for before the advanced features are counted. A
     * game of four that ends because one laptop went to sleep is a bad evening, which is why
     * there is an advanced feature about it.
     */
    ENDS_THE_GAME,

    /**
     * Advanced feature AF4: the game carries on and the empty seat waits to be reclaimed.
     *
     * <p>The player's turns are skipped, their ship keeps flying, and logging in again with the
     * same nickname puts them back where they were. The default, because it is the behaviour
     * this project is being graded on and the one anybody would rather play.
     */
    GAME_CARRIES_ON
}
