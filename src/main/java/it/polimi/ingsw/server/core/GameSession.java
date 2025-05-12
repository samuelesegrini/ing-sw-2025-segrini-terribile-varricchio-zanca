package it.polimi.ingsw.server.core;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.message.setup.GameSessionStateChangedEvent;
import it.polimi.ingsw.common.message.setup.PlayerJoinedGameSessionNotification;
import it.polimi.ingsw.common.message.setup.PlayerLeftGameSessionNotification;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class GameSession {
    private static final Logger LOGGER = Logger.getLogger(GameSession.class.getName());

    private final String sessionId;
    private final EventBus sessionEventBus;
    private final ServerNetworkManager networkManager;

    private final Map<String, PlayerInfoDTO> players = new ConcurrentHashMap<>();
    private final Map<String, String> networkClientToGamePlayerId = new ConcurrentHashMap<>();

    private final int maxPlayers;
    private volatile GameSessionState currentState;

    private String gameName;
    private final GameLevel gameLevel;

    /**
     * Creates a new GameSession with the specified settings and creator's nickname.
     * The game name is determined from the settings, defaulting to "{creatorNickname}'s Game"
     * if no name is provided in the settings.
     * @param globalServerEventBus The shared server event bus.
     * @param networkManager The server network manager.
     * @param settings The game settings.
     * @param creatorNickname The nickname of the player creating the session (used for default name).
     */
    public GameSession(EventBus globalServerEventBus, ServerNetworkManager networkManager, GameSettingsDTO settings, String creatorNickname) {
        this.sessionId = "session-" + UUID.randomUUID().toString();
        this.sessionEventBus = Objects.requireNonNull(globalServerEventBus);
        this.networkManager = Objects.requireNonNull(networkManager);

        Objects.requireNonNull(settings, "Game settings cannot be null for session creation.");
        Objects.requireNonNull(creatorNickname, "Creator nickname cannot be null.");

        this.gameLevel = settings.getGameLevel();
        this.maxPlayers = settings.getMaxPlayers();

        // Determine game name: use provided name if not null/empty, otherwise default to creator's game name
        this.gameName = (settings.getGameName() != null && !settings.getGameName().trim().isEmpty())
                ? settings.getGameName().trim()
                : creatorNickname + "'s Game";


        this.currentState = GameSessionState.LOBBY;
        LOGGER.info("New GameSession created with ID: " + this.sessionId +
                ", Name: '" + this.gameName + "'" +
                ", Level: " + this.gameLevel +
                ", Max Players: " + this.maxPlayers +
                ", Initial State: " + this.currentState);
    }


    public String getGameName() {
        return gameName;
    }

    // Keep setter for potential future needs, but it won't be used for initial creation now
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public synchronized int getCurrentPlayerCount() {
        return players.size();
    }

    public String getSessionId() {
        return sessionId;
    }

    public synchronized GameSessionState getCurrentState() {
        return currentState;
    }

    private synchronized boolean transitionToState(GameSessionState newState) {
        if (this.currentState == newState) {
            LOGGER.finer("Session " + sessionId + " is already in state " + newState + ". No transition.");
            return false;
        }
        GameSessionState oldState = this.currentState;
        this.currentState = newState;
        LOGGER.info("GameSession " + sessionId + " transitioned from " + oldState + " to " + newState);
        broadcastSessionStateChange(oldState, newState);
        onEnterState(newState);
        return true;
    }

    private void onEnterState(GameSessionState enteredState) {
        switch (enteredState) {
            case SHIP_BUILDING:
                LOGGER.info("Session " + sessionId + " entered SHIP_BUILDING. Initializing ship building phase...");
                break;
            case FLIGHT_PREPARATION:
                LOGGER.info("Session " + sessionId + " entered FLIGHT_PREPARATION. Preparing for flight...");
                break;
            case FLIGHT:
                LOGGER.info("Session " + sessionId + " entered FLIGHT. Starting flight sequence...");
                break;
            case SCORING:
                LOGGER.info("Session " + sessionId + " entered SCORING. Calculating final scores...");
                break;
            case FINISHED:
                LOGGER.info("Session " + sessionId + " has FINISHED.");
                break;
            case ABORTED:
                LOGGER.info("Session " + sessionId + " has been ABORTED.");
                break;
            default:
                break;
        }
    }

    private void broadcastSessionStateChange(GameSessionState oldState, GameSessionState newState) {
        GameSessionStateChangedEvent event = new GameSessionStateChangedEvent(this.sessionId, oldState, newState, getPlayersInfo());
        LOGGER.finer("Broadcasting session state change for " + sessionId + " to its players.");
        broadcastToSessionPlayers(event);
    }


    public synchronized boolean addPlayer(String networkClientId, String gamePlayerId, String nickname) {
        if (currentState != GameSessionState.LOBBY) {
            LOGGER.warning("Cannot add player " + nickname + " to session " + sessionId + ". Game is not in LOBBY state (current: " + currentState + ")");
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage(
                            "Cannot join game: Game is already in progress or finished.",
                            ErrorMessage.ErrorType.VALIDATION
                    ));
            return false;
        }
        if (players.size() >= maxPlayers) {
            LOGGER.warning("Session " + sessionId + " is full. Cannot add player " + nickname);
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage(
                            "Cannot join game: Lobby is full.",
                            ErrorMessage.ErrorType.VALIDATION
                    ));
            return false;
        }

        // Check if the player is already in this session (e.g., re-connecting)
        if (players.containsKey(gamePlayerId)) {
            LOGGER.info("Player " + nickname + " (ID: " + gamePlayerId + ") already in session " + sessionId + ". Updating network ID if changed.");
            // Remove old mapping if it exists and is different (player connected from new socket)
            networkClientToGamePlayerId.entrySet().removeIf(entry -> entry.getValue().equals(gamePlayerId) && !entry.getKey().equals(networkClientId));
            // Add/update the network ID for this game player ID
            networkClientToGamePlayerId.put(networkClientId, gamePlayerId);

            // Send current state to the re-joining player
            networkManager.sendMessageToClient(networkClientId, new PlayerJoinedGameSessionNotification(sessionId, players.get(gamePlayerId), getPlayersInfo()));
            networkManager.sendMessageToClient(networkClientId, new GameSessionStateChangedEvent(sessionId, currentState, currentState, getPlayersInfo()));
            // No need to broadcast to others unless their status changed, which it didn't just by re-joining.
            // If readiness was lost on disconnect, that's handled elsewhere.
            return true;
        }


        PlayerInfoDTO playerInfo = new PlayerInfoDTO(gamePlayerId, nickname);
        
        // Set the isHost flag to true if this player is the creator
        if (gamePlayerId.equals(creatorGamePlayerId)) {
            playerInfo.setHost(true);
            playerInfo.setReady(true); // Auto-mark the host as ready
            LOGGER.info("Player " + nickname + " marked as HOST for session " + sessionId + " and automatically set as READY");
        }
        
        players.put(gamePlayerId, playerInfo);
        networkClientToGamePlayerId.put(networkClientId, gamePlayerId);
        LOGGER.info("Player " + nickname + " (ID: " + gamePlayerId + ", NetworkID: " + networkClientId + ") added to session " + sessionId);

        // Notify all players (including the new one) about the updated lobby state
        PlayerJoinedGameSessionNotification joinNotification = new PlayerJoinedGameSessionNotification(
                this.sessionId,
                playerInfo, // The newly joined player
                getPlayersInfo() // The full, updated list of players
        );
        broadcastToSessionPlayers(joinNotification);

        return true;
    }

    public synchronized void removePlayer(String networkClientIdToRemove) {
        String gamePlayerId = networkClientToGamePlayerId.remove(networkClientIdToRemove);
        if (gamePlayerId != null) {
            PlayerInfoDTO removedPlayer = players.remove(gamePlayerId);
            if (removedPlayer != null) {
                LOGGER.info("Player " + removedPlayer.getNickname() + " (ID: " + gamePlayerId + ") removed from session " + sessionId);

                // Check if the removed player was the host
                boolean wasHost = isCreator(gamePlayerId);
                
                // Notify remaining players
                if (!players.isEmpty()) {
                    PlayerLeftGameSessionNotification leftNotification = new PlayerLeftGameSessionNotification(
                            this.sessionId,
                            removedPlayer.getPlayerId(),
                            removedPlayer.getNickname(),
                            getPlayersInfo(),
                            wasHost // Include information about whether the player was the host
                    );
                    broadcastToSessionPlayers(leftNotification);
                    
                    // If host left while in LOBBY state, abort the game
                    if (wasHost && currentState == GameSessionState.LOBBY) {
                        LOGGER.info("Host left session " + sessionId + " while in LOBBY state. Aborting game.");
                        transitionToState(GameSessionState.ABORTED);
                    }
                }

                // If the session is not already finished or aborted, check if it should be aborted now.
                if (currentState != GameSessionState.FINISHED && currentState != GameSessionState.ABORTED && players.isEmpty()) {
                    LOGGER.info("Last player left session " + sessionId + ". Aborting game.");
                    transitionToState(GameSessionState.ABORTED);
                }

            } else {
                LOGGER.warning("Attempted to remove player with gamePlayerId " + gamePlayerId + " associated with networkClientId " + networkClientIdToRemove + " from session " + sessionId + ", but gamePlayerId was not found in players map.");
            }
        } else {
            LOGGER.warning("Attempted to remove player with unknown network client ID: " + networkClientIdToRemove + " from session " + sessionId);
        }
    }

    private String creatorGamePlayerId = null;

    public GameSession(EventBus globalServerEventBus, ServerNetworkManager networkManager, GameSettingsDTO settings, String creatorGamePlayerId, String creatorNickname) {
        this.sessionId = "session-" + UUID.randomUUID().toString();
        this.sessionEventBus = Objects.requireNonNull(globalServerEventBus);
        this.networkManager = Objects.requireNonNull(networkManager);

        Objects.requireNonNull(settings, "Game settings cannot be null for session creation.");
        Objects.requireNonNull(creatorGamePlayerId, "Creator game player ID cannot be null.");
        Objects.requireNonNull(creatorNickname, "Creator nickname cannot be null.");

        this.gameLevel = settings.getGameLevel();
        this.maxPlayers = settings.getMaxPlayers();
        this.creatorGamePlayerId = creatorGamePlayerId; // Set creator ID

        // Determine game name: use provided name if not null/empty, otherwise default to creator's game name
        this.gameName = (settings.getGameName() != null && !settings.getGameName().trim().isEmpty())
                ? settings.getGameName().trim()
                : creatorNickname + "'s Game"; // Using creator's nickname as default


        this.currentState = GameSessionState.LOBBY;
        LOGGER.info("New GameSession created with ID: " + this.sessionId +
                ", Name: '" + this.gameName + "'" + // Log with quotes for clarity
                ", Level: " + this.gameLevel +
                ", Max Players: " + this.maxPlayers +
                ", Initial State: " + this.currentState);
    }


    public synchronized void setPlayerReady(String networkClientId, boolean isReady) {
        if (currentState != GameSessionState.LOBBY) {
            LOGGER.warning("Cannot set player ready status for session " + sessionId + ". Not in LOBBY state.");
            networkManager.sendMessageToClient(networkClientId, new ErrorMessage("Cannot change ready state: not in lobby.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            return;
        }
        String gamePlayerId = networkClientToGamePlayerId.get(networkClientId);
        if (gamePlayerId != null) {
            PlayerInfoDTO playerInfo = players.get(gamePlayerId);
            if (playerInfo != null) {
                if (playerInfo.isReady() == isReady) return; // No change

                playerInfo.setReady(isReady);
                LOGGER.info("Player " + playerInfo.getNickname() + " in session " + sessionId + " readiness set to: " + isReady);

                // Instead of automatically starting the game, just broadcast the updated state
                // so the host can see all players are ready and click the Start Game button
                broadcastSessionStateChange(currentState, currentState);
            } else {
                LOGGER.warning("Network client " + networkClientId + " mapped to unknown gamePlayerId " + gamePlayerId + " in session " + sessionId + " during setPlayerReady.");
            }
        } else {
            LOGGER.warning("Received setPlayerReady from unknown network client ID: " + networkClientId + " for session " + sessionId);
        }
    }


    /**
     * Checks if all conditions to start the game are met and starts it if they are.
     * @return true if the game started as a result of this call, false otherwise.
     */
    private boolean checkAndStartGameIfReady() {
        int minPlayersToStart = 2;

        boolean allPlayersReady = players.values().stream().allMatch(PlayerInfoDTO::isReady);
        boolean hasMinPlayers = players.size() >= minPlayersToStart;

        boolean canStart = hasMinPlayers && allPlayersReady;

        if (canStart) {
            LOGGER.info("All " + players.size() + " players in session " + sessionId + " are ready, and conditions met! Starting game.");
            boolean transitioned = transitionToState(GameSessionState.SHIP_BUILDING);
            if (!transitioned) {
                LOGGER.warning("Failed to transition session " + sessionId + " to SHIP_BUILDING after all players ready.");
                return false; // Transition failed
            }
            return true; // Game started
        }
        return false; // Game did not start
    }

    private synchronized void broadcastToSessionPlayers(it.polimi.ingsw.common.message.Message message) {
        LOGGER.finer("Broadcasting message of type " + message.getClass().getSimpleName() + " to " + networkClientToGamePlayerId.size() + " network clients in session " + sessionId);
        networkClientToGamePlayerId.keySet().forEach(clientId -> networkManager.sendMessageToClient(clientId, message));
    }

    public synchronized List<String> getPlayerNetworkClientIds() {
        return new ArrayList<>(networkClientToGamePlayerId.keySet());
    }

    public synchronized boolean hasPlayer(String gamePlayerId) {
        return players.containsKey(gamePlayerId);
    }

    public synchronized GameLobbyInfoDTO getLobbyInfo() {
        return new GameLobbyInfoDTO(
                this.sessionId,
                this.gameName,
                this.players.size(),
                this.maxPlayers,
                this.currentState,
                this.gameLevel
        );
    }

    public synchronized List<PlayerInfoDTO> getPlayersInfo() {
        return new ArrayList<>(players.values());
    }

    public synchronized boolean isCreator(String gamePlayerId) {
        return Objects.equals(this.creatorGamePlayerId, gamePlayerId);
    }

    /**
     * Checks if all players in the session are ready. 
     * Used by the host before starting the game.
     * 
     * @return true if all players are ready and there are enough players to start
     */
    public synchronized boolean checkAllPlayersReady() {
        // Need at least 2 players to start
        int minPlayersToStart = 2;
        
        // Game can start if minimum players are present AND all are ready
        boolean allPlayersReady = players.values().stream().allMatch(PlayerInfoDTO::isReady);
        boolean hasMinPlayers = players.size() >= minPlayersToStart;
        
        return hasMinPlayers && allPlayersReady;
    }
    
    /**
     * Starts the game by transitioning to the SHIP_BUILDING state.
     * Should only be called by the host after verifying all players are ready.
     * 
     * @return true if game started successfully, false otherwise
     */
    public synchronized boolean startGame() {
        if (currentState != GameSessionState.LOBBY) {
            LOGGER.warning("Cannot start game for session " + sessionId + ". Not in LOBBY state.");
            return false;
        }
        
        if (!checkAllPlayersReady()) {
            LOGGER.warning("Cannot start game for session " + sessionId + ". Not all players are ready or not enough players.");
            return false;
        }
        
        LOGGER.info("Host starting game for session " + sessionId + " with " + players.size() + " players.");
        
        // Transition to SHIP_BUILDING state
        boolean transitioned = transitionToState(GameSessionState.SHIP_BUILDING);
        if (!transitioned) {
            LOGGER.warning("Failed to transition session " + sessionId + " to SHIP_BUILDING when host started game.");
            return false;
        }
        
        return true;
    }
}