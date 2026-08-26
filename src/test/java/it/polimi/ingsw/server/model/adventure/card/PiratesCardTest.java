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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the Pirates against manual p.19.
 *
 * <p>The rule worth being careful with is <em>when</em> they fire. They work down the whole
 * route first, and only then do the dice come out: one roll per shot, made once, applied
 * to every ship they beat.
 *
 * <p>Rolling separately for each victim would be a much gentler card. Sharing the rolls
 * means one bad seven wrecks the same column on every ship that lost, which is exactly the
 * shared disaster the card is for — and it is the thing an implementation gets wrong by
 * doing the obvious thing and firing as it goes.
 *
 * <p>Components involved: {@link PiratesCard}, {@link EnemyResolution}, {@link Volley}.
 */
class PiratesCardTest {

    /** Grid column 2 is printed as column 6, so a roll of six names the ship's spine. */
    private static final int DICE_FOR_SPINE = 6;

    private static final Position SPINE = new Position(1, 2);
    private static final Position PORT_WING = new Position(2, 1);
    private static final Position BATTERY = new Position(3, 2);

    private static PiratesCard pirates(int firepower, int credits, int flightDays,
                                       List<PiratesCard.ShotPattern> shots) {
        return new PiratesCard(
                new AdventureCardIdentity("pirates_lvl1", AdventureCardType.PIRATES,
                        CardLevel.LEVEL_I, false),
                firepower, credits, shots, flightDays);
    }

    private static PiratesCard.ShotPattern heavyFromTheBow() {
        return new PiratesCard.ShotPattern(HitKind.HEAVY_FIRE, Direction.NORTH);
    }

    private static PiratesCard.ShotPattern lightFromTheBow() {
        return new PiratesCard.ShotPattern(HitKind.LIGHT_FIRE, Direction.NORTH);
    }

    /** Cells a cannon fits into, in an order where each one touches the ship. */
    private static final List<Position> CANNON_BAYS = List.of(
            new Position(2, 3), new Position(2, 4), new Position(2, 1), new Position(2, 0));

    /** A ship with a hull piece on its spine, one forward cannon per point of firepower. */
    private static Ship shipWithFirepower(int points) {
        Ship ship = Ships.openShip();
        Ships.put(ship, SPINE, ComponentKind.STRUCTURAL_MODULE);
        for (int i = 0; i < points; i++) {
            Ships.put(ship, CANNON_BAYS.get(i), ComponentKind.SINGLE_CANNON, Rotation.NONE);
        }
        ship.fillRemainingCabinsWithHumans();
        return ship;
    }

    /** As above, with a north-facing shield and a charged battery. */
    private static Ship shipWithShield() {
        Ship ship = Ships.openShip();
        Ships.put(ship, SPINE, ComponentKind.STRUCTURAL_MODULE);
        Ships.put(ship, PORT_WING, ComponentKind.SHIELD, Rotation.NONE);
        Ships.put(ship, BATTERY, ComponentKind.BATTERY);
        ship.chargeBatteries();
        ship.fillRemainingCabinsWithHumans();
        return ship;
    }

