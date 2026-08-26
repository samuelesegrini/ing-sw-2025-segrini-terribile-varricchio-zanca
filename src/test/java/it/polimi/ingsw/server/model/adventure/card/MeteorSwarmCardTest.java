package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.data.GameData;
import it.polimi.ingsw.server.data.GameDataLoader;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.AdventureCardType;
import it.polimi.ingsw.server.model.adventure.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.component.ComponentKind;
import it.polimi.ingsw.server.model.component.ComponentTile;
import it.polimi.ingsw.server.model.component.Tiles;
import it.polimi.ingsw.server.model.flight.Dice;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Connector;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the Meteor Swarm against manual p.13 and p.19.
 *
 * <p>The card that settles arguments about tidy construction. The same rocks are thrown at
 * every ship, and what happens next is entirely down to how each one was built: a smooth
 * side turns a small meteor aside for nothing, an exposed connector does not, and a big
 * meteor cares about neither and can only be shot.
 *
 * <p>Rolling once for the whole swarm rather than once per ship is what makes it a shared
 * disaster. Rolling per ship would quietly halve how dangerous the card is, so the test
 * that pins it puts two identical ships side by side and expects identical damage.
 *
 * <p>Components involved: {@link MeteorSwarmCard}, {@link Volley}, {@link Ship}.
 */
class MeteorSwarmCardTest {

    /** Grid column 2 is printed as column 6, so a roll of six names the ship's spine. */
    private static final int DICE_FOR_SPINE = 6;

    private static final Position SPINE = new Position(1, 2);
    private static final Position PORT_WING = new Position(2, 1);
    private static final Position BATTERY = new Position(3, 2);

    private static MeteorSwarmCard swarm(ThreatPattern... meteors) {
        return new MeteorSwarmCard(
                new AdventureCardIdentity("meteor-swarm_lvl1_01", AdventureCardType.METEOR_SWARM,
                        CardLevel.LEVEL_I, true),
                List.of(meteors));
    }

    private static ThreatPattern smallFromTheBow() {
        return new ThreatPattern(HitKind.SMALL_METEOR, Direction.NORTH);
    }

    private static ThreatPattern bigFromTheBow() {
        return new ThreatPattern(HitKind.BIG_METEOR, Direction.NORTH);
    }

    /** A ship whose spine shows a connector to the bow, so a small meteor catches on it. */
    private static Ship shipWithExposedSpine() {
        Ship ship = Ships.openShip();
        Ships.put(ship, SPINE, ComponentKind.STRUCTURAL_MODULE);
        ship.fillRemainingCabinsWithHumans();
        return ship;
    }

    /** A ship whose spine turns a smooth side to the bow, so a small meteor bounces off. */
    private static Ship shipWithSmoothSpine() {
        Ship ship = Ships.openShip();
        ship.place(SPINE, new ComponentTile("smooth-top", ComponentKind.STRUCTURAL_MODULE,
                Tiles.sides(Connector.PLAIN, Connector.UNIVERSAL,
                        Connector.UNIVERSAL, Connector.UNIVERSAL), 0), Rotation.NONE);
        ship.fillRemainingCabinsWithHumans();
        return ship;
    }

    private static Flight flightOf(Dice dice, Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return FlightFixtures.levelTwoFlight(ships, dice);
    }

    private static void takeIt(AdventureResolution resolution, PlayerColor player) {
        resolution.submit(PlayerChoice.DefenceChosen.none(player));
    }

    @Nested
    @DisplayName("the same rocks for everybody")
    class TheSameRocks {

        @Test
        @DisplayName("one roll per meteor, applied to every ship")
        void oneRollPerMeteorForEverybody() {
            Ship red = shipWithExposedSpine();
            Ship blue = shipWithExposedSpine();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, blue);

            AdventureResolution resolution = swarm(bigFromTheBow()).resolve(flight);
            takeIt(resolution, PlayerColor.RED);
            takeIt(resolution, PlayerColor.BLUE);

            assertTrue(red.componentAt(SPINE).isEmpty());
            assertTrue(blue.componentAt(SPINE).isEmpty(), "the same column, on both ships");
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("every ship is walked through the swarm, leader first")
        void everyShipIsWalkedThrough() {
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE),
                    shipWithExposedSpine(), shipWithExposedSpine());

            AdventureResolution resolution = swarm(bigFromTheBow()).resolve(flight);

