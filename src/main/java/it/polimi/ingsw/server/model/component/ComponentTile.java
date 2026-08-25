package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Direction;

import java.util.EnumMap;
import java.util.Map;

/**
 * One of the 152 tiles players draw from the shared pool.
 *
 * <p>A tile is immutable printed data. Everything that changes during a game — where
 * it sits, how it is turned, what it holds — belongs to the ship it is welded to and
 * to the {@link ShipComponent} that wraps it.
 *
 * <p>Starting cabins are not tiles: they are handed out at setup rather than drawn,
 * and they carry a player colour. They are modelled by {@link StartingCabinTile}.
 *
 * @param id         the tile's identifier, taken from its artwork file name
 * @param kind       what the tile does
 * @param connectors what each of the four printed sides carries
 * @param capacity   printed slots or charges, or {@code 0} when the kind has none
 */
public record ComponentTile(String id, ComponentKind kind, Map<Direction, Connector> connectors, int capacity)
        implements Tile {

    /**
     * Validates the tile and takes a defensive copy of its connectors.
     *
     * @throws IllegalArgumentException if the identifier is blank, the kind is a
     *                                  starting cabin, a side is missing, or the
     *                                  capacity does not match the kind
     * @throws NullPointerException     if the kind or the connector map is {@code null}
     */
    public ComponentTile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a component tile needs an identifier");
        }
        if (kind == ComponentKind.STARTING_CABIN) {
            throw new IllegalArgumentException(id + ": starting cabins are not drawable tiles");
        }
        connectors = sidesOf(id, connectors);
        if (kind.hasCapacity() == (capacity <= 0)) {
            throw new IllegalArgumentException(
                    id + ": " + kind + " has capacity " + capacity + ", which its kind does not allow");
        }
    }

    /**
     * Copies the four sides into an immutable map, rejecting an incomplete set.
     *
     * @param id         the tile identifier, used in the failure message
     * @param connectors the sides to copy
     * @return an immutable map holding all four directions
     */
    static Map<Direction, Connector> sidesOf(String id, Map<Direction, Connector> connectors) {
        Map<Direction, Connector> copy = new EnumMap<>(Direction.class);
        copy.putAll(connectors);
        if (copy.size() != Direction.values().length) {
            throw new IllegalArgumentException(id + ": a tile has four sides, got " + copy.keySet());
        }
        return Map.copyOf(copy);
    }
}
