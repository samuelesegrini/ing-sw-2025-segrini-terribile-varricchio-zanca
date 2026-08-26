package it.polimi.ingsw.server.projection;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.BatteryPlan;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.protocol.view.BuildingView;
import it.polimi.ingsw.common.protocol.view.CellView;
import it.polimi.ingsw.common.protocol.view.FlightView;
import it.polimi.ingsw.common.protocol.view.ShipView;
import it.polimi.ingsw.common.protocol.view.TileView;
import it.polimi.ingsw.server.model.board.ShipBoardSpec;
import it.polimi.ingsw.server.model.building.BuildingTimer;
import it.polimi.ingsw.server.model.building.ComponentPool;
import it.polimi.ingsw.server.model.building.ShipBuilder;
import it.polimi.ingsw.server.model.building.StartSpaces;
import it.polimi.ingsw.server.model.component.BatteryComponent;
import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.server.model.component.CannonComponent;
import it.polimi.ingsw.server.model.component.CargoHoldComponent;
import it.polimi.ingsw.server.model.component.EngineComponent;
import it.polimi.ingsw.server.model.component.LifeSupportComponent;
import it.polimi.ingsw.server.model.component.ShieldComponent;
import it.polimi.ingsw.server.model.component.ShipComponent;
import it.polimi.ingsw.server.model.component.StructuralComponent;
import it.polimi.ingsw.server.model.component.Tile;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.Route;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the model into the views the protocol carries.
 *
 * <p>The mapping layer architecture § 3.1 said projections would cost. It buys the thing that
 * makes the rest of the design hold: the client never receives a {@code Ship}, so no
 * information leaks by accident, no domain class has to be {@code Serializable}, and the wire
 * format can change without the model noticing.
 *
 * <p>Everything here is a pure function of the model. Nothing decides anything, nothing
 * mutates anything, and calling it twice gives the same answer twice — which is why the whole
 * class is static methods rather than an object with a lifetime.
 */
public final class Projections {

    private Projections() {
    }

    // ------------------------------------------------------------------ ships

    /**
     * Projects a ship, as everybody at the table can see it.
     *
     * <p>There is one ship view and every player gets the same one. Ships are public in this
     * game — requirement G6, and the manual has players looking at each other's work — so
     * building it per recipient would add a chance for a projection to leak by being built
     * differently for different people, in exchange for nothing.
     *
     * @param ship the ship
     * @return what it looks like
     */
    public static ShipView of(Ship ship) {
        ShipBoardSpec board = ship.board();
        Set<Position> outline = new LinkedHashSet<>();
        for (int row = 0; row < board.rows(); row++) {
            for (int column = 0; column < board.columns(); column++) {
                Position cell = new Position(row, column);
                if (board.isUsable(cell)) {
                    outline.add(cell);
                }
            }
        }

        Map<Position, CellView> cells = new LinkedHashMap<>();
        ship.components().forEach((cell, component) -> cells.put(cell, cellOf(component)));

        return new ShipView(board.rows(), board.columns(),
                board.firstPrintedRow(), board.firstPrintedColumn(), outline, cells, List.of(),
                ship.lostComponentCount(),
                // With nothing powered: a view showing what a ship could manage with every
                // battery spent would be showing a number the player cannot act on.
                ship.attributes(BatteryPlan.none()),
                ship.validate());
    }

    /**
     * Projects a ship along with the tiles its owner has set aside.
     *
     * <p>Reserved tiles are public — they sit in the two printed slots where everyone can see
     * them — and they matter to everyone, because a reserved tile never welded counts against
     * its owner at scoring time (p.7).
     *
     * @param ship     the ship
     * @param reserved the tiles set aside
     * @return what it looks like
     */
    public static ShipView of(Ship ship, List<? extends Tile> reserved) {
        ShipView bare = of(ship);
        return new ShipView(bare.rows(), bare.columns(), bare.firstPrintedRow(),
                bare.firstPrintedColumn(), bare.outline(), bare.cells(),
                reserved.stream().map(Projections::tileOf).toList(),
                bare.lostComponents(), bare.attributes(), bare.validation());
    }

