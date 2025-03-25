package it.polimi.ingsw.model.domain.general.loader;

import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.general.config.CardConfig;

@FunctionalInterface
public interface AdventureCardCreator {
    /**
     * Creates an adventure card from the given configuration
     * @param config The card configuration
     * @return The created adventure card
     */
    AdventureCard create(CardConfig config);
} 