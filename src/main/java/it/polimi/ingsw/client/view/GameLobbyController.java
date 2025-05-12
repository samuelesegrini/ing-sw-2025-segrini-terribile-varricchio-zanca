package it.polimi.ingsw.client.view;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.setup.SetPlayerReadyCommand;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Controller for the Game Lobby view (waiting room).
 * Displays players in the current lobby and allows starting/leaving the game.
 */
public class GameLobbyController implements ViewModelAwareController {
    private static final Logger LOGGER = Logger.getLogger(GameLobbyController.class.getName());

    @FXML private Label gameNameLabel;
    @FXML private Label gameLevelLabel;
    @FXML private Label playersCountLabel;
    @FXML private Label statusLabel;
    
    @FXML private TableView<PlayerInfoDTO> playersTable;
    @FXML private TableColumn<PlayerInfoDTO, String> colPlayerNickname;
    @FXML private TableColumn<PlayerInfoDTO, Boolean> colPlayerStatus;
    @FXML private TableColumn<PlayerInfoDTO, Boolean> colPlayerIsHost;
    
    @FXML private Button startGameButton;
    @FXML private Button readyButton;
    @FXML private Button leaveGameButton;
    
    private ClientViewModel viewModel;
    private GameClientController clientController;
    private Stage primaryStage;
    private boolean isCurrentPlayerReady = false;
    
    @FXML
    public void initialize() {
        LOGGER.info("GameLobbyController initialize called.");
        
        // Set up table columns
        colPlayerNickname.setCellValueFactory(new PropertyValueFactory<>("nickname"));
        colPlayerStatus.setCellValueFactory(new PropertyValueFactory<>("ready"));
        
        // Custom cell factory to display READY/WAITING text instead of true/false
        colPlayerStatus.setCellFactory(col -> new TableCell<PlayerInfoDTO, Boolean>() {
            @Override
            protected void updateItem(Boolean isReady, boolean empty) {
                super.updateItem(isReady, empty);
                if (empty || isReady == null) {
                    setText(null);
                } else {
                    setText(isReady ? "READY" : "WAITING");
                }
            }
        });
        
        // Set up host column cell factory with a checkmark for the host
        colPlayerIsHost.setCellValueFactory(new PropertyValueFactory<>("host"));
        colPlayerIsHost.setCellFactory(col -> new TableCell<PlayerInfoDTO, Boolean>() {
            @Override
            protected void updateItem(Boolean isHost, boolean empty) {
                super.updateItem(isHost, empty);
                setText(null);
                setGraphic(null);
                
                if (!empty && isHost != null && isHost) {
                    // Unicode checkmark symbol for the host
                    setText("✓");
                    setStyle("-fx-font-weight: bold; -fx-alignment: center;");
                }
            }
        });
        
        // Buttons will be configured in setViewModel once we know the player's role
    }
    
    @Override
    public void setViewModel(ClientViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
        LOGGER.info("GameLobbyController received ViewModel. Binding data.");
        
        // Bind status message
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        
        // Bind table to players list
        playersTable.setItems(viewModel.getPlayersInCurrentLobby());
        
        // Bind lobby info (if available)
        GameLobbyInfoDTO lobbyInfo = viewModel.currentLobbyInfoProperty().get();
        if (lobbyInfo != null) {
            gameNameLabel.setText(lobbyInfo.getGameName());
            gameLevelLabel.setText("Level: " + lobbyInfo.getGameLevel());
            
            // Create a binding for player count that updates when the observable list changes
            playersCountLabel.textProperty().bind(
                Bindings.concat("Players: ", 
                    Bindings.size(viewModel.getPlayersInCurrentLobby()),
                    "/", 
                    lobbyInfo.getMaxPlayers())
            );
        }
        
        // Get current player ID
        String currentPlayerId = viewModel.loggedInPlayerIdProperty().get();
        if (currentPlayerId != null) {
            // Check for both host status and player readiness when the player list changes
            viewModel.getPlayersInCurrentLobby().addListener((javafx.collections.ListChangeListener<PlayerInfoDTO>) c -> {
                updateButtonsBasedOnPlayerStatus(currentPlayerId);
            });
            
            // Initial check
            updateButtonsBasedOnPlayerStatus(currentPlayerId);
        }
    }
    
    /**
     * Updates button visibility and status based on the current player's role and readiness
     */
    private void updateButtonsBasedOnPlayerStatus(String currentPlayerId) {
        boolean isHost = false;
        boolean allPlayersReady = true;
        boolean isReady = false;
        
        // Find the current player in the list
        for (PlayerInfoDTO player : viewModel.getPlayersInCurrentLobby()) {
            // Check if this is the current player
            if (player.getPlayerId().equals(currentPlayerId)) {
                isHost = player.isHost();
                isReady = player.isReady();
                this.isCurrentPlayerReady = isReady;
            }
            
            // Check if all players are ready for game start
            if (!player.isReady()) {
                allPlayersReady = false;
            }
        }
        
        final boolean fIsHost = isHost;
        final boolean fAllPlayersReady = allPlayersReady;
        final boolean fIsReady = isReady;
        
        // Update UI on JavaFX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> updateButtonUI(fIsHost, fAllPlayersReady, fIsReady));
        } else {
            updateButtonUI(isHost, allPlayersReady, isReady);
        }
    }
    
    /**
     * Updates the UI components based on player status
     */
    private void updateButtonUI(boolean isHost, boolean allPlayersReady, boolean isReady) {
        // Configure button visibility based on host status
        startGameButton.setVisible(isHost);
        readyButton.setVisible(!isHost);
        
        // Configure start button based on readiness
        startGameButton.setDisable(!allPlayersReady);
        
        // Update ready button text based on current status
        readyButton.setText(isReady ? "I'm Not Ready" : "I'm Ready");
    }
    
    @FXML
    private void handleStartGameButton() {
        LOGGER.info("Start Game button clicked.");
        viewModel.startGame();
    }
    
    @FXML
    private void handleReadyButton() {
        // Toggle ready status
        boolean newReadyStatus = !isCurrentPlayerReady;
        LOGGER.info("Ready button clicked. Setting ready status to: " + newReadyStatus);
        viewModel.setPlayerReady(newReadyStatus);
    }
    
    @FXML
    private void handleLeaveGameButton() {
        LOGGER.info("Leave Lobby button clicked.");
        viewModel.leaveGame();
    }
    
    @Override
    public void setClientApp(GameClientController clientController) {
        this.clientController = Objects.requireNonNull(clientController);
        LOGGER.info("GameLobbyController received GameClientController reference.");
    }
    
    @Override
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = Objects.requireNonNull(primaryStage);
        LOGGER.info("GameLobbyController received primary stage reference.");
    }
} 