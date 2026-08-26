package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.ShipAttribute;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.flight.Dice;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.BatteryPlan;
import it.polimi.ingsw.server.model.ship.Direction;
import it.polimi.ingsw.server.model.ship.HitKind;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Rotation;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the Combat Zone against manual p.13.
 *
 * <p>The card that inverts the usual advantage. Everywhere else being in front is good;
 * here a tie is broken <em>against</em> the player furthest ahead, so the leader pays for
 * being the leader. Implementing the tie-break the other way round would quietly make the
 * card reward exactly what it exists to punish.
 *
 * <p>Each line is its own pass, and the route order is read again for every one of them:
 * losing flight days on one line can change who leads before the next is evaluated. The
 * manual notes that explicitly, which is the clue that reading the order once would be
 * wrong.
 *
 * <p>Components involved: {@link CombatZoneCard}, {@link CombatLine}, {@link Volley}.
 */
class CombatZoneCardTest {

    /** Grid column 2 is printed as column 6, so a roll of six names the ship's spine. */
    private static final int DICE_FOR_SPINE = 6;

    private static final Position SPINE = new Position(1, 2);
    private static final Position SECOND_CABIN = new Position(3, 2);

    /** Cells a cannon fits into, in an order where each one touches the ship. */
    private static final List<Position> CANNON_BAYS = List.of(
            new Position(2, 3), new Position(2, 4), new Position(2, 1), new Position(2, 0));

    private static CombatZoneCard zone(CombatLine... lines) {
        return new CombatZoneCard(
                new AdventureCardIdentity("combat-zone_lvl1", AdventureCardType.COMBAT_ZONE,
                        CardLevel.LEVEL_I, true),
                List.of(lines));
    }

    /** A ship with the given crew cabins and forward cannons. */
    private static Ship ship(int extraCabins, int cannons) {
        Ship s = Ships.openShip();
        Ships.put(s, SPINE, ComponentKind.STRUCTURAL_MODULE);
        if (extraCabins > 0) {
            Ships.put(s, SECOND_CABIN, ComponentKind.CABIN);
        }
        for (int i = 0; i < cannons; i++) {
            Ships.put(s, CANNON_BAYS.get(i), ComponentKind.SINGLE_CANNON, Rotation.NONE);
        }
        s.fillRemainingCabinsWithHumans();
        return s;
    }

    private static Flight flightOf(Ship leader, Ship trailer) {
        return flightOf(Dice.scripted(DICE_FOR_SPINE), leader, trailer);
    }

    private static Flight flightOf(Dice dice, Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return FlightFixtures.levelTwoFlight(ships, dice);
    }

    private static void declareAll(AdventureResolution resolution) {
        while (resolution.pending().orElse(null) instanceof PlayerPrompt.DeclarePower call) {
            resolution.submit(new PlayerChoice.Declaration(call.player(), BatteryPlan.none()));
        }
    }

    @Nested
    @DisplayName("who pays")
    class WhoPays {

        @Test
        @DisplayName("the lowest value on the line pays")
        void theLowestValuePays() {
            Ship red = ship(1, 0);
            Ship blue = ship(0, 0);
            Flight flight = flightOf(red, blue);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            zone(new CombatLine(ShipAttribute.CREW, new CombatPenalty.LoseFlightDays(3)))
                    .resolve(flight).pending();

            assertEquals(blueBefore - 3, flight.route().positionOf(PlayerColor.BLUE),
                    "blue had two crew to red's four");
        }

        @Test
        @DisplayName("a tie is broken against the player furthest ahead, which is the whole point of the card")
        void aTieIsBrokenAgainstTheLeader() {
            Ship red = ship(0, 0);
            Ship blue = ship(0, 0);
            Flight flight = flightOf(red, blue);
            int redBefore = flight.route().positionOf(PlayerColor.RED);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            zone(new CombatLine(ShipAttribute.CREW, new CombatPenalty.LoseFlightDays(2)))
                    .resolve(flight).pending();

            assertEquals(redBefore - 2, flight.route().positionOf(PlayerColor.RED),
                    "red led, so red paid");
            assertEquals(blueBefore, flight.route().positionOf(PlayerColor.BLUE));
        }

