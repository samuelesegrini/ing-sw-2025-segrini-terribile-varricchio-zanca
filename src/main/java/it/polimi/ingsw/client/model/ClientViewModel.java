package it.polimi.ingsw.client.model;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.network.ClientNetworkManager;
import it.polimi.ingsw.client.ui.UIFactory;
import it.polimi.ingsw.client.ui.UIThreadHandler;
import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.event.EventBus;
import it.polimi.ingsw.common.event.MessageHandler;
import it.polimi.ingsw.common.message.setup.*;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.message.system.ServerLoginResponse;
import it.polimi.ingsw.common.model.GameSessionState;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central client-side state and view model.
 * Holds application status, login details, lobby list, and current game session state.
 * Listens to server messages and updates observable properties for UI binding.
 */
public class ClientViewModel {
    private static final Logger LOGGER = Logger.getLogger(ClientViewModel.class.getName());
    private static boolean isGUIMode = true; // Default to GUI mode
    private final UIThreadHandler uiThreadHandler;

    // --- Application Status ---
    public enum AppStatus {
        NOT_CONNECTED,
        CONNECTING,
        LOGIN_SCREEN, // User needs to enter host/port/nickname
        LOGGING_IN,
        LOGGED_IN_BROWSING_LOBBIES, // Logged in, viewing list of available/running games
        GAME_LOBBY, // Logged in, inside a specific game session lobby
        GAME_BUILDING, // In game, building phase
        GAME_FLIGHT,   // In game, flight phase
        GAME_FINISHED, // Game ended, showing results
        DISCONNECTED // Disconnected from server after connection was established
    }

    private final ObjectProperty<AppStatus> appStatus = new SimpleObjectProperty<>(AppStatus.NOT_CONNECTED);
    private final StringProperty statusMessage = new SimpleStringProperty("Not connected."); // General status/error message for UI

    // --- Login State ---
    private final StringProperty loggedInPlayerId = new SimpleStringProperty(null);
    private final StringProperty loggedInNickname = new SimpleStringProperty(null);

    // --- Lobby Browser State (visible in LOGGED_IN_BROWSING_LOBBIES) ---
    private final ObservableList<GameLobbyInfoDTO> joinableGames = FXCollections.observableArrayList();
    private final ObservableList<GameLobbyInfoDTO> runningGames = FXCollections.observableArrayList();

    // --- Current Game Session State (visible in GAME_LOBBY, GAME_BUILDING, etc.) ---
    private final StringProperty currentSessionId = new SimpleStringProperty(null);
    private final ObjectProperty<GameLobbyInfoDTO> currentLobbyInfo = new SimpleObjectProperty<>(null); // Includes state, name, max players, etc.
    private final ObservableList<PlayerInfoDTO> playersInCurrentLobby = FXCollections.observableArrayList();


    private final EventBus clientEventBus;
    private ClientNetworkManager networkManager;
    private ExecutorService backgroundTaskExecutor; // Added field

    /**
     * Set the UI mode for all ViewModels
     * @param guiMode true for GUI mode, false for TUI mode
     */
    public static void setGUIMode(boolean guiMode) {
        isGUIMode = guiMode;
    }

    /**
     * Returns whether the application is running in GUI mode
     * @return true if in GUI mode, false if in TUI mode
     */
    public static boolean isGUIMode() {
        return isGUIMode;
    }

    public ClientViewModel(EventBus clientEventBus) {
        this.clientEventBus = Objects.requireNonNull(clientEventBus);
        this.clientEventBus.register(this); // Register this ViewModel as an EventBus listener

        // Create the appropriate UI thread handler based on mode
        this.uiThreadHandler = UIFactory.createThreadHandler(isGUIMode ? 
                GameClientController.UIMode.GUI : GameClientController.UIMode.TUI);

        // Initial status update to reflect the starting state (Connection View)
        setAppStatus(AppStatus.LOGIN_SCREEN);
    }

