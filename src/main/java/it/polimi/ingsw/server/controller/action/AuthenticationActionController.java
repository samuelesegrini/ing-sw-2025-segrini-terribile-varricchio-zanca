package it.polimi.ingsw.server.controller.action;

import it.polimi.ingsw.common.message.system.ClientLoginRequest;
import it.polimi.ingsw.server.controller.CommandContext;
import it.polimi.ingsw.server.core.ConnectionMonitorService; // For CommandContext if it gets it
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;
import it.polimi.ingsw.server.event.*; // All internal server events

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import it.polimi.ingsw.common.event.EventBus;


public class AuthenticationActionController {
    private static final Logger LOGGER = Logger.getLogger(AuthenticationActionController.class.getName());

    public AuthenticationActionController() {
        LOGGER.info("AuthenticationActionController initialized.");
    }

    // Method to handle client login requests
    public void handleLogin(CommandContext<ClientLoginRequest> context) {
        ClientLoginRequest request = context.command();
        String networkClientId = context.networkClientId();
        String nickname = request.getNickname().trim();

        Map<String, String> networkClientToGamePlayerMap = context.networkClientToGamePlayerMap();
        Map<String, String> activePlayersByIdMap = context.activePlayersByIdMap(); // gamePlayerId -> nickname
        // Rebuild activePlayersByNickname each time, or use a dedicated identity service.
        Map<String, String> activePlayersByNickname = activePlayersByIdMap.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue().toLowerCase(), Map.Entry::getKey, (v1,v2)->v1)); // nick.toLower -> gamePlayerId

        PlayerSessionRegistry playerSessionRegistry = context.playerSessionRegistry();
        GameSessionManager sessionManager = context.sessionManager();
        EventBus serverEventBus = context.serverEventBus();


        LOGGER.info("Authhandling login for nickname: '" + nickname + "' from NetID: " + networkClientId);

        if (nickname.isEmpty() || nickname.length() < 3 || nickname.length() > 20) {
            serverEventBus.post(new InternalLoginFailedEvent(networkClientId, "Login failed: Nickname must be 3-20 characters."));
            return;
        }

        // Case 1: This specific network connection is ALREADY logged in as someone.
        if (networkClientToGamePlayerMap.containsKey(networkClientId)) {
            String alreadyLoggedInGamePlayerId = networkClientToGamePlayerMap.get(networkClientId);
            String alreadyLoggedInNickname = activePlayersByIdMap.get(alreadyLoggedInGamePlayerId);
            LOGGER.info("NetID " + networkClientId + " (Player " + alreadyLoggedInNickname + ") is already logged in. Resending success.");
            serverEventBus.post(new InternalLoginSuccessEvent(networkClientId, alreadyLoggedInGamePlayerId, alreadyLoggedInNickname));
            // TODO: If they were in a game, re-send game state via a separate mechanism (e.g. InternalPlayerReconnectedEvent for current game)
            // This path implies client tried to login again without proper disconnect, or is just confirming.
            return;
        }

        // Case 2: Nickname is potentially in use by another gamePlayerId (could be same player on different connection OR different player)
        synchronized (activePlayersByIdMap) { // Main lock for managing player identities
            String existingGamePlayerIdForNickname = activePlayersByNickname.get(nickname.toLowerCase());

            if (existingGamePlayerIdForNickname != null) {
                // Nickname is in use. Check if it's a valid reconnection for this GamePlayerId.
                String sessionId = playerSessionRegistry.getSessionIdForGamePlayer(existingGamePlayerIdForNickname);
                GameSession session = (sessionId != null) ? sessionManager.getSession(sessionId) : null;

                if (session != null && session.isPlayerTemporarilyDisconnected(existingGamePlayerIdForNickname)) {
                    LOGGER.info("Reconnection attempt for " + nickname + " (GameID: " + existingGamePlayerIdForNickname + ") with new NetID: " + networkClientId + " for session " + sessionId);

                    // 1. Clean up any old network ID associated with this gamePlayerId from global map
                    String oldNetId = null;
                    for(Map.Entry<String, String> entry : networkClientToGamePlayerMap.entrySet()){
                        if(entry.getValue().equals(existingGamePlayerIdForNickname)){
                            oldNetId = entry.getKey();
                            break;
                        }
                    }
                    if(oldNetId != null) {
                        networkClientToGamePlayerMap.remove(oldNetId);
                        playerSessionRegistry.removePlayerFromAnySessionByNetworkId(oldNetId); // Ensure PSR knows old net id is gone
                        LOGGER.fine("Removed old NetID " + oldNetId + " mapping for rejoining GameID " + existingGamePlayerIdForNickname);
                    }


                    // 2. Establish new mapping
                    networkClientToGamePlayerMap.put(networkClientId, existingGamePlayerIdForNickname);
                    // activePlayersByIdMap still correctly maps existingGamePlayerIdForNickname -> nickname

                    // 3. Update PlayerSessionRegistry with new NetID for this gamePlayerId in this session
                    playerSessionRegistry.registerPlayerInSession(networkClientId, existingGamePlayerIdForNickname, sessionId);

                    // 4. Notify GameSession to update its internal network ID for the player and mark as active
                    session.markPlayerAsReconnected(existingGamePlayerIdForNickname, networkClientId);

                    // 5. Publish event for NotificationController to send "Welcome Back" and state
                    serverEventBus.post(new InternalPlayerReconnectedEvent(
                            sessionId, existingGamePlayerIdForNickname, nickname, networkClientId,
                            session.getActivePlayersInfoDTOs() // Send current active players list
                    ));
                    return;
                } else {
                    // Nickname is taken, and it's not a valid reconnection (e.g., player is still active, or session ended)
                    LOGGER.warning("Login failed for " + nickname + ". Nickname taken or player not eligible for reconnection.");
                    serverEventBus.post(new InternalLoginFailedEvent(networkClientId, "Login failed: Nickname '" + nickname + "' is already in use or reconnection not possible."));
                    return;
                }
            }

            // Case 3: New player login, nickname is available.
            String newGamePlayerId = "player-" + UUID.randomUUID().toString();
            activePlayersByIdMap.put(newGamePlayerId, nickname);
            networkClientToGamePlayerMap.put(networkClientId, newGamePlayerId);

            LOGGER.info("Login successful for NEW player '" + nickname + "'. Assigned GamePlayerID: " + newGamePlayerId + " to NetID: " + networkClientId);
            serverEventBus.post(new InternalLoginSuccessEvent(networkClientId, newGamePlayerId, nickname));
        }
    }

    /**
     * Handles a "hard" client disconnection reported by the network layer.
     */
    public void handlePlayerDisconnect(String networkClientId,
                                       PlayerSessionRegistry playerSessionRegistry,
                                       Map<String, String> networkClientToGamePlayerMap,
                                       Map<String, String> activePlayersByIdMap,
                                       EventBus serverEventBus,
                                       GameSessionManager sessionManager) {
        LOGGER.info("AuthHandling hard disconnect for NetID: " + networkClientId);

        // If monitored, tell ConnectionMonitor this client is definitely gone
        // ConnectionMonitorService monitor = context.connectionMonitorService(); // if context was passed
        // if(monitor!=null) monitor.confirmHardDisconnect(networkClientId);

        String gamePlayerId = networkClientToGamePlayerMap.remove(networkClientId);

        if (gamePlayerId != null) {
            String nickname = activePlayersByIdMap.remove(gamePlayerId);
            // Clean derived map: Map<String, String> activePlayersByNickname needs update
            if (nickname != null) {
                // activePlayersByNickname.remove(nickname.toLowerCase()); // Assuming this map is rebuilt or managed by identity service
                LOGGER.info("Identity removed due to disconnect: GameID=" + gamePlayerId + ", Nickname=" + nickname);
            } else {
                LOGGER.warning("GamePlayerID " + gamePlayerId + " was mapped from NetID " + networkClientId + " but not in activePlayersByIdMap!");
            }

            String sessionId = playerSessionRegistry.getSessionIdForGamePlayer(gamePlayerId); // Get session BEFORE cleaning PSR for gamePlayerId
            playerSessionRegistry.removePlayerFromAnySessionByNetworkId(networkClientId); // Clean current netID mapping
            if(sessionId != null) {
                playerSessionRegistry.removeGamePlayerFromSessionMap(gamePlayerId, sessionId); // Also clean gamePlayerID mapping to this session
            }


            if (sessionId != null) {
                GameSession session = sessionManager.getSession(sessionId);
                if (session != null) {
                    LOGGER.info("Notifying session " + sessionId + " about hard disconnect of player " + gamePlayerId);
                    session.removePlayerHard(networkClientId); // Use gamePlayerId if currentNetworkIdToGameIdMap in session reliable for gamePlayerId
                    // Or let GameSession find gamePlayerId from networkClientId via its internal map for removal.
                    // Current GameSession.removePlayerHard() expects networkClientId.
                } else {
                    LOGGER.warning("Player " + gamePlayerId + " was in session " + sessionId + " (PSR) but session not found in manager.");
                }
            }
            serverEventBus.post(new InternalGameLobbyListPotentiallyChangedEvent()); // Game lists might change
        } else {
            LOGGER.warning("Hard disconnected NetID " + networkClientId + " was not mapped to a GamePlayerID.");
        }
    }
}