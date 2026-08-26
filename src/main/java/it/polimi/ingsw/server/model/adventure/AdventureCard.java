package it.polimi.ingsw.server.model.adventure;

import it.polimi.ingsw.common.game.AdventureCardIdentity;
import it.polimi.ingsw.common.game.AdventureCardType;
import it.polimi.ingsw.common.game.CardLevel;
import it.polimi.ingsw.server.model.adventure.resolution.AdventureResolution;
import it.polimi.ingsw.server.model.flight.Flight;

/**
 * One adventure card, with the rules it brings.
 *
 * <p>Deliberately not sealed. Nothing switches over card types — a card hands back its
 * own resolution and the controller drives that without knowing which card it is — so
 * sealing would buy exhaustiveness nobody needs while making every new card an edit to a
 * central list.
 *
 * <p>That is the same reason there is no visitor. The previous implementation had one,
 * and its single class reached 1147 lines because every card's logic had to live there.
 */
public interface AdventureCard {

    /**
     * Returns what deck building needs to know about this card.
     *
     * @return its identifier, type and level
     */
    AdventureCardIdentity identity();

    /**
     * Returns the adventure this card presents.
     *
     * @return the card type
     */
    default AdventureCardType type() {
        return identity().type();
    }

    /**
     * Returns the difficulty band printed on this card.
     *
     * @return the card level
     */
    default CardLevel level() {
        return identity().level();
    }

    /**
     * Begins resolving this card against a flight.
     *
     * @param flight the flight it has been turned over in
     * @return the resolution to drive
     */
    AdventureResolution resolve(Flight flight);
}
