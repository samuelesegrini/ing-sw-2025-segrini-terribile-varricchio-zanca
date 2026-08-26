package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.common.game.ComponentKind;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.flight.FlightFixtures;
import it.polimi.ingsw.server.model.ship.Ship;
import it.polimi.ingsw.server.model.ship.Ships;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the goods a card offers can be taken.
 *
 * <p>Until this existed they could not. Three cards opened cargo operations, sent an
 * {@code ArrangeCargo} naming what was on offer, and waited for a {@code Done} that closed them
 * again — with no answer in between. Every test that reached one answered {@code Done}, because
 * that was the only answer there was, so the cards passed, the flights ran to the end, and the
 * cubes quietly never moved.
 *
 * <p>Goods are most of the scoring on a level II flight. A game that could not load any is a
 * game where the only credits come from beating enemies, which is not the game.
 */
class TakingCargoTest {

    private static final Position HOLD = new Position(2, 3);
    private static final Position SPECIAL = new Position(1, 2);
    private static final Position PLAIN_HOLD = new Position(3, 2);

    /**
     * A ship with room for cubes: an ordinary hold, another, and one that takes red.
     */
    private static Ship shipWithHolds() {
        Ship ship = Ships.openShip();
        Ships.put(ship, HOLD, ComponentKind.CARGO_HOLD);
        Ships.put(ship, PLAIN_HOLD, ComponentKind.CARGO_HOLD);
        Ships.put(ship, SPECIAL, ComponentKind.SPECIAL_CARGO_HOLD);
        return ship;
    }

    /**
     * A ship on a planet, with an offer in front of it.
     *
     * @param resolution the card being resolved
     * @param player     whoever the card asked, which is decided by the route and not by us
     * @param ship       their ship
     * @param offered    what is on the ground
     */
    private record Landing(AdventureResolution resolution, PlayerColor player, Ship ship,
                           PlayerPrompt.ArrangeCargo offered) {
    }

    /**
     * Lands whoever the card asks first, and stops at the offer.
     *
     * <p>Which player that is comes from the route, and the route from the order the fixture's
     * map happened to iterate — which is not fixed. Reading the colour off the prompt rather
     * than assuming it is the one thing that makes these tests the same on every run.
     *
     * @param goods what the planet is holding
     * @return the landing
     */
    private static Landing landOn(Map<GoodColor, Integer> goods) {
        Map<PlayerColor, Ship> ships = new LinkedHashMap<>();
        ships.put(PlayerColor.RED, shipWithHolds());
        ships.put(PlayerColor.BLUE, shipWithHolds());
        Flight flight = FlightFixtures.levelTwoFlight(ships);

        AdventureResolution resolution = planets(goods).resolve(flight);
        PlayerPrompt.ChoosePlanet planet =
                (PlayerPrompt.ChoosePlanet) resolution.pending().orElseThrow();
        PlayerColor asked = planet.player();
        resolution.submit(new PlayerChoice.PlanetChosen(asked,
                planet.planets().keySet().iterator().next()));

        return new Landing(resolution, asked, flight.shipOf(asked),
                (PlayerPrompt.ArrangeCargo) resolution.pending().orElseThrow());
    }

    private static PlanetsCard planets(Map<GoodColor, Integer> goods) {
        return new PlanetsCard(
                new AdventureCardIdentity("planets", AdventureCardType.PLANETS,
                        CardLevel.LEVEL_II, false),
                List.of(goods), 1);
    }

