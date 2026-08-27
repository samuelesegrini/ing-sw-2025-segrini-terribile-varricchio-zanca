package it.polimi.ingsw.server.persistence;

import it.polimi.ingsw.common.game.GameLevel;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.protocol.Command;

import java.io.Serializable;
import java.util.List;

/**
 * Everything needed to put a game back exactly as it was.
 *
 * <p>Not a picture of the aggregate but a description of how it came to be: who was playing,
 * which rules, which shuffle, and every command that was accepted since. A game is a
 * deterministic function of those four things — one thread applies one command at a time, and
 * the catalogue is bundled with the build — so replaying them lands on the same state, down to
 * the next card the deck will turn over.
 *
 * <p>This is a Memento whose state happens to be a recipe. The architecture originally called
 * for a field-by-field snapshot and argued that would be simpler than replaying a log. In this
 * codebase it is not: the aggregate has no serialization anywhere, its shuffle is a
 * {@code RandomGenerator} which is not serializable at all, and it holds a catalogue of cards
 * that are behaviour rather than data. Replay uses {@code Game.apply}, which is the most
 * exercised path in the project and already deterministic by construction.
 *
 * @param version  the format this was written in, so a snapshot from another one is refused
 *                 rather than half-understood
 * @param gameId   what the game was called; players reconnect to it by name
 * @param level    which rules
 * @param seats    who was playing, and in which colour
 * @param seed     the shuffle, so the deck deals the same cards in the same order
 * @param accepted every command the game accepted, oldest first
 */
public record GameSnapshot(int version, String gameId, GameLevel level, List<Seated> seats,
                           long seed, List<Recorded> accepted) implements Serializable {

    /** The format this build writes and is willing to read. */
    public static final int FORMAT = 1;

    /**
     * A player's place at the table.
     *
     * @param nickname what they log back in as
     * @param colour   which ship is theirs
     */
    public record Seated(String nickname, PlayerColor colour) implements Serializable {
    }

    /**
     * One thing somebody did.
     *
     * @param player  who did it
     * @param command what they sent
     */
    public record Recorded(PlayerColor player, Command command) implements Serializable {
    }

    /**
     * Takes defensive copies.
     *
     * @throws NullPointerException     if anything but the command list is {@code null}
     * @throws IllegalArgumentException if the game has no name or nobody at the table
     */
    public GameSnapshot {
        if (gameId == null || level == null || seats == null || accepted == null) {
            throw new NullPointerException("a snapshot needs a game, a level, seats and a history");
        }
        if (gameId.isBlank()) {
            throw new IllegalArgumentException("a snapshot of a game with no name is unusable");
        }
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("a snapshot of a table with nobody at it is unusable");
        }
        seats = List.copyOf(seats);
        accepted = List.copyOf(accepted);
    }

    /**
     * Tells whether this build can read this snapshot.
     *
     * @return {@code true} when the format matches the one this build writes
     */
    public boolean isReadable() {
        return version == FORMAT;
    }
}
