package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.common.message.request.LeaveGameRequest;
import it.polimi.ingsw.common.message.request.SetPlayerReadyRequest;
import it.polimi.ingsw.common.message.request.StartGameRequest;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.beans.PropertyChangeEvent;
import java.util.*;

/**
 * Simplified GUI view for game lobby where players wait to start the game.
 * Based on UITest WaitingRoomScreen pattern for consistency.
 */
public class GuiGameLobbyView extends BaseUIView {
    private Stage stage;
    private Scene waitingRoomScene;
    
    // UI Components
    private VBox contentContainer;
    private VBox playersVBox;
    private Label gameTitleLabel;
    private Label gameInfoLabel;
    private Label playerCountLabel;
    private Button startGameButton;
    private Button readyButton;
    private Button leaveRoomButton;
    private HBox actionsBar;
    private ScrollPane playersScrollPane;

    public GuiGameLobbyView(Stage stage) {
        this.stage = stage;
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.GAME_LOBBY;
    }

    @Override
    public String getTitle() {
        return "Game Lobby";
    }

    @Override
    public void initialize(it.polimi.ingsw.client.ui.core.UIContext context) {
        super.initialize(context);
        createUI();
        setupActions();
    }
    
    private void createUI() {
        contentContainer = new VBox();
        contentContainer.getStyleClass().addAll("panel-medium-glass", "waiting-room-main-content");
        contentContainer.setPadding(new Insets(30));
        contentContainer.setSpacing(20);
        contentContainer.setAlignment(Pos.TOP_CENTER);
        
        // Header: Waiting Room Title
        Label title = new Label("WAITING ROOM");
        title.getStyleClass().addAll("label-title", "waiting-room-title");
        
        // Game Info Section
        VBox gameInfoSection = createGameInfoSection();
        
        // Players List Section
        VBox playersSection = createPlayersSection();
        
        // Actions Bar
        actionsBar = createActionsBar();
        
        contentContainer.getChildren().addAll(title, gameInfoSection, playersSection, actionsBar);
        
        // Wrap in root container
        VBox rootContainer = new VBox();
        rootContainer.getStyleClass().add("waiting-room-root");
        rootContainer.getChildren().add(contentContainer);
        
        waitingRoomScene = new Scene(rootContainer, 800, 600);
        
        // Load CSS
        try {
            waitingRoomScene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: " + e.getMessage());
        }
        try {
            waitingRoomScene.getStylesheets().add(
                getClass().getResource("/css/waiting-room.css").toExternalForm());
        } catch (Exception e) {
            LOGGER.warning("Could not load CSS files: " + e.getMessage());
        }
    }
    
    @Override
    protected void onShow() {
        // Clear any loading states that might have been inherited
        if (context != null && context.getNotificationService() != null) {
            context.getNotificationService().hideLoading();
        }
        
        stage.setScene(waitingRoomScene);
        stage.setTitle("Galaxy Trucker - " + getTitle());
        
        updateLobbyDisplay();
        
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    @Override
    protected void onHide() {
        // GUI views don't need special hiding logic
    }

    @Override
    protected void onRefresh() {
        Platform.runLater(this::updateLobbyDisplay);
    }
    
    /**
     * Update game details if they change externally.
     */
    public void updateGameDetails() {
        Platform.runLater(this::updateLobbyDisplay);
    }
    
    /**
     * Set host controls visibility.
     */
    public void setHostControls(boolean isHost) {
        Platform.runLater(() -> {
            if (isHost && !actionsBar.getChildren().contains(startGameButton)) {
                actionsBar.getChildren().add(1, startGameButton);
            } else if (!isHost && actionsBar.getChildren().contains(startGameButton)) {
                actionsBar.getChildren().remove(startGameButton);
            }
            updateButtonStates();
        });
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "playersInLobby":
            case "currentGameInfo":
            case "playerReady":
                updateLobbyDisplay();
                break;
            case "currentView":
                // Handle view transitions
                if (evt.getNewValue() == ClientState.ViewState.GAME) {
                    // Game started, transition to game view will be handled by manager
                    context.getNotificationService().showSuccess("Game Started", "The game has begun!");
                }
                break;
        }
    }

    private VBox createGameInfoSection() {
        VBox gameInfoSection = new VBox();
        gameInfoSection.getStyleClass().add("waiting-room-info-section");
        gameInfoSection.setSpacing(10);
        
        gameTitleLabel = new Label();
        gameTitleLabel.getStyleClass().add("label-header");
        
        gameInfoLabel = new Label();
        gameInfoLabel.getStyleClass().add("label-subtitle");
        
        playerCountLabel = new Label();
        playerCountLabel.getStyleClass().add("label-header");
        
        gameInfoSection.getChildren().addAll(gameTitleLabel, gameInfoLabel, playerCountLabel);
        return gameInfoSection;
    }
    
