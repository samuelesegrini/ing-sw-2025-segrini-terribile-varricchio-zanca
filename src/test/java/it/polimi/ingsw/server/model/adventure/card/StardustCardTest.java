package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
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
 * Checks Stardust against manual p.13.
 *
 * <p>This is the card that makes tidy building pay: a ship's cost is exactly the number
 * of sides it left hanging out, and there is no defence and no choice. Two details decide
 * whether it is right — a connector counts once whatever its pipe count, and a smooth
 * side never counts at all — and both are the difference between a well built ship
 * shrugging the card off and being crippled by it.
 *
 * <p>Components involved: {@link StardustCard}, {@link Flight}, {@link Ship}.
 */
class StardustCardTest {

    private static final AdventureCardIdentity IDENTITY = new AdventureCardIdentity(
            "stardust_lvl1", AdventureCardType.STARDUST, CardLevel.LEVEL_I, true);

    /** A ship with the given number of extra hull pieces welded in a row to starboard. */
    private static Ship shipWithHull(int extraPieces) {
        Ship ship = Ships.openShip();
        for (int i = 0; i < extraPieces; i++) {
            Ships.put(ship, new Position(2, 3 + i), ComponentKind.STRUCTURAL_MODULE);
        }
        return ship;
    }

    private static Flight flightOf(Map<PlayerColor, Ship> ships) {
        return FlightFixtures.levelTwoFlight(ships);
    }

    private static Map<PlayerColor, Ship> twoShips(Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return ships;
    }

    @Test
    @DisplayName("a ship pays one flight day for each connector it leaves hanging out")
    void aShipPaysADayPerExposedConnector() {
        Ship bare = Ships.openShip();
        Flight flight = flightOf(twoShips(bare, Ships.openShip()));

        assertEquals(4, bare.exposedConnectors(), "a lone starting cabin shows all four sides");
        int before = flight.route().positionOf(PlayerColor.RED);

        new StardustCard(IDENTITY).resolve(flight).pending();

        assertEquals(before - 4, flight.route().positionOf(PlayerColor.RED));
    }

    @Test
    @DisplayName("the untidier ship pays more, which is the whole point of the card")
    void theUntidierShipPaysMore() {
        Ship tidy = Ships.openShip();
        Ship untidy = shipWithHull(2);
        Flight flight = flightOf(twoShips(tidy, untidy));

        int tidyBefore = flight.route().positionOf(PlayerColor.RED);
        int untidyBefore = flight.route().positionOf(PlayerColor.BLUE);

        new StardustCard(IDENTITY).resolve(flight).pending();

        int tidyPaid = tidyBefore - flight.route().positionOf(PlayerColor.RED);
        int untidyPaid = untidyBefore - flight.route().positionOf(PlayerColor.BLUE);

        assertTrue(untidyPaid > tidyPaid,
                "the ship with more sides showing paid more: " + untidyPaid + " against " + tidyPaid);
    }

    @Test
    @DisplayName("a player who gave up is not on the route and pays nothing")
    void aRetireePaysNothing() {
        Flight flight = flightOf(twoShips(Ships.openShip(), Ships.openShip()));
        flight.giveUp(PlayerColor.BLUE);

        new StardustCard(IDENTITY).resolve(flight).pending();

        assertThrows(IllegalArgumentException.class,
                () -> flight.route().positionOf(PlayerColor.BLUE));
        assertTrue(flight.hasGivenUp(PlayerColor.BLUE));
    }

    @Test
    @DisplayName("the card asks nobody anything, so it is finished the moment it is turned over")
    void theCardAsksNobodyAnything() {
        Flight flight = flightOf(twoShips(Ships.openShip(), Ships.openShip()));

        AdventureResolution resolution = new StardustCard(IDENTITY).resolve(flight);

        assertTrue(resolution.isComplete());
        assertThrows(IllegalStateException.class,
                () -> resolution.submit(new PlayerChoice.Leave(PlayerColor.RED)));
    }

    @Test
    @DisplayName("turning the card over twice does not charge anyone twice")
    void theCardAppliesOnlyOnce() {
        Flight flight = flightOf(twoShips(Ships.openShip(), Ships.openShip()));
        int before = flight.route().positionOf(PlayerColor.RED);

        AdventureResolution resolution = new StardustCard(IDENTITY).resolve(flight);
        resolution.pending();
        resolution.pending();
        resolution.pending();

        assertEquals(before - 4, flight.route().positionOf(PlayerColor.RED));
    }

    @Test
    @DisplayName("the card knows what it is, so deck building can place it without asking")
    void theCardKnowsWhatItIs() {
        StardustCard card = new StardustCard(IDENTITY);

        assertEquals(AdventureCardType.STARDUST, card.type());
        assertEquals(CardLevel.LEVEL_I, card.level());
        assertEquals("stardust_lvl1", card.identity().id());
    }
}
