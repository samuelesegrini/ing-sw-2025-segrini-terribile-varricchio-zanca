package it.polimi.ingsw.server.model.component;

import it.polimi.ingsw.server.model.ship.Connector;
import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.Rotation;

/**
 * A piece welded to a ship: a {@link Tile}, the way it is turned, and whatever state
 * the rules give it once it is in play.
 *
 * <p>The split between this and {@link Tile} is the point of the design. A tile is
 * printed cardboard, shared by every game on the server and never modified. A ship
 * component is one ship's copy of it, and only the three kinds that hold something —
 * batteries, cargo holds and cabins — carry any state at all.
 *
 * <p>The hierarchy is sealed so that a switch over it is exhaustive: adding a kind of
 * component becomes a compile error at every place that has to handle one, rather than
 * a silent fall-through.
 *
 * <p>Each variant exposes only what its kind can do. There is no {@code getCharges} on
 * a cannon to return zero, and no {@code muzzleDirection} on a cabin to throw.
 */
public sealed interface ShipComponent
        permits BatteryComponent, CabinComponent, CannonComponent, CargoHoldComponent,
        EngineComponent, LifeSupportComponent, ShieldComponent, StructuralComponent {

    /**
     * Returns the printed piece this component was made from.
     *
     * @return the tile
     */
    Tile tile();

    /**
     * Returns how far the piece is turned from its printed orientation.
     *
     * @return the rotation applied when it was placed
     */
    Rotation rotation();

    /**
     * Returns the identifier of the underlying piece.
     *
     * @return the tile identifier
     */
    default String id() {
        return tile().id();
    }

    /**
     * Returns what this component does.
     *
     * @return the kind of the underlying tile
     */
    default ComponentKind kind() {
        return tile().kind();
    }

    /**
     * Returns the connector this component shows on the given side, as placed.
     *
     * @param side the direction to look at
     * @return the connector a neighbouring cell sees from that direction
     */
    default Connector connectorFacing(Direction side) {
        return tile().connectorFacing(side, rotation());
    }

    /**
     * Wraps a printed piece into the component its kind calls for.
     *
     * <p>This is the only place that maps a {@link ComponentKind} onto a variant, so
     * the mapping cannot drift between call sites.
     *
     * @param tile     the printed piece
     * @param rotation how far it is turned
     * @return a component of the matching variant, with any state at its starting value
     */
    static ShipComponent place(Tile tile, Rotation rotation) {
        return switch (tile.kind()) {
            case STRUCTURAL_MODULE -> new StructuralComponent(tile, rotation);
            case SINGLE_CANNON, DOUBLE_CANNON -> new CannonComponent(tile, rotation);
            case SINGLE_ENGINE, DOUBLE_ENGINE -> new EngineComponent(tile, rotation);
            case SHIELD -> new ShieldComponent(tile, rotation);
            case PURPLE_LIFE_SUPPORT, BROWN_LIFE_SUPPORT -> new LifeSupportComponent(tile, rotation);
            case BATTERY -> new BatteryComponent(tile, rotation);
            case CARGO_HOLD, SPECIAL_CARGO_HOLD -> new CargoHoldComponent(tile, rotation);
            case CABIN, STARTING_CABIN -> new CabinComponent(tile, rotation);
        };
    }
}
