package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.model.player.PlayerColor;

/**
 * One player's account at journey's end, itemised.
 *
 * <p>Itemised rather than reduced to a single number, because a player who finishes on
 * two credits wants to know that the eight they earned for arriving first went on the
 * ten components they lost getting there. The total is the sum; the parts are the story.
 *
 * @param player            whose account this is
 * @param finishedTheFlight whether they were still on the route at the end
 * @param finishReward      credits for the finishing order, zero for anyone who gave up
 * @param prettiestShip     credits for the fewest exposed connectors, zero for anyone who gave up
 * @param goodsSold         credits from cargo, halved and rounded up for anyone who gave up
 * @param lostComponents    credits charged for everything left in the discard pile
 */
public record ScoreSheet(PlayerColor player,
                         boolean finishedTheFlight,
                         int finishReward,
                         int prettiestShip,
                         int goodsSold,
                         int lostComponents) {

    /**
     * Returns what the player walks away with.
     *
     * <p>May be negative. The manual is quite clear that trucking is a risky business,
     * and a player who lost half their ship can end the flight owing money.
     *
     * @return the total credits
     */
    public int total() {
        return finishReward + prettiestShip + goodsSold - lostComponents;
    }

    /**
     * Tells whether this player made a profit.
     *
     * <p>"Add up all your cosmic credits. If the result is one or more, you have won"
     * (manual p.15). Winning the game and beating everyone else are different questions,
     * and the manual answers the first one generously.
     *
     * @return {@code true} when the total is at least one credit
     */
    public boolean isProfitable() {
        return total() >= 1;
    }
}
