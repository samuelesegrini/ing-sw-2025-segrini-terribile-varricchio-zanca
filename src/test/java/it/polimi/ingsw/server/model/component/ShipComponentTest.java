package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that {@link ShipComponent#place} produces the right variant for every kind,
 * and that a variant refuses a tile it cannot represent.
 *
 * <p>This mapping is the one place a kind turns into a behaviour. If it drifts, a
 * cabin could end up wrapped as a cannon and the mistake would only surface much later,
 * far from its cause.
 *
 * <p>Components involved: {@link ShipComponent} and all eight of its variants,
 * {@link Tile}.
 */
class ShipComponentTest {

    @DisplayName("every component kind maps to a variant, so no kind is left unhandled")
    @ParameterizedTest(name = "{0}")
    @EnumSource(ComponentKind.class)
    void everyKind_producesAComponent(ComponentKind kind) {
        Tile tile = kind == ComponentKind.STARTING_CABIN
                ? Tiles.startingCabin(PlayerColor.BLUE)
                : Tiles.of(kind, kind.hasCapacity() ? 2 : 0);

        ShipComponent placed = ShipComponent.place(tile, Rotation.NONE);

        assertEquals(kind, placed.kind());
        assertEquals(tile.id(), placed.id());
    }

    @Test
    @DisplayName("each kind is wrapped in the variant that exposes what it can do")
    void kinds_mapToTheirOwnVariants() {
        assertInstanceOf(StructuralComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.STRUCTURAL_MODULE), Rotation.NONE));
        assertInstanceOf(CannonComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.DOUBLE_CANNON), Rotation.NONE));
        assertInstanceOf(EngineComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.SINGLE_ENGINE), Rotation.NONE));
        assertInstanceOf(ShieldComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.SHIELD), Rotation.NONE));
        assertInstanceOf(LifeSupportComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.PURPLE_LIFE_SUPPORT), Rotation.NONE));
        assertInstanceOf(BatteryComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.BATTERY, 3), Rotation.NONE));
        assertInstanceOf(CargoHoldComponent.class,
                ShipComponent.place(Tiles.of(ComponentKind.CARGO_HOLD, 2), Rotation.NONE));
        assertInstanceOf(CabinComponent.class,
                ShipComponent.place(Tiles.startingCabin(PlayerColor.RED), Rotation.NONE));
    }

    @Test
    @DisplayName("a variant refuses a tile of a kind it cannot represent")
    void variant_refusesAForeignTile() {
        ComponentTile cabin = Tiles.of(ComponentKind.CABIN);

        assertThrows(IllegalArgumentException.class, () -> new CannonComponent(cabin, Rotation.NONE));
        assertThrows(IllegalArgumentException.class, () -> new EngineComponent(cabin, Rotation.NONE));
        assertThrows(IllegalArgumentException.class, () -> new BatteryComponent(cabin, Rotation.NONE));
    }

    @Test
    @DisplayName("a placed component reports the side it actually shows, not the printed one")
    void placedComponent_reportsRotatedSides() {
        ComponentTile tile = new ComponentTile("test", ComponentKind.STRUCTURAL_MODULE,
                Tiles.sides(Connector.SINGLE, Connector.DOUBLE, Connector.PLAIN, Connector.UNIVERSAL), 0);

        ShipComponent placed = ShipComponent.place(tile, Rotation.CLOCKWISE_90);

        assertEquals(Connector.SINGLE, placed.connectorFacing(Direction.EAST));
        assertEquals(Connector.UNIVERSAL, placed.connectorFacing(Direction.NORTH));
    }
}
