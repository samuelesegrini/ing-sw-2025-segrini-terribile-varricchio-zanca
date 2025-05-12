package it.polimi.ingsw.server.core;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GameSessionManager {
    private static final Logger LOGGER = Logger.getLogger(GameSessionManager.class.getName());

    private final EventBus serverEventBus;
    private final ServerNetworkManager networkManager;
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();

    public GameSessionManager(EventBus serverEventBus, ServerNetworkManager networkManager) {
        this.serverEventBus = Objects.requireNonNull(serverEventBus);
        this.networkManager = Objects.requireNonNull(networkManager);
        LOGGER.info("GameSessionManager initialized.");
    }

    /**
     * Creates a new game session and adds the creator to it.
     * @param creatorNetworkClientId The network client ID of the player creating the game.
     * @param creatorGamePlayerId The game-specific player ID of the creator.
     * @param creatorNickname The nickname of the creator.
     * @param settings The desired settings for the new game.
     * @return The newly created GameSession, or null if creation failed.
     */
    public GameSession createNewGameSession(String creatorNetworkClientId, String creatorGamePlayerId, String creatorNickname, GameSettingsDTO settings) {
        Objects.requireNonNull(creatorNetworkClientId);
        Objects.requireNonNull(creatorGamePlayerId);
        Objects.requireNonNull(creatorNickname);
        Objects.requireNonNull(settings);

        // Pass settings, creator ID, and creator nickname to GameSession constructor.
        // The GameSession constructor now handles setting the game name based on settings or default.
        GameSession newSession = new GameSession(serverEventBus, networkManager, settings, creatorGamePlayerId, creatorNickname); // Updated constructor call

        activeSessions.put(newSession.getSessionId(), newSession);
        LOGGER.info("Created new game session: " + newSession.getSessionId() + " named '" + newSession.getGameName() + "' by " + creatorNickname); // Log the name determined by the session

        // Automatically add the creator to their new session
        boolean added = newSession.addPlayer(creatorNetworkClientId, creatorGamePlayerId, creatorNickname);
        if (!added) {
            LOGGER.severe("FATAL: Could not add creator " + creatorNickname + " (ID: " + creatorGamePlayerId + ") to their own new session " + newSession.getSessionId() + ". Removing session.");
            activeSessions.remove(newSession.getSessionId());
            // This case should ideally not happen if maxPlayers >= 1
            return null;
        }

        // Optional: Post a global event that a new game is available
        // serverEventBus.post(new GlobalGameCreatedEvent(newSession.getLobbyInfo()));
        // Clients listening for this could auto-refresh their game lists.

        return newSession;
    }

    public GameSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public List<GameLobbyInfoDTO> getJoinableGames() {
        return activeSessions.values().stream()
                .filter(session -> session.getCurrentState() == GameSessionState.LOBBY &&
                        session.getCurrentPlayerCount() < session.getMaxPlayers())
                .map(GameSession::getLobbyInfo)
                .collect(Collectors.toList());
    }

    public List<GameLobbyInfoDTO> getRunningGames() {
        return activeSessions.values().stream()
                .filter(session -> session.getCurrentState() != GameSessionState.LOBBY &&
                        session.getCurrentState() != GameSessionState.FINISHED &&
                        session.getCurrentState() != GameSessionState.ABORTED)
                .map(GameSession::getLobbyInfo)
                .collect(Collectors.toList());
    }

    public List<GameLobbyInfoDTO> getAllGames() { // For comprehensive view or admin
        return activeSessions.values().stream()
                .map(GameSession::getLobbyInfo)
                .collect(Collectors.toList());
    }


    /**
     * Adds a player to a specific game session if it's in LOBBY state and not full.
     * @param sessionId The ID of the session to join.
     * @param networkClientId The network client ID of the joining player.
     * @param gamePlayerId The game-specific player ID.
     * @param nickname The player's nickname.
     * @return true if successfully added, false otherwise (session not found, full, or not in lobby).
     */
    public boolean addPlayerToSession(String sessionId, String networkClientId, String gamePlayerId, String nickname) {
        GameSession session = getSession(sessionId);
        if (session != null) {
            // GameSession's addPlayer method should check if it's in LOBBY state and not full.
            return session.addPlayer(networkClientId, gamePlayerId, nickname);
        }
        LOGGER.warning("Attempted to add player " + nickname + " (ID: " + gamePlayerId + ") to non-existent session: " + sessionId);
        // Sending error message back is handled by LoginController or the caller.
        return false;
    }

    /**
     * Removes a player from all sessions they might be in, typically on disconnect.
     * @param networkClientId The network client ID of the player to remove.
     */
    public void removePlayerFromAllSessions(String networkClientId) {
        LOGGER.info("Removing player associated with network client ID " + networkClientId + " from all sessions.");
        // Iterate through all sessions and remove the player by their networkClientId
        // GameSession's removePlayer method now takes networkClientId
        // Using a copy of values to avoid ConcurrentModificationException if removePlayer triggers cleanup
        new ArrayList<>(activeSessions.values()).forEach(session -> session.removePlayer(networkClientId));

        // Clean up finished/aborted sessions (optional, could be a periodic task or triggered here)
        cleanupOldSessions();
    }

    /**
     * Cleans up sessions that are finished or aborted and have no players remaining.
     */
    private void cleanupOldSessions() {
        int initialSize = activeSessions.size();
        activeSessions.entrySet().removeIf(entry -> {
            GameSession session = entry.getValue();
            boolean shouldCleanup = (session.getCurrentState() == GameSessionState.FINISHED ||
                    session.getCurrentState() == GameSessionState.ABORTED) &&
                    session.getCurrentPlayerCount() == 0; // Remove if empty and finished/aborted
            if (shouldCleanup) {
                LOGGER.info("Cleaning up session " + session.getSessionId() + " which is " + session.getCurrentState() + " and empty.");
            }
            return shouldCleanup;
        });
        int cleanedCount = initialSize - activeSessions.size();
        if (cleanedCount > 0) {
            LOGGER.info("Cleaned up " + cleanedCount + " old game sessions.");
        }
    }

    /**
     * Future enhancement: Add methods to properly archive completed games
     * with statistical data and results for historical tracking.
     */
}