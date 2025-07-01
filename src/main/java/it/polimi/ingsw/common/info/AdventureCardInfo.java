package it.polimi.ingsw.common.info;

import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;

import java.io.Serializable;

/**
 * Information about an adventure card that can be safely shared with clients.
 */
public class AdventureCardInfo implements Serializable {
    private final String id;
    private final CardLevel level;
    private final String description;
    private final AdventureType type;

    public AdventureCardInfo(String id, CardLevel level, String description, AdventureType type) {
        this.id = id;
        this.level = level;
        this.description = description;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public CardLevel getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public AdventureType getType() {
        return type;
    }
}