            assertEquals(PlayerColor.RED, resolution.pending().orElseThrow().player());
            takeIt(resolution, PlayerColor.RED);
            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player());
        }

        @Test
        @DisplayName("a player who gave up is not in the swarm's way")
        void aRetireeIsNotInTheWay() {
            Ship blue = shipWithExposedSpine();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), shipWithExposedSpine(), blue);
            flight.giveUp(PlayerColor.BLUE);

            AdventureResolution resolution = swarm(bigFromTheBow()).resolve(flight);
            takeIt(resolution, PlayerColor.RED);

            assertTrue(resolution.isComplete());
            assertTrue(blue.componentAt(SPINE).isPresent());
        }
    }

    @Nested
    @DisplayName("small meteors")
    class SmallMeteors {

        @Test
        @DisplayName("a smooth side turns one aside for nothing, which is why tidy ships survive")
        void aSmoothSideTurnsOneAside() {
            Ship red = shipWithSmoothSpine();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithSmoothSpine());

            AdventureResolution resolution = swarm(smallFromTheBow()).resolve(flight);
            takeIt(resolution, PlayerColor.RED);

            assertTrue(red.componentAt(SPINE).isPresent(), "it bounced");
        }

        @Test
        @DisplayName("an exposed connector catches one, and the piece is lost")
        void anExposedConnectorCatchesOne() {
            Ship red = shipWithExposedSpine();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithExposedSpine());

            AdventureResolution resolution = swarm(smallFromTheBow()).resolve(flight);
            takeIt(resolution, PlayerColor.RED);

            assertTrue(red.componentAt(SPINE).isEmpty());
        }

        @Test
        @DisplayName("a shield covering that side stops one, for a charge")
        void aShieldStopsOne() {
            Ship red = Ships.openShip();
            Ships.put(red, SPINE, ComponentKind.STRUCTURAL_MODULE);
            Ships.put(red, PORT_WING, ComponentKind.SHIELD, Rotation.NONE);
            Ships.put(red, BATTERY, ComponentKind.BATTERY);
            red.chargeBatteries();
            red.fillRemainingCabinsWithHumans();

            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithExposedSpine());
            AdventureResolution resolution = swarm(smallFromTheBow()).resolve(flight);

            PlayerPrompt.ChooseDefence incoming =
                    (PlayerPrompt.ChooseDefence) resolution.pending().orElseThrow();
            assertEquals(Set.of(PORT_WING), incoming.options());

            resolution.submit(PlayerChoice.DefenceChosen.using(PlayerColor.RED, PORT_WING));

            assertTrue(red.componentAt(SPINE).isPresent(), "the shield held");
            assertEquals(1, red.availableCharges());
        }
    }

    @Nested
    @DisplayName("big meteors")
    class BigMeteors {

        @Test
        @DisplayName("a smooth side is no help at all against one")
        void aSmoothSideIsNoHelp() {
            Ship red = shipWithSmoothSpine();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithSmoothSpine());

            AdventureResolution resolution = swarm(bigFromTheBow()).resolve(flight);
            takeIt(resolution, PlayerColor.RED);

            assertTrue(red.componentAt(SPINE).isEmpty());
        }

        @Test
        @DisplayName("one at the bow can only be shot down its own column")
        void oneAtTheBowNeedsACannonInItsColumn() {
            Ship red = shipWithExposedSpine();
            Ships.put(red, new Position(2, 3), ComponentKind.SINGLE_CANNON, Rotation.NONE);
            Ships.put(red, new Position(3, 2), ComponentKind.SINGLE_CANNON, Rotation.NONE);

            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithExposedSpine());
            AdventureResolution resolution = swarm(bigFromTheBow()).resolve(flight);

            PlayerPrompt.ChooseDefence incoming =
                    (PlayerPrompt.ChooseDefence) resolution.pending().orElseThrow();

            assertEquals(Set.of(new Position(3, 2)), incoming.options(),
                    "the cannon one column over cannot reach it");

            resolution.submit(PlayerChoice.DefenceChosen.using(PlayerColor.RED, new Position(3, 2)));

            assertTrue(red.componentAt(SPINE).isPresent(), "it was shot down");
        }
    }

    @Nested
    @DisplayName("several meteors")
    class SeveralMeteors {

        @Test
        @DisplayName("they arrive in the order the card prints them")
        void theyArriveInPrintedOrder() {
            Ship red = shipWithExposedSpine();
            Ships.put(red, new Position(2, 3), ComponentKind.STRUCTURAL_MODULE);

            // Six names the spine's column, seven the one beside it.
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE, 7), red, shipWithExposedSpine());
            AdventureResolution resolution = swarm(bigFromTheBow(), bigFromTheBow()).resolve(flight);

            takeIt(resolution, PlayerColor.RED);
            assertTrue(red.componentAt(SPINE).isEmpty(), "the first meteor took the spine");

            takeIt(resolution, PlayerColor.RED);
            assertTrue(red.componentAt(new Position(2, 3)).isEmpty(), "the second took the wing");
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("a swarm with no meteors is not a swarm, and is refused")
        void anEmptySwarmIsRefused() {
            assertThrows(IllegalArgumentException.class, MeteorSwarmCardTest::swarmOfNothing);
        }

        @Test
        @DisplayName("cannon fire is not a meteor, and a swarm carrying one is refused")
        void cannonFireIsNotAMeteor() {
            assertThrows(IllegalArgumentException.class,
                    () -> swarm(new ThreatPattern(HitKind.HEAVY_FIRE, Direction.NORTH)));
        }

        @Test
        @DisplayName("the shipped swarms carry the meteors printed on them, in order")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            MeteorSwarmCard card =
                    (MeteorSwarmCard) data.playableCard("meteor-swarm_lvl1_01").orElseThrow();

            assertEquals(List.of(
                            new ThreatPattern(HitKind.BIG_METEOR, Direction.NORTH),
                            new ThreatPattern(HitKind.SMALL_METEOR, Direction.WEST),
                            new ThreatPattern(HitKind.SMALL_METEOR, Direction.EAST)),
                    card.meteors());
        }
    }

    private static MeteorSwarmCard swarmOfNothing() {
        return swarm();
    }
}
