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

        @Override
        public void setAside(String gameId) {
            // Nothing was kept, so there is nothing that could have failed to come back.
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

    /**
     * Puts a snapshot out of the way, because this build cannot bring it back.
     *
     * <p>Not deleted: a game that will not replay is the one artifact that would explain why,
     * and throwing it away throws away the bug report with it. Not left where it is either —
     * a server that keeps its games reads this directory at every startup, so a snapshot it
     * can never recover would be complained about for ever, and a warning that appears every
     * single time is one people learn to scroll past.
     *
     * @param gameId which game
     */
    void setAside(String gameId);
}
