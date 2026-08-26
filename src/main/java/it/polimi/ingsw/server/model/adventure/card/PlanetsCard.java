package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.TurnByTurnResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.goods.GoodColor;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Planets: two to four of them, one ship each, first come first served.
 *
 * <p>The card where being in front is worth the most, because the leader picks first and
 * a planet taken is a planet gone. Landing purely to deny somebody the good one is a
 * legitimate move, and the manual says so outright (p.12).
 *
 * <p>Two orderings matter and they run opposite ways. Choosing and loading go in route
 * order, which is also what settles a run on the bank: a ship in front takes the last red
 * cube and a ship behind finds none. Then the flight days are paid in <em>reverse</em>
 * route order, once everybody has decided.
 *
 * @param identity   what deck building knows about this card
 * @param planets    what is on each planet, in the order they are printed
 * @param flightDays what landing costs
 */
public record PlanetsCard(AdventureCardIdentity identity, List<Map<GoodColor, Integer>> planets,
                          int flightDays) implements AdventureCard {

    /**
     * Validates the printed values and takes a defensive copy.
     *
     * @throws IllegalArgumentException if there are no planets, one is empty, or the cost
     *                                  is negative
     */
    public PlanetsCard {
        if (flightDays < 0) {
            throw new IllegalArgumentException("landing cannot pay flight days");
        }
        planets = planets.stream().map(PlanetsCard::copyGoods).toList();
        if (planets.isEmpty()) {
            throw new IllegalArgumentException("a planets card needs planets");
        }
    }

    private static Map<GoodColor, Integer> copyGoods(Map<GoodColor, Integer> goods) {
        Map<GoodColor, Integer> copy = new EnumMap<>(GoodColor.class);
        goods.forEach((color, count) -> {
            if (count < 0) {
                throw new IllegalArgumentException("a planet cannot hold " + count + " " + color + " cubes");
            }
            if (count > 0) {
                copy.put(color, count);
            }
        });
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("a planet with nothing on it is not worth the flight days");
        }
        return Map.copyOf(copy);
    }

    /**
     * Returns what is on a planet.
     *
     * @param number the planet's printed number, counting from one
     * @return its goods
     * @throws IllegalArgumentException if there is no such planet
     */
    public Map<GoodColor, Integer> planet(int number) {
        if (number < 1 || number > planets.size()) {
            throw new IllegalArgumentException("this card has no planet " + number);
        }
        return planets.get(number - 1);
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /**
     * Offers the free planets down the route, then charges everybody who landed.
     */
    private static final class Resolution extends TurnByTurnResolution {

        private final Flight flight;
        private final PlanetsCard card;
        private final Set<Integer> taken = new LinkedHashSet<>();
        private final Map<PlayerColor, Integer> landed = new LinkedHashMap<>();

        Resolution(Flight flight, PlanetsCard card) {
            super(flight.stillFlying());
            this.flight = flight;
            this.card = card;
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            Map<Integer, Map<GoodColor, Integer>> free = new LinkedHashMap<>();
            for (int number = 1; number <= card.planets().size(); number++) {
                if (!taken.contains(number)) {
                    free.put(number, card.planet(number));
                }
            }
            if (free.isEmpty()) {
                // Every planet is occupied, so there is nothing to ask about.
                return Optional.empty();
            }
            return Optional.of(new PlayerPrompt.ChoosePlanet(player, free, card.flightDays()));
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            return switch (choice) {
                case PlayerChoice.Leave ignored -> false;
                case PlayerChoice.PlanetChosen chosen -> {
                    land(chosen.player(), chosen.planet());
                    yield false;
                }
                case PlayerChoice.Done done -> {
                    flight.shipOf(done.player()).endCargoOperations();
                    yield false;
                }
                default -> throw new IllegalArgumentException(
                        "a planets card is waiting for a landing or a refusal, not " + choice);
            };
        }

        private void land(PlayerColor player, int number) {
            if (!taken.add(number)) {
                throw new IllegalArgumentException("planet " + number + " already has a ship on it");
            }
            landed.put(player, number);

            Ship ship = flight.shipOf(player);
            ship.beginCargoOperations(card.planet(number));
            askAgain(new PlayerPrompt.ArrangeCargo(player, card.planet(number), ship.cargo().keySet()));
        }

        /**
         * Charges everybody who landed, furthest behind first.
         *
         * <p>Held until every player has decided, and then applied in reverse route order
         * (manual p.12) — which is what stops a ship in front being pushed onto a space a
         * ship behind is about to leave.
         */
        @Override
        protected void afterEveryone() {
            if (landed.isEmpty()) {
                return;
            }
            Map<PlayerColor, Integer> days = new LinkedHashMap<>();
            landed.keySet().forEach(player -> days.put(player, card.flightDays()));
            flight.route().loseFlightDaysTogether(days);
        }
    }
}
