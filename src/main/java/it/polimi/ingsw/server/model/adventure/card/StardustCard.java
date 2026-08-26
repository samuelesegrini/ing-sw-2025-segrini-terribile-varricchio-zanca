package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.AutomaticResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.player.PlayerColor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stardust: every ship loses a flight day for each connector it leaves hanging out.
 *
 * <p>The card that makes tidy building pay. It asks nobody anything and offers no defence
 * — a ship's cost is exactly the number of exposed sides it built, counted once each
 * whatever their pipe count (manual p.13).
 *
 * <p>Carries no printed values at all, which is why it needs no data beyond its identity.
 *
 * @param identity what deck building knows about this card
 */
public record StardustCard(AdventureCardIdentity identity) implements AdventureCard {

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight);
    }

    /**
     * Counts every ship's exposed connectors and sends it back that far.
     *
     * <p>Reverse route order, the player furthest behind moving first (manual p.13), which
     * is what {@code Route} already does for simultaneous day loss. Doing it any other way
     * would let a ship in front be pushed onto a space a ship behind was about to vacate.
     */
    private static final class Resolution extends AutomaticResolution {

        private final Flight flight;

        Resolution(Flight flight) {
            this.flight = flight;
        }

        @Override
        protected void apply() {
            Map<PlayerColor, Integer> days = new LinkedHashMap<>();
            for (PlayerColor player : flight.stillFlying()) {
                days.put(player, flight.shipOf(player).exposedConnectors());
            }
            flight.route().loseFlightDaysTogether(days);
        }
    }
}
