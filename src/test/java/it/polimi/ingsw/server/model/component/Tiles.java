package it.polimi.ingsw.server.model.component;


import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.PlayerColor;
import java.util.EnumMap;
import java.util.Map;

/**
 * Builds tiles for tests.
 *
 * <p>Most tests care about one property of a tile — its kind, or one of its sides —
 * and nothing about the other three. Spelling out four connectors at every call site
 * buries the property under test in noise, so this fills in a universal-on-every-side
 * default and lets a test override only what it is about.
 *
 * <p>Public because the ship tests build ships out of tiles too, and two copies of this
 * would drift apart.
 */
public final class Tiles {

    private Tiles() {
    }

    /** Returns a map with the same connector on all four sides. */
    public static Map<Direction, Connector> allSides(Connector connector) {
        Map<Direction, Connector> sides = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            sides.put(direction, connector);
        }
        return sides;
    }

    /** Returns the four sides in north, east, south, west order. */
    public static Map<Direction, Connector> sides(Connector north, Connector east, Connector south, Connector west) {
        Map<Direction, Connector> connectors = new EnumMap<>(Direction.class);
        connectors.put(Direction.NORTH, north);
        connectors.put(Direction.EAST, east);
        connectors.put(Direction.SOUTH, south);
        connectors.put(Direction.WEST, west);
        return connectors;
    }

    /** Returns a tile of the given kind, universal on every side, with no capacity. */
    public static ComponentTile of(ComponentKind kind) {
        return new ComponentTile(kind.name().toLowerCase() + "-tile", kind, allSides(Connector.UNIVERSAL), 0);
    }

    /** Returns a tile of the given kind and capacity, universal on every side. */
    public static ComponentTile of(ComponentKind kind, int capacity) {
        return new ComponentTile(kind.name().toLowerCase() + "-tile", kind, allSides(Connector.UNIVERSAL), capacity);
    }

    /** Returns a starting cabin for the given player. */
    public static StartingCabinTile startingCabin(PlayerColor color) {
        return new StartingCabinTile("starting-cabin-" + color.name().toLowerCase(),
                color, allSides(Connector.UNIVERSAL));
    }
}
