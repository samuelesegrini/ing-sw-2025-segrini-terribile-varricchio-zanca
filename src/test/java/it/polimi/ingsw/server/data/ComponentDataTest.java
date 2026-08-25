package it.polimi.ingsw.server.data;

import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the shipped component catalogue against the counts printed on page 3 of the
 * manual, and against the tile artwork.
 *
 * <p>These assertions are the reason the data can be trusted by every rule that reads
 * it. They caught two defects when they were first written: a battery tile whose
 * artwork shows three cells but whose data said two, and a double engine whose
 * connector sat on the wrong side.
 *
 * <p>Components involved: {@link GameDataLoader}, {@link ComponentTile},
 * {@link ComponentKind}.
 */
class ComponentDataTest {

    private static GameData data;

    @BeforeAll
    static void loadCatalogue() {
        data = GameDataLoader.loadBundled();
    }

    @DisplayName("the number of tiles of each kind matches the manual's component overview")
    @ParameterizedTest(name = "{0} appears {1} times")
    @CsvSource({
            "STRUCTURAL_MODULE, 8",
            "SINGLE_CANNON, 25",
            "DOUBLE_CANNON, 11",
            "SINGLE_ENGINE, 21",
            "DOUBLE_ENGINE, 9",
            "CABIN, 17",
            "CARGO_HOLD, 15",
            "SPECIAL_CARGO_HOLD, 9",
            "BATTERY, 17",
            "SHIELD, 8",
            "PURPLE_LIFE_SUPPORT, 6",
            "BROWN_LIFE_SUPPORT, 6"
    })
    void tilesOfKind_matchTheManualCount(ComponentKind kind, int expected) {
        assertEquals(expected, data.tilesOfKind(kind).size(), kind + " count differs from manual p.3");
    }

    @Test
    @DisplayName("the drawable pool holds 152 tiles, with the four starting cabins kept apart")
    void drawablePool_holdsOneHundredAndFiftyTwoTiles() {
        assertEquals(152, data.tiles().size());
        assertEquals(PlayerColor.values().length, data.startingCabins().size());
    }

    @Test
    @DisplayName("batteries split eleven two-charge to six three-charge, as printed on manual p.3")
    void batteryCapacities_splitElevenToSix() {
        Map<Integer, Long> byCapacity = data.tilesOfKind(ComponentKind.BATTERY).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ComponentTile::capacity, java.util.stream.Collectors.counting()));
        assertEquals(Map.of(2, 11L, 3, 6L), byCapacity);
    }

    @Test
    @DisplayName("cargo holds carry two or three slots and special holds one or two")
    void cargoCapacities_stayWithinTheirPrintedRanges() {
        data.tilesOfKind(ComponentKind.CARGO_HOLD)
                .forEach(tile -> assertTrue(Set.of(2, 3).contains(tile.capacity()),
                        tile.id() + " has an impossible capacity of " + tile.capacity()));
        data.tilesOfKind(ComponentKind.SPECIAL_CARGO_HOLD)
                .forEach(tile -> assertTrue(Set.of(1, 2).contains(tile.capacity()),
                        tile.id() + " has an impossible capacity of " + tile.capacity()));
    }

    @Test
    @DisplayName("every tile carries a connector on at least one side, so it can be welded to a ship")
    void everyTile_hasSomewhereToWeld() {
        data.tiles().forEach(tile -> assertTrue(
                tile.connectors().values().stream().anyMatch(Connector::isConnector),
                tile.id() + " is smooth on all four sides and could never be attached"));
    }

    @Test
    @DisplayName("every starting cabin is universal on all four sides, so any tile can attach to it")
    void startingCabins_areUniversalOnEverySide() {
        data.startingCabins().values().forEach(cabin ->
                cabin.connectors().forEach((side, connector) -> assertEquals(
                        Connector.UNIVERSAL, connector,
                        cabin.id() + " is not universal facing " + side)));
    }

    @Test
    @DisplayName("engines exhaust south when unrotated, which is what makes the placement rule a rotation check")
    void everyEngine_exhaustsSouthWhenUnrotated() {
        data.tiles().stream()
                .filter(tile -> tile.kind().isEngine())
                .forEach(tile -> assertEquals(Direction.SOUTH, tile.exhaustDirection(Rotation.NONE), tile.id()));
    }

    @Test
    @DisplayName("cannons face north when unrotated, so a forward cannon is one at rotation NONE")
    void everyCannon_facesNorthWhenUnrotated() {
        data.tiles().stream()
                .filter(tile -> tile.kind().isCannon())
                .forEach(tile -> assertEquals(Direction.NORTH, tile.muzzleDirection(Rotation.NONE), tile.id()));
    }

    @Test
    @DisplayName("shields cover north and east when unrotated, always two adjacent sides")
    void everyShield_coversTwoAdjacentSides() {
        data.tilesOfKind(ComponentKind.SHIELD).forEach(tile ->
                assertEquals(Set.of(Direction.NORTH, Direction.EAST), tile.shieldedSides(Rotation.NONE), tile.id()));
    }
}
