package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.ShipBoardDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Event sent by the server to all players in a session to provide an updated
 * view of a specific player's ship board. This is used to keep all clients
 * synchronized with the state of ships, especially if players can view each other's ships.
 */
public class PlayerShipUpdateEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final String playerId; // The player whose ship this update pertains to
    private final ShipBoardDTO shipBoard;

    public PlayerShipUpdateEvent(String playerId, ShipBoardDTO shipBoard) {
        super();
        this.playerId = Objects.requireNonNull(playerId, "playerId cannot be null");
        this.shipBoard = Objects.requireNonNull(shipBoard, "shipBoard cannot be null");
    }

    public String getPlayerId() {
        return playerId;
    }

    public ShipBoardDTO getShipBoard() {
        return shipBoard; // ShipBoardDTO is a record, its lists are copied on construction/access.
    }

    @Override
    public String toString() {
        return "PlayerShipUpdateEvent{" +
                "playerId='" + playerId + '\'' +
                ", shipBoardHash=" + (shipBoard != null ? shipBoard.hashCode() : "null") + // Avoid printing full board
                ", timestamp=" + getTimestamp() +
                '}';
    }
}