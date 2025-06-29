package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.core.UIView;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.gui.components.*;
import it.polimi.ingsw.client.ui.gui.NotificationManager;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.ShipValidationService;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI view for ship building phase using Simple Direct Model Architecture.
 */
public class GuiShipBuildingView extends BaseUIView implements UIView {
    private Stage stage;
    private ClientController controller;
    private UIContext uiContext;
    private NotificationManager notificationManager;
    
    // UI Components
    private ShipGridView shipGridView;
    private EnhancedHandView enhancedHandView;
    private ComponentJunkyardView componentJunkyard;
    private VBox otherPlayersColumn;
    private Label timerLabel;
    private Label shipStatsLabel;
    private Button validateShipButton;
    private Button flipTimerButton;
    private Button autoPlaceButton;
    private Button clearShipButton;
    private TextArea validationErrorsArea;
    private ProgressBar buildingProgressBar;
    private Label componentCountLabel;
    
    // Other players management
    private final Map<String, PlayerMiniView> playerMiniViews = new ConcurrentHashMap<>();
    private static final int MAX_MINI_VIEWS = 3;
    
    private Component selectedComponent = null;
    
    // Processing state management for optimistic UI feedback
    private final Map<String, ProcessingRequest> processingRequests = new ConcurrentHashMap<>();
    private final Map<Position, Component> optimisticPlacements = new ConcurrentHashMap<>();

    public GuiShipBuildingView(Stage stage, ClientController controller, UIContext uiContext) {
        this.stage = stage;
        this.controller = controller;
        this.uiContext = uiContext;
        this.notificationManager = new NotificationManager();
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.BUILDING;
    }

    @Override
    public String getTitle() {
        return "Ship Building";
    }

