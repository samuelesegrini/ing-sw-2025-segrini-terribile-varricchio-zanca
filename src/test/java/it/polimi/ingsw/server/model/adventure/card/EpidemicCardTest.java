package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.common.game.AlienColor;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Connector;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.common.game.Rotation;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks Epidemic against manual p.19.
 *
 * <p>The card that punishes packing crew quarters together — the manual's own advice is to
 * build so that no two cabins touch. Two words in the rule do all the work, and both get
 * their own test.
 *
 * <p><b>Occupied</b>: an empty cabin neither catches the infection nor passes it on, so a
 * ship that emptied one earlier gets off lighter. <b>Welded</b>: cabins merely sitting
 * side by side, smooth against smooth, are not joined and are not touched.
 *
 * <p>The third thing worth pinning is that the infected set is worked out before anybody
 * is removed. Taking crew one cabin at a time could empty a cabin and spare its
 * neighbour, which would make the outcome depend on the order the cabins happen to be
 * listed in.
 *
 * <p>Components involved: {@link EpidemicCard}, {@link Ship}, {@link Flight}.
 */
class EpidemicCardTest {

    private static final AdventureCardIdentity IDENTITY = new AdventureCardIdentity(
            "epidemic_lvl2", AdventureCardType.EPIDEMIC, CardLevel.LEVEL_II, false);

    private static final Position NORTH_CABIN = new Position(1, 2);
    private static final Position FAR_CABIN = new Position(2, 0);
    private static final Position BRIDGE = new Position(2, 1);

    /** Places a cabin whose north side is smooth, so it never welds to whatever is above it. */
    private static void putCabinWithSmoothTop(Ship ship, Position cell) {
        ship.place(cell, new ComponentTile("cabin-smooth-top", ComponentKind.CABIN,
                Tiles.sides(Connector.PLAIN, Connector.UNIVERSAL,
                        Connector.UNIVERSAL, Connector.UNIVERSAL), 0), Rotation.NONE);
    }

    /** Places a cabin whose south side is smooth, the other half of an unwelded pair. */
    private static void putCabinWithSmoothBottom(Ship ship, Position cell) {
        ship.place(cell, new ComponentTile("cabin-smooth-bottom", ComponentKind.CABIN,
                Tiles.sides(Connector.UNIVERSAL, Connector.UNIVERSAL,
                        Connector.PLAIN, Connector.UNIVERSAL), 0), Rotation.NONE);
    }

