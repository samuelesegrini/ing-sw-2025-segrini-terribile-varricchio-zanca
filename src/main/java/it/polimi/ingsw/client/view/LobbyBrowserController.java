package it.polimi.ingsw.client.view;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections; // If needed for placeholder/testing
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.util.Optional;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Controller for the Lobby Browser view, which displays available and running game lobbies.
 * Allows users to refresh the lobby list, create new games, and join existing ones.
 * Interacts with the ClientViewModel to display data and trigger network actions.
 */
public class LobbyBrowserController implements ViewModelAwareController {
    private static final Logger LOGGER = Logger.getLogger(LobbyBrowserController.class.getName());

    @FXML private TableView<GameLobbyInfoDTO> joinableGamesTable;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colJoinableName;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colJoinablePlayers;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colJoinableLevel;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colJoinableState;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colJoinableSessionId;

    @FXML private TableView<GameLobbyInfoDTO> runningGamesTable;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colRunningName;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colRunningPlayers;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colRunningLevel;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colRunningState;
    @FXML private TableColumn<GameLobbyInfoDTO, String> colRunningSessionId;

    @FXML private Button refreshButton;
    @FXML private Button createButton;
    @FXML private Button joinButton;
    @FXML private Label statusLabel;

    private ClientViewModel viewModel;
    private Stage primaryStage;
    private GameClientController clientController;

    /**
     * Initializes the controller by setting up table columns and binding UI controls.
     * Called automatically by JavaFX after the FXML is loaded.
     */
    @FXML
    public void initialize() {
        LOGGER.info("LobbyBrowserController initialize called.");
        
        // Setup joinable games table
        colJoinableName.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGameName()));
            
        colJoinablePlayers.setCellValueFactory(cellData -> {
            GameLobbyInfoDTO game = cellData.getValue();
            return new SimpleStringProperty(game.getCurrentPlayerCount() + "/" + game.getMaxPlayers());
        });
        
        colJoinableLevel.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGameLevel().toString()));
            
        colJoinableState.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGameSessionState().toString()));
            
        colJoinableSessionId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSessionId()));

        // Setup running games table
        colRunningName.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGameName()));
            
        colRunningPlayers.setCellValueFactory(cellData -> {
            GameLobbyInfoDTO game = cellData.getValue();
            return new SimpleStringProperty(game.getCurrentPlayerCount() + "/" + game.getMaxPlayers());
        });
        
        colRunningLevel.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGameLevel().toString()));
            
        colRunningState.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getGameSessionState().toString()));
            
        colRunningSessionId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getSessionId()));

        // Disable Join button when no game is selected
        joinButton.disableProperty().bind(
                joinableGamesTable.getSelectionModel().selectedItemProperty().isNull()
        );
    }

    /**
     * Sets the ViewModel and binds to its properties.
     * Establishes bidirectional data flow between UI and model.
     * 
     * @param viewModel The ClientViewModel instance
     */
    @Override
    public void setViewModel(ClientViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel);
        LOGGER.info("LobbyBrowserController received ViewModel. Binding data.");

        joinableGamesTable.setItems(viewModel.getJoinableGames());
        runningGamesTable.setItems(viewModel.getRunningGames());
        statusLabel.textProperty().bind(viewModel.statusMessageProperty());
        viewModel.requestGameList();
    }

    /**
     * Sets the primary stage reference, needed for creating dialogs.
     * 
     * @param primaryStage The JavaFX primary stage
     */
    @Override
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = Objects.requireNonNull(primaryStage);
        LOGGER.info("LobbyBrowserController received primary stage reference.");
    }

    /**
     * Sets the client controller reference.
     * 
     * @param clientController The GameClientController instance
     */
    @Override
    public void setClientApp(GameClientController clientController) {
        this.clientController = Objects.requireNonNull(clientController);
        LOGGER.info("LobbyBrowserController received GameClientController reference.");
    }

    /**
     * Handles Refresh button clicks by requesting an updated game list.
     */
    @FXML
    private void handleRefreshButton() {
        LOGGER.info("Refresh button clicked.");
        viewModel.requestGameList();
    }

    /**
     * Handles Create Game button clicks by opening the game settings dialog.
     * If settings are provided, requests game creation through the view model.
     */
    @FXML
    private void handleCreateButton() {
        LOGGER.info("Create Game button clicked.");
        
        if (primaryStage == null) {
            LOGGER.severe("Primary stage not set in LobbyBrowserController. Cannot open dialog.");
            viewModel.setStatusMessage("Error: Cannot create game (internal error).");
            return;
        }
        
        GameSettingsDialog dialog = new GameSettingsDialog();
        Optional<GameSettingsDTO> result = dialog.showAndWait(primaryStage);
        
        result.ifPresent(settings -> {
            LOGGER.info("Creating game with settings: " + settings);
            viewModel.createGame(settings);
        });
    }

    /**
     * Handles Join Game button clicks by requesting to join the selected game.
     */
    @FXML
    private void handleJoinButton() {
        LOGGER.info("Join button clicked.");
        GameLobbyInfoDTO selectedGame = joinableGamesTable.getSelectionModel().getSelectedItem();
        if (selectedGame != null) {
            LOGGER.info("Attempting to join game session: " + selectedGame.getSessionId());
            viewModel.joinGame(selectedGame.getSessionId());
        } else {
            LOGGER.warning("Join button was clicked but no game was selected.");
            viewModel.setStatusMessage("Please select a game to join.");
        }
    }
}