    @Override
    protected void onShow() {
        System.out.println("[DEBUG] GuiShipBuildingView.onShow() called");
        BorderPane root = createLayout();
        
        // Add notification overlay to the root
        StackPane rootWithNotifications = new StackPane();
        rootWithNotifications.getChildren().addAll(root, notificationManager.getNotificationContainer());
        StackPane.setAlignment(notificationManager.getNotificationContainer(), Pos.TOP_RIGHT);
        
        Scene scene = new Scene(rootWithNotifications, 1600, 900);
        
        // Add keyboard shortcuts
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case R -> {
                    if (selectedComponent != null) {
                        selectedComponent.rotate();
                        enhancedHandView.refresh();
                    }
                }
                case ESCAPE -> {
                    selectedComponent = null;
                    if (enhancedHandView != null) {
                        enhancedHandView.clearSelection();
                    }
                }
                case SPACE -> {
                    if (selectedComponent != null) {
                        autoPlaceSelectedComponent();
                    }
                }
                case V -> controller.validateShip();
                case T -> controller.takeTile();
                case F -> controller.flipBuildingTimer();
            }
        });
        
        // Load CSS
        try {
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/ship-building.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load ship building stylesheet: " + e.getMessage());
        }
        
        stage.setScene(scene);
        stage.setTitle("Galaxy Trucker - " + getTitle());
        
        // Register with ClientState for automatic refresh using new architecture
        if (uiContext != null && uiContext.getClientState() != null) {
            uiContext.getClientState().registerRefreshableView(this);
        }
        
        if (!stage.isShowing()) {
            stage.show();
        }
        
        // Initial refresh
        refresh();
    }

    @Override
    protected void onHide() {
        // Clean up
    }

    @Override
    protected void onRefresh() {
        refresh();
    }
    
    @Override
    public void refresh() {
        System.out.println("[TAKETILE DEBUG] GuiShipBuildingView.refresh() called from thread: " + Thread.currentThread().getName());
        if (uiContext != null && uiContext.getClientState() != null && uiContext.getClientState().isInGame()) {
            Platform.runLater(() -> {
                System.out.println("[TAKETILE DEBUG] GuiShipBuildingView.refresh() running on JavaFX thread");
                
                Ship ship = uiContext.getClientState().getLocalPlayerShip();
                ComponentDeck deck = uiContext.getClientState().getComponentDeck();
                
                if (ship != null && shipGridView != null) {
                    shipGridView.refresh();
                    updateShipStatsFromServerModel(ship);
                }
                
                if (deck != null) {
                    if (componentJunkyard != null) {
                        componentJunkyard.refresh();
                    }
                    if (enhancedHandView != null) {
                        System.out.println("[TAKETILE DEBUG] About to call enhancedHandView.refresh()");
                        enhancedHandView.refresh();
                    } else {
                        System.out.println("[TAKETILE DEBUG] ERROR: enhancedHandView is null!");
                    }
                } else {
                    System.out.println("[TAKETILE DEBUG] ERROR: ComponentDeck is null!");
                }
                
                // Update other players display
                updateOtherPlayers();
            });
        } else {
            System.out.println("[TAKETILE DEBUG] GuiShipBuildingView.refresh() - not in game or context missing");
        }
    }

    private BorderPane createLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.getStyleClass().add("ship-building-root");
        
        root.setTop(createTopPanel());
        root.setCenter(createMainContent());
        
        return root;
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Ship Building Phase");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        timerLabel = new Label("Time Remaining: --:--");
        timerLabel.setStyle("-fx-font-size: 18px;");
        
        buildingProgressBar = new ProgressBar(0.0);
        buildingProgressBar.setPrefWidth(300);
        buildingProgressBar.setVisible(false);
        
        componentCountLabel = new Label("Components: 0/∞");
        componentCountLabel.setStyle("-fx-font-size: 14px;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        flipTimerButton = new Button("Flip Timer");
        flipTimerButton.setOnAction(e -> controller.flipBuildingTimer());
        flipTimerButton.setTooltip(new Tooltip("Flip the building timer to advance to next stage"));
        
        validateShipButton = new Button("Validate Ship");
        validateShipButton.setOnAction(e -> controller.validateShip());
        validateShipButton.setTooltip(new Tooltip("Check if your ship design is valid"));
        
        autoPlaceButton = new Button("Auto Place");
        autoPlaceButton.setOnAction(e -> autoPlaceSelectedComponent());
        autoPlaceButton.setTooltip(new Tooltip("Automatically find valid position for selected component"));
        
        clearShipButton = new Button("Clear Ship");
        clearShipButton.setOnAction(e -> clearShipGrid());
        clearShipButton.setTooltip(new Tooltip("Remove all components from ship"));
        
        buttonBox.getChildren().addAll(flipTimerButton, validateShipButton, autoPlaceButton, clearShipButton);
        
        topPanel.getChildren().addAll(titleLabel, timerLabel, buildingProgressBar, componentCountLabel, buttonBox);
        return topPanel;
    }

    private HBox createMainContent() {
        HBox mainContent = new HBox(15);
        
        // Left side content
        HBox leftSideContent = new HBox(15);
        
        // Other players column
        otherPlayersColumn = new VBox(10);
        otherPlayersColumn.setPrefWidth(250);
        otherPlayersColumn.setMaxWidth(250);
        otherPlayersColumn.setAlignment(Pos.TOP_CENTER);
        
        Label otherPlayersLabel = new Label("Other Players");
        otherPlayersColumn.getChildren().add(otherPlayersLabel);
        
        // Junkyard area
        VBox junkyardSection = new VBox(10);
        componentJunkyard = new ComponentJunkyardView(uiContext, new ComponentJunkyardView.JunkyardClickHandler() {
            @Override
            public void onFaceDownTileClicked() {
                controller.takeTile();
            }
            
            @Override
            public void onComponentClicked(Component component) {
                controller.requestFaceUpTile(component.getId());
            }
        });
        
        junkyardSection.getChildren().add(componentJunkyard);
        VBox.setVgrow(componentJunkyard, Priority.ALWAYS);
        
        leftSideContent.getChildren().addAll(otherPlayersColumn, junkyardSection);
        HBox.setHgrow(junkyardSection, Priority.ALWAYS);
        
        // Right section - Ship building area
        VBox activePlayerArea = createShipBuildingArea();
        activePlayerArea.setPrefWidth(500);
        
        mainContent.getChildren().addAll(leftSideContent, activePlayerArea);
        HBox.setHgrow(leftSideContent, Priority.ALWAYS);
        
        return mainContent;
    }
    
    private VBox createShipBuildingArea() {
        VBox buildingArea = new VBox(15);
        buildingArea.setAlignment(Pos.CENTER);
        
        Label gridLabel = new Label("Ship Construction");
        
        // Create ship grid
        shipGridView = new ShipGridView(uiContext, new ShipGridView.ShipGridClickHandler() {
            @Override
            public void onCellClicked(int row, int col) {
                handleCellClick(row, col);
            }
            
            @Override
            public void onCellRightClicked(int row, int col) {
                handleCellRightClick(row, col);
            }
        });
        
        shipStatsLabel = new Label("Ship Stats: Engines: 0, Cannons: 0, Crew: 0, Cargo: 0");
        
        // Hand management area
        VBox handArea = createHandManagementArea();
        
        buildingArea.getChildren().addAll(gridLabel, shipGridView, shipStatsLabel, handArea);
        return buildingArea;
    }

    private VBox createHandManagementArea() {
        VBox handArea = new VBox(15);
        handArea.setAlignment(Pos.CENTER);
        
        // Create hand view
        enhancedHandView = new EnhancedHandView(uiContext, new EnhancedHandView.HandActionHandler() {
            @Override
            public void onComponentSelected(Component component) {
                selectedComponent = component;
            }
            
            @Override
            public void onRotateComponent() {
                if (selectedComponent != null) {
                    selectedComponent.rotate();
                    enhancedHandView.refresh();
                }
            }
            
            @Override
            public void onReturnComponent() {
                if (selectedComponent != null) {
                    controller.returnTile(selectedComponent.getId());
                    selectedComponent = null;
                }
            }
            
            @Override
            public void onReserveComponent() {
                if (selectedComponent != null) {
                    controller.reserveComponent(selectedComponent.getId());
                }
            }
            
            @Override
            public void onPlaceComponent() {
                if (selectedComponent != null) {
                    showAlert("Place Component", "Click on an empty grid cell to place the component.");
                } else {
                    showAlert("No Component", "Select a component first.");
                }
            }
        });
        
        // Validation errors section
        Label errorsLabel = new Label("⚠️ Ship Validation");
        
        validationErrorsArea = new TextArea();
        validationErrorsArea.setPrefHeight(80);
        validationErrorsArea.setMaxHeight(80);
        validationErrorsArea.setEditable(false);
        validationErrorsArea.setWrapText(true);
        
        VBox validationSection = new VBox(8);
        validationSection.getChildren().addAll(errorsLabel, validationErrorsArea);
        validationSection.setAlignment(Pos.CENTER);
        
        handArea.getChildren().addAll(enhancedHandView, validationSection);
        
        return handArea;
    }

    private void handleCellClick(int row, int col) {
        System.out.println(String.format("[DEBUG] Cell clicked: row=%d, col=%d", row, col));
        
        if (selectedComponent == null) {
            showAlert("No Component Selected", "Please select a component from the inventory first.");
            return;
        }
        
        // ENHANCED: Perform client-side validation before sending to server
        Ship ship = uiContext.getClientState().getLocalPlayerShip();
        if (ship != null) {
            Position position = new Position(row, col);
            
            // Use comprehensive validation system
            ShipValidationService.ValidationResult validationResult = 
                ShipValidationService.validateComponentPlacement(ship, selectedComponent, position);
            
            if (!validationResult.isValid()) {
                // Show detailed validation errors
                notificationManager.showValidationErrors(
                    "Invalid Placement", 
                    validationResult.getErrors(), 
                    validationResult.getWarnings()
                );
                return;
            }
            
            // Show warnings as placement hints if valid but with suggestions
            if (!validationResult.getWarnings().isEmpty()) {
                notificationManager.showPlacementHints(validationResult.getWarnings());
            }
        }
        
        // ENHANCED: Optimistic UI feedback - temporarily place component for immediate visual feedback
        Position position = new Position(row, col);
        String requestId = selectedComponent.getId() + "_" + System.currentTimeMillis();
        
        // Store optimistic placement
        optimisticPlacements.put(position, selectedComponent);
        processingRequests.put(requestId, new ProcessingRequest("PLACE_TILE", position));
        
        // Show loading indicator
        notificationManager.showLoading("Placing component...");
        
        // Update UI immediately with optimistic state
        if (shipGridView != null) {
            shipGridView.setOptimisticPlacement(row, col, selectedComponent);
        }
        
        // Send placement request to server
        controller.placeTile(selectedComponent.getId(), row, col, selectedComponent.getCurrentDirection().ordinal());
        
        // Clear selection - server will update state via events
        selectedComponent = null;
        if (enhancedHandView != null) {
            enhancedHandView.clearSelection();
        }
        
        // Schedule cleanup of processing state in case server doesn't respond
        Timeline timeoutCleanup = new Timeline(new KeyFrame(
            Duration.seconds(5),
            e -> {
                processingRequests.remove(requestId);
                optimisticPlacements.remove(position);
                notificationManager.hideLoading();
                if (shipGridView != null) {
                    shipGridView.clearOptimisticPlacement(row, col);
                }
            }
        ));
        timeoutCleanup.play();
    }

    private void handleCellRightClick(int row, int col) {
        System.out.println(String.format("[DEBUG] Right-click on cell (%d,%d)", row, col));
        
        // Remove component from grid - this will be handled by server
        ComponentTileView removedComponent = shipGridView.removeComponent(row, col);
        
        if (removedComponent != null) {
            Component component = removedComponent.getComponent();
            if (component != null) {
                // Send return request to server
                controller.returnTile(component.getId());
            }
        }
    }

    private void updateShipStatsFromServerModel(Ship ship) {
        if (shipStatsLabel != null && ship != null) {
            try {
                // Use server Ship's stats if available
                ship.updateStats(); // Ensure stats are current
                double engines = ship.getEngines();
                double cannons = ship.getCannons();
                int crew = ship.getCrew();
                int batteries = ship.getBatteries();
                int shields = ship.getShields();
                int lifeSupport = ship.getLifeSupport();
                
                shipStatsLabel.setText(String.format(
                    "Ship Stats: Engines: %.1f, Cannons: %.1f, Crew: %d, Batteries: %d, Shields: %d, Life Support: %d",
                    engines, cannons, crew, batteries, shields, lifeSupport
                ));
                
                // Update component count
                if (componentCountLabel != null) {
                    int placedComponents = ship.getPlacedComponentsCount();
                    componentCountLabel.setText(String.format("Components Placed: %d", placedComponents));
                }
            } catch (Exception e) {
                shipStatsLabel.setText("Ship Stats: Calculating...");
            }
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Updates the display of other players in the game.
     * Creates or updates PlayerMiniView components for each other player.
     */
    private void updateOtherPlayers() {
        if (otherPlayersColumn == null || uiContext == null || uiContext.getClientState() == null) {
            return;
        }
        
        List<Player> otherPlayers = getOtherPlayers();
        System.out.println("[DEBUG] updateOtherPlayers() found " + otherPlayers.size() + " other players");
        
        // Remove old mini views that are no longer needed
        List<String> currentPlayerIds = new ArrayList<>();
        for (Player player : otherPlayers) {
            currentPlayerIds.add(player.getId().toString());
        }
        
        // Remove mini views for players who left
        playerMiniViews.entrySet().removeIf(entry -> {
            String playerId = entry.getKey();
            if (!currentPlayerIds.contains(playerId)) {
                PlayerMiniView miniView = entry.getValue();
                otherPlayersColumn.getChildren().remove(miniView);
                System.out.println("[DEBUG] Removed mini view for player: " + playerId);
                return true;
            }
            return false;
        });
        
        // Create or update mini views for current other players
        for (Player player : otherPlayers) {
            String playerId = player.getId().toString();
            PlayerMiniView miniView = playerMiniViews.get(playerId);
            
            if (miniView == null) {
                // Create new mini view
                miniView = new PlayerMiniView(playerId, player.getNickname());
                miniView.setContext(uiContext); // Set context for proper ship grid access
                playerMiniViews.put(playerId, miniView);
                otherPlayersColumn.getChildren().add(miniView);
                System.out.println("[DEBUG] Created mini view for player: " + player.getNickname() + " (" + playerId + ")");
            }
            
            // Update mini view with current player data
            miniView.updatePlayer(player);
            miniView.updateShip(player.getShip());
        }
        
        // Limit to MAX_MINI_VIEWS
        if (playerMiniViews.size() > MAX_MINI_VIEWS) {
            System.out.println("[DEBUG] Too many mini views (" + playerMiniViews.size() + "), limiting to " + MAX_MINI_VIEWS);
        }
    }
    
    /**
     * Gets the list of other players (excluding the local player).
     * @return List of other players in the game
     */
    private List<Player> getOtherPlayers() {
        if (uiContext == null || uiContext.getClientState() == null || 
            uiContext.getClientState().getGameModel() == null) {
            return new ArrayList<>();
        }
        
        List<Player> allPlayers = uiContext.getClientState().getGameModel().getPlayers();
        String localPlayerId = uiContext.getClientState().getPlayerId();
        
        if (allPlayers == null || localPlayerId == null) {
            return new ArrayList<>();
        }
        
        List<Player> otherPlayers = new ArrayList<>();
        for (Player player : allPlayers) {
            if (!player.getId().toString().equals(localPlayerId)) {
                otherPlayers.add(player);
            }
        }
        
        System.out.println("[DEBUG] getOtherPlayers() - Total players: " + allPlayers.size() + 
                          ", Local player: " + localPlayerId + ", Other players: " + otherPlayers.size());
        
        // Debug: Print all player information for verification
        if (allPlayers.size() > 0) {
            System.out.println("[DEBUG] All players in game model:");
            for (int i = 0; i < allPlayers.size(); i++) {
                Player p = allPlayers.get(i);
                System.out.println("[DEBUG]   Player " + i + ": " + p.getId() + " (" + p.getNickname() + ") - " + 
                                  (p.getId().toString().equals(localPlayerId) ? "LOCAL" : "OTHER"));
            }
        } else {
            System.out.println("[DEBUG] WARNING: No players found in game model - client state may not be synchronized");
        }
        
        return otherPlayers;
    }
    
    /**
     * Automatically places the selected component in the first valid position.
     */
    private void autoPlaceSelectedComponent() {
        if (selectedComponent == null) {
            showAlert("No Component", "Please select a component first.");
            return;
        }
        
        Ship ship = uiContext.getClientState().getLocalPlayerShip();
        if (ship == null) {
            showAlert("No Ship", "Ship data not available.");
            return;
        }
        
        Component[][] board = ship.getBoard();
        
        // Try all positions to find first valid placement
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                if (board[row][col] == null) {
                    // Try placing component here
                    controller.placeTile(selectedComponent.getId(), row, col, selectedComponent.getCurrentDirection().ordinal());
                    selectedComponent = null;
                    if (enhancedHandView != null) {
                        enhancedHandView.clearSelection();
                    }
                    return;
                }
            }
        }
        
        showAlert("No Space", "No valid position found for this component.");
    }
    
    /**
     * Clears all components from the ship grid.
     */
    private void clearShipGrid() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Clear Ship");
        confirmation.setHeaderText("Remove all components?");
        confirmation.setContentText("This will return all placed components to your hand. Are you sure?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Ship ship = uiContext.getClientState().getLocalPlayerShip();
                if (ship != null) {
                    Component[][] board = ship.getBoard();
                    for (int row = 0; row < board.length; row++) {
                        for (int col = 0; col < board[0].length; col++) {
                            if (board[row][col] != null) {
                                controller.returnTile(board[row][col].getId());
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Updates building progress and timer display.
     */
    public void updateBuildingProgress(long timeRemaining, double progress) {
        Platform.runLater(() -> {
            if (timeRemaining > 0) {
                long minutes = timeRemaining / 60000;
                long seconds = (timeRemaining % 60000) / 1000;
                timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds));
                
                buildingProgressBar.setProgress(1.0 - progress);
                buildingProgressBar.setVisible(true);
                
                // Change color based on time remaining
                if (progress > 0.8) {
                    timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: red;");
                } else if (progress > 0.6) {
                    timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: orange;");
                } else {
                    timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: green;");
                }
            } else {
                timerLabel.setText("Building Phase Active");
                buildingProgressBar.setVisible(false);
                timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: black;");
            }
        });
    }
    
    /**
     * Updates validation errors display.
     */
    public void updateValidationErrors(List<String> errors, List<String> warnings) {
        Platform.runLater(() -> {
            if (validationErrorsArea != null) {
                StringBuilder sb = new StringBuilder();
                
                if (errors != null && !errors.isEmpty()) {
                    sb.append("❌ ERRORS:\n");
                    for (String error : errors) {
                        sb.append("  • ").append(error).append("\n");
                    }
                }
                
                if (warnings != null && !warnings.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("⚠️ WARNINGS:\n");
                    for (String warning : warnings) {
                        sb.append("  • ").append(warning).append("\n");
                    }
                }
                
                if (sb.length() == 0) {
                    sb.append("✅ Ship design is valid!");
                }
                
                validationErrorsArea.setText(sb.toString());
            }
        });
    }
    
    /**
     * Represents a processing request for optimistic UI feedback
     */
    private static class ProcessingRequest {
        private final String type;
        private final long timestamp;
        private final Object data;
        
        public ProcessingRequest(String type, Object data) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.data = data;
        }
        
        public String getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public Object getData() { return data; }
        
        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - timestamp > timeoutMs;
        }
    }

}