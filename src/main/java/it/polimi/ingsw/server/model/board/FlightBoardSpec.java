package it.polimi.ingsw.server.model.board;

import java.util.List;

/**
 * The flight board of one level: its route, its start spaces and what it pays out.
 *
 * <p>The route is a closed loop. Start space offsets are given in route steps from the
 * last of them, so that the first player to finish building takes the space furthest
 * ahead (manual p.8).
 *
 * @param routeLength       how many spaces the loop holds
 * @param startingPositions the offsets of the start spaces, best first
 * @param hourglassSpaces   how many times the timer runs, or zero when the flight is untimed
 * @param rewards           the credits printed on the board
 * @param deck              how the adventure deck is assembled
 */
public record FlightBoardSpec(int routeLength,
                              List<Integer> startingPositions,
                              int hourglassSpaces,
                              RewardTable rewards,
                              DeckComposition deck) {

    /**
     * Validates the board and takes a defensive copy of the start spaces.
     *
     * @throws IllegalArgumentException if the route is empty, there are no start spaces,
     *                                  or a start space falls outside the route
     */
    public FlightBoardSpec {
        if (routeLength < 1) {
            throw new IllegalArgumentException("a route needs at least one space, got " + routeLength);
        }
        startingPositions = List.copyOf(startingPositions);
        if (startingPositions.isEmpty()) {
            throw new IllegalArgumentException("a board needs start spaces");
        }
        for (int offset : startingPositions) {
            if (offset < 0 || offset >= routeLength) {
                throw new IllegalArgumentException("start space " + offset + " is off a route of " + routeLength);
            }
        }
        if (hourglassSpaces < 0) {
            throw new IllegalArgumentException("hourglass spaces cannot be negative, got " + hourglassSpaces);
        }
    }

    /**
     * Returns the start space for a player who finished building in the given order.
     *
     * @param finishingOrder the finishing position, counting from one
     * @return the route offset of that start space
     * @throws IllegalArgumentException if no such start space exists
     */
    public int startingPosition(int finishingOrder) {
        if (finishingOrder < 1 || finishingOrder > startingPositions.size()) {
            throw new IllegalArgumentException(
                    "no start space for finishing position " + finishingOrder
                            + "; the board has " + startingPositions.size());
        }
        return startingPositions.get(finishingOrder - 1);
    }

    /**
     * Returns how many players this board can seat.
     *
     * @return the number of start spaces
     */
    public int maximumPlayers() {
        return startingPositions.size();
    }

    /**
     * Tells whether building is under a time limit on this board.
     *
     * @return {@code true} when the board prints hourglass spaces
     */
    public boolean isTimed() {
        return hourglassSpaces > 0;
    }
}