        @Test
        @DisplayName("crew is counted rather than declared, because nothing about it is optional")
        void crewIsCountedNotDeclared() {
            Flight flight = flightOf(ship(1, 0), ship(0, 0));

            AdventureResolution resolution =
                    zone(new CombatLine(ShipAttribute.CREW, new CombatPenalty.LoseFlightDays(1)))
                            .resolve(flight);

            assertTrue(resolution.isComplete(), "nobody was asked anything");
        }

        @Test
        @DisplayName("firepower is declared, and compared in halves")
        void firepowerIsDeclaredAndComparedInHalves() {
            Ship red = ship(0, 1);
            Ship blue = ship(0, 0);
            Flight flight = flightOf(red, blue);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            AdventureResolution resolution =
                    zone(new CombatLine(ShipAttribute.FIREPOWER, new CombatPenalty.LoseFlightDays(2)))
                            .resolve(flight);

            PlayerPrompt prompt = resolution.pending().orElseThrow();
            assertTrue(prompt instanceof PlayerPrompt.DeclarePower);
            assertEquals(ShipAttribute.FIREPOWER, ((PlayerPrompt.DeclarePower) prompt).attribute());

            declareAll(resolution);

            assertEquals(blueBefore - 2, flight.route().positionOf(PlayerColor.BLUE));
        }
    }

    @Nested
    @DisplayName("the penalties")
    class Penalties {

        @Test
        @DisplayName("losing crew is the player's choice of cabins")
        void losingCrewIsTheirChoice() {
            Ship blue = ship(1, 0);
            Flight flight = flightOf(ship(1, 1), blue);

            AdventureResolution resolution =
                    zone(new CombatLine(ShipAttribute.FIREPOWER, new CombatPenalty.LoseCrew(2)))
                            .resolve(flight);
            declareAll(resolution);

            PlayerPrompt prompt = resolution.pending().orElseThrow();
            assertTrue(prompt instanceof PlayerPrompt.GiveUpCrew);
            assertEquals(PlayerColor.BLUE, prompt.player());

            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.BLUE,
                    List.of(SECOND_CABIN, SECOND_CABIN)));

