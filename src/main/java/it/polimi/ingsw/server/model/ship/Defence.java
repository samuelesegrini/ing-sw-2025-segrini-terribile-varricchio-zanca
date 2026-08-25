package it.polimi.ingsw.server.model.ship;

import java.util.Optional;

/**
 * What a player is putting in the way of an incoming threat, if anything.
 *
 * <p>One component at most: a shield turned the right way, or a cannon able to shoot the
 * meteor down. Which of the two is appropriate depends on what is coming, and the ship
 * decides whether the choice is a legal one.
 *
 * @param component the shield or cannon being used, or empty to take the hit
 */
public record Defence(Optional<Position> component) {

    /**
     * Validates the choice.
     *
     * @throws NullPointerException if the optional itself is {@code null}
     */
    public Defence {
        if (component == null) {
            throw new NullPointerException("a defence is an optional component, not null");
        }
    }

    /**
     * Returns the choice to take the hit.
     *
     * @return a defence that does nothing
     */
    public static Defence none() {
        return new Defence(Optional.empty());
    }

    /**
     * Returns a defence using one component.
     *
     * @param component the shield or cannon to use
     * @return the defence
     */
    public static Defence using(Position component) {
        return new Defence(Optional.of(component));
    }
}