    private VBox createPlayersSection() {
        VBox playersSection = new VBox(15);
        playersSection.setAlignment(Pos.TOP_CENTER);
        
        Label playersListTitle = new Label("PLAYERS IN ROOM:");
        playersListTitle.getStyleClass().addAll("label-subtitle", "waiting-room-players-title");
        
        playersVBox = new VBox();
        playersVBox.getStyleClass().add("waiting-room-players-vbox");
        playersVBox.setSpacing(8);
        
        playersScrollPane = new ScrollPane(playersVBox);
        playersScrollPane.getStyleClass().add("scroll-pane-styled");
        playersScrollPane.setFitToWidth(true);
        playersScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        playersScrollPane.setPrefHeight(200);
        VBox.setVgrow(playersScrollPane, Priority.ALWAYS);
        
        playersSection.getChildren().addAll(playersListTitle, playersScrollPane);
        return playersSection;
    }
    
    private HBox createActionsBar() {
        HBox actionsBar = new HBox();
        actionsBar.getStyleClass().add("waiting-room-actions-bar");
        actionsBar.setAlignment(Pos.CENTER);
        actionsBar.setSpacing(20);
        
        startGameButton = new Button("START GAME");
        startGameButton.getStyleClass().addAll("button", "button-success", "waiting-room-start-button");
        startGameButton.setVisible(false); // Initially hidden, shown if host
        applyButtonAnimations(startGameButton);
        
        readyButton = new Button("I'M READY");
        readyButton.getStyleClass().addAll("button", "button-primary");
        applyButtonAnimations(readyButton);
        
        leaveRoomButton = new Button("LEAVE ROOM");
        leaveRoomButton.getStyleClass().addAll("button", "button-danger", "waiting-room-leave-button");
        applyButtonAnimations(leaveRoomButton);
        
        actionsBar.getChildren().addAll(leaveRoomButton, readyButton);
        
        return actionsBar;
    }

    private void updateLobbyDisplay() {
        if (context == null || context.getModel() == null) {
            return;
        }
        
        // Check if UI components are loaded
        if (gameTitleLabel == null) {
            LOGGER.warning("UI components not loaded yet, skipping display update");
            return;
        }

        ClientModel model = context.getModel();
        
        // Update game info
        if (model.getCurrentGameInfo() != null) {
            gameTitleLabel.setText("Game: " + model.getCurrentGameInfo().getGameName() + 
                " (ID: " + model.getCurrentGameInfo().getGameId() + ")");
            gameInfoLabel.setText("Level: " + model.getCurrentGameInfo().getGameLevel().toString().replace("_", " "));
            playerCountLabel.setText("Players: " + model.getCurrentGameInfo().getCurrentPlayers() + 
                                    "/" + model.getCurrentGameInfo().getMaxPlayers());
        }

        // Update players table
        updatePlayersTable();

        // Update button states
        updateButtonStates();

        // Update status
        updateStatusLabel();
    }

    private void updatePlayersTable() {
        if (playersVBox == null) {
            return; // UI not loaded yet
        }
        
        List<Player> playersRaw = context.getModel().getPlayersInLobby();
        if (playersRaw != null) {
            // Deduplicate players by ID (keep the last occurrence)
            Map<String, Player> uniquePlayers = new LinkedHashMap<>();
            for (Player player : playersRaw) {
                uniquePlayers.put(player.getPlayerId(), player);
            }
            List<Player> players = new ArrayList<>(uniquePlayers.values());
            
            playersVBox.getChildren().clear();
            
            if (players.isEmpty()) {
                Label noPlayersLabel = new Label("Waiting for players to join...");
                noPlayersLabel.getStyleClass().add("waiting-room-no-players-label");
                playersVBox.getChildren().add(noPlayersLabel);
            } else {
                String currentPlayerId = context.getController().getPlayerId();
                String hostId = getHostPlayerId();
                
                for (Player player : players) {
                    VBox playerEntry = createPlayerEntry(player, 
                        player.getPlayerId().equals(hostId),
                        player.getPlayerId().equals(currentPlayerId));
                    playersVBox.getChildren().add(playerEntry);
                }
            }
        }
    }
    
