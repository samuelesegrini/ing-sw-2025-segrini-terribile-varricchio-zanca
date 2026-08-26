package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerChoice;
import it.polimi.ingsw.server.model.adventure.resolution.PlayerPrompt;
import it.polimi.ingsw.server.model.adventure.resolution.TurnByTurnResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.common.game.GoodColor;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.Position;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Abandoned Station: salvage for whoever has the crew to go and look.
 *
 * <p>The gentler of the two abandoned sites. It costs flight days and nothing else — no
 * crew is lost, which is the whole difference from an abandoned ship (manual p.12). What
 * it wants instead is a crew large enough to search the place, and a player who is short
 * is simply not offered it.
 *
 * <p>Only one player gets it. The offer passes down the route until somebody takes it,
 * and then everybody behind them is cut out.
 *
 * @param identity    what deck building knows about this card
 * @param minimumCrew how many crew a ship needs before it may dock
 * @param goods       what the station is holding
 * @param flightDays  what docking costs
 */
public record AbandonedStationCard(AdventureCardIdentity identity, int minimumCrew,
                                   Map<GoodColor, Integer> goods, int flightDays) implements AdventureCard {

    /**
     * Validates the printed values and takes a defensive copy of the goods.
     *
     * @throws IllegalArgumentException if the crew requirement or the cost is negative,
     *                                  or the station is holding nothing
     */
    public AbandonedStationCard {
        if (minimumCrew < 0 || flightDays < 0) {
            throw new IllegalArgumentException("a station cannot demand negative crew or days");
        }
        Map<GoodColor, Integer> cargo = new EnumMap<>(GoodColor.class);
        goods.forEach((color, count) -> {
            if (count < 0) {
                throw new IllegalArgumentException("a station cannot hold " + count + " " + color + " cubes");
            }
            if (count > 0) {
                cargo.put(color, count);
            }
        });
        if (cargo.isEmpty()) {
            throw new IllegalArgumentException("a station with nothing in it is not worth docking at");
        }
        goods = Map.copyOf(cargo);
    }

    /**
     * Returns how many cubes the station is holding in total.
     *
     * @return the size of the haul
     */
    public int haulSize() {
        return goods.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight, this);
    }

    /**
     * Offers the station down the route until somebody docks, then lets them load.
     */
    private static final class Resolution extends TurnByTurnResolution {

        private final Flight flight;
        private final AbandonedStationCard card;

        Resolution(Flight flight, AbandonedStationCard card) {
            super(flight.stillFlying());
            this.flight = flight;
            this.card = card;
        }

        @Override
        protected Optional<PlayerPrompt> promptFor(PlayerColor player) {
            if (flight.shipOf(player).crewCount() < card.minimumCrew()) {
                // Not a refusal: a ship too small to search the place is never offered it.
                return Optional.empty();
            }
            return Optional.of(new PlayerPrompt.TakeOrLeave(player,
                    "an abandoned station holding " + card.haulSize() + " cubes", card.flightDays()));
        }

        @Override
        protected boolean apply(PlayerChoice choice) {
            return switch (choice) {
                case PlayerChoice.Leave ignored -> false;
                case PlayerChoice.Take take -> {
                    dock(take.player());
                    // Nobody behind is asked, and the docking player still has to stow.
                    yield true;
                }
                case PlayerChoice.Done done -> {
                    castOff(done.player());
                    yield true;
                }
                default -> throw new IllegalArgumentException(
                        "an abandoned station is waiting to be taken, left or finished with, not " + choice);
            };
        }

        private void dock(PlayerColor player) {
            Ship ship = flight.shipOf(player);
            ship.beginCargoOperations(card.goods());
            askAgain(new PlayerPrompt.ArrangeCargo(player, card.goods(), holdsOf(ship)));
        }

        private void castOff(PlayerColor player) {
            flight.shipOf(player).endCargoOperations();
            // The days are paid on leaving, so a player who found nowhere to put the
            // salvage still pays for having gone to look (manual p.19).
            flight.route().fallBack(player, card.flightDays());
        }

        private static Set<Position> holdsOf(Ship ship) {
            return ship.cargo().keySet().stream().collect(Collectors.toUnmodifiableSet());
        }
    }
}
