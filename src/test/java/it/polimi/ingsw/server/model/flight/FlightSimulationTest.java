package it.polimi.ingsw.server.model.flight;

import it.polimi.ingsw.server.model.component.CabinComponent;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.server.model.ship.Defence;
import it.polimi.ingsw.server.model.ship.DamageReport;
import it.polimi.ingsw.common.game.Direction;
import it.polimi.ingsw.server.model.ship.Hit;
import it.polimi.ingsw.common.game.HitKind;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flies a whole scripted flight through the model, from launch to the final table.
 *
 * <p>Every step below is covered by a unit test of its own. What this one checks is that
 * they compose: that a ship moving changes who leads, that the new leader's advantage
 * shows up in the finishing reward, that damage taken in the middle is still being paid
 * for at the end, and that a player who walks away is scored on different terms without
 * dropping out of the results.
 *
 * <p>Integration bugs live exactly here — in the handover between two pieces that each
 * work perfectly alone — which is why the milestone is not finished until this passes.
 *
 * <p>Components involved: {@link Flight}, {@link Route}, {@link Ship},
 * {@link FlightScorer}.
 */
class FlightSimulationTest {

    private static final Position HOLD = new Position(1, 2);
    private static final Position ENGINE = new Position(3, 2);
    private static final Position SPARE_HULL = new Position(2, 1);

    /** A ship with a hold, an engine and two humans aboard — enough to fly and to trade. */
    private static Ship crewedShip() {
        Ship ship = Ships.openShip();
        Ships.put(ship, HOLD, ComponentKind.SPECIAL_CARGO_HOLD);
        Ships.put(ship, ENGINE, ComponentKind.SINGLE_ENGINE);
        Ships.put(ship, SPARE_HULL, ComponentKind.STRUCTURAL_MODULE);
        ship.boardHumansIn(Ships.CABIN);
        return ship;
    }

    @Test
    @DisplayName("a three-player flight runs from launch to the final table with every mechanic in play")
    void aWholeFlightRunsThrough() {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, crewedShip());
        ships.put(PlayerColor.BLUE, crewedShip());
        ships.put(PlayerColor.GREEN, crewedShip());

        Flight flight = FlightFixtures.levelTwoFlight(ships);

        // Launch: markers on the level II start spaces, red furthest ahead.
        assertEquals(List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN), flight.stillFlying());
        assertTrue(flight.players().stream().allMatch(player -> flight.shipOf(player).crewIsAboard()));

        // Card one, Planets: green lands and loads, then pays for it in flight days.
        Ship greenShip = flight.shipOf(PlayerColor.GREEN);
        greenShip.beginCargoOperations(Map.of(GoodColor.RED, 1, GoodColor.GREEN, 1));
        assertTrue(greenShip.load(HOLD, GoodColor.RED));
        assertTrue(greenShip.load(HOLD, GoodColor.GREEN));
        greenShip.endCargoOperations();
        flight.route().fallBack(PlayerColor.GREEN, 2);

        assertEquals(2, greenShip.cargoCount());
        assertEquals(List.of(PlayerColor.RED, PlayerColor.BLUE, PlayerColor.GREEN), flight.stillFlying());

        // Card two, Open Space: everyone declares engine power and moves. Blue takes the lead.
        for (PlayerColor player : List.copyOf(flight.stillFlying())) {
            int power = flight.shipOf(player).attributes(
                    it.polimi.ingsw.server.model.ship.BatteryPlan.none()).enginePower();
            assertEquals(1, power, "one single engine apiece");
        }
        flight.route().advance(PlayerColor.BLUE, 5);

        assertEquals(PlayerColor.BLUE, flight.stillFlying().getFirst(),
                "overtaking hands the lead over, and with it the deck");

        // Card three, a meteor swarm: one roll, applied to every ship. Red loses a hull piece.
        // Printed column 5 is grid column 1, where the spare hull sits.
        Hit meteor = new Hit(HitKind.BIG_METEOR, Direction.NORTH, 5);
        DamageReport onRed = flight.shipOf(PlayerColor.RED).applyHit(meteor, Defence.none());

        assertEquals(DamageReport.Outcome.DESTROYED, onRed.outcome());
        assertFalse(onRed.brokeUp(), "the hull piece was at the edge, so nothing came adrift");
        assertEquals(1, flight.shipOf(PlayerColor.RED).lostComponentCount());

        // Card four, Slavers: green loses their crew and is forced out once the card is done.
        ((CabinComponent) greenShip.componentAt(Ships.CABIN).orElseThrow()).evacuate();

        assertEquals(java.util.Set.of(PlayerColor.GREEN), flight.playersForcedOut());
        flight.enforceGiveUpRules();

        assertTrue(flight.hasGivenUp(PlayerColor.GREEN));
        assertEquals(List.of(PlayerColor.BLUE, PlayerColor.RED), flight.stillFlying());
        assertFalse(flight.isSolo());

        // Journey's end.
        List<ScoreSheet> table = FlightScorer.settle(flight);

        ScoreSheet blue = sheetFor(table, PlayerColor.BLUE);
        ScoreSheet red = sheetFor(table, PlayerColor.RED);
        ScoreSheet green = sheetFor(table, PlayerColor.GREEN);

        assertEquals(8, blue.finishReward(), "blue overtook and arrived first");
        assertEquals(6, red.finishReward());
        assertEquals(0, green.finishReward(), "green gave up and never arrived");

        assertEquals(0, green.prettiestShip(), "a retiree does not enter the beauty contest");
        assertEquals(4, red.prettiestShip(), "losing a hull piece left red the tidiest ship still flying");
        assertEquals(0, blue.prettiestShip());

        assertEquals(3, green.goodsSold(), "six credits of cargo, halved and rounded up");
        assertEquals(0, blue.goodsSold());

        assertEquals(1, red.lostComponents(), "the meteor is still being paid for");
        assertEquals(0, blue.lostComponents());

        // Red arrived second and still wins: the meteor cost a credit and left the tidiest
        // ship still flying, which is worth four. Losing a component can pay for itself,
        // and the model reproduces that without anybody having coded it as a rule.
        assertEquals(9, red.total(), "six for arriving, four for the ship, less one for the meteor");
        assertEquals(8, blue.total(), "eight for arriving first and nothing else");
        assertEquals(3, green.total(), "half a cargo, and no reward for a flight they left");

        assertEquals(PlayerColor.RED, table.getFirst().player(), "the table reads richest first");
        assertTrue(table.stream().allMatch(ScoreSheet::isProfitable), "everybody turned a profit");
        assertEquals(3, table.size(), "including the player who gave up");
    }

    private static ScoreSheet sheetFor(List<ScoreSheet> table, PlayerColor player) {
        return table.stream()
                .filter(sheet -> sheet.player() == player)
                .findFirst()
                .orElseThrow();
    }
}
