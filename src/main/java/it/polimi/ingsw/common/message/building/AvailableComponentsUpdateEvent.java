package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ComponentDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event sent by the server to all players, updating the list of components
 * available in the face-up pile (if this mechanic is used).
 */
public class AvailableComponentsUpdateEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final List<ComponentDTO> faceUpPile;

    public AvailableComponentsUpdateEvent(List<ComponentDTO> faceUpPile) {
        super();
        this.faceUpPile = new ArrayList<>(Objects.requireNonNull(faceUpPile, "faceUpPile cannot be null"));
    }

    public List<ComponentDTO> getFaceUpPile() {
        return new ArrayList<>(faceUpPile); // Defensive copy
    }

    @Override
    public String toString() {
        return "AvailableComponentsUpdateEvent{" +
                "faceUpPileSize=" + faceUpPile.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}