    private static Flight flightWith(Ship ship) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, ship);
        ships.put(PlayerColor.BLUE, Ships.openShip());
        return FlightFixtures.levelTwoFlight(ships);
    }

    private static void turnOver(Flight flight) {
        new EpidemicCard(IDENTITY).resolve(flight).pending();
    }

    @Test
    @DisplayName("two occupied cabins welded together each lose somebody")
    void weldedOccupiedCabinsBothLoseSomebody() {
        Ship ship = Ships.openShip();
        Ships.put(ship, NORTH_CABIN, ComponentKind.CABIN);
        ship.fillRemainingCabinsWithHumans();

        assertEquals(4, ship.crewCount());

        turnOver(flightWith(ship));

        assertEquals(2, ship.crewCount(), "one bunk emptied in each of the two joined cabins");
    }

    @Test
    @DisplayName("a lone cabin has nobody to catch it from")
    void aLoneCabinIsUntouched() {
        Ship ship = Ships.openShip();
        ship.fillRemainingCabinsWithHumans();

        turnOver(flightWith(ship));

        assertEquals(2, ship.crewCount());
    }

    @Test
    @DisplayName("cabins side by side but not welded are untouched, on a ship that is otherwise legal")
    void unweldedCabinsAreUntouched() {
        Ship ship = Ships.openShip();
        // Both cabins reach the ship through the hull pieces to their right, and meet each
        // other smooth against smooth — legal to build, and not a joint.
        Ships.put(ship, BRIDGE, ComponentKind.STRUCTURAL_MODULE);
        Ships.put(ship, new Position(1, 1), ComponentKind.STRUCTURAL_MODULE);
        putCabinWithSmoothTop(ship, FAR_CABIN);
        putCabinWithSmoothBottom(ship, new Position(1, 0));
        ship.fillRemainingCabinsWithHumans();

        assertTrue(ship.validate().isLegal());
        assertEquals(6, ship.crewCount());

        turnOver(flightWith(ship));

        assertEquals(6, ship.crewCount(), "no two occupied cabins are welded to each other");
    }

    @Test
    @DisplayName("an empty cabin neither catches the infection nor passes it on")
    void anEmptyCabinBreaksTheChain() {
        Ship ship = Ships.openShip();
        Ships.put(ship, NORTH_CABIN, ComponentKind.CABIN);
        Ships.put(ship, new Position(0, 2), ComponentKind.CABIN);
        ship.boardHumansIn(Ships.CABIN);
        ship.boardHumansIn(new Position(0, 2));

        assertEquals(4, ship.crewCount(), "the middle cabin was left empty");

        turnOver(flightWith(ship));

        assertEquals(4, ship.crewCount(), "the empty cabin between them broke the chain");
    }

    @Test
    @DisplayName("a chain of three joined cabins loses one from each, worked out before anybody leaves")
    void aChainOfThreeLosesOneEach() {
        Ship ship = Ships.openShip();
        Ships.put(ship, NORTH_CABIN, ComponentKind.CABIN);
        Ships.put(ship, new Position(0, 2), ComponentKind.CABIN);
        ship.fillRemainingCabinsWithHumans();

        assertEquals(6, ship.crewCount());

        turnOver(flightWith(ship));

        assertEquals(3, ship.crewCount(), "all three were joined to an occupied neighbour");
    }

    @Test
    @DisplayName("an alien caught by the infection leaves, and it is the only one aboard that cabin")
    void anAlienCanBeTheOneToLeave() {
        Ship ship = Ships.openShip();
        Ships.put(ship, NORTH_CABIN, ComponentKind.CABIN);
        Ships.put(ship, new Position(0, 2), ComponentKind.PURPLE_LIFE_SUPPORT);
        ship.boardAlienIn(NORTH_CABIN, AlienColor.PURPLE);
        ship.fillRemainingCabinsWithHumans();

        assertEquals(3, ship.crewCount(), "two humans in the starting cabin and one alien");

        turnOver(flightWith(ship));

        assertEquals(1, ship.crewCount(), "the alien left and the starting cabin lost a human");
        assertTrue(ship.aliens().isEmpty());
    }

    @Test
    @DisplayName("a player who gave up is not aboard the route and catches nothing")
    void aRetireeCatchesNothing() {
        Ship ship = Ships.openShip();
        Ships.put(ship, NORTH_CABIN, ComponentKind.CABIN);
        ship.fillRemainingCabinsWithHumans();

        Flight flight = flightWith(ship);
        flight.giveUp(PlayerColor.RED);

        turnOver(flight);

        assertEquals(4, ship.crewCount());
    }

    @Test
    @DisplayName("the card asks nobody anything and applies only once")
    void theCardAsksNobodyAnything() {
        Ship ship = Ships.openShip();
        Ships.put(ship, NORTH_CABIN, ComponentKind.CABIN);
        ship.fillRemainingCabinsWithHumans();

        AdventureResolution resolution = new EpidemicCard(IDENTITY).resolve(flightWith(ship));
        resolution.pending();
        resolution.pending();

        assertTrue(resolution.isComplete());
        assertEquals(2, ship.crewCount(), "turning the card over twice does not infect anyone twice");
        assertThrows(IllegalStateException.class,
                () -> resolution.submit(new PlayerChoice.Leave(PlayerColor.RED)));
    }
}
