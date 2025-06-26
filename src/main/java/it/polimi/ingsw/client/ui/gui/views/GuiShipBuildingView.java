package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.UIRefreshable;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.gui.components.*;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI view for ship building phase using Simple Direct Model Architecture.
 */
public class GuiShipBuildingView extends BaseUIView implements UIRefreshable {
    private Stage stage;
    private ClientController controller;
    private UIContext uiContext;
    
    // UI Components
    private ShipGridView shipGridView;
    private EnhancedHandView enhancedHandView;
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
    
    private Component selectedComponent = null;

    public GuiShipBuildingView(Stage stage, ClientController controller, UIContext uiContext) {
        this.stage = stage;
        this.controller = controller;
        this.uiContext = uiContext;
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.GAME;
    }

    @Override
    public String getTitle() {
        return "Ship Building";
    }

    @Override
    protected void onShow() {
        System.out.println("[DEBUG] GuiShipBuildingView.onShow() called");
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
        if (uiContext != null && uiContext.getClientState() != null && uiContext.getClientState().isInGame()) {
            Platform.runLater(() -> {
                System.out.println("[DEBUG] GuiShipBuildingView.refresh() using direct server models");
                
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
                        enhancedHandView.refresh();
                    }
                }
                
                // Update other players display
                updateOtherPlayers();
            });
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
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        flipTimerButton = new Button("Flip Timer");
        flipTimerButton.setOnAction(e -> controller.flipBuildingTimer());
        
        validateShipButton = new Button("Validate Ship");
        validateShipButton.setOnAction(e -> controller.validateShip());
        
        buttonBox.getChildren().addAll(flipTimerButton, validateShipButton);
        
        topPanel.getChildren().addAll(titleLabel, timerLabel, buttonBox);
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
        
        // Send placement request to server
        controller.placeTile(selectedComponent.getId(), row, col, selectedComponent.getCurrentDirection().ordinal());
        
        // Clear selection - server will update state via events
        selectedComponent = null;
        if (enhancedHandView != null) {
            enhancedHandView.clearSelection();
        }
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
                
                shipStatsLabel.setText(String.format(
                    "Ship Stats: Engines: %.1f, Cannons: %.1f, Crew: %d, Batteries: %d",
                    engines, cannons, crew, batteries
                ));
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

}