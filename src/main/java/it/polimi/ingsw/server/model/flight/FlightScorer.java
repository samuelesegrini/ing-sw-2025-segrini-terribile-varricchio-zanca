package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.model.board.RewardTable;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Settles the accounts once the last adventure card has been resolved.
 *
 * <p>Four things are added up, and three of them treat a player who gave up differently
 * (manual p.15, p.20):
 *
 * <ul>
 *   <li><b>Finishing order</b> — only for those still on the route. Somebody who gave up
 *       did not arrive, so there is nothing to pay them for.</li>
 *   <li><b>The prettiest ship</b> — fewest exposed connectors, again among finishers
 *       only, and <em>every</em> tied ship collects the full reward rather than sharing
 *       it.</li>
 *   <li><b>Goods</b> — full price for finishers, half the total rounded up for anyone who
 *       gave up. Half of the whole manifest, not half of each cube, which is a different
 *       and slightly larger number.</li>
 *   <li><b>Components lost</b> — a credit each, from everyone. This is the one line that
 *       does not care whether the player finished.</li>
 * </ul>
 */
public final class FlightScorer {

    private FlightScorer() {
        // Settlement is a calculation, not an object.
    }

    /**
     * Works out what everyone in a flight ends up with.
     *
     * <p>Selling hands every cube back to the bank, so this changes the ships it scores.
     * It is meant to be run once, at the end.
     *
     * @param flight the finished flight
     * @return one sheet per player, richest first
     */
    public static List<ScoreSheet> settle(Flight flight) {
        RewardTable rewards = flight.level().flightBoard().rewards();
        List<PlayerColor> finishers = flight.stillFlying();
        OptionalInt prettiest = fewestExposedConnectors(flight, finishers);

        List<ScoreSheet> sheets = new ArrayList<>();
        for (PlayerColor player : flight.players()) {
            Ship ship = flight.shipOf(player);
            boolean finished = finishers.contains(player);
            int placement = finishers.indexOf(player) + 1;

            boolean isPrettiest = finished && prettiest.isPresent()
                    && ship.exposedConnectors() == prettiest.getAsInt();

            sheets.add(new ScoreSheet(
                    player,
                    finished,
                    finished ? rewards.finishReward(placement) : 0,
                    isPrettiest ? rewards.prettiestShip() : 0,
                    sellCargo(ship, rewards, finished),
                    flight.creditsEarned(player),
                    ship.lostComponentCount() * rewards.lostComponentPenalty()));
        }
        sheets.sort(Comparator.comparingInt(ScoreSheet::total).reversed());
        return List.copyOf(sheets);
    }

    /**
     * Returns the fewest exposed connectors among the players who finished.
     *
     * @param flight    the finished flight
     * @param finishers the players still on the route
     * @return the winning count, or empty when nobody finished
     */
    private static OptionalInt fewestExposedConnectors(Flight flight, List<PlayerColor> finishers) {
        return finishers.stream()
                .mapToInt(player -> flight.shipOf(player).exposedConnectors())
                .min();
    }

    /**
     * Sells a ship's cargo and returns what it fetched.
     *
     * <p>A player who gave up gets half the total, rounded up — half of the whole
     * manifest rather than half of each cube, which for a mixed cargo is a slightly
     * better deal (manual p.20).
     *
     * @param ship     the ship to empty
     * @param rewards  the price list printed on the board
     * @param finished whether this player completed the flight
     * @return the credits earned
     */
    private static int sellCargo(Ship ship, RewardTable rewards, boolean finished) {
        int full = ship.sellAllCargo().stream()
                .mapToInt(rewards::priceOf)
                .sum();
        return finished ? full : Math.ceilDiv(full, 2);
    }

    /**
     * Returns the price a colour fetches, for a client that wants to show a manifest.
     *
     * @param rewards the price list printed on the board
     * @param color   the colour to price
     * @return its full value in credits
     */
    public static int priceOf(RewardTable rewards, GoodColor color) {
        return rewards.priceOf(color);
    }
}
