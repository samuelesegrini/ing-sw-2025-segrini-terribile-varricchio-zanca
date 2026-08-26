package it.polimi.ingsw.common.protocol.view;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.PlayerColor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The route and the card on the table.
 *
 * <p>Positions are absolute, not modulo the length of the route, so that a lapped ship is
 * distinguishable from one that is merely behind. A player one whole lap down is out of
 * the race (p.14), and a position that wrapped could not tell you that.
 *
 * @param routeLength how many spaces the board has
 * @param positions   where each ship stands, by absolute position
 * @param order       who plays first, second and so on, read off the route
 * @param card        the card being resolved, {@code null} between cards
 * @param cardsLeft   how many cards the deck still holds
 */
public record FlightView(int routeLength, Map<PlayerColor, Integer> positions,
                         List<PlayerColor> order, AdventureCardIdentity card,
                         int cardsLeft) implements Serializable {

    /**
     * Takes defensive copies of the route.
     *
     * @throws IllegalArgumentException if the route has no length or the deck a negative size
     */
    public FlightView {
        if (routeLength < 1) {
            throw new IllegalArgumentException("a route needs a positive length, got " + routeLength);
        }
        if (cardsLeft < 0) {
            throw new IllegalArgumentException("a deck cannot hold " + cardsLeft + " cards");
        }
        positions = Map.copyOf(positions);
        order = List.copyOf(order);
    }

    /**
     * Returns the card on the table.
     *
     * @return the card being resolved, or empty between cards
     */
    public Optional<AdventureCardIdentity> cardIfAny() {
        return Optional.ofNullable(card);
    }
}