    // Method to inject NetworkManager (called by ClientApp)
    public void setNetworkManager(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    // Added setter for background task executor
    public void setBackgroundTaskExecutor(ExecutorService backgroundTaskExecutor) {
        this.backgroundTaskExecutor = Objects.requireNonNull(backgroundTaskExecutor);
    }

    // --- Observable Properties for UI Binding ---
    public ObjectProperty<AppStatus> appStatusProperty() { return appStatus; }
    public ReadOnlyStringProperty statusMessageProperty() { return statusMessage; }
    public ReadOnlyStringProperty loggedInPlayerIdProperty() { return loggedInPlayerId; }
    public ReadOnlyStringProperty loggedInNicknameProperty() { return loggedInNickname; }
    public ObservableList<GameLobbyInfoDTO> getJoinableGames() { return joinableGames; } // Directly return observable list
    public ObservableList<GameLobbyInfoDTO> getRunningGames() { return runningGames; } // Directly return observable list
    public ReadOnlyStringProperty currentSessionIdProperty() { return currentSessionId; }
    public ReadOnlyObjectProperty<GameLobbyInfoDTO> currentLobbyInfoProperty() { return currentLobbyInfo; }
    public ObservableList<PlayerInfoDTO> getPlayersInCurrentLobby() { return playersInCurrentLobby; }


    // --- State Update Methods (Called by Handlers) ---

    /** Sets the application status. */
    public void setAppStatus(AppStatus newStatus) {
        uiThreadHandler.runOnUIThread(() -> {
            LOGGER.info("App Status Transition: " + appStatus.get() + " -> " + newStatus);
            appStatus.set(newStatus);
        });
    }

    /** Sets the status message for the UI. */
    public void setStatusMessage(String message) {
        uiThreadHandler.runOnUIThread(() -> statusMessage.set(message));
    }

    /** Handles successful connection initiation (before login request is sent). */
    public void handleConnectionInitiated() {
        setAppStatus(AppStatus.CONNECTING);
        setStatusMessage("Connecting...");
    }

    /** Handles successful connection established (ready to send login request). */
    public void handleConnected() {
        setAppStatus(AppStatus.LOGGING_IN);
        setStatusMessage("Connected. Logging in...");
    }

    /** Handles connection failure. */
    public void handleConnectionFailed(String reason) {
        setAppStatus(AppStatus.NOT_CONNECTED); // Or DISCONNECTED if it failed after connect
        setStatusMessage("Connection failed: " + reason);
        resetLoginState(); // Clear any previous login info
        resetGameSessionState(); // Clear any previous game info
    }

    /** Handles server disconnection. */
    public void handleDisconnected(String reason) {
        // If we were already NOT_CONNECTED, it's likely a startup connection failure.
        // If we were in any other state, it's an unexpected disconnect.
        AppStatus statusBeforeDisconnect = appStatus.get();
        if (statusBeforeDisconnect != AppStatus.NOT_CONNECTED && statusBeforeDisconnect != AppStatus.LOGIN_SCREEN && statusBeforeDisconnect != AppStatus.CONNECTING) {
            setAppStatus(AppStatus.DISCONNECTED);
            setStatusMessage("Disconnected from server: " + reason);
        } else {
            // If disconnecting from login/connecting stage, just set status message.
            setStatusMessage("Connection attempt failed or disconnected: " + reason);
            setAppStatus(AppStatus.LOGIN_SCREEN); // Go back to the login screen UI
        }
        resetLoginState(); // Clear any previous login info
        resetGameSessionState(); // Clear any previous game info
    }


    /** Handles successful login response. */
    public void handleLoginSuccess(String playerId, String nickname, String message) {
        uiThreadHandler.runOnUIThread(() -> {
            loggedInPlayerId.set(playerId);
            loggedInNickname.set(nickname);
            setStatusMessage(message);
            setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES); // Move to lobby browser view
            // Automatically request game list after successful login
            requestGameList();
        });
    }

    /** Handles failed login response. */
    public void handleLoginFailure(String reason) {
        uiThreadHandler.runOnUIThread(() -> {
            setStatusMessage("Login failed: " + reason);
            setAppStatus(AppStatus.LOGIN_SCREEN); // Stay on login screen
            resetLoginState();
        });
    }

    /** Handles incoming game list. */
    public void handleGameListResponse(List<GameLobbyInfoDTO> joinable, List<GameLobbyInfoDTO> running) {
        uiThreadHandler.runOnUIThread(() -> {
            joinableGames.setAll(joinable);
            runningGames.setAll(running);
            LOGGER.info("ViewModel updated game lists: " + joinable.size() + " joinable, " + running.size() + " running.");
        });
    }

