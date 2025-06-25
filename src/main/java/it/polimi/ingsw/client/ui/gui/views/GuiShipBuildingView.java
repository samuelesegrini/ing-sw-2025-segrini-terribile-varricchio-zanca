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

import java.beans.PropertyChangeEvent;
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
        
        // Register with ClientState for automatic refresh
        if (uiContext != null && uiContext.getClientState() != null) {
            uiContext.getClientState().setCurrentView(
                uiContext.getClientState().getCurrentView(), 
                this
            );
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
                int engines = ship.countComponentsByType(it.polimi.ingsw.server.model.enums.ship.ComponentType.ENGINE);
                int cannons = ship.countComponentsByType(it.polimi.ingsw.server.model.enums.ship.ComponentType.CANNON);
                int crew = ship.countComponentsByType(it.polimi.ingsw.server.model.enums.ship.ComponentType.CREW);
                int cargo = ship.countComponentsByType(it.polimi.ingsw.server.model.enums.ship.ComponentType.CARGO);
                
                shipStatsLabel.setText(String.format(
                    "Ship Stats: Engines: %d, Cannons: %d, Crew: %d, Cargo: %d",
                    engines, cannons, crew, cargo
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

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        // Handle property changes for lobby/connection state
        refresh();
    }
}