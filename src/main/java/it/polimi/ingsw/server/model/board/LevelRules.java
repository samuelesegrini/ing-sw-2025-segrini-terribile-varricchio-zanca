package it.polimi.ingsw.server.model.board;

/**
 * Which optional rules a level turns on.
 *
 * <p>The test flight is the complete game minus everything introduced after page 15:
 * no timer, no reserving, no peeking, no aliens, and no credit charged for fixing an
 * illegal ship. Keeping these as data rather than as checks scattered through the code
 * means a rule can only be on or off in one place.
 *
 * @param hourglass                 whether building runs against a timer
 * @param componentReservation      whether components may be set aside
 * @param cardPilePeeking           whether players may look at the predictable piles
 * @param aliens                    whether life support modules and aliens are in play
 * @param illegalShipCreditPenalty  whether fixing a ship already in flight costs a credit
 */
public record LevelRules(boolean hourglass,
                         boolean componentReservation,
                         boolean cardPilePeeking,
                         boolean aliens,
                         boolean illegalShipCreditPenalty) {
}