    /** Handles response after attempting to create a game. */
    public void handleCreateGameResponse(boolean success, String sessionId, String errorMessage, GameLobbyInfoDTO newGameInfo, List<PlayerInfoDTO> playersInLobby) {
        uiThreadHandler.runOnUIThread(() -> {
            if (success) {
                setStatusMessage("Game '" + newGameInfo.getGameName() + "' created.");
                // Update current session state
                currentSessionId.set(sessionId);
                currentLobbyInfo.set(newGameInfo); // Initial lobby info
                
                // Update player list with creator info
                if (playersInLobby != null && !playersInLobby.isEmpty()) {
                    playersInCurrentLobby.setAll(playersInLobby);
                } else {
                    playersInCurrentLobby.clear();
                    LOGGER.warning("Created game session has no players in response. This is unusual.");
                }
                
                setAppStatus(AppStatus.GAME_LOBBY); // Transition to game lobby view
            } else {
                setStatusMessage("Failed to create game: " + errorMessage);
                // Stay on the lobby browsing screen (or return to it)
                if(appStatus.get() != AppStatus.LOGGED_IN_BROWSING_LOBBIES) {
                    setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
                }
            }
        });
    }

    /** Handles response after attempting to join a game. */
    public void handleJoinGameResponse(boolean success, String sessionId, String errorMessage, List<PlayerInfoDTO> playersInLobby) {
        uiThreadHandler.runOnUIThread(() -> {
            if (success) {
                setStatusMessage("Joined game session: " + sessionId); // More detailed name available later
                currentSessionId.set(sessionId);
                // currentLobbyInfo and playersInCurrentLobby will be fully populated by the subsequent GameSessionStateChangedEvent
                if (playersInLobby != null) {
                    playersInCurrentLobby.setAll(playersInLobby);
                    
                    // Find this game's info from joinable games and update the currentLobbyInfo
                    joinableGames.stream()
                        .filter(game -> game.getSessionId().equals(sessionId))
                        .findFirst()
                        .ifPresent(gameInfo -> {
                            // Create an updated version with current player count
                            GameLobbyInfoDTO updatedInfo = new GameLobbyInfoDTO(
                                gameInfo.getSessionId(),
                                gameInfo.getGameName(),
                                playersInLobby.size(), // Use actual player count from list
                                gameInfo.getMaxPlayers(),
                                gameInfo.getGameSessionState(),
                                gameInfo.getGameLevel()
                            );
                            currentLobbyInfo.set(updatedInfo);
                        });
                }
                setAppStatus(AppStatus.GAME_LOBBY); // Transition to game lobby view
            } else {
                setStatusMessage("Failed to join game: " + errorMessage);
                // Stay on the lobby browsing screen (or return to it)
                if(appStatus.get() != AppStatus.LOGGED_IN_BROWSING_LOBBIES) {
                    setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
                }
            }
        });
    }

    /** Handles player joined notification for the current session. */
    public void handlePlayerJoinedNotification(String sessionId, PlayerInfoDTO joinedPlayer, List<PlayerInfoDTO> allPlayers) {
        if (isCurrentSession(sessionId)) {
            uiThreadHandler.runOnUIThread(() -> {
                playersInCurrentLobby.setAll(allPlayers);
                setStatusMessage(joinedPlayer.getNickname() + " joined the lobby.");
                // Update lobby info to reflect new player count, etc.
                // This might require the full GameLobbyInfoDTO to be sent in this notification,
                // or rely on GameSessionStateChangedEvent which often follows player changes.
                // For now, just update the player list.
                updateCurrentLobbyInfoFromPlayers(allPlayers); // Attempt to update count/state based on player list
            });
        }
    }

