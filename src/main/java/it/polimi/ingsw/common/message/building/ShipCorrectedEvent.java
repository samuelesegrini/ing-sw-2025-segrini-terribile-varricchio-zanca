package it.polimi.ingsw.common.message.building;


import it.polimi.ingsw.common.dto.PenaltyDTO;
import it.polimi.ingsw.common.dto.ShipBoardDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Event sent by the server to a player (or all players) after their ship
 * has been automatically corrected due to rule violations found during the spot check.
 */
public class ShipCorrectedEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String playerId;
    private final ShipBoardDTO correctedShipBoard;
    private final List<PenaltyDTO> penaltiesApplied;

    public ShipCorrectedEvent(String playerId, ShipBoardDTO correctedShipBoard, List<PenaltyDTO> penaltiesApplied) {
        super();
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.correctedShipBoard = Objects.requireNonNull(correctedShipBoard, "correctedShipBoard cannot be null");
        this.penaltiesApplied = new ArrayList<>(Objects.requireNonNull(penaltiesApplied, "penaltiesApplied list cannot be null"));
    }

    public String getPlayerId() {
        return playerId;
    }

    public ShipBoardDTO getCorrectedShipBoard() {
        return correctedShipBoard;
    }

    public List<PenaltyDTO> getPenaltiesApplied() {
        return new ArrayList<>(penaltiesApplied); // Defensive copy
    }

    @Override
    public String toString() {
        return "ShipCorrectedEvent{" +
                "playerId='" + playerId + '\'' +
                ", penaltiesCount=" + penaltiesApplied.size() +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}
