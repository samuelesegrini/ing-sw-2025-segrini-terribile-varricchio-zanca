package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.gui.components.*;
import it.polimi.ingsw.common.PlayerInfo;
import it.polimi.ingsw.server.model.domain.ship.Position;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure ComponentInstance-based GUI view for ship building phase.
 * NO ComponentType conversions, NO legacy code, PURE ComponentInstance system.
 */
public class GuiShipBuildingView extends BaseUIView {
    private Stage stage;
    private ClientController controller;
    
    // UI Components
    private ShipGridView shipGridView;
    private ComponentInventoryView componentInventoryView;
    private ComponentJunkyardView componentJunkyard;
    private VBox otherPlayersColumn;
    private Label timerLabel;
    private Label shipStatsLabel;
    private Button validateShipButton;
    private Button flipTimerButton;
    private TextArea validationErrorsArea;
    
    // Other players management
    private final Map<String, PlayerMiniView> playerMiniViews = new ConcurrentHashMap<>();
    private static final int MAX_MINI_VIEWS = 3;
    
    // Current selection - ONLY ComponentInstance, NO legacy
    private ComponentInstance selectedComponent = null;
    
    // Batch update tracking
    private volatile boolean updatesPending = false;
    private final Object updateLock = new Object();

    public GuiShipBuildingView(Stage stage, ClientController controller) {
        this.stage = stage;
        this.controller = controller;
    }

    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.GAME;
    }

    @Override
    public String getTitle() {
        return "Ship Building";
    }

    @Override
    protected void onShow() {
        // Always create a fresh scene for ship building view
        BorderPane root = createLayout();
        Scene scene = new Scene(root, 1600, 900);
        
        // Load CSS
        try {
            scene.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/ship-building.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load ship building stylesheet: " + e.getMessage());
        }
        
        stage.setScene(scene);
        stage.setTitle("Galaxy Trucker - " + getTitle());
        
        // Setup property change listeners
        setupPropertyChangeListeners();
        
        // Update displays efficiently - batch all updates
        batchUpdateAllDisplays();
        
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    private VBox createTopPanel() {
        VBox topPanel = new VBox(10);
        topPanel.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Ship Building Phase");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        timerLabel = new Label("Time Remaining: --:--");
        timerLabel.setStyle("-fx-font-size: 18px;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        flipTimerButton = new Button("Flip Timer");
        flipTimerButton.setOnAction(e -> handleFlipTimer());
        
        validateShipButton = new Button("Validate Ship");
        validateShipButton.setOnAction(e -> handleValidateShip());
        
        buttonBox.getChildren().addAll(flipTimerButton, validateShipButton);
        
        topPanel.getChildren().addAll(titleLabel, timerLabel, buttonBox);
        return topPanel;
    }

    private HBox createMainContent() {
        HBox mainContent = new HBox(15);
        mainContent.getStyleClass().add("main-content-split");
        
        // Left side content
        HBox leftSideContent = new HBox(15);
        leftSideContent.getStyleClass().add("left-side-content");
        
        // Other players column
        otherPlayersColumn = new VBox(10);
        otherPlayersColumn.getStyleClass().add("other-players-column");
        otherPlayersColumn.setPrefWidth(250);
        otherPlayersColumn.setMaxWidth(250);
        otherPlayersColumn.setAlignment(Pos.TOP_CENTER);
        
        Label otherPlayersLabel = new Label("Other Players");
        otherPlayersLabel.getStyleClass().addAll("section-title", "other-players-title");
        otherPlayersColumn.getChildren().add(otherPlayersLabel);
        
        // Junkyard area - PURE ComponentInstance
        VBox junkyardSection = new VBox(10);
        junkyardSection.getStyleClass().add("junkyard-section");
        
        componentJunkyard = new ComponentJunkyardView(new ComponentJunkyardView.JunkyardClickHandler() {
            @Override
            public void onFaceDownTileClicked() {
                handleRandomComponentRequest();
            }
            
            @Override
            public void onComponentClicked(ComponentInstance component) {
                handleSpecificComponentRequest(component);
            }
        });
        
        junkyardSection.getChildren().add(componentJunkyard);
        VBox.setVgrow(componentJunkyard, javafx.scene.layout.Priority.ALWAYS);
        
        leftSideContent.getChildren().addAll(otherPlayersColumn, junkyardSection);
        HBox.setHgrow(junkyardSection, javafx.scene.layout.Priority.ALWAYS);
        
        // Right section - Active player ship building area
        VBox activePlayerArea = createShipBuildingArea();
        activePlayerArea.getStyleClass().add("active-player-area");
        activePlayerArea.setPrefWidth(500);
        
        mainContent.getChildren().addAll(leftSideContent, activePlayerArea);
        HBox.setHgrow(leftSideContent, javafx.scene.layout.Priority.ALWAYS);
        
        return mainContent;
    }
    
    private VBox createShipBuildingArea() {
        VBox buildingArea = new VBox(15);
        buildingArea.setAlignment(Pos.CENTER);
        buildingArea.getStyleClass().add("ship-building-center");
        
        Label gridLabel = new Label("Ship Construction");
        gridLabel.getStyleClass().add("section-title");
        
        // Create ship grid with ComponentInstance handlers
        shipGridView = new ShipGridView(new ShipGridView.ShipGridClickHandler() {
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
        shipStatsLabel.getStyleClass().add("ship-stats-label");
        
        // Hand management area - PURE ComponentInstance
        VBox handArea = createHandManagementArea();
        
        buildingArea.getChildren().addAll(gridLabel, shipGridView, shipStatsLabel, handArea);
        return buildingArea;
    }

    private VBox createHandManagementArea() {
        VBox handArea = new VBox(10);
        handArea.getStyleClass().add("hand-management-area");
        
        Label handLabel = new Label("Hand (Max 2 components)");
        handLabel.getStyleClass().add("section-title");
        
        Label instructionsLabel = new Label("Click face-down tiles for random components, face-up components for specific ones. " + 
                                           "Returned components appear face-up in the warehouse for others to see.");
        instructionsLabel.setStyle("-fx-font-style: italic; -fx-font-size: 11px; -fx-text-fill: #cccccc;");
        instructionsLabel.setWrapText(true);
        
        // Component inventory for held components ONLY
        componentInventoryView = new ComponentInventoryView(new ComponentInventoryView.ComponentClickHandler() {
            @Override
            public void onAvailableComponentClicked(ComponentInstance component) {
                // Not used in new layout
            }
            
            @Override
            public void onHeldComponentClicked(ComponentInstance component) {
                handleHeldComponentClicked(component);
            }
        });
        
        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        
        Button returnComponentButton = new Button("Return Selected Component");
        returnComponentButton.getStyleClass().addAll("button", "button-standard");
        returnComponentButton.setOnAction(e -> handleReturnComponent());
        
        Button rotateComponentButton = new Button("Rotate Selected Component");
        rotateComponentButton.getStyleClass().addAll("button", "button-standard");
        rotateComponentButton.setOnAction(e -> handleRotateComponent());
        
        actionButtons.getChildren().addAll(returnComponentButton, rotateComponentButton);
        
        // Validation errors section
        Label errorsLabel = new Label("Validation Errors");
        errorsLabel.getStyleClass().add("section-title");
        
        validationErrorsArea = new TextArea();
        validationErrorsArea.setPrefHeight(80);
        validationErrorsArea.setEditable(false);
        validationErrorsArea.getStyleClass().add("validation-errors-area");
        validationErrorsArea.setWrapText(true);
        
        handArea.getChildren().addAll(handLabel, instructionsLabel, componentInventoryView, actionButtons, errorsLabel, validationErrorsArea);
        return handArea;
    }

    private void handleCellClick(int row, int col) {
        if (selectedComponent == null) {
            showAlert("No Component Selected", "Please select a component from the inventory first.");
            return;
        }
        
        Position position = new Position(row, col);
        LocalGameState gameState = LocalGameState.getInstance();
        
        if (!gameState.canPlaceComponent(selectedComponent, position)) {
            showAlert("Invalid Placement", "Cannot place component at this position. Check connectors and placement rules.");
            return;
        }
        
        // Create visual component and place on grid
        ComponentTileView tileView = new ComponentTileView(selectedComponent);
        tileView.setPlaced(true);
        
        if (shipGridView.placeComponent(tileView, row, col)) {
            // Send placement request to server
            controller.placeTile(selectedComponent.getId(), row, col, selectedComponent.getCurrentDirection().ordinal());
            
            // Update local ship state (will be confirmed by server)
            gameState.placeTile(selectedComponent, position, selectedComponent.getCurrentDirection().ordinal());
            
            // Remove from held components
            if (gameState.getHeldTiles().contains(selectedComponent)) {
                gameState.removeHeldTile(selectedComponent);
                updateComponentInventoryDisplay();
            }
            
            // Clear selection
            clearComponentSelection();
        }
    }

    private void handleCellRightClick(int row, int col) {
        // Remove component from grid
        ComponentTileView removedComponent = shipGridView.removeComponent(row, col);
        
        if (removedComponent != null) {
            LocalGameState gameState = LocalGameState.getInstance();
            ComponentInstance componentInstance = removedComponent.getComponentInstance();
            
            if (componentInstance != null) {
                gameState.removeTile(new Position(row, col));
                gameState.addAvailableTile(componentInstance);
                
                // Update visual displays
                updateComponentInventoryDisplay();
                updateJunkyardDisplay();
                updateShipStats();
            }
        }
    }

    private void handleRandomComponentRequest() {
        controller.takeTile();
    }
    
    private void handleSpecificComponentRequest(ComponentInstance component) {
        controller.requestFaceUpTile(component.getId());
        
        // Remove the component from face-up junkyard (it's now taken)
        LocalGameState gameState = LocalGameState.getInstance();
        gameState.removeFaceUpJunkyardTile(component);
        updateJunkyardDisplay();
        
        System.out.println("Requested specific component from warehouse: " + component);
    }
    
    private void handleHeldComponentClicked(ComponentInstance component) {
        selectedComponent = component;
        clearComponentSelection();
        // Visual feedback will be handled by the component view
    }

    private void handleReturnComponent() {
        LocalGameState gameState = LocalGameState.getInstance();
        if (selectedComponent != null && gameState.getHeldTiles().contains(selectedComponent)) {
            controller.returnTile(selectedComponent.getId());
            
            gameState.removeHeldTile(selectedComponent);
            gameState.addFaceUpJunkyardTile(selectedComponent);
            
            // Update UI displays
            updateComponentInventoryDisplay();
            updateJunkyardDisplay();
            
            clearComponentSelection();
            
            System.out.println("Returned component to warehouse as face-up: " + selectedComponent);
        } else {
            showAlert("No Selection", "Please select a component from the held components first.");
        }
    }
    
    private void handleRotateComponent() {
        if (selectedComponent != null) {
            selectedComponent.rotate();
            // Update the visual representation
            updateComponentInventoryDisplay();
            System.out.println("Rotated component: " + selectedComponent + " to direction: " + selectedComponent.getCurrentDirection());
        } else {
            showAlert("No Selection", "Please select a component to rotate first.");
        }
    }
    
    private void clearComponentSelection() {
        selectedComponent = null;
        // Remove selection styling from all components
        if (componentInventoryView != null) {
            componentInventoryView.clearSelection();
        }
    }

    private void handleFlipTimer() {
        controller.flipBuildingTimer();
    }

    private void handleValidateShip() {
        controller.validateShip();
    }

    private void updateShipDisplay() {
        Platform.runLater(() -> {
            if (shipGridView == null) return;
            LocalGameState gameState = LocalGameState.getInstance();
            updateShipDisplayWithData(gameState.getShipGrid());
        });
    }
    
    private void updateShipDisplayWithData(ComponentInstance[][] shipGridData) {
        if (shipGridView == null || shipGridData == null) return;
        
        // Use efficient update instead of full clear and rebuild
        shipGridView.updateFromGridData(shipGridData);
    }

    private void updateTimerDisplay() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            updateTimerDisplayWithData(gameState.getBuildingTimeRemaining(), gameState.isBuildingTimerFlipped());
        });
    }
    
    private void updateTimerDisplayWithData(long timeRemaining, boolean timerFlipped) {
        if (timerLabel == null) return;
        
        if (timeRemaining > 0) {
            long minutes = timeRemaining / 60000;
            long seconds = (timeRemaining % 60000) / 1000;
            timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds));
        } else {
            timerLabel.setText("Time Remaining: --:--");
        }
        
        if (timerFlipped) {
            timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: red;");
        } else {
            timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: black;");
        }
    }

    private void updateShipStats() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            updateShipStatsWithData(gameState.getAllShipStats());
        });
    }
    
    private void updateShipStatsWithData(Map<LocalGameState.ComponentStatType, Integer> stats) {
        if (shipStatsLabel == null || stats == null) return;
        
        shipStatsLabel.setText(String.format(
            "Ship Stats - Engines: %d, Cannons: %d, Crew: %d, Cargo: %d, Batteries: %d, Shields: %d",
            stats.get(LocalGameState.ComponentStatType.ENGINES),
            stats.get(LocalGameState.ComponentStatType.CANNONS),
            stats.get(LocalGameState.ComponentStatType.CREW),
            stats.get(LocalGameState.ComponentStatType.CARGO),
            stats.get(LocalGameState.ComponentStatType.BATTERIES),
            stats.get(LocalGameState.ComponentStatType.SHIELDS)
        ));
    }

    private void updateComponentInventoryDisplay() {
        Platform.runLater(() -> {
            if (componentInventoryView == null) return;
            LocalGameState gameState = LocalGameState.getInstance();
            updateComponentInventoryDisplayWithData(gameState.getHeldTiles());
        });
    }
    
    private void updateComponentInventoryDisplayWithData(List<ComponentInstance> heldComponents) {
        if (componentInventoryView == null) return;
        
        // Update held components - PURE ComponentInstance
        componentInventoryView.updateHeldComponents(heldComponents);
        
        // Clear available components since they're now in junkyard
        componentInventoryView.updateAvailableComponents(new ArrayList<>());
    }
    
    private void updateJunkyardDisplay() {
        Platform.runLater(() -> {
            if (componentJunkyard == null) return;
            LocalGameState gameState = LocalGameState.getInstance();
            updateJunkyardDisplayWithData(gameState.getAvailableTiles(), gameState.getFaceUpJunkyardTiles());
        });
    }
    
    private void updateJunkyardDisplayWithData(List<ComponentInstance> availableComponents, List<ComponentInstance> faceUpComponents) {
        if (componentJunkyard == null) return;
        
        // Get all available components for the scattered pile
        List<ComponentInstance> allComponents = new ArrayList<>();
        allComponents.addAll(availableComponents);
        allComponents.addAll(faceUpComponents);
        
        // Get specifically face-up components
        List<ComponentInstance> faceUpComponentsCopy = new ArrayList<>(faceUpComponents);
        
        // Estimated face-down count (total minus face-up)
        int faceDownCount = Math.max(0, allComponents.size() - faceUpComponentsCopy.size());
        
        // Update the warehouse with physical pile behavior
        componentJunkyard.updateWarehouse(allComponents, faceUpComponentsCopy, faceDownCount);
    }

    private void updateValidationErrors() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            updateValidationErrorsWithData(gameState.getValidationErrors());
        });
    }
    
    private void updateValidationErrorsWithData(List<String> errors) {
        if (validationErrorsArea == null) return;
        
        if (errors.isEmpty()) {
            validationErrorsArea.setText("No validation errors");
            validationErrorsArea.setStyle("-fx-text-fill: green;");
        } else {
            validationErrorsArea.setText(String.join("\n", errors));
            validationErrorsArea.setStyle("-fx-text-fill: red;");
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
    
    private void updateOtherPlayersDisplay() {
        Platform.runLater(() -> {
            if (otherPlayersColumn == null) return;
            
            LocalGameState gameState = LocalGameState.getInstance();
            
            // Get real players from client model
            ClientModel clientModel = controller.getModel();
            String currentPlayerId = clientModel.getPlayerId();
            List<PlayerInfo> allPlayers = clientModel.getPlayersInLobby();
            
            // Clear existing mini-views except the title
            otherPlayersColumn.getChildren().removeIf(node -> node instanceof PlayerMiniView);
            playerMiniViews.clear();
            
            // Add mini-views for other players (exclude current player)
            int addedPlayers = 0;
            for (PlayerInfo player : allPlayers) {
                if (player.getPlayerId() != null && !player.getPlayerId().equals(currentPlayerId) && addedPlayers < MAX_MINI_VIEWS) {
                    Map<Position, ComponentInstance> playerShip = gameState.getOtherPlayerShip(player.getPlayerId());
                    
                    // Handle null or empty nicknames
                    String playerName = player.getNickname();
                    if (playerName == null || playerName.trim().isEmpty()) {
                        playerName = "Player " + player.getPlayerId().substring(0, Math.min(8, player.getPlayerId().length()));
                    }
                    
                    addOtherPlayerDirect(player.getPlayerId(), playerName, playerShip);
                    addedPlayers++;
                }
            }
        });
    }
    
    private void addOtherPlayerDirect(String playerId, String playerName, Map<Position, ComponentInstance> shipGrid) {
        if (otherPlayersColumn == null || playerId == null) return;
        
        // Don't add if already exists
        if (playerMiniViews.containsKey(playerId)) {
            PlayerMiniView existing = playerMiniViews.get(playerId);
            // PURE ComponentInstance - no conversions
            existing.updateShipDisplay(shipGrid);
            return;
        }
        
        PlayerMiniView miniView = new PlayerMiniView(playerId, playerName);
        // PURE ComponentInstance - no conversions
        miniView.updateShipDisplay(shipGrid);
        
        playerMiniViews.put(playerId, miniView);
        otherPlayersColumn.getChildren().add(miniView);
    }
    
    public void addOtherPlayer(String playerId, String playerName, Map<Position, ComponentInstance> shipGrid) {
        Platform.runLater(() -> {
            addOtherPlayerDirect(playerId, playerName, shipGrid);
        });
    }
    
    public void updateOtherPlayerShip(String playerId, Map<Position, ComponentInstance> shipGrid) {
        Platform.runLater(() -> {
            PlayerMiniView miniView = playerMiniViews.get(playerId);
            if (miniView != null) {
                // PURE ComponentInstance - no conversions
                miniView.updateShipDisplay(shipGrid);
            }
        });
    }
    
    public void removeOtherPlayer(String playerId) {
        Platform.runLater(() -> {
            PlayerMiniView miniView = playerMiniViews.remove(playerId);
            if (miniView != null && otherPlayersColumn != null) {
                otherPlayersColumn.getChildren().remove(miniView);
                System.out.println("Removed mini-view for player: " + miniView.getPlayerName());
            }
        });
    }
    
    public void highlightOtherPlayer(String playerId, boolean highlight) {
        Platform.runLater(() -> {
            PlayerMiniView miniView = playerMiniViews.get(playerId);
            if (miniView != null) {
                miniView.setHighlighted(highlight);
            }
        });
    }
    
    // Layout creation methods
    private BorderPane createLayout() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.getStyleClass().add("ship-building-root");
        
        root.setTop(createTopPanel());
        root.setCenter(createMainContent());
        
        return root;
    }
    
    private void updateAllDisplays() {
        updateShipDisplay();
        updateTimerDisplay();
        updateShipStats();
        updateJunkyardDisplay();
        updateComponentInventoryDisplay();
        updateValidationErrors();
    }
    
    private void batchUpdateAllDisplays() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            
            // Batch all state reads to avoid multiple property accesses
            ComponentInstance[][] shipGridData = gameState.getShipGrid();
            long timeRemaining = gameState.getBuildingTimeRemaining();
            boolean timerFlipped = gameState.isBuildingTimerFlipped();
            var shipStats = gameState.getAllShipStats();
            var heldComponents = gameState.getHeldTiles();
            var availableComponents = gameState.getAvailableTiles();
            var faceUpComponents = gameState.getFaceUpJunkyardTiles();
            var validationErrors = gameState.getValidationErrors();
            
            // Update all displays with batched data
            updateShipDisplayWithData(shipGridData);
            updateTimerDisplayWithData(timeRemaining, timerFlipped);
            updateShipStatsWithData(shipStats);
            updateJunkyardDisplayWithData(availableComponents, faceUpComponents);
            updateComponentInventoryDisplayWithData(heldComponents);
            updateValidationErrorsWithData(validationErrors);
            updateOtherPlayersDisplay();
        });
    }
    
    private void setupPropertyChangeListeners() {
        // Property change listeners are handled in onPropertyChange method
    }


    @Override
    protected void onHide() {
        // GUI views don't need special hiding logic
    }

    @Override
    protected void onRefresh() {
        updateAllDisplays();
        updateOtherPlayersDisplay();
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        // Batch multiple rapid property changes into a single update
        synchronized (updateLock) {
            if (!updatesPending) {
                updatesPending = true;
                Platform.runLater(() -> {
                    synchronized (updateLock) {
                        updatesPending = false;
                        handleBatchedPropertyChanges();
                    }
                });
            }
        }
        
        // Handle immediate updates for critical events
        String propertyName = evt.getPropertyName();
        switch (propertyName) {
            case "phaseTransition" -> {
                if ("FLIGHT".equals(evt.getNewValue())) {
                    showAlert("Phase Transition", "Building phase complete! Transitioning to flight phase.");
                }
            }
            case "shipGridConfig" -> {
                if (shipGridView != null) {
                    shipGridView.updateGridLayout();
                    shipGridView.refreshCellStyling();
                }
            }
            case "playerShipUpdated" -> {
                if (evt.getNewValue() instanceof Map<?, ?> shipData) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) shipData;
                    String playerId = (String) data.get("playerId");
                    @SuppressWarnings("unchecked")
                    Map<Position, ComponentInstance> shipGrid = (Map<Position, ComponentInstance>) data.get("shipGrid");
                    if (playerId != null && shipGrid != null) {
                        LocalGameState.getInstance().updateOtherPlayerShip(playerId, shipGrid);
                        updateOtherPlayerShip(playerId, shipGrid);
                    }
                }
            }
            case "currentPlayerTurn" -> {
                if (evt.getOldValue() instanceof String oldPlayerId) {
                    highlightOtherPlayer(oldPlayerId, false);
                }
                if (evt.getNewValue() instanceof String newPlayerId) {
                    highlightOtherPlayer(newPlayerId, true);
                }
            }
        }
    }
    
    private void handleBatchedPropertyChanges() {
        // Perform a full update when batched changes are processed
        batchUpdateAllDisplays();
    }
}