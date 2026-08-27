package it.polimi.ingsw.server.persistence;

import java.util.List;

/**
 * Somewhere to keep games, or nowhere.
 *
 * <p>An interface for the sake of {@link #NONE}. Most tests are not about persistence and
 * should not leave files behind to prove it, and a server told to keep nothing should not be
 * carrying a directory around to ignore.
 */
public interface Snapshots {

    /** A place that keeps nothing and finds nothing, for when persistence is not wanted. */
    Snapshots NONE = new Snapshots() {

        @Override
        public void save(GameSnapshot snapshot) {
            // Deliberately nowhere.
        }

        @Override
        public List<GameSnapshot> loadAll() {
            return List.of();
        }

        @Override
        public void delete(String gameId) {
            // Nothing was kept, so there is nothing to forget.
        }
    };

    /**
     * Writes a game down, replacing any earlier record of it.
     *
     * @param snapshot what to keep
     */
    void save(GameSnapshot snapshot);

    /**
     * Reads back every game that was kept.
     *
     * @return the snapshots this build can read
     */
    List<GameSnapshot> loadAll();

    /**
     * Forgets a game, because it has finished.
     *
     * @param gameId which game
     */
    void delete(String gameId);
}
