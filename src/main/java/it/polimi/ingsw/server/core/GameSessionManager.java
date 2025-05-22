package it.polimi.ingsw.server.core;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GameSessionManager {
    private static final Logger LOGGER = Logger.getLogger(GameSessionManager.class.getName());

    private final EventBus serverEventBus;
    private final ServerNetworkManager networkManager;
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final PlayerSessionRegistry playerSessionRegistry;

    public GameSessionManager(EventBus serverEventBus, ServerNetworkManager networkManager, PlayerSessionRegistry playerSessionRegistry) { // << NEW PARAM
        this.serverEventBus = Objects.requireNonNull(serverEventBus);
        this.networkManager = Objects.requireNonNull(networkManager);
        this.playerSessionRegistry = Objects.requireNonNull(playerSessionRegistry);
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

        String newSessionId = "session-" + UUID.randomUUID().toString();
        GameSession newSession = new GameSession(newSessionId, settings, creatorGamePlayerId, creatorNickname, serverEventBus /*, persistenceService*/);

        activeSessions.put(newSession.getSessionId(), newSession);
        LOGGER.info("Created new game session: " + newSession.getSessionId() + " named '" + newSession.getGameName() + "' by " + creatorNickname);

        boolean added = newSession.addPlayer(creatorNetworkClientId, creatorGamePlayerId, creatorNickname);
        if (!added) {
            LOGGER.severe("FATAL: Could not add creator " + creatorNickname + " to their own session " + newSession.getSessionId() + ". Removing session.");
            activeSessions.remove(newSession.getSessionId());
            return null;
        }
        return newSession;
    }

    public GameSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public List<GameLobbyInfoDTO> getJoinableGames() {
        LOGGER.info("GSM.getJoinableGames() - Checking activeSessions. Count: " + activeSessions.size());
        activeSessions.forEach((id, session) -> {
            LOGGER.info("  Session ID: " + id +
                    ", Name: " + session.getGameName() +
                    ", State: " + session.getCurrentState() +
                    ", Players: " + session.getTotalRegisteredPlayerCount() + "/" + session.getMaxPlayers() +
                    ", AllReady (if lobby): " + (session.getCurrentState() == GameSessionState.LOBBY ? session.checkAllActivePlayersReady() : "N/A"));
        });
        return activeSessions.values().stream()
                .filter(session -> session.getCurrentState() == GameSessionState.LOBBY &&
                        session.getTotalRegisteredPlayerCount() < session.getMaxPlayers())
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
    public List<GameLobbyInfoDTO> getAllGames() {
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
            boolean success = session.addPlayer(networkClientId, gamePlayerId, nickname);
            return success;
        }
        LOGGER.warning("Attempted to add player " + nickname + " to non-existent session: " + sessionId);
        return false;
    }

    public void removePlayerFromAllSessions(String networkClientId) {
        LOGGER.info("GSM processing disconnect for NetID " + networkClientId);
        String sessionId = this.playerSessionRegistry.getSessionIdForNetworkClient(networkClientId);
        if (sessionId != null) {
            GameSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.removePlayerHard(networkClientId);
            } else {
                LOGGER.warning("PSR indicated NetID " + networkClientId + " was in session " + sessionId + ", but session not found in GSM activeSessions.");
            }
            this.playerSessionRegistry.removePlayerFromAnySessionByNetworkId(networkClientId);
        } else {
            LOGGER.info("NetID " + networkClientId + " not found in any session via PSR. No session-specific removal needed from GSM.");
        }
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
                    session.getTotalRegisteredPlayerCount() == 0;
            if (shouldCleanup) {
                LOGGER.info("Cleaning up session " + session.getSessionId() + " which is " + session.getCurrentState() + " and has no registered players.");
            }
            return shouldCleanup;
        });
        int cleanedCount = initialSize - activeSessions.size();
        if (cleanedCount > 0) {
            LOGGER.info("Cleaned up " + cleanedCount + " old game sessions.");
        }
    }
}