package it.polimi.ingsw.server.model.building;

import it.polimi.ingsw.server.model.board.FlightBoardSpec;
import it.polimi.ingsw.common.game.PlayerColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * The start spaces at the head of the route, and who has taken which.
 *
 * <p>Spaces are numbered from 1, with 1 the furthest ahead. How a finishing player gets
 * one depends on the level ({@link StartSpacePolicy}), but two things are always true:
 * a space can only be taken once, and a space numbered above the number of players in
 * the game is not in use at all (manual p.17). With two players, spaces 3 and 4 stay
 * empty however many are printed on the board.
 *
 * <p>A player who is forced to stop because the last hourglass period ran out does not
 * get to choose — they take the best space still free (manual p.17).
 */
public final class StartSpaces {

    private final FlightBoardSpec board;
    private final int playerCount;
    private final StartSpacePolicy policy;
    private final Map<PlayerColor, Integer> claimed = new LinkedHashMap<>();

    /**
     * Creates the start spaces for one game.
     *
     * @param board       the flight board being played on
     * @param playerCount how many players are in the game
     * @param policy      how a finishing player gets a space
     * @throws IllegalArgumentException if the player count does not fit the board
     */
    public StartSpaces(FlightBoardSpec board, int playerCount, StartSpacePolicy policy) {
        if (playerCount < 2 || playerCount > board.maximumPlayers()) {
            throw new IllegalArgumentException(
                    "a game of " + playerCount + " does not fit a board seating 2 to " + board.maximumPlayers());
        }
        this.board = board;
        this.playerCount = playerCount;
        this.policy = policy;
    }

    /**
     * Returns how a finishing player gets a space here.
     *
     * @return the policy in force
     */
    public StartSpacePolicy policy() {
        return policy;
    }

    /**
     * Returns the spaces still free, best first.
     *
     * <p>Spaces numbered above the player count never appear: they are not in play.
     *
     * @return the free space numbers, counting from one
     */
    public List<Integer> free() {
        return IntStream.rangeClosed(1, playerCount)
                .filter(space -> !claimed.containsValue(space))
                .boxed()
                .toList();
    }

    /**
     * Returns which space a player took.
     *
     * @param player the player to look up
     * @return their space number, or empty when they have not finished
     */
    public OptionalInt spaceOf(PlayerColor player) {
        Integer space = claimed.get(player);
        return space == null ? OptionalInt.empty() : OptionalInt.of(space);
    }

    /**
     * Returns where on the route a player's marker stands.
     *
     * @param player the player to look up
     * @return the route offset of their start space, or empty when they have not finished
     */
    public OptionalInt routePositionOf(PlayerColor player) {
        OptionalInt space = spaceOf(player);
        return space.isEmpty() ? OptionalInt.empty() : OptionalInt.of(board.startingPosition(space.getAsInt()));
    }

    /**
     * Returns how many players have finished.
     *
     * @return the number of spaces taken
     */
    public int claimedCount() {
        return claimed.size();
    }

    /**
     * Gives a finishing player their place on the route.
     *
     * <p>An empty choice means "the best space left". That covers the test flight, where
     * spaces simply go out in finishing order, and it covers a player forced to stop when
     * the last hourglass period runs out — neither gets to pick.
     *
     * @param player the player who has finished
     * @param chosen the space they picked, or empty to take the best free one
     * @return the space number they end up with
     * @throws IllegalStateException    if the player has already finished, or nothing is free
     * @throws IllegalArgumentException if the choice is not one they may make
     */
    public int claim(PlayerColor player, OptionalInt chosen) {
        if (claimed.containsKey(player)) {
            throw new IllegalStateException("the " + player + " player has already taken space " + claimed.get(player));
        }
        List<Integer> free = free();
        if (free.isEmpty()) {
            throw new IllegalStateException("every start space is taken");
        }

        int space = chosen.isEmpty() ? free.getFirst() : checkedChoice(chosen.getAsInt(), free);
        claimed.put(player, space);
        return space;
    }

    private int checkedChoice(int wanted, List<Integer> free) {
        if (policy != StartSpacePolicy.CHOSEN_BY_PLAYER) {
            throw new IllegalArgumentException(
                    "start spaces are handed out in finishing order on this board, not chosen");
        }
        if (wanted > playerCount) {
            throw new IllegalArgumentException(
                    "space " + wanted + " is not in play in a game of " + playerCount);
        }
        if (!free.contains(wanted)) {
            throw new IllegalArgumentException("space " + wanted + " is already taken");
        }
        return wanted;
    }

    /**
     * Returns who is standing on a space.
     *
     * @param space the space number, counting from one
     * @return the player there, or empty when it is free
     */
    public Optional<PlayerColor> occupantOf(int space) {
        return claimed.entrySet().stream()
                .filter(entry -> entry.getValue() == space)
                .map(Map.Entry::getKey)
                .findFirst();
    }
}