    @Test
    @DisplayName("a cube a card offers ends up in the hold the player named")
    void takingACube() {
        Landing landing = landOn(Map.of(GoodColor.BLUE, 2));

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.BLUE));

        assertEquals(List.of(GoodColor.BLUE), landing.ship().cargo().get(HOLD));
    }

    @Test
    @DisplayName("the offer shrinks as cubes are taken, so nobody is offered the same one twice")
    void theOfferShrinks() {
        Landing landing = landOn(Map.of(GoodColor.BLUE, 2));
        assertEquals(2, landing.offered().offered().get(GoodColor.BLUE));

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.BLUE));
        PlayerPrompt.ArrangeCargo again =
                (PlayerPrompt.ArrangeCargo) landing.resolution().pending().orElseThrow();

        assertEquals(1, again.offered().get(GoodColor.BLUE),
                "one of the two is aboard, and only one is left on the ground");
    }

    @Test
    @DisplayName("several cubes can be taken before saying so, because stowing is not one decision")
    void takingSeveral() {
        Landing landing = landOn(Map.of(GoodColor.BLUE, 2));

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.BLUE));
        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), PLAIN_HOLD, GoodColor.BLUE));
        landing.resolution().submit(new PlayerChoice.Done(landing.player()));

        assertEquals(2, landing.ship().cargoCount());
    }

    @Test
    @DisplayName("a red cube goes only where red cubes may go")
    void redCubesAreFussy() {
        Landing landing = landOn(Map.of(GoodColor.RED, 1));

        assertThrows(IllegalArgumentException.class, () -> landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.RED)),
                "an ordinary hold will not take red");

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), SPECIAL, GoodColor.RED));
        assertEquals(List.of(GoodColor.RED), landing.ship().cargo().get(SPECIAL));
    }

    @Test
    @DisplayName("a cube can be shifted to free the hold something else wants")
    void makingRoom() {
        // The reason moving exists: a special hold is the only place a red cube can go, and it
        // may already be holding something that would sit anywhere.
        Landing landing = landOn(Map.of(GoodColor.BLUE, 1, GoodColor.RED, 1));

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), SPECIAL, GoodColor.BLUE));
        landing.resolution().submit(
                PlayerChoice.CargoStowed.move(landing.player(), SPECIAL, HOLD, GoodColor.BLUE));
        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), SPECIAL, GoodColor.RED));

        assertEquals(List.of(GoodColor.BLUE), landing.ship().cargo().get(HOLD));
        assertEquals(List.of(GoodColor.RED), landing.ship().cargo().get(SPECIAL));
    }

    @Test
    @DisplayName("a cube thrown overboard goes back to the bank")
    void jettisoning() {
        Landing landing = landOn(Map.of(GoodColor.BLUE, 1));

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.BLUE));
        landing.resolution().submit(
                PlayerChoice.CargoStowed.jettison(landing.player(), HOLD, GoodColor.BLUE));

        assertEquals(0, landing.ship().cargoCount());
    }

    @Test
    @DisplayName("saying you have finished ends it, and the card carries on")
    void finishing() {
        Landing landing = landOn(Map.of(GoodColor.BLUE, 1));

        landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.BLUE));
        landing.resolution().submit(new PlayerChoice.Done(landing.player()));

        assertTrue(landing.resolution().pending()
                        .map(prompt -> prompt.player() != landing.player())
                        .orElse(true),
                "this player is finished with this card");
        assertEquals(1, landing.ship().cargoCount(), "and keeps what they took");
    }

    @Test
    @DisplayName("a cube the card never offered cannot be taken")
    void takingWhatIsNotThere() {
        Landing landing = landOn(Map.of(GoodColor.BLUE, 1));

        assertThrows(IllegalArgumentException.class, () -> landing.resolution().submit(
                PlayerChoice.CargoStowed.load(landing.player(), HOLD, GoodColor.YELLOW)),
                "this planet has no yellow on it");
    }

    @Test
    @DisplayName("a stowing answer needs everything the move it names requires")
    void malformedAnswers() {
        assertThrows(NullPointerException.class,
                () -> PlayerChoice.CargoStowed.load(null, HOLD, GoodColor.BLUE));
        assertThrows(NullPointerException.class,
                () -> PlayerChoice.CargoStowed.load(PlayerColor.RED, null, GoodColor.BLUE));
        assertThrows(NullPointerException.class,
                () -> PlayerChoice.CargoStowed.load(PlayerColor.RED, HOLD, null));
        assertThrows(NullPointerException.class, () -> new PlayerChoice.CargoStowed(
                PlayerColor.RED, PlayerChoice.Stowing.MOVE, HOLD, null, GoodColor.BLUE));
    }
}
