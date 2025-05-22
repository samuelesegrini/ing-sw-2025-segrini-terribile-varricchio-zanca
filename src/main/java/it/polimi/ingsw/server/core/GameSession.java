package it.polimi.ingsw.server.core;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.event.*;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GameSession {
    private static final Logger LOGGER = Logger.getLogger(GameSession.class.getName());

    private final String sessionId;
    private final EventBus serverEventBus;

    public GameModel getGameModel() {
        return gameModel;
    }

    protected static class PlayerInSessionStatus {
        String networkClientId;
        final String gamePlayerId;
        final String nickname;
        boolean isReady;
        final boolean isHost;
        AtomicBoolean isTemporarilyDisconnected = new AtomicBoolean(false);

        PlayerInSessionStatus(String networkClientId, String gamePlayerId, String nickname, boolean isHost) {
            this.networkClientId = networkClientId;
            this.gamePlayerId = gamePlayerId;
            this.nickname = nickname;
            this.isHost = isHost;
            // Host is ready by default
            this.isReady = isHost;
        }

        PlayerInfoDTO toPlayerInfoDTO() {
            String displayName = nickname + (isTemporarilyDisconnected.get() ? " (Disconnected)" : "");
            return new PlayerInfoDTO(gamePlayerId, displayName, isReady, isHost);
        }

        boolean isActive() { // Convenience method
            return !isTemporarilyDisconnected.get();
        }
    }

    private final Map<String, PlayerInSessionStatus> playersByGameId = new ConcurrentHashMap<>();
    // This map tracks which networkClientId is *currently* associated with a gamePlayerId IN THIS SESSION
    private final Map<String, String> currentNetworkIdToGameIdMap = new ConcurrentHashMap<>();

    private final int maxPlayers;
    private volatile GameSessionState currentState;
    private String gameName;
    private final GameLevel gameLevel;
    private final String creatorGamePlayerId;

    private GameModel gameModel;

    public GameSession(String sessionId, GameSettingsDTO settings, String creatorGamePlayerId, String creatorNickname,
                       EventBus serverEventBus) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.serverEventBus = Objects.requireNonNull(serverEventBus);

        this.gameLevel = settings.getGameLevel();
        this.maxPlayers = settings.getMaxPlayers();
        this.creatorGamePlayerId = Objects.requireNonNull(creatorGamePlayerId);
        this.gameName = (settings.getGameName() != null && !settings.getGameName().trim().isEmpty())
                ? settings.getGameName().trim() : creatorNickname + "'s Game";
        this.currentState = GameSessionState.LOBBY;

        LOGGER.info("New GameSession " + sessionId + " created: '" + gameName + "', Level: " + gameLevel + ", MaxPlayers: " + maxPlayers);
        // GameModel initialized when SHIP_BUILDING starts
    }


    //Getters (Synchronized where appropriate if state can change)
    public String getSessionId() {
        return sessionId;
    }

    public String getGameName() {
        return gameName;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public GameSessionState getCurrentState() {
        return currentState;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isCreator(String gamePlayerId) {
        return creatorGamePlayerId.equals(gamePlayerId);
    }

    public synchronized int getActivePlayerCount() {
        return (int) playersByGameId.values().stream().filter(PlayerInSessionStatus::isActive).count();
    }

    public synchronized int getTotalRegisteredPlayerCount() {
        return playersByGameId.size();
    }

    public synchronized PlayerInfoDTO getPlayerInfoDTO(String gamePlayerId) {
        PlayerInSessionStatus p = playersByGameId.get(gamePlayerId);
        return (p != null) ? p.toPlayerInfoDTO() : null;
    }

    public synchronized String getPlayerNickname(String gamePlayerId) {
        PlayerInSessionStatus p = playersByGameId.get(gamePlayerId);
        return (p != null) ? p.nickname : null;
    }

    public synchronized List<PlayerInfoDTO> getActivePlayersInfoDTOs() {
        return playersByGameId.values().stream()
                .filter(PlayerInSessionStatus::isActive)
                .map(PlayerInSessionStatus::toPlayerInfoDTO)
                .collect(Collectors.toList());
    }

    public synchronized List<PlayerInfoDTO> getAllRegisteredPlayersInfoDTOs() {
        return playersByGameId.values().stream()
                .map(PlayerInSessionStatus::toPlayerInfoDTO)
                .collect(Collectors.toList());
    }

    public synchronized boolean isPlayerTemporarilyDisconnected(String gamePlayerId) {
        PlayerInSessionStatus p = playersByGameId.get(gamePlayerId);
        return p != null && p.isTemporarilyDisconnected.get();
    }

    /**
     * Constructs a GameLobbyInfoDTO representing the current state of this session's lobby.
     * This method is thread-safe.
     *
     * @return A new GameLobbyInfoDTO instance.
     */
    public synchronized GameLobbyInfoDTO getLobbyInfo() {
        return new GameLobbyInfoDTO(
                this.sessionId,
                this.gameName,
                this.getTotalRegisteredPlayerCount(),
                this.maxPlayers,
                this.currentState,
                this.gameLevel
        );
    }


    //Player Management
    public synchronized boolean addPlayer(String networkClientId, String gamePlayerId, String nickname) {
        if (currentState != GameSessionState.LOBBY) {
            LOGGER.warning("SessID " + sessionId + ": Cannot add player " + nickname + ". Not in LOBBY (state=" + currentState + ")");
            return false;
        }

        PlayerInSessionStatus existingPlayerStatus = playersByGameId.get(gamePlayerId);

        if (existingPlayerStatus != null) { // Player is rejoining
            if (!existingPlayerStatus.isTemporarilyDisconnected.get()) {
                LOGGER.warning("SessID " + sessionId + ": Player " + nickname + " (GameID: " + gamePlayerId + ") trying to join but is already active with NetID " + existingPlayerStatus.networkClientId);
                if (!Objects.equals(existingPlayerStatus.networkClientId, networkClientId)) {
                    currentNetworkIdToGameIdMap.remove(existingPlayerStatus.networkClientId); // Remove old mapping
                    LOGGER.info("SessID " + sessionId + ": Player " + nickname + " already active, updating NetID from " + existingPlayerStatus.networkClientId + " to " + networkClientId);
                }
            }
            existingPlayerStatus.networkClientId = networkClientId;
            currentNetworkIdToGameIdMap.put(networkClientId, gamePlayerId);
            existingPlayerStatus.isTemporarilyDisconnected.set(false);
            LOGGER.info("SessID " + sessionId + ": Player " + nickname + " (GameID: " + gamePlayerId + ") re-joined/re-associated with NetID: " + networkClientId);
            serverEventBus.post(new InternalPlayerReconnectedEvent(sessionId, gamePlayerId, nickname, networkClientId, getActivePlayersInfoDTOs()));
            checkAndHandleGameResumption();
            return true;
        }

        // New player joining
        if (playersByGameId.size() >= maxPlayers) {
            LOGGER.warning("SessID " + sessionId + ": Lobby full. Cannot add new player " + nickname);
            return false;
        }

        boolean isHost = gamePlayerId.equals(creatorGamePlayerId);
        PlayerInSessionStatus newPlayerStatus = new PlayerInSessionStatus(networkClientId, gamePlayerId, nickname, isHost);
        playersByGameId.put(gamePlayerId, newPlayerStatus);
        currentNetworkIdToGameIdMap.put(networkClientId, gamePlayerId);

        LOGGER.info("SessID " + sessionId + ": New player " + nickname + " (GameID: " + gamePlayerId + ", NetID: " + networkClientId + ") added.");
        serverEventBus.post(new InternalPlayerJoinedMySessionEvent(sessionId, newPlayerStatus.toPlayerInfoDTO(), getAllRegisteredPlayersInfoDTOs(), networkClientId));
        return true;
    }

    /**
     * Handles a "hard" removal of a player, e.g., client explicitly left or fully timed out.
     *
     * @param networkClientIdToRemove The network ID of the client connection that is gone.
     */
    public synchronized void removePlayerHard(String networkClientIdToRemove) {
        String gamePlayerId = currentNetworkIdToGameIdMap.remove(networkClientIdToRemove);
        if (gamePlayerId == null) {
            LOGGER.info("SessID " + sessionId + ": Attempted hard removal for NetID " + networkClientIdToRemove + ", but no current gamePlayerId mapping. Client might have already been marked temporarily_disconnected or never fully joined session logic.");
            return;
        }

        PlayerInSessionStatus removedPlayerStatus = playersByGameId.remove(gamePlayerId);
        if (removedPlayerStatus != null) {
            LOGGER.info("SessID " + sessionId + ": Player " + removedPlayerStatus.nickname + " (GameID: " + gamePlayerId + ") hard removed.");

            GameSessionState stateBeforeCheck = this.currentState;
            boolean stateChanged = checkAndHandleGameSuspensionOrAbortion();
            GameSessionState stateAfterCheck = this.currentState;

            serverEventBus.post(new InternalPlayerLeftSessionEvent(
                    sessionId, gamePlayerId, removedPlayerStatus.nickname,
                    getActivePlayersInfoDTOs(),
                    removedPlayerStatus.isHost,
                    (stateChanged ? stateAfterCheck : stateBeforeCheck)
            ));
        } else {
            LOGGER.warning("SessID " + sessionId + ": NetID " + networkClientIdToRemove + " mapped to GameID " + gamePlayerId + ", but GameID not found in playersByGameId map during hard removal.");
        }
    }

    public synchronized void markPlayerAsTemporarilyDisconnected(String gamePlayerIdToMark, String associatedNetworkClientId) {
        PlayerInSessionStatus playerStatus = playersByGameId.get(gamePlayerIdToMark);
        if (playerStatus != null && Objects.equals(playerStatus.networkClientId, associatedNetworkClientId)) {
            if (playerStatus.isTemporarilyDisconnected.compareAndSet(false, true)) {
                currentNetworkIdToGameIdMap.remove(associatedNetworkClientId);

                LOGGER.info("SessID " + sessionId + ": Player " + playerStatus.nickname + " marked as temporarily disconnected.");
                serverEventBus.post(new InternalPlayerTemporarilyDisconnectedEvent(sessionId, gamePlayerIdToMark, playerStatus.nickname));
                checkAndHandleGameSuspensionOrAbortion();
            }
        } else if (playerStatus != null) {
            LOGGER.warning("SessID " + sessionId + ": Mismatch when marking player temporarily disconnected. Expected NetID " + playerStatus.networkClientId + ", got " + associatedNetworkClientId + " for GameID " + gamePlayerIdToMark);
        }
    }

    public synchronized void markPlayerAsReconnected(String gamePlayerIdToReconnect, String newNetworkClientId) {
        PlayerInSessionStatus playerStatus = playersByGameId.get(gamePlayerIdToReconnect);
        if (playerStatus != null) { // Player record exists
            if (playerStatus.isTemporarilyDisconnected.get()) {
                playerStatus.isTemporarilyDisconnected.set(false);
                LOGGER.info("SessID " + sessionId + ": Player " + playerStatus.nickname + " marked as reconnected.");
            } else {
                LOGGER.info("SessID " + sessionId + ": Player " + playerStatus.nickname + " was re-associating network ID but was not marked as temp_disconnected. Marking active.");
            }
            String oldNetIdMappedToThisGamePlayer = null;
            for (Map.Entry<String, String> entry : currentNetworkIdToGameIdMap.entrySet()) {
                if (entry.getValue().equals(gamePlayerIdToReconnect) && !entry.getKey().equals(newNetworkClientId)) {
                    oldNetIdMappedToThisGamePlayer = entry.getKey();
                    break;
                }
            }
            if (oldNetIdMappedToThisGamePlayer != null)
                currentNetworkIdToGameIdMap.remove(oldNetIdMappedToThisGamePlayer);


            playerStatus.networkClientId = newNetworkClientId;
            currentNetworkIdToGameIdMap.put(newNetworkClientId, gamePlayerIdToReconnect);

            checkAndHandleGameResumption();
        } else {
            LOGGER.warning("SessID " + sessionId + ": Tried to mark unknown GameID " + gamePlayerIdToReconnect + " as reconnected.");
        }
    }

    // --- State Transitions & Game Logic ---
    private void onEnterState(GameSessionState enteredState) {
        LOGGER.fine("SessID " + sessionId + ": Entered state " + enteredState);
        if (enteredState == GameSessionState.SHIP_BUILDING) {
            if (this.gameModel == null) {
                LOGGER.info("SessID " + sessionId + ": Initializing GameModel with " + getTotalRegisteredPlayerCount() + " registered players for SHIP_BUILDING.");
            } else {
                LOGGER.info("SessID " + sessionId + ": GameModel already exists. Skipping re-initialization.");
            }
        }
    }

    public synchronized void setPlayerReady(String requestingNetworkClientId, boolean isReady) {
        if (currentState != GameSessionState.LOBBY) {
            LOGGER.warning("SessID " + sessionId + ": Cannot set player ready. Not in LOBBY state.");
            serverEventBus.post(new InternalOperationErrorEvent(requestingNetworkClientId, "Cannot change ready: not in lobby.", false));
            return;
        }
        String gamePlayerId = currentNetworkIdToGameIdMap.get(requestingNetworkClientId);
        if (gamePlayerId == null) {
            LOGGER.warning("SessID " + sessionId + ": NetID " + requestingNetworkClientId + " not mapped to GameID for SetPlayerReady.");
            serverEventBus.post(new InternalOperationErrorEvent(requestingNetworkClientId, "Player not recognized in session.", false));
            return;
        }

        PlayerInSessionStatus playerStatus = playersByGameId.get(gamePlayerId);
        if (playerStatus != null) {
            if (playerStatus.isTemporarilyDisconnected.get()) {
                LOGGER.warning("SessID " + sessionId + ": Player " + playerStatus.nickname + " is disconnected, cannot change ready status.");
                serverEventBus.post(new InternalOperationErrorEvent(requestingNetworkClientId, "Cannot change ready: you are disconnected.", false));
                return;
            }
            if (playerStatus.isReady != isReady) {
                playerStatus.isReady = isReady;
                LOGGER.info("SessID " + sessionId + ": Player " + playerStatus.nickname + " readiness set to " + isReady);
                serverEventBus.post(new InternalPlayerReadyStatusChangedInSessionEvent(sessionId, getAllRegisteredPlayersInfoDTOs()));
                //saveState();
            }
        } else {
            LOGGER.severe("SessID " + sessionId + ": Critical inconsistency! NetID " + requestingNetworkClientId + " mapped to GameID " + gamePlayerId + " but GameID not in playersByGameId.");
            serverEventBus.post(new InternalOperationErrorEvent(requestingNetworkClientId, "Server error processing readiness.", true));
        }
    }

    public synchronized boolean startGame(String requestingHostGamePlayerId) {
        if (currentState != GameSessionState.LOBBY) {
            LOGGER.warning("SessID " + sessionId + ": startGame called but not in LOBBY state.");
            return false;
        }
        if (!Objects.equals(requestingHostGamePlayerId, creatorGamePlayerId)) {
            LOGGER.warning("SessID " + sessionId + ": startGame called by non-host " + requestingHostGamePlayerId);
            return false;
        }
        if (!checkAllActivePlayersReady()) {
            LOGGER.warning("SessID " + sessionId + ": startGame called by host but not all active players are ready.");
            return false;
        }
        if (getActivePlayerCount() < 2 && gameLevel != GameLevel.TEST_FLIGHT) {
            // Test flight right now allows 1 player for testing, then it has to be changed
            LOGGER.warning("SessID " + sessionId + ": startGame called by host but not enough active players (" + getActivePlayerCount() + ").");
            return false;
        }


        LOGGER.info("SessID " + sessionId + ": Host " + requestingHostGamePlayerId + " initiating game start.");
        if (transitionToState(GameSessionState.SHIP_BUILDING)) {
            serverEventBus.post(new InternalGameStartedInSessionEvent(sessionId, getActivePlayersInfoDTOs()));
            return true;
        }
        return false;
    }

    public synchronized boolean checkAllActivePlayersReady() {
        if (getActivePlayerCount() == 0) return false;
        return playersByGameId.values().stream()
                .filter(PlayerInSessionStatus::isActive)
                .allMatch(p -> p.isReady);
    }

    //Abort event still need to be implemented
    private synchronized boolean checkAndHandleGameSuspensionOrAbortion() {
        if (currentState == GameSessionState.FINISHED || currentState == GameSessionState.ABORTED) {
            return false;
        }

        long activePlayers = getActivePlayerCount();
        boolean stateChanged = false;

//        if (activePlayers == 0) {
//            LOGGER.info("SessID " + sessionId + ": All players inactive/disconnected. Aborting game.");
//            stateChanged = transitionToState(GameSessionState.ABORTED);
//        } else if (activePlayers == 1 && (currentState == GameSessionState.SHIP_BUILDING || currentState == GameSessionState.FLIGHT || currentState == GameSessionState.FLIGHT_PREPARATION)) {
//            PlayerInSessionStatus lastPlayer = playersByGameId.values().stream()
//                    .filter(PlayerInSessionStatus::isActive).findFirst().orElse(null);
//            if (lastPlayer != null) {
//                LOGGER.info("SessID " + sessionId + ": Only one player (" + lastPlayer.nickname + ") active. Suspending game logic (no state change).");
//                serverEventBus.post(new InternalGameSuspendedEvent(sessionId, lastPlayer.nickname));
//            }
//        }
        return stateChanged;
    }

    //Resume event still need to be implemented
    private synchronized boolean checkAndHandleGameResumption() {
        if (currentState == GameSessionState.ABORTED || currentState == GameSessionState.FINISHED) {
            return false;
        }
        long activePlayerCount = getActivePlayerCount();
//        if (activePlayerCount > 1) {
//            LOGGER.info("SessID " + sessionId + ": Active player count is " + activePlayerCount + ". Broadcasting game resume signal.");
//            serverEventBus.post(new InternalGameResumedEvent(sessionId, getActivePlayersInfoDTOs()));
//            return true;
//        }
        return false;
    }

    public synchronized Map<String, String> getCurrentNetworkIdToGameIdMap() {
        return new ConcurrentHashMap<>(currentNetworkIdToGameIdMap);
    }

    public synchronized String getGameIdForCurrentNetId(String networkClientId) {
        return this.currentNetworkIdToGameIdMap.get(networkClientId);
    }

    // Modify in GameSession.java
    public synchronized boolean transitionToState(GameSessionState newState) {
        if (this.currentState == newState && newState != GameSessionState.LOBBY) {
            LOGGER.finer("SessID " + sessionId + ": Already in state " + newState + ". No transition action.");
            return false;
        }
        GameSessionState oldState = this.currentState;
        this.currentState = newState;
        LOGGER.info("SessID " + sessionId + ": State transitioned from " + oldState + " to " + newState);

        serverEventBus.post(new InternalSessionActualStateChangedEvent(
                sessionId, oldState, newState, getAllRegisteredPlayersInfoDTOs()
        ));

        onEnterState(newState);
        return true;
    }
}