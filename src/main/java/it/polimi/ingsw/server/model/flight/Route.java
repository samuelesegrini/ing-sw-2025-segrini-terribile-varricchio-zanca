package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.common.game.PlayerColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The loop of spaces the ships fly round, and where each one stands on it.
 *
 * <p>Two decisions here shape everything the adventure cards do.
 *
 * <p><b>Positions are absolute, not modular.</b> A marker's position counts every space
 * it has ever advanced, so two ships a full lap apart hold different numbers even though
 * they sit on the same space. Storing positions modulo the route length would make a
 * lapped ship indistinguishable from one right behind the leader, and being lapped is
 * what forces a player out of the race (manual p.20).
 *
 * <p><b>Movement counts empty spaces.</b> Occupied spaces are jumped over and do not
 * count against the distance (manual p.10). That is what lets a ship declaring three
 * engine power overtake two rivals and still travel three empty spaces, and it is why
 * moving is a loop rather than an addition.
 */
public final class Route {

    private final int length;
    private final Map<PlayerColor, Integer> positions = new LinkedHashMap<>();

    /**
     * Creates an empty route.
     *
     * @param length how many spaces the loop holds
     * @throws IllegalArgumentException if the loop is empty
     */
    public Route(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("a route needs at least one space, got " + length);
        }
        this.length = length;
    }

    /**
     * Returns how many spaces the loop holds.
     *
     * @return the route length
     */
    public int length() {
        return length;
    }

    // ---------------------------------------------------------------- markers

    /**
     * Puts a player's marker on the route at a start space.
     *
     * @param player   whose marker it is
     * @param position the space to start on
     * @throws IllegalStateException    if that player is already on the route
     * @throws IllegalArgumentException if the space is off the route or already taken
     */
    public void enter(PlayerColor player, int position) {
        if (positions.containsKey(player)) {
            throw new IllegalStateException("the " + player + " marker is already on the route");
        }
        if (position < 0 || position >= length) {
            throw new IllegalArgumentException("space " + position + " is off a route of " + length);
        }
        occupantOf(position).ifPresent(other -> {
            throw new IllegalArgumentException("space " + position + " is taken by the " + other + " player");
        });
        positions.put(player, position);
    }

    /**
     * Takes a player's marker off the route.
     *
     * <p>What happens when a player gives up: they become a spectator, and no later card
     * touches them (manual p.20).
     *
     * @param player whose marker to remove
     * @return {@code true} when a marker was actually there
     */
    public boolean leave(PlayerColor player) {
        return positions.remove(player) != null;
    }

    /**
     * Tells whether a player is still flying.
     *
     * @param player the player to look for
     * @return {@code true} when their marker is on the route
     */
    public boolean isFlying(PlayerColor player) {
        return positions.containsKey(player);
    }

    /**
     * Returns how many ships are still flying.
     *
     * @return the number of markers on the route
     */
    public int flyingCount() {
        return positions.size();
    }

    /**
     * Returns a player's absolute progress along the loop.
     *
     * @param player the player to look up
     * @return how many spaces they have travelled in total
     * @throws IllegalArgumentException if they are not on the route
     */
    public int positionOf(PlayerColor player) {
        Integer position = positions.get(player);
        if (position == null) {
            throw new IllegalArgumentException("the " + player + " player is not on the route");
        }
        return position;
    }

    /**
     * Returns which space of the loop a player is standing on.
     *
     * @param player the player to look up
     * @return the space, between zero and the route length
     * @throws IllegalArgumentException if they are not on the route
     */
    public int spaceOf(PlayerColor player) {
        return Math.floorMod(positionOf(player), length);
    }

    /**
     * Returns who is standing on a space of the loop.
     *
     * @param space the space to look at; any absolute position is accepted
     * @return the player there, or empty when it is free
     */
    public Optional<PlayerColor> occupantOf(int space) {
        int wanted = Math.floorMod(space, length);
        return positions.entrySet().stream()
                .filter(entry -> Math.floorMod(entry.getValue(), length) == wanted)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    // ---------------------------------------------------------------- order

    /**
     * Returns everyone still flying, leader first.
     *
     * <p>Recomputed on every call rather than cached. Route order changes the moment
     * anybody moves, and a card that read a stale order would ask the wrong player to
     * act — which is precisely how a combat zone would penalise the wrong ship.
     *
     * @return the players in route order
     */
    public List<PlayerColor> routeOrder() {
        return positions.entrySet().stream()
                .sorted(Map.Entry.<PlayerColor, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns everyone still flying, furthest behind first.
     *
     * @return the players in reverse route order
     */
    public List<PlayerColor> reverseRouteOrder() {
        return routeOrder().reversed();
    }

    /**
     * Returns whoever is furthest ahead.
     *
     * <p>The leader turns over the adventure cards, and hands the deck on the moment
     * somebody passes them (manual p.10).
     *
     * @return the leader, or empty when nobody is flying
     */
    public Optional<PlayerColor> leader() {
        return routeOrder().stream().findFirst();
    }

    // ---------------------------------------------------------------- moving

    /**
     * Advances a ship by a number of empty spaces.
     *
     * <p>Occupied spaces are jumped and do not count, so a ship can overtake several
     * rivals and still travel the full distance (manual p.10).
     *
     * @param player  whose marker to move
     * @param spaces  how many empty spaces to travel; zero leaves the marker alone
     * @return the player's new absolute position
     * @throws IllegalArgumentException if they are not on the route or the distance is negative
     */
    public int advance(PlayerColor player, int spaces) {
        return travel(player, spaces, 1);
    }

    /**
     * Sends a ship backwards by a number of empty spaces.
     *
     * <p>Losing flight days, whether the player chose to or not (manual p.10).
     *
     * @param player whose marker to move
     * @param spaces how many empty spaces to fall back
     * @return the player's new absolute position
     * @throws IllegalArgumentException if they are not on the route or the distance is negative
     */
    public int fallBack(PlayerColor player, int spaces) {
        return travel(player, spaces, -1);
    }

    /**
     * Applies flight days lost by several players at once.
     *
     * <p>They fall back in reverse route order — the player furthest behind moves first
     * (manual p.10). The order is fixed before anybody moves, so that one player's
     * retreat cannot reshuffle the queue behind them mid-resolution.
     *
     * @param days how many days each player loses
     * @throws IllegalArgumentException if any of them is not on the route
     */
    public void loseFlightDaysTogether(Map<PlayerColor, Integer> days) {
        List<PlayerColor> order = reverseRouteOrder().stream()
                .filter(days::containsKey)
                .toList();
        for (PlayerColor player : order) {
            fallBack(player, days.get(player));
        }
    }

    private int travel(PlayerColor player, int spaces, int step) {
        if (spaces < 0) {
            throw new IllegalArgumentException("a ship travels a positive number of spaces, got " + spaces);
        }
        int position = positionOf(player);
        int remaining = spaces;
        while (remaining > 0) {
            position += step;
            if (isFreeFor(player, position)) {
                remaining--;
            }
        }
        positions.put(player, position);
        return position;
    }

    private boolean isFreeFor(PlayerColor mover, int position) {
        return occupantOf(position).filter(other -> other != mover).isEmpty();
    }

    // ---------------------------------------------------------------- lapping

    /**
     * Returns how far behind the leader a player is, in spaces.
     *
     * @param player the player to measure
     * @return the gap, zero for the leader
     * @throws IllegalArgumentException if they are not on the route
     */
    public int gapToLeader(PlayerColor player) {
        int position = positionOf(player);
        return leader().map(this::positionOf).orElse(position) - position;
    }

    /**
     * Tells whether a player has been lapped.
     *
     * <p>More than a full lap behind the leader, which forces them out of the race
     * (manual p.20). Exactly one lap is not enough: the manual says "more than".
     *
     * @param player the player to check
     * @return {@code true} when the leader is more than a whole loop ahead
     * @throws IllegalArgumentException if they are not on the route
     */
    public boolean isLapped(PlayerColor player) {
        return gapToLeader(player) > length;
    }

    /**
     * Returns everyone the leader has lapped.
     *
     * <p>Checked only once the current adventure card is fully resolved, never in the
     * middle of one (manual p.20).
     *
     * @return the players who must give up
     */
    public Set<PlayerColor> lappedPlayers() {
        return positions.keySet().stream()
                .filter(this::isLapped)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Returns every player still flying, in no particular order.
     *
     * @return the players on the route
     */
    public Set<PlayerColor> players() {
        return Set.copyOf(positions.keySet());
    }

    /**
     * Returns where every marker stands, leader first.
     *
     * @return an ordered map from player to absolute position
     */
    public Map<PlayerColor, Integer> standings() {
        Map<PlayerColor, Integer> ordered = new LinkedHashMap<>();
        routeOrder().forEach(player -> ordered.put(player, positions.get(player)));
        return Collections.unmodifiableMap(ordered);
    }
}
