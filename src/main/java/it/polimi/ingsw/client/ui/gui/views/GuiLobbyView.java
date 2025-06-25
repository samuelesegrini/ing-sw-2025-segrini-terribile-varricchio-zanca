package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.common.message.request.CreateGameRequest;
import it.polimi.ingsw.common.message.request.JoinGameRequest;
import it.polimi.ingsw.common.message.request.ListGamesRequest;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.logging.Logger;

/**
 * Simplified GUI view for the main lobby where players can browse and join games.
 * Based on UITest RefactoredLobbyScreen pattern for consistency.
 */
public class GuiLobbyView extends BaseUIView {
    private static final Logger LOGGER = Logger.getLogger(GuiLobbyView.class.getName());

    private final Stage stage;
    private Scene lobbyScene;
    
    // UI Components
    private FlowPane waitingGamesFlowPane;
    private FlowPane inProgressGamesFlowPane;
    private Button createGameButton;
    private Button refreshButton;
    private Button logoutButton;
    private ProgressIndicator loadingIndicator;
    private ScrollPane mainScrollPane;
    private Label statusLabel;
    private Label playerLabel;
    private VBox contentContainer;

    public GuiLobbyView(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(UIContext context) {
        super.initialize(context);
        createUI();
        setupActions();
    }

    private void createUI() {
        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().addAll("panel-medium-glass", "lobby-main-content-container");
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.TOP_CENTER);
        
        // Header
        HBox header = createHeader();
        
        // Games sections
        VBox gamesSection = createGamesSection();
        
        // Actions bar
        HBox actionsBar = createActionsBar();
        
        // Status label
        statusLabel = new Label("Welcome to Galaxy Trucker!");
        statusLabel.getStyleClass().add("label-header");
        statusLabel.setAlignment(Pos.CENTER);
        
        mainContainer.getChildren().addAll(header, gamesSection, actionsBar, statusLabel);
        
        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.getStyleClass().add("progress-indicator-styled");
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(80, 80);
        
        // Main scroll pane
        mainScrollPane = new ScrollPane(mainContainer);
        mainScrollPane.getStyleClass().add("scroll-pane-styled");
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Content container (StackPane for overlay)
        contentContainer = new VBox();
        contentContainer.getStyleClass().add("lobby-screen-root");
        contentContainer.getChildren().addAll(mainScrollPane, loadingIndicator);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);
        
        lobbyScene = new Scene(contentContainer, 1000, 700);
        