    /** Handles player left notification for the current session. */
    public void handlePlayerLeftNotification(String sessionId, String leftPlayerId, String leftPlayerNickname, 
                                            List<PlayerInfoDTO> remainingPlayers, boolean wasHost) {
        if (isCurrentSession(sessionId)) {
            uiThreadHandler.runOnUIThread(() -> {
                // If we already left (currentSessionId is null), don't process this notification
                if (currentSessionId.get() == null) {
                    LOGGER.info("Ignoring PlayerLeftGameSessionNotification for player " + leftPlayerNickname + 
                              " as we've already processed leaving the session");
                    return;
                }
                
                playersInCurrentLobby.setAll(remainingPlayers);
                
                // Handle host leaving separately from regular player leaving
                if (wasHost) {
                    if (Objects.equals(leftPlayerId, loggedInPlayerId.get())) {
                        // We were the host and we left
                        setStatusMessage("You left the lobby. Host duties relinquished.");
                    } else {
                        // The host left (not us) - Show prominent message about game being aborted
                        setStatusMessage("⚠️ GAME ABORTED: Host (" + leftPlayerNickname + ") left the lobby! ⚠️");
                        
                        // Show a simple alert to inform players the game has been aborted
                        uiThreadHandler.runOnUIThread(() -> {
                            try {
                                if (isGUIMode) {
                                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                        javafx.scene.control.Alert.AlertType.WARNING);
                                    alert.setTitle("Game Aborted");
                                    alert.setHeaderText("Host Has Left The Game");
                                    alert.setContentText("Host \"" + leftPlayerNickname + "\" has left the lobby.\n" +
                                                       "The game has been aborted.\n\n" +
                                                       "You will be returned to the lobby browser in a few seconds.");
                                    
                                    // Apply some styling to make it stand out
                                    javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
                                    dialogPane.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                                    
                                    // Show non-blocking
                                    alert.initModality(javafx.stage.Modality.NONE);
                                    alert.show();
                                    
                                    // Close after 5 seconds
                                    javafx.animation.PauseTransition closeAlert = new javafx.animation.PauseTransition(
                                        javafx.util.Duration.seconds(5));
                                    closeAlert.setOnFinished(e -> alert.close());
                                    closeAlert.play();
                                } else {
                                    // TUI mode alert
                                    System.err.println("\n=========================================");
                                    System.err.println("⚠️  GAME ABORTED: HOST LEFT THE LOBBY  ⚠️");
                                    System.err.println("Host \"" + leftPlayerNickname + "\" has left the lobby.");
                                    System.err.println("The game has been aborted.");
                                    System.err.println("You will be returned to the lobby browser.");
                                    System.err.println("=========================================\n");
                                }
                            } catch (Exception e) {
                                LOGGER.warning("Could not show alert: " + e.getMessage());
                            }
                        });
                        
                        // Use delay for transition to lobby browser
                        if (isGUIMode) {
                            // JavaFX PauseTransition for GUI mode
                            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(3)
                            );
                            pause.setOnFinished(event -> {
                                // Reset game session and go back to lobby browser
                                resetGameSessionState();
                                setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
                                
                                // Refresh the game list
                                requestGameList();
                            });
                            pause.play();
                        } else {
                            // Simple thread sleep for TUI mode
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                resetGameSessionState();
                                setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
                                requestGameList();
                            }
                        }
                        
                        // Return early to prevent the code below from executing immediately
                        return;
                    }
                } else {
                    // Regular player left (not host)
                    setStatusMessage(leftPlayerNickname + " left the lobby.");
                }
                
                updateCurrentLobbyInfoFromPlayers(remainingPlayers);

                // If the client themselves left the session, navigate back to lobby browser
                if (Objects.equals(leftPlayerId, loggedInPlayerId.get())) {
                    LOGGER.info("Client (" + loggedInNickname.get() + ") left session " + sessionId + ". Navigating back to lobby browser.");
                    resetGameSessionState(); // Clear current session info
                    setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
                }
            });
        }
    }

    /** Handles game session state changes for the current session. */
    public void handleGameSessionStateChanged(String sessionId, String oldStateName, String newStateName, List<PlayerInfoDTO> playersInSession) {
        if (isCurrentSession(sessionId)) {
            uiThreadHandler.runOnUIThread(() -> {
                // Update players list (might include readiness changes)
                playersInCurrentLobby.setAll(playersInSession);

                // Update current lobby info object
                // Need to find the current GameLobbyInfoDTO for this session.
                // GameSessionStateChangedEvent doesn't carry the full DTO, just the state name.
                // We might need to reconstruct or fetch it, or modify the event.
                // For now, let's assume the playersInSession list is sufficient to update the player display.
                // The AppStatus transition is based on the *new* state.
                GameSessionState newState = GameSessionState.valueOf(newStateName);
                GameLobbyInfoDTO currentInfo = currentLobbyInfo.get();
                if (currentInfo != null) {
                    GameLobbyInfoDTO updatedInfo = new GameLobbyInfoDTO(
                        currentInfo.getSessionId(),
                        currentInfo.getGameName(),
                        playersInSession.size(), 
                        currentInfo.getMaxPlayers(),
                        newState,
                        currentInfo.getGameLevel()
                    );
                    currentLobbyInfo.set(updatedInfo);
                }

                setStatusMessage("Session " + sessionId + " state changed to " + newStateName);

                // Transition AppStatus based on new game session state
                switch (newState) {
                    case LOBBY:
                        setAppStatus(AppStatus.GAME_LOBBY);
                        break;
                    case SHIP_BUILDING:
                        setAppStatus(AppStatus.GAME_BUILDING);
                        // Future enhancement: Initialize ship building UI components
                        break;
                    case FLIGHT_PREPARATION:
                        setAppStatus(AppStatus.GAME_FLIGHT);
                        // Future enhancement: Initialize flight preparation phase
                        break;
                    case FLIGHT:
                        setAppStatus(AppStatus.GAME_FLIGHT);
                        // Future enhancement: Initialize active flight phase
                        break;
                    case SCORING:
                        setAppStatus(AppStatus.GAME_FINISHED);
                        // Future enhancement: Display final scoring calculations
                        break;
                    case FINISHED:
                        setAppStatus(AppStatus.GAME_FINISHED);
                        resetGameSessionState();
                        break;
                    case ABORTED:
                        setStatusMessage("⚠️ GAME ABORTED: Session has been terminated ⚠️");
                        resetGameSessionState();
                        
                        // Display notification to user about the aborted game
                        try {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.WARNING);
                            alert.setTitle("Game Aborted");
                            alert.setHeaderText("Game Session Terminated");
                            alert.setContentText("This game session has been aborted.\n\n" +
                                              "You will be returned to the lobby browser.");
                            
                            javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
                            dialogPane.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                            
                            alert.initModality(javafx.stage.Modality.NONE);
                            alert.show();
                            
                            // Auto-dismiss after 5 seconds
                            javafx.animation.PauseTransition closeAlert = new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(5));
                            closeAlert.setOnFinished(e -> alert.close());
                            closeAlert.play();
                        } catch (Exception e) {
                            LOGGER.warning("Could not show aborted game alert: " + e.getMessage());
                        }
                        
                        setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
                        break;
                }
            });
        } else {
            LOGGER.finer("Received GameSessionStateChangedEvent for non-current session " + sessionId);
        }
    }


    /** Handles generic error messages from the server. */
    @MessageHandler
    public void onErrorMessage(ErrorMessage error) {
        uiThreadHandler.runOnUIThread(() -> {
            LOGGER.log(Level.SEVERE, "Received ErrorMessage from server: [" + error.getErrorType() + "] " + error.getReason());
            setStatusMessage("Server Error: " + error.getReason());
            // Decide if this error requires navigating back or just showing a message.
            // For a general error, just updating status message is usually enough.
            // Specific controllers might handle errors related to their context (e.g., illegal action in game).
        });
    }


    // --- Helper methods ---

    /** Checks if the given session ID matches the client's current session. */
    private boolean isCurrentSession(String sessionId) {
        return currentSessionId.get() != null && Objects.equals(currentSessionId.get(), sessionId);
    }

    /** Clears login state. */
    private void resetLoginState() {
        uiThreadHandler.runOnUIThread(() -> {
            loggedInPlayerId.set(null);
            loggedInNickname.set(null);
        });
    }

    /** Clears current game session state. */
    private void resetGameSessionState() {
        uiThreadHandler.runOnUIThread(() -> {
            currentSessionId.set(null);
            currentLobbyInfo.set(null);
            playersInCurrentLobby.clear();
        });
    }

    // --- Actions (Triggered by UI controllers) ---

    /** Requests game list from server. */
    public void requestGameList() {
        if (networkManager == null || !networkManager.isConnected()) {
            setStatusMessage("Cannot request game list: not connected to server.");
            return;
        }
        if (backgroundTaskExecutor == null) {
            LOGGER.severe("Background task executor is null. Cannot request game list in background.");
            setStatusMessage("Internal error: Background task executor not available.");
            return;
        }
        
        setStatusMessage("Requesting game list...");
        
        backgroundTaskExecutor.submit(() -> {
            RequestGameListCommand request = new RequestGameListCommand();
            boolean sent = networkManager.sendMessage(request);
            if (!sent) {
                uiThreadHandler.runOnUIThread(() -> setStatusMessage("Failed to request game list."));
            }
        });
    }
    
    /** Creates a new game with the specified settings. */
    public void createGame(GameSettingsDTO settings) {
        if (networkManager == null || !networkManager.isConnected()) {
            setStatusMessage("Cannot create game: not connected to server.");
            return;
        }
        if (backgroundTaskExecutor == null) {
            LOGGER.severe("Background task executor is null. Cannot create game in background.");
            setStatusMessage("Internal error: Background task executor not available.");
            return;
        }
        
        setStatusMessage("Creating new game...");
        
        backgroundTaskExecutor.submit(() -> {
            CreateGameRequestCommand request = new CreateGameRequestCommand(settings);
            boolean sent = networkManager.sendMessage(request);
            if (!sent) {
                uiThreadHandler.runOnUIThread(() -> setStatusMessage("Failed to send game creation request."));
            }
        });
    }
    
    /** Joins an existing game. */
    public void joinGame(String sessionId) {
        if (networkManager == null || !networkManager.isConnected()) {
            setStatusMessage("Cannot join game: not connected to server.");
            return;
        }
        if (backgroundTaskExecutor == null) {
            LOGGER.severe("Background task executor is null. Cannot join game in background.");
            setStatusMessage("Internal error: Background task executor not available.");
            return;
        }
        
        setStatusMessage("Joining game...");
        
        backgroundTaskExecutor.submit(() -> {
            JoinGameRequestCommand request = new JoinGameRequestCommand(sessionId);
            boolean sent = networkManager.sendMessage(request);
            if (!sent) {
                uiThreadHandler.runOnUIThread(() -> setStatusMessage("Failed to send join game request."));
            }
        });
    }
    
    /** Leaves the current game. */
    public void leaveGame() {
        if (networkManager == null || !networkManager.isConnected()) {
            // If not connected to server, still simulate local leave by changing view
            resetGameSessionState();
            setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
            return;
        }
        
        if (currentSessionId.get() == null) {
            // Not in a game, just go back to lobby browser
            setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
            return;
        }
        
        if (backgroundTaskExecutor == null) {
            LOGGER.severe("Background task executor is null. Cannot leave game in background.");
            setStatusMessage("Internal error: Background task executor not available.");
            resetGameSessionState();
            setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
            return;
        }
        
        // Store session ID before resetting
        final String sessionIdToLeave = currentSessionId.get();
        
        // Immediately reset game state and transition back to lobby
        LOGGER.info("Client leaving game session: " + sessionIdToLeave + ". Updating view immediately.");
        setStatusMessage("Leaving game...");
        resetGameSessionState();
        setAppStatus(AppStatus.LOGGED_IN_BROWSING_LOBBIES);
        
        // In background, send the leave message to the server
        backgroundTaskExecutor.submit(() -> {
            LeaveGameRequestCommand request = new LeaveGameRequestCommand(sessionIdToLeave);
            boolean sent = networkManager.sendMessage(request);
            if (!sent) {
                uiThreadHandler.runOnUIThread(() -> {
                    setStatusMessage("Failed to send leave game request to server. Local state has been updated.");
                });
            } else {
                // Successfully sent leave message
                uiThreadHandler.runOnUIThread(() -> {
                    // Also request updated game list since we've left a game
                    requestGameList();
                });
            }
        });
    }
    
    /** Starts the current game (if host). */
    public void startGame() {
        if (networkManager == null || !networkManager.isConnected()) {
            setStatusMessage("Cannot start game: not connected to server.");
            return;
        }
        
        if (currentSessionId.get() == null) {
            setStatusMessage("Cannot start game: not in a game session.");
            return;
        }
        
        if (backgroundTaskExecutor == null) {
            LOGGER.severe("Background task executor is null. Cannot start game in background.");
            setStatusMessage("Internal error: Background task executor not available.");
            return;
        }
        
        setStatusMessage("Requesting game start...");
        
        backgroundTaskExecutor.submit(() -> {
            StartGameRequestCommand request = new StartGameRequestCommand(currentSessionId.get());
            boolean sent = networkManager.sendMessage(request);
            if (!sent) {
                uiThreadHandler.runOnUIThread(() -> setStatusMessage("Failed to send start game request."));
            }
        });
    }

    // Helper to update player count in lobby info (approximate, relies on player list)
    private void updateCurrentLobbyInfoFromPlayers(List<PlayerInfoDTO> players) {
        GameLobbyInfoDTO currentInfo = currentLobbyInfo.get();
        if (currentInfo != null) {
            GameLobbyInfoDTO updatedInfo = new GameLobbyInfoDTO(
                currentInfo.getSessionId(),
                currentInfo.getGameName(),
                players.size(), 
                currentInfo.getMaxPlayers(),
                currentInfo.getGameSessionState(),
                currentInfo.getGameLevel()
            );
            currentLobbyInfo.set(updatedInfo);
        }
    }

    // --- Message Handlers (Registered with EventBus) ---

    @MessageHandler
    public void onServerLoginResponse(ServerLoginResponse response) {
        if (response.isSuccess()) {
            handleLoginSuccess(response.getPlayerId(), response.getMessage().replace("Welcome, ", "").replace("Welcome back, ", "").replace("!", ""), response.getMessage());
        } else {
            handleLoginFailure(response.getMessage());
        }
    }

    @MessageHandler
    public void onGameListResponse(GameListResponseEvent event) {
        // Always update game lists to keep them current, regardless of the current view
        handleGameListResponse(event.getJoinableGames(), event.getRunningGames());
    }

    @MessageHandler
    public void onCreateGameResponse(CreateGameResponseEvent event) {
        handleCreateGameResponse(event.isSuccess(), event.getSessionId(), event.getErrorMessage(), event.getNewGameInfo(), event.getPlayersInLobby());
    }

    @MessageHandler
    public void onJoinGameResponse(JoinGameResponseEvent event) {
        handleJoinGameResponse(event.isSuccess(), event.getSessionId(), event.getErrorMessage(), event.getPlayersInThisLobby());
    }

    @MessageHandler
    public void onPlayerJoinedGameSessionNotification(PlayerJoinedGameSessionNotification notification) {
        handlePlayerJoinedNotification(notification.getSessionId(), notification.getJoinedPlayer(), notification.getAllPlayersInSession());
    }

    @MessageHandler
    public void onPlayerLeftGameSessionNotification(PlayerLeftGameSessionNotification notification) {
        handlePlayerLeftNotification(
            notification.getSessionId(), 
            notification.getLeftPlayerId(), 
            notification.getLeftPlayerNickname(), 
            notification.getRemainingPlayersInSession(),
            notification.wasHost()
        );
    }

    @MessageHandler
    public void onGameSessionStateChangedEvent(GameSessionStateChangedEvent event) {
        handleGameSessionStateChanged(event.getSessionId(), event.getOldStateName(), event.getNewStateName(), event.getPlayersInSession());
    }

    // Handle network disconnect message from ClientNetworkManager
    @MessageHandler
    public void onServerDisconnectedSystemEvent(ClientNetworkManager.ServerDisconnectedSystemEvent event) {
        handleDisconnected("Server shut down or connection lost.");
    }

    // LobbyStateUpdateEvent is potentially redundant, but keep handler just in case it's still used
    @MessageHandler
    public void onLobbyStateUpdateEvent(LobbyStateUpdateEvent event) {
        if (isCurrentSession(event.getGameSessionId())) {
            uiThreadHandler.runOnUIThread(() -> {
                playersInCurrentLobby.setAll(event.getPlayersInLobby());
                // Optionally update status message
                setStatusMessage("Lobby updated for session " + event.getGameSessionId());
                // Update lobby info player count
                updateCurrentLobbyInfoFromPlayers(event.getPlayersInLobby());
            });
        } else {
            LOGGER.finer("Received LobbyStateUpdateEvent for non-current session " + event.getGameSessionId() + ". Ignoring.");
        }
    }

    // Note: ErrorMessage is handled by the specific handler above.

    /**
     * Toggles the player's ready status in the current game lobby.
     * 
     * @param ready The new ready status
     */
    public void setPlayerReady(boolean ready) {
        String sessionId = currentSessionId.get();
        if (sessionId == null) {
            LOGGER.warning("Cannot set ready status: not in a game session");
            setStatusMessage("Error: Not in a game session");
            return;
        }
        
        if (networkManager == null) {
            LOGGER.warning("Cannot set ready status: network manager is null");
            setStatusMessage("Error: Network connection not available");
            return;
        }
        
        LOGGER.info("Setting player ready status to " + ready + " for session " + sessionId);
        SetPlayerReadyCommand command = new SetPlayerReadyCommand(sessionId, ready);
        networkManager.sendMessage(command);
        
        // Update status message
        setStatusMessage(ready ? "You are now ready" : "You are now not ready");
        
        // Note: We don't update the UI directly here.
        // The server will respond with an updated GameSessionStateChangedEvent
        // which will update the players list including our own ready status.
    }
}