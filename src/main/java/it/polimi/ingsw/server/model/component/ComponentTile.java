package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * One of the 152 tiles players draw from the shared pool.
 *
 * <p>A tile is immutable printed data. Everything that changes during a game — where
 * it sits, how it is turned, what it holds — belongs to the ship it is welded to.
 *
 * <p>Starting cabins are not tiles: they are handed out at setup rather than drawn,
 * and they carry a player colour. They are modelled by {@link StartingCabinTile}.
 *
 * @param id         the tile's identifier, taken from its artwork file name
 * @param kind       what the tile does
 * @param connectors what each of the four printed sides carries
 * @param capacity   printed slots or charges, or {@code 0} when the kind has none
 */
public record ComponentTile(String id, ComponentKind kind, Map<Direction, Connector> connectors, int capacity) {

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

    /**
     * Returns the connector that faces the given direction once the tile is turned.
     *
     * @param side     the direction to look at on the placed tile
     * @param rotation how far the tile is turned from its printed orientation
     * @return the connector the neighbouring cell sees
     */
    public Connector connectorFacing(Direction side, Rotation rotation) {
        return connectors.get(side.rotatedBy(rotation.inverse()));
    }

    /**
     * Returns where this engine's exhaust points once the tile is turned.
     *
     * <p>Every engine is printed with its exhaust pointing {@link Direction#SOUTH},
     * verified across all 30 engine tiles, so the rule that engines must fire toward
     * the stern reduces to {@link Rotation#NONE}.
     *
     * @param rotation how far the tile is turned
     * @return the direction the exhaust points
     * @throws IllegalStateException if this tile is not an engine
     */
    public Direction exhaustDirection(Rotation rotation) {
        if (!kind.isEngine()) {
            throw new IllegalStateException(id + " is not an engine");
        }
        return Direction.SOUTH.rotatedBy(rotation);
    }

    /**
     * Returns where this cannon's muzzle points once the tile is turned.
     *
     * <p>Every cannon is printed facing {@link Direction#NORTH}, verified across all 36
     * cannon tiles. A cannon facing north counts full firepower; any other facing
     * counts half.
     *
     * @param rotation how far the tile is turned
     * @return the direction the muzzle points
     * @throws IllegalStateException if this tile is not a cannon
     */
    public Direction muzzleDirection(Rotation rotation) {
        if (!kind.isCannon()) {
            throw new IllegalStateException(id + " is not a cannon");
        }
        return Direction.NORTH.rotatedBy(rotation);
    }

    /**
     * Returns the two sides this shield protects once the tile is turned.
     *
     * <p>Every shield is printed covering north and east, verified across all eight
     * shield tiles. Manual p.7 notes that where the shield sits on the ship makes no
     * difference; only which way it faces does.
     *
     * @param rotation how far the tile is turned
     * @return the two protected directions
     * @throws IllegalStateException if this tile is not a shield
     */
    public Set<Direction> shieldedSides(Rotation rotation) {
        if (kind != ComponentKind.SHIELD) {
            throw new IllegalStateException(id + " is not a shield");
        }
        return Set.of(Direction.NORTH.rotatedBy(rotation), Direction.EAST.rotatedBy(rotation));
    }
}