    private static Flight flightOf(Dice dice, Ship leader, Ship trailer) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, leader);
        ships.put(PlayerColor.BLUE, trailer);
        return FlightFixtures.levelTwoFlight(ships, dice);
    }

    private static void declare(AdventureResolution resolution, PlayerColor player) {
        resolution.submit(new PlayerChoice.Declaration(player, BatteryPlan.none()));
    }

    private static void takeTheHit(AdventureResolution resolution, PlayerColor player) {
        resolution.submit(PlayerChoice.DefenceChosen.none(player));
    }

    @Nested
    @DisplayName("when the dice come out")
    class WhenTheDiceComeOut {

        @Test
        @DisplayName("nothing is fired until the pirates have worked down the whole route")
        void firingWaitsForTheWholeRoute() {
            Ship red = shipWithFirepower(0);
            Ship blue = shipWithFirepower(0);
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, blue);

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);

            declare(resolution, PlayerColor.RED);

            assertTrue(red.componentAt(SPINE).isPresent(),
                    "red lost the fight but nothing has been fired yet");
            assertEquals(PlayerColor.BLUE, resolution.pending().orElseThrow().player());
        }

        @Test
        @DisplayName("one roll per shot, shared by every ship they beat")
        void oneRollSharedByEverybodyBeaten() {
            Ship red = shipWithFirepower(0);
            Ship blue = shipWithFirepower(0);
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, blue);

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);

            takeTheHit(resolution, PlayerColor.RED);
            takeTheHit(resolution, PlayerColor.BLUE);

            assertTrue(red.componentAt(SPINE).isEmpty(), "the same column was wrecked on both ships");
            assertTrue(blue.componentAt(SPINE).isEmpty());
            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("a ship that drew is not fired on, because only losers are shot at")
        void onlyLosersAreShotAt() {
            Ship red = shipWithFirepower(3);
            Ship blue = shipWithFirepower(0);
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, blue);

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);
            takeTheHit(resolution, PlayerColor.BLUE);

            assertTrue(red.componentAt(SPINE).isPresent(), "red drew and was left alone");
            assertTrue(blue.componentAt(SPINE).isEmpty());
        }

        @Test
        @DisplayName("beating them stops the card before any dice are rolled at all")
        void beatingThemStopsEverything() {
            Ship red = shipWithFirepower(4);
            Ship blue = shipWithFirepower(0);
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, blue);

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Leave(PlayerColor.RED));

            assertTrue(resolution.isComplete());
            assertTrue(blue.componentAt(SPINE).isPresent(), "blue was never attacked");
        }

        @Test
        @DisplayName("with nobody beaten the card ends quietly")
        void nobodyBeatenEndsQuietly() {
            Ship red = shipWithFirepower(3);
            Ship blue = shipWithFirepower(3);
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, blue);

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);

            assertTrue(resolution.isComplete());
            assertTrue(red.componentAt(SPINE).isPresent(), "a draw leaves a ship untouched");
        }
    }

    @Nested
    @DisplayName("taking the fire")
    class TakingTheFire {

        @Test
        @DisplayName("every shot on the card is offered in turn")
        void everyShotIsOfferedInTurn() {
            Ship red = shipWithFirepower(0);
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE, 12), red, shipWithFirepower(3));

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow(), heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);

            assertTrue(resolution.pending().orElseThrow() instanceof PlayerPrompt.ChooseDefence);
            takeTheHit(resolution, PlayerColor.RED);

            PlayerPrompt second = resolution.pending().orElseThrow();
            assertTrue(second instanceof PlayerPrompt.ChooseDefence, "the second shot is still to come");
            takeTheHit(resolution, PlayerColor.RED);

            assertTrue(resolution.isComplete());
        }

        @Test
        @DisplayName("a shield turned the right way stops light fire, for a charge")
        void aShieldStopsLightFire() {
            Ship red = shipWithShield();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithFirepower(3));

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(lightFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);

            PlayerPrompt.ChooseDefence shot =
                    (PlayerPrompt.ChooseDefence) resolution.pending().orElseThrow();
            assertEquals(java.util.Set.of(PORT_WING), shot.options());

            resolution.submit(PlayerChoice.DefenceChosen.using(PlayerColor.RED, PORT_WING));

            assertTrue(red.componentAt(SPINE).isPresent(), "the shield held");
            assertEquals(1, red.availableCharges());
        }

        @Test
        @DisplayName("nothing is offered against heavy fire, because nothing works")
        void nothingStopsHeavyFire() {
            Ship red = shipWithShield();
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE), red, shipWithFirepower(3));

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);

            PlayerPrompt.ChooseDefence shot =
                    (PlayerPrompt.ChooseDefence) resolution.pending().orElseThrow();

            assertEquals(java.util.Set.of(), shot.options());
            assertTrue(shot.target().isPresent(), "the player can at least see what they are losing");
        }

        @Test
        @DisplayName("a roll that names no line on the board is still offered, and misses")
        void aRollThatMissesIsStillOffered() {
            Ship red = shipWithFirepower(0);
            Flight flight = flightOf(Dice.scripted(2), red, shipWithFirepower(3));

            AdventureResolution resolution =
                    pirates(3, 4, 1, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            declare(resolution, PlayerColor.BLUE);

            PlayerPrompt.ChooseDefence shot =
                    (PlayerPrompt.ChooseDefence) resolution.pending().orElseThrow();
            assertTrue(shot.target().isEmpty(), "a roll of two names no column");

            takeTheHit(resolution, PlayerColor.RED);

            assertTrue(red.componentAt(SPINE).isPresent(), "nothing was lost");
        }
    }

    @Nested
    @DisplayName("beating them")
    class BeatingThem {

        @Test
        @DisplayName("the reward is credits, and declining costs nothing")
        void theRewardIsCredits() {
            Flight flight = flightOf(Dice.scripted(DICE_FOR_SPINE),
                    shipWithFirepower(4), shipWithFirepower(0));
            int before = flight.route().positionOf(PlayerColor.RED);

            AdventureResolution resolution =
                    pirates(3, 4, 2, List.of(heavyFromTheBow())).resolve(flight);
            declare(resolution, PlayerColor.RED);
            resolution.submit(new PlayerChoice.Take(PlayerColor.RED));

            assertEquals(4, flight.creditsEarned(PlayerColor.RED));
            assertEquals(before - 2, flight.route().positionOf(PlayerColor.RED));
        }
    }

    @Nested
    @DisplayName("the printed values")
    class PrintedValues {

        @Test
        @DisplayName("pirates who do not shoot are not pirates, and are refused")
        void piratesWithoutShotsAreRefused() {
            assertThrows(IllegalArgumentException.class, () -> pirates(5, 4, 1, List.of()));
        }

        @Test
        @DisplayName("a meteor is not a cannon shot, and a card carrying one is refused")
        void aMeteorIsNotAShot() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PiratesCard.ShotPattern(HitKind.BIG_METEOR, Direction.NORTH));
        }

        @Test
        @DisplayName("the shipped pirate cards carry the values printed on them")
        void theShippedCardsMatchTheirArtwork() {
            GameData data = GameDataLoader.loadBundled();
            PiratesCard card = (PiratesCard) data.playableCard("pirates_lvl1").orElseThrow();

            assertEquals(5, card.firepower());
            assertEquals(4, card.credits());
            assertEquals(1, card.flightDays());
            assertEquals(List.of(
                            new PiratesCard.ShotPattern(HitKind.LIGHT_FIRE, Direction.NORTH),
                            new PiratesCard.ShotPattern(HitKind.HEAVY_FIRE, Direction.NORTH),
                            new PiratesCard.ShotPattern(HitKind.LIGHT_FIRE, Direction.NORTH)),
                    card.shots());
        }

        @Test
        @DisplayName("scripted dice repeat their last roll rather than running out")
        void scriptedDiceRepeatTheirLastRoll() {
            Dice dice = Dice.scripted(7, 4);

            assertEquals(7, dice.roll());
            assertEquals(4, dice.roll());
            assertEquals(4, dice.roll());
            assertThrows(IllegalArgumentException.class, () -> Dice.scripted(13));
            assertFalse(Dice.scripted(7).roll() != 7);
        }
    }
}
