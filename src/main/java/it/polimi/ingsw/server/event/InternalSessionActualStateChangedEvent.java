// File: it.polimi.ingsw.server.event.InternalSessionActualStateChangedEvent.java
package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.model.GameSessionState; // Common model state
import java.util.List;
import java.util.Objects;

/**
 * Generic internal event published by GameSession when its state *actually* changes
 * (and it's not covered by a more specific event like InternalGameStartedEvent).
 * NotificationController listens to this to send GameSessionStateChangedEvent to clients.
 */
public record InternalSessionActualStateChangedEvent(
        String sessionId,
        GameSessionState oldState,
        GameSessionState newState,
        List<PlayerInfoDTO> playersInSession
) implements Message {
    private static final long serialVersionUID = 1L;

    public InternalSessionActualStateChangedEvent {
        Objects.requireNonNull(sessionId);
        // oldState can be null for initial state setting
        Objects.requireNonNull(newState);
        Objects.requireNonNull(playersInSession);
    }
}