        // Load CSS
        try {
            lobbyScene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load stylesheet: " + e.getMessage());
        }
        try {
            lobbyScene.getStylesheets().add(
                getClass().getResource("/css/lobby.css").toExternalForm());
        } catch (Exception e) {
            LOGGER.warning("Could not load CSS files: " + e.getMessage());
        }
    }

    private HBox createHeader() {
        HBox header = new HBox(20);
        header.getStyleClass().add("lobby-header-box");
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("GAME LOBBY");
        titleLabel.getStyleClass().add("label-title");
        titleLabel.setId("lobby-title-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        String nickname = (context != null && context.getModel() != null) ? context.getModel().getNickname() : null;
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "Guest Player";
        }
        playerLabel = new Label("Player: " + nickname);
        playerLabel.getStyleClass().addAll("label-header", "lobby-player-info-label");
        
        logoutButton = new Button("LOGOUT");
        logoutButton.getStyleClass().addAll("button", "button-danger", "lobby-logout-button");
        setupButtonAnimations(logoutButton);
        
        header.getChildren().addAll(titleLabel, spacer, playerLabel, logoutButton);
        return header;
    }
    
    private VBox createGamesSection() {
        VBox gamesSection = new VBox(30);
        gamesSection.getStyleClass().add("lobby-game-lists-area");
        gamesSection.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(gamesSection, Priority.ALWAYS);
        
        // Waiting games section
        VBox waitingSection = createGameListSection("AVAILABLE GAMES");
        waitingGamesFlowPane = new FlowPane();
        waitingGamesFlowPane.getStyleClass().add("lobby-games-flowpane");
        waitingGamesFlowPane.setHgap(15);
        waitingGamesFlowPane.setVgap(15);
        VBox.setVgrow(waitingGamesFlowPane, Priority.ALWAYS);
        waitingSection.getChildren().add(waitingGamesFlowPane);
        
        // In progress games section
        VBox inProgressSection = createGameListSection("GAMES IN PROGRESS");
        inProgressGamesFlowPane = new FlowPane();
        inProgressGamesFlowPane.getStyleClass().add("lobby-games-flowpane");
        inProgressGamesFlowPane.setHgap(15);
        inProgressGamesFlowPane.setVgap(15);
        VBox.setVgrow(inProgressGamesFlowPane, Priority.ALWAYS);
        inProgressSection.getChildren().add(inProgressGamesFlowPane);
        
        gamesSection.getChildren().addAll(waitingSection, inProgressSection);
        return gamesSection;
    }
    
    private VBox createGameListSection(String title) {
        VBox sectionContainer = new VBox(15);
        sectionContainer.getStyleClass().add("lobby-game-list-section-container");
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("label-subtitle", "lobby-section-title-label");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        
        sectionContainer.getChildren().add(titleLabel);
        return sectionContainer;
    }
    
    private HBox createActionsBar() {
        HBox actionsBar = new HBox(20);
        actionsBar.getStyleClass().add("lobby-actions-bar");
        actionsBar.setAlignment(Pos.CENTER);
        
        createGameButton = new Button("CREATE NEW GAME");
        createGameButton.getStyleClass().addAll("button", "button-primary", "lobby-create-game-button");
        setupButtonAnimations(createGameButton);
        
        refreshButton = new Button("REFRESH LISTS");
        refreshButton.getStyleClass().addAll("button", "button-standard", "lobby-refresh-button");
        setupButtonAnimations(refreshButton);
        
        actionsBar.getChildren().addAll(createGameButton, refreshButton);
        return actionsBar;
    }

    private void setupActions() {
        // Create game button
        createGameButton.setOnAction(e -> showCreateGameDialog());
        
        // Refresh button
        refreshButton.setOnAction(e -> fetchGameListFromServer());
        
        // Logout button
        logoutButton.setOnAction(e -> handleLogout());
    }
    
    private void setupButtonAnimations(Button button) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(100), button);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(100), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        button.setOnMouseEntered(event -> scaleUp.playFromStart());
        button.setOnMouseExited(event -> scaleDown.playFromStart());
    }
    
    private void showCreateGameDialog() {
        Dialog<CreateGameRequest> dialog = new Dialog<>();
        dialog.setTitle("Create New Game");
        dialog.setHeaderText(null); // Remove default header for custom styling
        
        // Set dialog buttons
        ButtonType createButtonType = new ButtonType("Create Game", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Apply Galaxy Trucker styling to dialog
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/css/common.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("panel-medium-glass");
        
        // Set dialog size and styling
        dialog.getDialogPane().setPrefSize(500, 450);
        dialog.getDialogPane().setStyle("-fx-background-color: rgba(16, 16, 32, 0.95);");
        
        // Create main container with proper styling
        VBox mainContainer = new VBox(20);
        mainContainer.getStyleClass().add("panel-dark-glass");
        mainContainer.setPadding(new Insets(30));
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(420);
        
        // Title
        Label titleLabel = new Label("Create New Game");
        titleLabel.getStyleClass().addAll("label-title");
        titleLabel.setAlignment(Pos.CENTER);
        
        Label subtitleLabel = new Label("Configure your Galaxy Trucker game");
        subtitleLabel.getStyleClass().addAll("label-subtitle");
        subtitleLabel.setAlignment(Pos.CENTER);
        
        // Create form controls
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);
        
        // Game Name field
        TextField gameNameField = new TextField("New Game");
        gameNameField.setPromptText("Enter game name (optional)");
        gameNameField.getStyleClass().add("text-field-styled");
        gameNameField.setMaxWidth(280);
        
        // Max Players selection
        ComboBox<Integer> maxPlayersCombo = new ComboBox<>();
        maxPlayersCombo.getItems().addAll(2, 3, 4);
        maxPlayersCombo.setValue(4); // Default to 4 players
        maxPlayersCombo.getStyleClass().add("choice-box-styled");
        maxPlayersCombo.setMaxWidth(280);
        
        // Game Level selection
        ComboBox<GameLevel> gameLevelCombo = new ComboBox<>();
        gameLevelCombo.getItems().addAll(GameLevel.TEST_FLIGHT, GameLevel.LEVEL_II);
        gameLevelCombo.setValue(GameLevel.TEST_FLIGHT); // Default to TEST_FLIGHT
        gameLevelCombo.getStyleClass().add("choice-box-styled");
        gameLevelCombo.setMaxWidth(280);
        
        // Custom cell factory for game level display
        gameLevelCombo.setCellFactory(listView -> new ListCell<GameLevel>() {
            @Override
            protected void updateItem(GameLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case TEST_FLIGHT:
                            setText("Test Flight (Beginner)");
                            break;
                        case LEVEL_II:
                            setText("Level II (Standard)");
                            break;
                        default:
                            setText(item.toString());
                    }
                }
            }
        });
        
        // Set button cell for display when closed
        gameLevelCombo.setButtonCell(new ListCell<GameLevel>() {
            @Override
            protected void updateItem(GameLevel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case TEST_FLIGHT:
                            setText("Test Flight (Beginner)");
                            break;
                        case LEVEL_II:
                            setText("Level II (Standard)");
                            break;
                        default:
                            setText(item.toString());
                    }
                }
            }
        });
        
        // Add descriptions with proper styling
        Label gameNameLabel = new Label("Game Name:");
        gameNameLabel.getStyleClass().add("label");
        
        Label maxPlayersLabel = new Label("Max Players:");
        maxPlayersLabel.getStyleClass().add("label");
        
        Label gameLevelLabel = new Label("Game Level:");
        gameLevelLabel.getStyleClass().add("label");
        
        Label levelDescLabel = new Label();
        levelDescLabel.setWrapText(true);
        levelDescLabel.setMaxWidth(280);
        levelDescLabel.getStyleClass().add("label");
        levelDescLabel.setStyle("-fx-font-size: 12pt; -fx-text-fill: #B0C4DE; -fx-opacity: 0.8;");
        
        // Update description and theme when level changes
        gameLevelCombo.setOnAction(e -> {
            GameLevel selectedLevel = gameLevelCombo.getValue();
            if (selectedLevel != null) {
                switch (selectedLevel) {
                    case TEST_FLIGHT:
                        levelDescLabel.setText("8 simple adventure cards, perfect for learning the game mechanics.");
                        // Apply Test Flight theme accent
                        titleLabel.setStyle("-fx-text-fill: #34C1FF;");
                        break;
                    case LEVEL_II:
                        levelDescLabel.setText("Mix of Level I and II cards with 3 predictable piles for strategic planning.");
                        // Apply Level II theme accent
                        titleLabel.setStyle("-fx-text-fill: #D455F5;");
                        break;
                }
            }
        });
        
        // Set initial description and theme
        levelDescLabel.setText("8 simple adventure cards, perfect for learning the game mechanics.");
        titleLabel.setStyle("-fx-text-fill: #34C1FF;"); // Test Flight theme as default
        
        // Layout components
        grid.add(gameNameLabel, 0, 0);
        grid.add(gameNameField, 1, 0);
        grid.add(maxPlayersLabel, 0, 1);
        grid.add(maxPlayersCombo, 1, 1);
        grid.add(gameLevelLabel, 0, 2);
        grid.add(gameLevelCombo, 1, 2);
        grid.add(levelDescLabel, 1, 3);
        
        // Assemble main container
        mainContainer.getChildren().addAll(titleLabel, subtitleLabel, grid);
        
        dialog.getDialogPane().setContent(mainContainer);
        
        // Style dialog buttons to match Galaxy Trucker theme
        Platform.runLater(() -> {
            Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
            Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            
            if (createButton instanceof Button) {
                ((Button) createButton).getStyleClass().addAll("button", "button-primary");
                setupButtonAnimations((Button) createButton);
            }
            
            if (cancelButton instanceof Button) {
                ((Button) cancelButton).getStyleClass().addAll("button");
                setupButtonAnimations((Button) cancelButton);
            }
        });
        
        // Convert result when Create button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String gameName = gameNameField.getText().trim();
                if (gameName.isEmpty()) {
                    gameName = "New Game";
                }
                return new CreateGameRequest(
                    maxPlayersCombo.getValue(),
                    gameLevelCombo.getValue(),
                    gameName
                );
            }
            return null;
        });
        
        // Show dialog and handle result
        dialog.showAndWait().ifPresent(request -> {
            LOGGER.info("Creating game: " + request.getGameName() + 
                       " (Level: " + request.getGameLevel() + 
                       ", Max Players: " + request.getMaxPlayers() + ")");
            statusLabel.setText("Creating game...");
            context.getController().createGame(request.getGameName(), request.getMaxPlayers(), request.getGameLevel().toString())
                    .thenAccept(success -> {
                        Platform.runLater(() -> {
                            if (success) {
                                statusLabel.setText("Game created successfully");
                            } else {
                                statusLabel.setText("Failed to create game");
                            }
                        });
                    });
        });
    }
    
    private void handleLogout() {
        // Clear authentication and return to login view
        context.getModel().setAuthenticated(false);
        context.getModel().setCurrentView(ClientState.ViewState.LOGIN);
    }

    private void fetchGameListFromServer() {
        if (context == null) {
            LOGGER.warning("Cannot refresh game list: context is null");
            return;
        }
        
        showLoading(true);
        statusLabel.setText("Refreshing game list...");
        
        context.getController().refreshGameList()
                .thenAccept(success -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        if (success) {
                            statusLabel.setText("Game list refreshed successfully");
                        } else {
                            statusLabel.setText("Failed to refresh game list");
                        }
                    });
                });
    }
    
    private void renderGamesListUI() {
        LOGGER.info("Updating games list");
        
        List<GameModel> availableGames = context.getModel().getAvailableGames();
        if (availableGames == null) {
            LOGGER.warning("Available games is null, hiding loading anyway");
            showLoading(false);
            statusLabel.setText("No games available");
            return;
        }
        
        LOGGER.info("Found " + availableGames.size() + " available games");
        
        waitingGamesFlowPane.getChildren().clear();
        inProgressGamesFlowPane.getChildren().clear();
        
        boolean hasWaitingGames = false;
        boolean hasInProgressGames = false;
        
        for (GameModel game : availableGames) {
            Node gameCard = createGameCard(game);
            
            // Separate games based on their current phase
            if (game.getCurrentPhase() == GamePhase.SETUP) {
                waitingGamesFlowPane.getChildren().add(gameCard);
                hasWaitingGames = true;
            } else {
                inProgressGamesFlowPane.getChildren().add(gameCard);
                hasInProgressGames = true;
            }
        }
        
        // Add "no games" labels if needed
        if (!hasWaitingGames) {
            waitingGamesFlowPane.getChildren().add(createNoGamesLabel());
        }
        if (!hasInProgressGames) {
            inProgressGamesFlowPane.getChildren().add(createNoGamesLabel());
        }
        
        showLoading(false);
        statusLabel.setText("Game list updated - " + availableGames.size() + " games found");
        LOGGER.info("Games list update complete, loading indicator hidden");
    }
    
    private Node createGameCard(GameModel game) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("panel-light-accent-box", "lobby-game-entry-pane");
        card.setPrefWidth(280);
        card.setPadding(new Insets(15));
        
        // Game name (title)
        Label nameLabel = new Label(game.getGameName());
        nameLabel.getStyleClass().addAll("label-subtitle", "lobby-game-name-label");
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        
        // Info box with phase and players
        HBox infoBox = new HBox(10);
        infoBox.setAlignment(Pos.CENTER);
        
        // Phase status
        String phaseText = getPhaseDisplayText(game.getCurrentPhase());
        Label phaseLabel = new Label(phaseText);
        phaseLabel.getStyleClass().add("lobby-phase-label");
        
        // Player count
        Label playersLabel = new Label(String.format("%d/%d players", 
            game.getCurrentPlayers(), game.getMaxPlayers()));
        playersLabel.getStyleClass().add("lobby-players-label");
        
        // Add separator
        Label separator = new Label("•");
        separator.getStyleClass().add("lobby-separator-label");
        
        infoBox.getChildren().addAll(phaseLabel, separator, playersLabel);
        
        // Action button
        Button actionButton;
        if (game.getCurrentPhase() == GamePhase.SETUP) {
            actionButton = new Button("JOIN GAME");
            actionButton.getStyleClass().addAll("button", "button-primary", "lobby-join-game-button");
            actionButton.setOnAction(e -> joinGame(game));
        } else {
            actionButton = new Button("IN PROGRESS");
            actionButton.getStyleClass().addAll("button", "button-secondary", "lobby-in-progress-button");
            actionButton.setDisable(true);
        }
        actionButton.setMaxWidth(Double.MAX_VALUE);
        setupButtonAnimations(actionButton);
        
        
        card.getChildren().addAll(nameLabel, infoBox, actionButton);
        return card;
    }
    
    private String getPhaseDisplayText(GamePhase phase) {
        return switch (phase) {
            case SETUP -> "Open";
            case BUILDING -> "Building";
            case FLIGHT -> "In Flight";
            case END -> "Ended";
        };
    }
    
    private Label createNoGamesLabel() {
        Label noGamesLabel = new Label("No games available in this category.");
        noGamesLabel.getStyleClass().add("lobby-no-games-label");
        noGamesLabel.setAlignment(Pos.CENTER);
        noGamesLabel.setPadding(new Insets(20));
        return noGamesLabel;
    }
    
    private void joinGame(GameModel game) {
        showLoading(true);
        statusLabel.setText("Joining game...");
        
        context.getController().joinGame(game.getGameId())
                .thenAccept(success -> {
                    Platform.runLater(() -> {
                        showLoading(false);
                        if (success) {
                            statusLabel.setText("Joined game successfully");
                        } else {
                            statusLabel.setText("Failed to join game");
                        }
                    });
                });
    }
    
    private void refreshPlayerNickname() {
        if (context != null && context.getModel() != null && playerLabel != null) {
            String currentNickname = context.getModel().getNickname();
            if (currentNickname != null && !currentNickname.trim().isEmpty()) {
                playerLabel.setText("Player: " + currentNickname);
            }
        }
    }
    
    private void showLoading(boolean isLoading) {
        // TEMPORARY FIX: Disable loading indicator for testing
        LOGGER.info("showLoading called with: " + isLoading + " - DISABLED FOR TESTING");
        
        // Comment out loading indicator for now to prevent stuck loading
        // loadingIndicator.setVisible(isLoading);
        // mainScrollPane.setEffect(isLoading ? new GaussianBlur(5) : null);
        // mainScrollPane.setDisable(isLoading);
        
        // Keep the loading indicator always hidden for testing
        loadingIndicator.setVisible(false);
        mainScrollPane.setEffect(null);
        mainScrollPane.setDisable(false);
        
        // Don't disable buttons during loading for testing
        if (createGameButton != null) createGameButton.setDisable(false);
        if (refreshButton != null) refreshButton.setDisable(false);
        if (logoutButton != null) logoutButton.setDisable(false);
    }


    @Override
    protected void onShow() {
        LOGGER.info("GuiLobbyView.onShow() called");
        
        // Refresh nickname when showing the lobby
        refreshPlayerNickname();
        
        // Clear any global loading states that might have been inherited
        if (context != null && context.getNotificationService() != null) {
            context.getNotificationService().hideLoading();
        }
        
        stage.setScene(lobbyScene);
        stage.setTitle("Galaxy Trucker - Game Browser");
        if (!stage.isShowing()) {
            stage.show();
        }
        
        // Don't automatically refresh - wait for the login process to complete
        // and trigger the property change event instead
        LOGGER.info("Waiting for game list to be populated via property change events");
        
        // If we already have games, update the display immediately
        if (context != null && context.getModel() != null && 
            context.getModel().getAvailableGames() != null) {
            LOGGER.info("Games already available, updating display");
            Platform.runLater(this::renderGamesListUI);
        } else {
            // Show a brief loading state but with timeout
            showLoading(true);
            statusLabel.setText("Loading game list...");
            
            // Hide loading after 3 seconds if no update comes
            Platform.runLater(() -> {
                javafx.concurrent.Task<Void> fallbackTask = new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        Thread.sleep(3000);
                        return null;
                    }
                    
                    @Override
                    protected void succeeded() {
                        Platform.runLater(() -> {
                            if (loadingIndicator.isVisible()) {
                                LOGGER.info("No games loaded after 3 seconds, hiding loading indicator");
                                showLoading(false);
                                statusLabel.setText("No games available");
                                // Add empty state
                                waitingGamesFlowPane.getChildren().clear();
                                inProgressGamesFlowPane.getChildren().clear();
                                waitingGamesFlowPane.getChildren().add(createNoGamesLabel());
                                inProgressGamesFlowPane.getChildren().add(createNoGamesLabel());
                            }
                        });
                    }
                };
                
                Thread fallbackThread = new Thread(fallbackTask);
                fallbackThread.setDaemon(true);
                fallbackThread.start();
            });
        }
    }

    @Override
    protected void onHide() {
        // Clear loading state when hiding this view
        showLoading(false);
    }

    @Override
    protected void onRefresh() {
        Platform.runLater(this::renderGamesListUI);
    }
    
    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        LOGGER.info("Property change received: " + evt.getPropertyName());
        switch (evt.getPropertyName()) {
            case "availableGames":
                LOGGER.info("Available games property changed, updating list");
                renderGamesListUI();
                break;
            case "currentView":
                // Hide loading when view changes away from lobby
                ClientState.ViewState newView = (ClientState.ViewState) evt.getNewValue();
                LOGGER.info("View changed to: " + newView);
                if (newView != ClientState.ViewState.LOBBY) {
                    showLoading(false);
                }
                break;
            case "nickname":
                // Update player label when nickname changes
                String newNickname = (String) evt.getNewValue();
                LOGGER.info("Nickname changed to: " + newNickname);
                if (playerLabel != null) {
                    if (newNickname == null || newNickname.trim().isEmpty()) {
                        newNickname = "Guest Player";
                    }
                    playerLabel.setText("Player: " + newNickname);
                }
                break;
            default:
                LOGGER.fine("Unhandled property change: " + evt.getPropertyName());
        }
    }

    @Override
    public String getTitle() {
        return "Game Browser";
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.LOBBY;
    }
}