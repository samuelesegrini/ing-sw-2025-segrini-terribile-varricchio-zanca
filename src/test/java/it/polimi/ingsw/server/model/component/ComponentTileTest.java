package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks how a {@link ComponentTile} presents itself once it is turned.
 *
 * <p>Rotation is where placement bugs hide: a tile is catalogued in its printed
 * orientation, and every connection check, engine clearance check and shield lookup
 * has to ask what the tile looks like after the turn rather than before it.
 */
class ComponentTileTest {

    private static Map<Direction, Connector> sides(Connector north, Connector east,
                                                   Connector south, Connector west) {
        return Tiles.sides(north, east, south, west);
    }

    private static ComponentTile structural(Connector north, Connector east,
                                            Connector south, Connector west) {
        return new ComponentTile("test-tile", ComponentKind.STRUCTURAL_MODULE,
                sides(north, east, south, west), 0);
    }

    @Test
    @DisplayName("an unrotated tile shows each side exactly as printed")
    void unrotatedTile_showsPrintedSides() {
        ComponentTile tile = structural(Connector.SINGLE, Connector.DOUBLE, Connector.PLAIN, Connector.UNIVERSAL);

        assertEquals(Connector.SINGLE, tile.connectorFacing(Direction.NORTH, Rotation.NONE));
        assertEquals(Connector.DOUBLE, tile.connectorFacing(Direction.EAST, Rotation.NONE));
        assertEquals(Connector.PLAIN, tile.connectorFacing(Direction.SOUTH, Rotation.NONE));
        assertEquals(Connector.UNIVERSAL, tile.connectorFacing(Direction.WEST, Rotation.NONE));
    }

    @Test
    @DisplayName("a quarter turn clockwise moves the printed north side onto the east face")
    void quarterTurn_movesNorthOntoEast() {
        ComponentTile tile = structural(Connector.SINGLE, Connector.DOUBLE, Connector.PLAIN, Connector.UNIVERSAL);

        assertEquals(Connector.SINGLE, tile.connectorFacing(Direction.EAST, Rotation.CLOCKWISE_90));
        assertEquals(Connector.DOUBLE, tile.connectorFacing(Direction.SOUTH, Rotation.CLOCKWISE_90));
        assertEquals(Connector.PLAIN, tile.connectorFacing(Direction.WEST, Rotation.CLOCKWISE_90));
        assertEquals(Connector.UNIVERSAL, tile.connectorFacing(Direction.NORTH, Rotation.CLOCKWISE_90));
    }

    @Test
    @DisplayName("an engine turned a quarter clockwise exhausts west, which no legal ship allows")
    void rotatedEngine_exhaustsAwayFromTheStern() {
        ComponentTile engine = new ComponentTile("engine", ComponentKind.SINGLE_ENGINE,
                sides(Connector.UNIVERSAL, Connector.PLAIN, Connector.PLAIN, Connector.PLAIN), 0);

        assertEquals(Direction.SOUTH, engine.exhaustDirection(Rotation.NONE));
        assertEquals(Direction.WEST, engine.exhaustDirection(Rotation.CLOCKWISE_90));
        assertEquals(Direction.NORTH, engine.exhaustDirection(Rotation.CLOCKWISE_180));
    }

    @Test
    @DisplayName("a cannon turned half way round points at the player, halving its firepower")
    void rotatedCannon_pointsWhereItIsTurned() {
        ComponentTile cannon = new ComponentTile("cannon", ComponentKind.SINGLE_CANNON,
                sides(Connector.PLAIN, Connector.PLAIN, Connector.DOUBLE, Connector.PLAIN), 0);

        assertEquals(Direction.NORTH, cannon.muzzleDirection(Rotation.NONE));
        assertEquals(Direction.SOUTH, cannon.muzzleDirection(Rotation.CLOCKWISE_180));
    }

    @Test
    @DisplayName("a shield turned a quarter clockwise covers east and south instead of north and east")
    void rotatedShield_coversTheTurnedPairOfSides() {
        ComponentTile shield = new ComponentTile("shield", ComponentKind.SHIELD,
                sides(Connector.PLAIN, Connector.PLAIN, Connector.SINGLE, Connector.UNIVERSAL), 0);

        assertEquals(Set.of(Direction.NORTH, Direction.EAST), shield.shieldedSides(Rotation.NONE));
        assertEquals(Set.of(Direction.EAST, Direction.SOUTH), shield.shieldedSides(Rotation.CLOCKWISE_90));
        assertEquals(Set.of(Direction.WEST, Direction.NORTH), shield.shieldedSides(Rotation.CLOCKWISE_270));
    }

    @Test
    @DisplayName("asking a cabin where its exhaust points is a programming error, not a rules question")
    void nonEngine_rejectsExhaustQuery() {
        ComponentTile cabin = new ComponentTile("cabin", ComponentKind.CABIN,
                sides(Connector.SINGLE, Connector.SINGLE, Connector.SINGLE, Connector.SINGLE), 0);

        assertThrows(IllegalStateException.class, () -> cabin.exhaustDirection(Rotation.NONE));
        assertThrows(IllegalStateException.class, () -> cabin.muzzleDirection(Rotation.NONE));
        assertThrows(IllegalStateException.class, () -> cabin.shieldedSides(Rotation.NONE));
    }

    @Test
    @DisplayName("a kind that carries no capacity rejects one, so a mistyped data file cannot slip through")
    void capacityMustMatchTheKind() {
        Map<Direction, Connector> sides = sides(Connector.SINGLE, Connector.PLAIN, Connector.PLAIN, Connector.PLAIN);

        assertThrows(IllegalArgumentException.class,
                () -> new ComponentTile("odd", ComponentKind.STRUCTURAL_MODULE, sides, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new ComponentTile("odd", ComponentKind.BATTERY, sides, 0));
    }

    @Test
    @DisplayName("a starting cabin is not a drawable tile, because it is handed out rather than drawn")
    void startingCabin_isNotADrawableTile() {
        Map<Direction, Connector> sides = sides(Connector.UNIVERSAL, Connector.UNIVERSAL,
                Connector.UNIVERSAL, Connector.UNIVERSAL);

        assertThrows(IllegalArgumentException.class,
                () -> new ComponentTile("start", ComponentKind.STARTING_CABIN, sides, 0));
    }
}