            assertEquals(2, blue.crewCount());
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("being shot at runs the same volley as any other incoming fire")
        void beingShotAtRunsAVolley() {
            Ship blue = ship(0, 0);
            Flight flight = flightOf(ship(0, 1), blue);

            AdventureResolution resolution = zone(new CombatLine(ShipAttribute.FIREPOWER,
                    new CombatPenalty.TakeFire(List.of(
                            new ThreatPattern(HitKind.HEAVY_FIRE, Direction.NORTH)))))
                    .resolve(flight);
            declareAll(resolution);

            PlayerPrompt prompt = resolution.pending().orElseThrow();
            assertTrue(prompt instanceof PlayerPrompt.ChooseDefence);

            resolution.submit(PlayerChoice.DefenceChosen.none(PlayerColor.BLUE));

            assertTrue(blue.componentAt(SPINE).isEmpty());
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("a card carrying a meteor is refused, because a combat zone fires cannons")
        void aMeteorIsNotAShot() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CombatPenalty.TakeFire(List.of(
                            new ThreatPattern(HitKind.BIG_METEOR, Direction.NORTH))));
        }
    }

    @Nested
    @DisplayName("line by line")
    class LineByLine {

        @Test
        @DisplayName("the route order is read again for each line, so a new leader pays the next tie")
        void theOrderIsReadAgainForEachLine() {
            // Red leads on six, blue trails on three, and both carry four crew. Red has no
            // cannons and pays four flight days on the first line, which drops it behind
            // blue. The second line is a tie on crew, so it falls on whoever leads now —
            // and reading the order once would still say red.
            Ship red = ship(1, 0);
            Ship blue = ship(1, 1);
            Flight flight = flightOf(red, blue);

            AdventureResolution resolution = zone(
                    new CombatLine(ShipAttribute.FIREPOWER, new CombatPenalty.LoseFlightDays(4)),
                    new CombatLine(ShipAttribute.CREW, new CombatPenalty.LoseFlightDays(1)))
                    .resolve(flight);
            declareAll(resolution);

            assertEquals(1, flight.route().positionOf(PlayerColor.RED),
                    "six back four, jumping blue on three");
            assertEquals(2, flight.route().positionOf(PlayerColor.BLUE),
                    "blue led going into the second line, so blue paid the tie");
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("all three lines of the shipped level I card are evaluated in order")
        void allThreeLinesRun() {
            GameData data = GameDataLoader.loadBundled();
            CombatZoneCard card = (CombatZoneCard) data.playableCard("combat-zone_lvl1").orElseThrow();

            Ship red = ship(1, 1);
            Ship blue = ship(0, 0);
            Flight flight = flightOf(red, blue);
            int blueBefore = flight.route().positionOf(PlayerColor.BLUE);

            AdventureResolution resolution = card.resolve(flight);
            resolution.pending();

            // Line one compares crew: blue is short and loses three flight days.
            assertEquals(blueBefore - 3, flight.route().positionOf(PlayerColor.BLUE));

            // Line two compares engine power: neither ship has an engine, so red pays for leading.
            declareAll(resolution);
            PlayerPrompt crewCall = resolution.pending().orElseThrow();
            assertTrue(crewCall instanceof PlayerPrompt.GiveUpCrew);
            assertEquals(PlayerColor.RED, crewCall.player(), "a tie, broken against the leader");
            resolution.submit(new PlayerChoice.CrewGiven(PlayerColor.RED,
                    List.of(SECOND_CABIN, SECOND_CABIN)));

            // Line three compares firepower and shoots the weakest.
            declareAll(resolution);
            assertTrue(resolution.pending().orElseThrow() instanceof PlayerPrompt.ChooseDefence);
        }
    }

    @Nested
    @DisplayName("flying alone")
    class FlyingAlone {

        @Test
        @DisplayName("the card is skipped when everybody else has given up")
        void aSoloFlightSkipsTheCard() {
            Ship red = ship(0, 0);
            Flight flight = flightOf(red, ship(1, 0));
            flight.giveUp(PlayerColor.BLUE);
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution =
                    zone(new CombatLine(ShipAttribute.CREW, new CombatPenalty.LoseFlightDays(3)))
                            .resolve(flight);

            assertTrue(resolution.isComplete());
            assertEquals(before, flight.route().positionOf(PlayerColor.RED));
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("a combat zone with no lines is not a combat zone, and is refused")
        void anEmptyZoneIsRefused() {
            assertThrows(IllegalArgumentException.class, CombatZoneCardTest::emptyZone);
        }

        @Test
        @DisplayName("the shipped level I card matches the manual: crew, engines, then cannons")
        void theLevelOneCardMatchesTheManual() {
            GameData data = GameDataLoader.loadBundled();
            CombatZoneCard card = (CombatZoneCard) data.playableCard("combat-zone_lvl1").orElseThrow();

            assertEquals(3, card.lines().size());
            assertEquals(new CombatLine(ShipAttribute.CREW, new CombatPenalty.LoseFlightDays(3)),
                    card.lines().get(0));
            assertEquals(new CombatLine(ShipAttribute.ENGINE_POWER, new CombatPenalty.LoseCrew(2)),
                    card.lines().get(1));
            assertEquals(new CombatLine(ShipAttribute.FIREPOWER, new CombatPenalty.TakeFire(List.of(
                            new ThreatPattern(HitKind.LIGHT_FIRE, Direction.SOUTH),
                            new ThreatPattern(HitKind.HEAVY_FIRE, Direction.SOUTH)))),
                    card.lines().get(2));
        }
    }

    private static CombatZoneCard emptyZone() {
        return zone();
    }
}
