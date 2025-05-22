package it.polimi.ingsw.server.event;

import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.Message;

import java.util.List;

/**
 * Internal server event published when a player (new or rejoining and previously disconnected)
 * successfully becomes an active participant in a game session.
 * This event is distinct from InternalGameJoinedEvent, which is more of a direct response
 * to the player who sent the JoinGameRequest. This event is primarily for informing
 * OTHER players in the session and for internal server logic that needs to know about
 * roster changes.
 */
public record InternalPlayerJoinedMySessionEvent(
        String sessionId,
        PlayerInfoDTO newlyJoinedPlayerInfo,    // Info of the player who just joined/became active
        List<PlayerInfoDTO> allPlayersInSession, // The complete, updated list of *active* players
        String joiningPlayerNetworkClientId     // Network ID of the player who joined (for context)
) implements Message {
    private static final long serialVersionUID = 1L;
}