package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.common.message.request.CreateGameRequest;
import it.polimi.ingsw.common.message.request.JoinGameRequest;
import it.polimi.ingsw.common.message.request.ListGamesRequest;
import it.polimi.ingsw.server.model.enums.GameLevel;
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
        
        Label playerLabel = new Label("Player: " + (context != null && context.getModel() != null ? 
            context.getModel().getNickname() : "Unknown"));
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
        refreshButton.setOnAction(e -> refreshGameList());
        
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
        // For now, create a simple game with default values
        // This can be expanded with a proper dialog later
        CreateGameRequest request = new CreateGameRequest(4, GameLevel.TEST_FLIGHT, "New Game");
        context.getController().sendRequest(request);
        statusLabel.setText("Creating game...");
    }
    
    private void handleLogout() {
        // Clear authentication and return to login view
        context.getModel().setAuthenticated(false);
        context.getModel().setCurrentView(ClientModel.ViewState.LOGIN);
    }

    private void refreshGameList() {
        if (context == null) {
            LOGGER.warning("Cannot refresh game list: context is null");
            return;
        }
        
        showLoading(true);
        statusLabel.setText("Refreshing game list...");
        
        ListGamesRequest request = new ListGamesRequest();
        context.getController().sendRequest(request);
        
        // Safety timeout to hide loading indicator if response doesn't come
        Platform.runLater(() -> {
            javafx.concurrent.Task<Void> timeoutTask = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(5000); // 5 second timeout
                    return null;
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        if (loadingIndicator.isVisible()) {
                            LOGGER.warning("Loading indicator timeout - forcing hide");
                            showLoading(false);
                            statusLabel.setText("Game list refresh timed out");
                        }
                    });
                }
            };
            
            Thread timeoutThread = new Thread(timeoutTask);
            timeoutThread.setDaemon(true);
            timeoutThread.start();
        });
    }
    
    private void updateGamesList() {
        LOGGER.info("Updating games list");
        
        List<GameInfo> availableGames = context.getModel().getAvailableGames();
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
        
        for (GameInfo game : availableGames) {
            Node gameCard = createGameCard(game);
            
            // For now, assume all games are waiting (can be enhanced based on game state)
            waitingGamesFlowPane.getChildren().add(gameCard);
            hasWaitingGames = true;
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
    
    private Node createGameCard(GameInfo game) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("panel-light-accent-box", "lobby-game-entry-pane");
        card.setPrefWidth(300);
        card.setPadding(new Insets(12));
        
        // Title box with game ID and level
        HBox titleBox = new HBox(10);
        titleBox.getStyleClass().add("lobby-game-entry-title-box");
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label gameIdLabel = new Label(game.getGameId());
        gameIdLabel.getStyleClass().add("lobby-game-id-label");
        
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        
        Label levelLabel = new Label(game.getGameLevel().toString().replace("_", " "));
        levelLabel.getStyleClass().add("lobby-level-label");
        
        titleBox.getChildren().addAll(gameIdLabel, titleSpacer, levelLabel);
        
        // Game name
        Label nameLabel = new Label(game.getGameName());
        nameLabel.getStyleClass().add("lobby-host-label");
        
        // Details box
        HBox detailsBox = new HBox(15);
        detailsBox.getStyleClass().add("lobby-game-entry-details-box");
        detailsBox.setAlignment(Pos.CENTER_LEFT);
        
        Label playersLabel = new Label(String.format("Players: %d/%d", 
            game.getCurrentPlayers(), game.getMaxPlayers()));
        playersLabel.getStyleClass().add("lobby-players-label");
        
        detailsBox.getChildren().add(playersLabel);
        
        // Action box
        HBox actionBox = new HBox();
        actionBox.getStyleClass().add("lobby-game-entry-action-box");
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button joinButton = new Button("JOIN GAME");
        joinButton.getStyleClass().addAll("button", "button-mini", "button-mini-action", "lobby-join-game-button");
        joinButton.setOnAction(e -> joinGame(game));
        setupButtonAnimations(joinButton);
        
        actionBox.getChildren().add(joinButton);
        
        card.getChildren().addAll(titleBox, nameLabel, detailsBox, actionBox);
        return card;
    }
    
    private Label createNoGamesLabel() {
        Label noGamesLabel = new Label("No games available in this category.");
        noGamesLabel.getStyleClass().add("lobby-no-games-label");
        noGamesLabel.setAlignment(Pos.CENTER);
        noGamesLabel.setPadding(new Insets(20));
        return noGamesLabel;
    }
    
    private void joinGame(GameInfo game) {
        showLoading(true);
        statusLabel.setText("Joining game...");
        
        JoinGameRequest request = new JoinGameRequest(game.getGameId());
        context.getController().sendRequest(request);
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
            Platform.runLater(this::updateGamesList);
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
        Platform.runLater(this::updateGamesList);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        LOGGER.info("Property change received: " + evt.getPropertyName());
        Platform.runLater(() -> {
            switch (evt.getPropertyName()) {
                case "availableGames":
                    LOGGER.info("Available games property changed, updating list");
                    updateGamesList();
                    break;
                case "currentView":
                    // Hide loading when view changes away from lobby
                    ClientModel.ViewState newView = (ClientModel.ViewState) evt.getNewValue();
                    LOGGER.info("View changed to: " + newView);
                    if (newView != ClientModel.ViewState.LOBBY) {
                        showLoading(false);
                    }
                    break;
                default:
                    LOGGER.fine("Unhandled property change: " + evt.getPropertyName());
            }
        });
    }

    @Override
    public String getTitle() {
        return "Game Browser";
    }

    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.LOBBY;
    }
}