package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.model.board.LevelSpec;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A flight in progress: whose ships are out there, where they stand, and who has given
 * up.
 *
 * <p>Giving up is not a way of losing. A player who walks away still sells their goods,
 * at half price, and can still finish with more credits than anybody who saw the flight
 * through — "any profit counts as a win" (manual p.20). So it has to be a first-class
 * state rather than an early exit: a retired player is still in the game, just not on
 * the route.
 *
 * <p>Two of the three ways to be forced out can be checked from here — losing the last
 * human, and being lapped. The third, declaring no engine power in Open Space, only
 * makes sense while that card is being resolved, so the card calls {@link #giveUp}
 * itself.
 */
public final class Flight {

    private final LevelSpec level;
    private final Route route;
    private final Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
    private final Set<PlayerColor> retired = new LinkedHashSet<>();

    /**
     * Starts a flight with every ship on its start space.
     *
     * @param level          the flight configuration being played
     * @param ships          each player's ship
     * @param startPositions where each player's marker begins
     * @throws IllegalArgumentException if a player has a ship but no start space, or the
     *                                  other way round
     */
    public Flight(LevelSpec level, Map<PlayerColor, Ship> ships, Map<PlayerColor, Integer> startPositions) {
        if (!ships.keySet().equals(startPositions.keySet())) {
            throw new IllegalArgumentException(
                    "every ship needs a start space: ships " + ships.keySet()
                            + " against spaces " + startPositions.keySet());
        }
        this.level = level;
        this.route = new Route(level.flightBoard().routeLength());
        this.ships.putAll(ships);
        startPositions.entrySet().stream()
                .sorted(Map.Entry.<PlayerColor, Integer>comparingByValue().reversed())
                .forEach(entry -> route.enter(entry.getKey(), entry.getValue()));
    }

    /**
     * Returns the flight configuration being played.
     *
     * @return the level specification
     */
    public LevelSpec level() {
        return level;
    }

    /**
     * Returns the route the ships are flying.
     *
     * @return the route
     */
    public Route route() {
        return route;
    }

    /**
     * Returns a player's ship.
     *
     * <p>Available whether or not they are still flying: a retired player's ship is still
     * scored for its cargo and its losses.
     *
     * @param player whose ship to return
     * @return their ship
     * @throws IllegalArgumentException if that player is not in this flight
     */
    public Ship shipOf(PlayerColor player) {
        Ship ship = ships.get(player);
        if (ship == null) {
            throw new IllegalArgumentException("the " + player + " player is not in this flight");
        }
        return ship;
    }

    /**
     * Returns everybody in the flight, whether they are still on the route or not.
     *
     * @return every player
     */
    public Set<PlayerColor> players() {
        return Set.copyOf(ships.keySet());
    }

    /**
     * Returns the players still on the route, leader first.
     *
     * @return those still flying, in route order
     */
    public List<PlayerColor> stillFlying() {
        return route.routeOrder();
    }

    /**
     * Returns the players who have given up.
     *
     * @return those who left the route
     */
    public Set<PlayerColor> retired() {
        return Set.copyOf(retired);
    }

    /**
     * Tells whether a player has left the route.
     *
     * @param player the player to check
     * @return {@code true} when they gave up
     */
    public boolean hasGivenUp(PlayerColor player) {
        return retired.contains(player);
    }

    /**
     * Tells whether one player is flying alone.
     *
     * <p>Combat Zone is skipped when everybody else has given up: it penalises whoever is
     * weakest at something, and with nobody to compare against that is meaningless
     * (manual p.20).
     *
     * @return {@code true} when exactly one ship is left on the route
     */
    public boolean isSolo() {
        return route.flyingCount() == 1;
    }

    /**
     * Tells whether the flight is over for want of ships.
     *
     * @return {@code true} when nobody is left on the route
     */
    public boolean isDeserted() {
        return route.flyingCount() == 0;
    }

    // ---------------------------------------------------------------- giving up

    /**
     * Returns the players the rules force out, without acting on it.
     *
     * <p>Two of the three conditions live here. A ship whose last human is gone cannot
     * fly — aliens will not do it alone. A ship the leader has lapped is out of the race.
     * The third, declaring no engine power on an Open Space card, is only meaningful
     * while that card is being resolved.
     *
     * <p>Checked <em>after</em> a card has been fully resolved, never in the middle of one
     * (manual p.20): a player who loses their last human partway through a combat zone
     * still suffers the rest of it.
     *
     * @return the players who must now give up
     */
    public Set<PlayerColor> playersForcedOut() {
        Set<PlayerColor> forced = new LinkedHashSet<>();
        for (PlayerColor player : route.players()) {
            if (shipOf(player).humanCount() == 0 || route.isLapped(player)) {
                forced.add(player);
            }
        }
        return Set.copyOf(forced);
    }

    /**
     * Takes a player out of the race.
     *
     * <p>Their marker leaves the route and no later card touches them. Their ship stays
     * where it is, because it is still scored: goods at half price, and the full penalty
     * for everything lost along the way (manual p.20).
     *
     * @param player the player giving up
     * @throws IllegalStateException    if they have already given up
     * @throws IllegalArgumentException if they are not in this flight
     */
    public void giveUp(PlayerColor player) {
        shipOf(player);
        if (!retired.add(player)) {
            throw new IllegalStateException("the " + player + " player has already given up");
        }
        route.leave(player);
    }

    /**
     * Takes out everybody the rules force out.
     *
     * <p>Run once a card is fully resolved. Order matters and is handled: being lapped is
     * measured against the leader, so a leader who is themselves forced out first would
     * change who counts as lapped. Both sets are therefore worked out before anyone
     * leaves.
     *
     * @return the players who were forced out
     */
    public Set<PlayerColor> enforceGiveUpRules() {
        Set<PlayerColor> forced = playersForcedOut();
        forced.forEach(this::giveUp);
        return forced;
    }
}
