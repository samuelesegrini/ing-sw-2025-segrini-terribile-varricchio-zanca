package it.polimi.ingsw.server.model.board;

/**
 * Everything that distinguishes one flight configuration from another.
 *
 * @param level       which flight this describes
 * @param flightBoard the route, start spaces, rewards and deck composition
 * @param shipBoard   the ship outline and how the dice address it
 * @param rules       which optional rules are in play
 */
public record LevelSpec(GameLevel level, FlightBoardSpec flightBoard, ShipBoardSpec shipBoard, LevelRules rules) {

    /**
     * Validates that the board and the rules agree with each other.
     *
     * @throws IllegalArgumentException if the timer or reservation rule contradicts the boards
     * @throws NullPointerException     if any part is {@code null}
     */
    public LevelSpec {
        if (level == null || flightBoard == null || shipBoard == null || rules == null) {
            throw new NullPointerException("a level specification needs all four of its parts");
        }
        if (rules.hourglass() != flightBoard.isTimed()) {
            throw new IllegalArgumentException(
                    level + ": the timer rule and the printed hourglass spaces disagree");
        }
        if (rules.componentReservation() != shipBoard.allowsReservation()) {
            throw new IllegalArgumentException(
                    level + ": the reservation rule and the printed reservation slots disagree");
        }
    }
}