    /**
     * Projects one welded component and whatever it holds.
     *
     * <p>The switch is exhaustive over a sealed hierarchy on purpose. A ninth kind of
     * component cannot be added without somebody being made to say what it looks like, which
     * is the alternative to a view that silently draws it as an empty square.
     */
    private static CellView cellOf(ShipComponent component) {
        TileView tile = tileOf(component.tile(), component.rotation());
        return switch (component) {
            case BatteryComponent battery ->
                    new CellView(tile, battery.charges(), List.of(), 0, null);
            case CargoHoldComponent hold ->
                    new CellView(tile, 0, hold.contents(), 0, null);
            case CabinComponent cabin ->
                    new CellView(tile, 0, List.of(), cabin.humans(), cabin.alien().orElse(null));
            // Written out rather than left to a default, because a default is exactly what
            // would let a ninth kind of component be drawn as an empty square by nobody's
            // decision.
            case CannonComponent ignored -> empty(tile);
            case EngineComponent ignored -> empty(tile);
            case ShieldComponent ignored -> empty(tile);
            case LifeSupportComponent ignored -> empty(tile);
            case StructuralComponent ignored -> empty(tile);
        };
    }

    private static CellView empty(TileView tile) {
        return new CellView(tile, 0, List.of(), 0, null);
    }

    private static TileView tileOf(Tile tile) {
        return tileOf(tile, it.polimi.ingsw.common.game.Rotation.NONE);
    }

    private static TileView tileOf(Tile tile, it.polimi.ingsw.common.game.Rotation rotation) {
        Map<Direction, Connector> facing = new EnumMap<>(Direction.class);
        for (Direction side : Direction.values()) {
            facing.put(side, tile.connectorFacing(side, rotation));
        }
        // The connectors are given as they face, with the rotation already applied, so that a
        // view can draw a ship without knowing how a tile turns. The rotation travels too,
        // because the artwork still has to be turned by that much.
        return new TileView(tile.id(), tile.kind(), rotation, facing);
    }

    // ------------------------------------------------------------------ the shipyard

    /**
     * Projects the shipyard, for one player.
     *
     * <p>Built per recipient, and this is the only reason a {@code GameView} has to be. Almost
     * everything about the shipyard is public — the heap, the discard pile, who has finished,
     * which start spaces are free — but the tile in a player's hand and the pile they are
     * peeking at are not, and sending either to the table would give away what the manual
     * keeps private (p.7, p.17).
     *
     * @param builder     the recipient's own shipyard
     * @param pool        the tiles everybody is drawing from
     * @param timer       the hourglass
     * @param starts      the starting line
     * @param finished    who has declared their ship done
     * @param peekedCards what the recipient can see of the pile they picked up, empty if none
     * @return what the shipyard looks like to this player
     */
    public static BuildingView of(ShipBuilder builder, ComponentPool pool, BuildingTimer timer,
                                  StartSpaces starts, Set<PlayerColor> finished,
                                  List<AdventureCardIdentity> peekedCards) {
        return new BuildingView(
                pool.faceDownCount(),
                pool.faceUp().stream().map(Projections::tileOf).toList(),
                builder.inHand().map(Projections::tileOf).orElse(null),
                List.copyOf(peekedCards),
                timer.isInPlay() ? timer.space() : null,
                timer.spaces(),
                timer.isRunning() ? Math.max(0, timer.remaining().toSeconds()) : 0,
                Set.copyOf(finished),
                starts.free());
    }

    // ------------------------------------------------------------------ the route

    /**
     * Projects the route and the card on the table.
     *
     * @param flight    the flight
     * @param card      the card being resolved, {@code null} between cards
     * @param cardsLeft how many the deck still holds
     * @return what the route looks like
     */
    public static FlightView of(Flight flight, AdventureCardIdentity card, int cardsLeft) {
        Route route = flight.route();
        return new FlightView(route.length(), route.standings(), route.routeOrder(),
                card, cardsLeft);
    }
}
