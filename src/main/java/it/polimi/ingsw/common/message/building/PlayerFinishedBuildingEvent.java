package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Event sent by the server to all players when a player signals they have
 * finished building their ship and has taken a spot on the flight board.
 */
public class PlayerFinishedBuildingEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final PlayerInfoDTO player;
    private final int flightOrderPosition; // Their position (1st, 2nd, etc.) in flight order

    public PlayerFinishedBuildingEvent(PlayerInfoDTO player, int flightOrderPosition) {
        super();
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.flightOrderPosition = flightOrderPosition;
        if (flightOrderPosition <= 0) {
            throw new IllegalArgumentException("Flight order position must be positive.");
        }
    }

    public PlayerInfoDTO getPlayer() {
        return player;
    }

    public int getFlightOrderPosition() {
        return flightOrderPosition;
    }

    @Override
    public String toString() {
        return "PlayerFinishedBuildingEvent{" +
                "player=" + player.getNickname() +
                ", flightOrderPosition=" + flightOrderPosition +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}