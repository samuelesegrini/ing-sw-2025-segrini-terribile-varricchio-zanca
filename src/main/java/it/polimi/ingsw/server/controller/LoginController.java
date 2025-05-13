package it.polimi.ingsw.server.controller;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.event.MessageHandler;
import it.polimi.ingsw.common.message.setup.*;
import it.polimi.ingsw.common.message.system.ClientLoginRequest;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.message.system.ServerLoginResponse;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.core.GameSessionManager;
import it.polimi.ingsw.server.network.ServerNetworkManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    private final ServerNetworkManager networkManager;
    private final EventBus serverEventBus;
    private final GameSessionManager sessionManager;

    private final Map<String, String> gamePlayerToNetworkClient = new ConcurrentHashMap<>();
    private final Map<String, String> networkClientToGamePlayer = new ConcurrentHashMap<>();
    private final Map<String, String> activePlayersById = new ConcurrentHashMap<>(); // Key: gamePlayerId, Value: nickname
    private final Map<String, String> activePlayersByNickname = new ConcurrentHashMap<>();


    public LoginController(ServerNetworkManager networkManager, EventBus serverEventBus, GameSessionManager sessionManager) {
        this.networkManager = networkManager;
        this.serverEventBus = serverEventBus;
        this.sessionManager = sessionManager;
        this.serverEventBus.register(this);
        LOGGER.info("LoginController initialized and registered with EventBus.");
    }

    /**
     * Sends updated game lists to all connected clients.
     * This is more reliable than broadcasting a notification as it avoids serialization issues.
     */
    private void updateAllClientsWithGameList() {
        LOGGER.fine("Sending game list updates to all connected clients");
        
        // Get the current game lists
        List<GameLobbyInfoDTO> joinableGames = sessionManager.getJoinableGames();
        List<GameLobbyInfoDTO> runningGames = sessionManager.getRunningGames();
        
        // Create the response event
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        
        // Send to all logged-in clients
        for (String networkClientId : networkClientToGamePlayer.keySet()) {
            networkManager.sendMessageToClient(networkClientId, response);
        }
    }

    @MessageHandler
    public void onClientMessage(ServerNetworkManager.IncomingClientMessage incomingMessage) {
        Object originalMsg = incomingMessage.getOriginalMessage();
        String networkClientId = incomingMessage.getClientId();

        if (originalMsg instanceof ClientLoginRequest) {
            handleLoginRequest(networkClientId, (ClientLoginRequest) originalMsg);
        } else if (originalMsg instanceof RequestGameListCommand) {
            handleRequestGameList(networkClientId);
        } else if (originalMsg instanceof CreateGameRequestCommand) {
            handleCreateGameRequest(networkClientId, (CreateGameRequestCommand) originalMsg);
        } else if (originalMsg instanceof JoinGameRequestCommand) {
            handleJoinGameRequest(networkClientId, (JoinGameRequestCommand) originalMsg);
        } else if (originalMsg instanceof LeaveGameRequestCommand) {
            handleLeaveGameRequest(networkClientId, (LeaveGameRequestCommand) originalMsg);
        } else if (originalMsg instanceof SetPlayerReadyCommand) {
            handleSetPlayerReadyCommand(networkClientId, (SetPlayerReadyCommand) originalMsg);
        } else if (originalMsg instanceof StartGameRequestCommand) {
            handleStartGameRequest(networkClientId, (StartGameRequestCommand) originalMsg);
        }
        // Add handlers for other client commands if LoginController's scope expands
    }

    private void handleLoginRequest(String networkClientId, ClientLoginRequest request) {
        String nickname = request.getNickname();
        LOGGER.info("Processing login request for nickname: '" + nickname + "' from network client ID: " + networkClientId);

        if (nickname == null || nickname.trim().isEmpty() || nickname.length() < 3 || nickname.length() > 20) {
            networkManager.sendMessageToClient(networkClientId,
                    new ServerLoginResponse("Login failed: Nickname must be 3-20 characters."));
            return;
        }

        if (networkClientToGamePlayer.containsKey(networkClientId)) {
            String existingGamePlayerId = networkClientToGamePlayer.get(networkClientId);
            String existingNickname = activePlayersById.get(existingGamePlayerId);
            LOGGER.info("Network client " + networkClientId + " (Player " + existingNickname + ") is attempting to log in again.");
            ServerLoginResponse response = new ServerLoginResponse(existingGamePlayerId, "Welcome back, " + existingNickname + "!");
            networkManager.sendMessageToClient(networkClientId, response);
            // Potentially, also resend current game/lobby state if they were in one.
            return;
        }

        synchronized (activePlayersByNickname) {
            if (activePlayersByNickname.containsKey(nickname.toLowerCase())) {
                networkManager.sendMessageToClient(networkClientId,
                        new ServerLoginResponse("Login failed: Nickname '" + nickname + "' is already taken."));
                return;
            }

            String gamePlayerId = "player-" + UUID.randomUUID().toString();
            activePlayersById.put(gamePlayerId, nickname);
            activePlayersByNickname.put(nickname.toLowerCase(), gamePlayerId);
            gamePlayerToNetworkClient.put(gamePlayerId, networkClientId);
            networkClientToGamePlayer.put(networkClientId, gamePlayerId);

            LOGGER.info("Login successful for '" + nickname + "'. Assigned game PlayerID: " + gamePlayerId);
            ServerLoginResponse response = new ServerLoginResponse(gamePlayerId, "Welcome, " + nickname + "!");
            networkManager.sendMessageToClient(networkClientId, response);
        }
    }

    private void handleRequestGameList(String networkClientId) {
        LOGGER.info("Processing RequestGameListCommand from network client ID: " + networkClientId);
        List<GameLobbyInfoDTO> joinableGames = sessionManager.getJoinableGames();
        List<GameLobbyInfoDTO> runningGames = sessionManager.getRunningGames();
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        networkManager.sendMessageToClient(networkClientId, response);
    }

    private void handleCreateGameRequest(String networkClientId, CreateGameRequestCommand request) {
        LOGGER.info("Processing CreateGameRequestCommand from network client ID: " + networkClientId);
        String gamePlayerId = networkClientToGamePlayer.get(networkClientId);
        if (gamePlayerId == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new CreateGameResponseEvent("Error: You must be logged in to create a game."));
            LOGGER.warning("CreateGameRequest from non-logged-in network client ID: " + networkClientId);
            return;
        }
        String nickname = activePlayersById.get(gamePlayerId);
        if (nickname == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new CreateGameResponseEvent("Error: Server-side player data inconsistency."));
            LOGGER.severe("Inconsistency: gamePlayerId " + gamePlayerId + " found for networkId " + networkClientId + " but no nickname associated.");
            return;
        }

        GameSettingsDTO settings = request.getSettings();
        GameSession newSession = sessionManager.createNewGameSession(networkClientId, gamePlayerId, nickname, settings);

        if (newSession != null) {
            List<PlayerInfoDTO> playersInLobby = newSession.getPlayersInfo();
            
            CreateGameResponseEvent response = new CreateGameResponseEvent(
                newSession.getSessionId(), 
                newSession.getLobbyInfo(), 
                playersInLobby
            );
            networkManager.sendMessageToClient(networkClientId, response);
            LOGGER.info("Game session " + newSession.getSessionId() + " created successfully by " + nickname);
            
            updateAllClientsWithGameList();
        } else {
            networkManager.sendMessageToClient(networkClientId,
                    new CreateGameResponseEvent("Failed to create game session. Please try again."));
            LOGGER.warning("Game session creation failed for player " + nickname);
        }
    }

    private void handleJoinGameRequest(String networkClientId, JoinGameRequestCommand request) {
        LOGGER.info("Processing JoinGameRequestCommand for session " + request.getSessionId() + " from network client ID: " + networkClientId);
        String gamePlayerId = networkClientToGamePlayer.get(networkClientId);
        if (gamePlayerId == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new JoinGameResponseEvent(request.getSessionId(), "Error: You must be logged in to join a game."));
            LOGGER.warning("JoinGameRequest from non-logged-in network client ID: " + networkClientId);
            return;
        }
        String nickname = activePlayersById.get(gamePlayerId);
        if (nickname == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new JoinGameResponseEvent(request.getSessionId(), "Error: Server-side player data inconsistency."));
            LOGGER.severe("Inconsistency: gamePlayerId " + gamePlayerId + " found for networkId " + networkClientId + " but no nickname associated for join.");
            return;
        }

        String sessionIdToJoin = request.getSessionId();
        boolean joined = sessionManager.addPlayerToSession(sessionIdToJoin, networkClientId, gamePlayerId, nickname);

        if (joined) {
            GameSession joinedSession = sessionManager.getSession(sessionIdToJoin);
            if (joinedSession != null) {
                List<PlayerInfoDTO> playersInLobby = joinedSession.getPlayersInfo();
                JoinGameResponseEvent response = new JoinGameResponseEvent(sessionIdToJoin, playersInLobby);
                networkManager.sendMessageToClient(networkClientId, response);
                LOGGER.info("Player " + nickname + " successfully joined session " + sessionIdToJoin);
                
                updateAllClientsWithGameList();
            } else {
                networkManager.sendMessageToClient(networkClientId,
                        new JoinGameResponseEvent(sessionIdToJoin, "Error: Joined session but could not retrieve session details."));
                LOGGER.severe("Joined session " + sessionIdToJoin + " but sessionManager.getSession returned null.");
            }
        } else {
            GameSession session = sessionManager.getSession(sessionIdToJoin);
            String errorMessage = "Failed to join game: ";
            if (session == null) {
                errorMessage += "Game not found.";
            } else if (session.getCurrentPlayerCount() >= session.getMaxPlayers()) {
                errorMessage += "Game is full.";
            } else if (session.getCurrentState() != it.polimi.ingsw.common.model.GameSessionState.LOBBY) {
                errorMessage += "Game has already started or is finished.";
            } else {
                errorMessage += "Unknown reason.";
            }
            networkManager.sendMessageToClient(networkClientId,
                    new JoinGameResponseEvent(sessionIdToJoin, errorMessage));
            LOGGER.warning("Player " + nickname + " failed to join session " + sessionIdToJoin + ". Reason implied by session state or existence.");
        }
    }

    private void handleLeaveGameRequest(String networkClientId, LeaveGameRequestCommand request) {
        LOGGER.info("Processing LeaveGameRequestCommand for session " + request.getSessionId() + " from network client ID: " + networkClientId);
        String gamePlayerId = networkClientToGamePlayer.get(networkClientId);
        if (gamePlayerId == null) {
            LOGGER.warning("LeaveGameRequest from non-logged-in network client ID: " + networkClientId);
            return;
        }
        
        // The session manager will handle removing the player and sending notifications
        sessionManager.removePlayerFromAllSessions(networkClientId);
        
        // Broadcast game list update to all clients since a player left
        updateAllClientsWithGameList();
    }

    private void handleSetPlayerReadyCommand(String networkClientId, SetPlayerReadyCommand command) {
        LOGGER.info("Processing SetPlayerReadyCommand for session " + command.getSessionId() + " from network client ID: " + networkClientId);
        
        // Get the player's game ID
        String gamePlayerId = networkClientToGamePlayer.get(networkClientId);
        if (gamePlayerId == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: You must be logged in to change readiness status.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("SetPlayerReadyCommand from non-logged-in network client ID: " + networkClientId);
            return;
        }
        
        // Get the session
        String sessionId = command.getSessionId();
        GameSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: Session " + sessionId + " not found.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("SetPlayerReadyCommand for non-existent session " + sessionId + " from network client " + networkClientId);
            return;
        }
        
        // Set player ready status
        session.setPlayerReady(networkClientId, command.isReady());
    }

    private void handleStartGameRequest(String networkClientId, StartGameRequestCommand command) {
        LOGGER.info("Processing StartGameRequestCommand for session " + command.getSessionId() + " from network client ID: " + networkClientId);
        
        // Get the player's game ID
        String gamePlayerId = networkClientToGamePlayer.get(networkClientId);
        if (gamePlayerId == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: You must be logged in to start a game.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("StartGameRequestCommand from non-logged-in network client ID: " + networkClientId);
            return;
        }
        
        // Get the session
        String sessionId = command.getSessionId();
        GameSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: Session " + sessionId + " not found.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("StartGameRequestCommand for non-existent session " + sessionId + " from network client " + networkClientId);
            return;
        }
        
        // Check if the player is the host
        if (!session.isCreator(gamePlayerId)) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: Only the host can start the game.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("Non-host player tried to start game session " + sessionId);
            return;
        }
        
        // Check if all players are ready
        boolean canStart = session.checkAllPlayersReady();
        if (!canStart) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: Cannot start game until all players are ready.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("Host tried to start game session " + sessionId + " but not all players are ready");
            return;
        }
        
        // Start the game (transition to SHIP_BUILDING state)
        boolean started = session.startGame();
        if (!started) {
            networkManager.sendMessageToClient(networkClientId,
                    new ErrorMessage("Error: Failed to start game.", ErrorMessage.ErrorType.ILLEGAL_ACTION));
            LOGGER.warning("Failed to start game session " + sessionId);
        } else {
            LOGGER.info("Game session " + sessionId + " started successfully by host " + gamePlayerId);
            
            // Update game lists (session moved from joinable to running)
            updateAllClientsWithGameList();
        }
    }

    public void handleClientDisconnect(String networkClientId) {
        LOGGER.info("LoginController handling disconnect for network client ID: " + networkClientId);
        String gamePlayerId = networkClientToGamePlayer.remove(networkClientId);

        if (gamePlayerId != null) {
            String nickname = activePlayersById.remove(gamePlayerId);
            if (nickname != null) {
                activePlayersByNickname.remove(nickname.toLowerCase());
                gamePlayerToNetworkClient.remove(gamePlayerId);
                LOGGER.info("Player removed due to disconnect: GameID=" + gamePlayerId + ", Nickname=" + nickname);
                sessionManager.removePlayerFromAllSessions(networkClientId);
                
                // Broadcast game list update since a player disconnected (which might affect game availability)
                updateAllClientsWithGameList();
            } else {
                LOGGER.warning("Disconnected network client " + networkClientId + " had a gamePlayerId " + gamePlayerId + " but no nickname found.");
            }
        } else {
            LOGGER.warning("Disconnected network client " + networkClientId + " was not mapped to a gamePlayerId (perhaps never fully logged in).");
        }
    }

    public void handleClientConnect(String networkClientId) {
        LOGGER.info("Network connection established with ID: " + networkClientId + ". Awaiting login request.");
    }
}