    private VBox createPlayerEntry(Player player, boolean isHost, boolean isCurrentUser) {
        VBox playerEntry = new VBox(5);
        playerEntry.getStyleClass().add("waiting-room-player-entry");
        playerEntry.setPadding(new Insets(10));
        
        HBox playerInfo = new HBox(10);
        playerInfo.setAlignment(Pos.CENTER_LEFT);
        
        // Player name with host indicator
        String displayName = player.getNickname();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Player " + player.getPlayerId().substring(0, Math.min(8, player.getPlayerId().length()));
        }
        if (isHost) {
            displayName += " (Host)";
        }
        if (isCurrentUser) {
            displayName += " (You)";
        }
        
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("label-header");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Ready status
        Label statusLabel = new Label(player.isReady() ? "Ready" : "Not Ready");
        statusLabel.getStyleClass().add("label-subtitle");
        if (player.isReady()) {
            statusLabel.setStyle("-fx-text-fill: #4CAF50;"); // Green for ready
        } else {
            statusLabel.setStyle("-fx-text-fill: #FF9800;"); // Orange for not ready
        }
        
        playerInfo.getChildren().addAll(nameLabel, spacer, statusLabel);
        playerEntry.getChildren().add(playerInfo);
        
        return playerEntry;
    }

    private void updateButtonStates() {
        if (startGameButton == null || readyButton == null) {
            return; // UI not loaded yet
        }
        
        String currentPlayerId = context.getController().getPlayerId();
        boolean isHost = currentPlayerId != null && currentPlayerId.equals(getHostPlayerId());
        boolean isReady = context.getModel().isPlayerReady(currentPlayerId);
        boolean allReady = areAllPlayersReady();
        
        // Show/hide host controls
        if (isHost && !actionsBar.getChildren().contains(startGameButton)) {
            actionsBar.getChildren().add(1, startGameButton); // Add after leave button
        } else if (!isHost && actionsBar.getChildren().contains(startGameButton)) {
            actionsBar.getChildren().remove(startGameButton);
        }
        startGameButton.setVisible(isHost);
        startGameButton.setDisable(!allReady);
        
        // Update ready button
        readyButton.setText(isReady ? "NOT READY" : "I'M READY");
        readyButton.getStyleClass().removeAll("button-primary", "button-secondary");
        readyButton.getStyleClass().add(isReady ? "button-secondary" : "button-primary");
    }

    private void updateStatusLabel() {
        // Status is now shown in the game info section instead of a separate status label
        if (gameInfoLabel == null) {
            return;
        }
        
        String status;
        if (areAllPlayersReady()) {
            boolean isHost = context.getController().getPlayerId().equals(getHostPlayerId());
            status = isHost ? "All players ready! You can start the game." : "All players ready! Waiting for host to start...";
        } else {
            status = "Waiting for all players to be ready...";
        }
        
        // Update the game info label to include status
        if (context.getModel().getCurrentGameInfo() != null) {
            String baseInfo = "Level: " + context.getModel().getCurrentGameInfo().getGameLevel().toString().replace("_", " ");
            gameInfoLabel.setText(baseInfo + "  |  " + status);
        }
    }

    private String getHostPlayerId() {
        // Get host from GameInfo which tracks the actual creator/host
        if (context.getModel().getCurrentGameInfo() != null) {
            return context.getModel().getCurrentGameInfo().getCreatorId();
        }
        // No fallback - if GameInfo is not available, no one is host
        return null;
    }

    private boolean areAllPlayersReady() {
        List<Player> players = context.getModel().getPlayersInLobby();
        return players != null && !players.isEmpty() && 
               players.stream().allMatch(Player::isReady);
    }

    private void setupActions() {
        // Ready button
        readyButton.setOnAction(e -> handleReadyButton());
        
        // Start game button
        startGameButton.setOnAction(e -> handleStartGameButton());
        
        // Leave room button
        leaveRoomButton.setOnAction(e -> handleLeaveGameButton());
    }
    
    private void applyButtonAnimations(Button button) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        button.setOnMousePressed(event -> scaleUp.playFromStart());
        button.setOnMouseReleased(event -> scaleDown.playFromStart());
    }
    
    public void handleReadyButton() {
        String playerId = context.getController().getPlayerId();
        boolean currentReady = context.getModel().isPlayerReady(playerId);
        
        context.getController().setPlayerReady(!currentReady);
    }

    public void handleStartGameButton() {
        LOGGER.info("Start game button clicked");
        context.getController().startGame();
    }

    public void handleLeaveGameButton() {
        LOGGER.info("Leave game button clicked");
        context.getController().leaveGame();
    }

}