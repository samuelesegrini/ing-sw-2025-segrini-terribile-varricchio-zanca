package it.polimi.ingsw.server.model.adventure.card;

import it.polimi.ingsw.server.model.adventure.AdventureCard;
import it.polimi.ingsw.server.model.adventure.AdventureCardIdentity;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.adventure.resolution.AutomaticResolution;
import it.polimi.ingsw.server.model.flight.Flight;
import it.polimi.ingsw.server.model.player.PlayerColor;
import it.polimi.ingsw.server.model.ship.Position;
import it.polimi.ingsw.server.model.ship.Ship;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Epidemic: every occupied cabin welded to another occupied cabin loses somebody.
 *
 * <p>The card that punishes packing crew quarters together. The manual's own advice is to
 * build so that no two cabins touch, and a player who ignored it pays for every joined
 * pair at once (manual p.19).
 *
 * <p>Two words in that rule do the work. <b>Occupied</b>: an empty cabin neither catches
 * the infection nor passes it on, so a ship that emptied one earlier gets off lighter.
 * And <b>welded</b>: cabins merely sitting side by side, smooth against smooth, are not
 * joined and are not affected — the same distinction that decides whether a life support
 * module keeps an alien alive.
 *
 * <p>Carries no printed values, so it needs nothing beyond its identity.
 *
 * @param identity what deck building knows about this card
 */
public record EpidemicCard(AdventureCardIdentity identity) implements AdventureCard {

    @Override
    public AdventureResolution resolve(Flight flight) {
        return new Resolution(flight);
    }

    /**
     * Empties one bunk in every cabin the infection reaches.
     */
    private static final class Resolution extends AutomaticResolution {

        private final Flight flight;

        Resolution(Flight flight) {
            this.flight = flight;
        }

        @Override
        protected void apply() {
            for (PlayerColor player : flight.stillFlying()) {
                Ship ship = flight.shipOf(player);
                // Worked out before anybody is removed. Taking crew one cabin at a time
                // could empty a cabin and spare its neighbour, which would make the
                // outcome depend on the order the cabins happen to be listed in.
                for (Position cabin : infectedCabinsOf(ship)) {
                    ship.loseOneCrewFrom(cabin);
                }
            }
        }

        private static Set<Position> infectedCabinsOf(Ship ship) {
            Set<Position> occupied = ship.occupiedCabins();
            Set<Position> infected = new LinkedHashSet<>();
            for (Position cabin : occupied) {
                if (ship.joinedTo(cabin).stream().anyMatch(occupied::contains)) {
                    infected.add(cabin);
                }
            }
            return infected;
        }
    }
}
