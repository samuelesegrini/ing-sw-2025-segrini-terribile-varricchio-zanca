package it.polimi.ingsw.client.ui.gui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.common.message.request.*;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

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

/**
 * Functional GUI view for ship building phase with interactive ship grid.
 */
public class GuiShipBuildingView extends BaseUIView {
    private Stage stage;
    private ClientController controller;
    
    // UI Components
    private GridPane shipGrid;
    private ListView<ComponentType> availableTilesList;
    private ListView<ComponentType> heldTilesList;
    private Label timerLabel;
    private Label shipStatsLabel;
    private Button validateShipButton;
    private Button flipTimerButton;
    private Button takeTileButton;
    private Button takeFaceUpTileButton;
    private TextArea validationErrorsArea;
    
    // Ship grid cells for visual feedback
    private Rectangle[][] gridCells = new Rectangle[5][7];
    private Label[][] gridLabels = new Label[5][7];
    
    // Current selection and state
    private ComponentType selectedComponent = null;
    private boolean dragMode = false;

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
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Create main layout
        root.setTop(createTopPanel());
        root.setCenter(createCenterPanel());
        root.setRight(createRightPanel());
        root.setBottom(createBottomPanel());
        
        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setTitle("Galaxy Trucker - " + getTitle());
        
        // Initialize display
        updateShipDisplay();
        updateTimerDisplay();
        updateShipStats();
        updateAvailableTiles();
        
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

    private VBox createCenterPanel() {
        VBox centerPanel = new VBox(10);
        centerPanel.setAlignment(Pos.CENTER);
        
        Label gridLabel = new Label("Ship Grid (5x7)");
        gridLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        shipGrid = new GridPane();
        shipGrid.setAlignment(Pos.CENTER);
        shipGrid.setHgap(2);
        shipGrid.setVgap(2);
        initializeShipGrid();
        
        shipStatsLabel = new Label("Ship Stats: Engines: 0, Cannons: 0, Crew: 0, Cargo: 0");
        shipStatsLabel.setStyle("-fx-font-size: 14px;");
        
        centerPanel.getChildren().addAll(gridLabel, shipGrid, shipStatsLabel);
        return centerPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(10);
        rightPanel.setPrefWidth(250);
        rightPanel.setPadding(new Insets(10));
        
        // Available tiles section
        Label availableLabel = new Label("Available Tiles");
        availableLabel.setStyle("-fx-font-weight: bold;");
        
        availableTilesList = new ListView<>();
        availableTilesList.setPrefHeight(150);
        availableTilesList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> selectedComponent = newVal
        );
        
        HBox tileButtonBox = new HBox(5);
        takeTileButton = new Button("Take Random");
        takeTileButton.setOnAction(e -> handleTakeRandomTile());
        
        takeFaceUpTileButton = new Button("Take Selected");
        takeFaceUpTileButton.setOnAction(e -> handleTakeSelectedTile());
        
        tileButtonBox.getChildren().addAll(takeTileButton, takeFaceUpTileButton);
        
        // Held tiles section
        Label heldLabel = new Label("Held Tiles (Max 2)");
        heldLabel.setStyle("-fx-font-weight: bold;");
        
        heldTilesList = new ListView<>();
        heldTilesList.setPrefHeight(100);
        heldTilesList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> selectedComponent = newVal
        );
        
        Button returnTileButton = new Button("Return Selected");
        returnTileButton.setOnAction(e -> handleReturnTile());
        
        // Validation errors section
        Label errorsLabel = new Label("Validation Errors");
        errorsLabel.setStyle("-fx-font-weight: bold;");
        
        validationErrorsArea = new TextArea();
        validationErrorsArea.setPrefHeight(100);
        validationErrorsArea.setEditable(false);
        
        rightPanel.getChildren().addAll(
            availableLabel, availableTilesList, tileButtonBox,
            heldLabel, heldTilesList, returnTileButton,
            errorsLabel, validationErrorsArea
        );
        
        return rightPanel;
    }

    private HBox createBottomPanel() {
        HBox bottomPanel = new HBox(10);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(10));
        
        Label instructionsLabel = new Label(
            "Instructions: Select a tile from Available or Held lists, then click on ship grid to place. " +
            "Right-click on placed tiles to remove them."
        );
        instructionsLabel.setStyle("-fx-font-style: italic;");
        
        bottomPanel.getChildren().add(instructionsLabel);
        return bottomPanel;
    }

    private void initializeShipGrid() {
        LocalGameState gameState = LocalGameState.getInstance();
        
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 7; col++) {
                Rectangle cell = new Rectangle(60, 60);
                Label label = new Label();
                label.setAlignment(Pos.CENTER);
                label.setPrefSize(60, 60);
                
                Position pos = new Position(row, col);
                
                // Check if position is forbidden or starting cabin
                if (pos.equals(new Position(2, 3))) {
                    // Starting cabin position
                    cell.setFill(Color.LIGHTBLUE);
                    label.setText("START");
                    label.setStyle("-fx-font-size: 8px; -fx-font-weight: bold;");
                } else if (!gameState.canPlaceComponent(ComponentType.STRUCTURAL, pos)) {
                    // Forbidden position
                    cell.setFill(Color.LIGHTGRAY);
                    label.setText("X");
                    label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                } else {
                    // Normal position
                    cell.setFill(Color.WHITE);
                    cell.setStroke(Color.BLACK);
                    cell.setStrokeWidth(1);
                    
                    // Add click handler for tile placement
                    final int finalRow = row;
                    final int finalCol = col;
                    
                    cell.setOnMouseClicked(e -> {
                        if (e.getButton().toString().equals("PRIMARY")) {
                            handleCellClick(finalRow, finalCol);
                        } else if (e.getButton().toString().equals("SECONDARY")) {
                            handleCellRightClick(finalRow, finalCol);
                        }
                    });
                }
                
                gridCells[row][col] = cell;
                gridLabels[row][col] = label;
                
                StackPane cellPane = new StackPane(cell, label);
                shipGrid.add(cellPane, col, row);
            }
        }
    }

    private void handleCellClick(int row, int col) {
        if (selectedComponent == null) {
            showAlert("No Component Selected", "Please select a component from Available or Held tiles first.");
            return;
        }
        
        Position position = new Position(row, col);
        LocalGameState gameState = LocalGameState.getInstance();
        
        if (!gameState.canPlaceComponent(selectedComponent, position)) {
            showAlert("Invalid Placement", "Cannot place component at this position.");
            return;
        }
        
        // Send placement request to server
        controller.placeTile(selectedComponent.name(), row, col, 0);
        
        // Remove from held tiles if it was selected from there
        if (gameState.getHeldTiles().contains(selectedComponent)) {
            gameState.removeHeldTile(selectedComponent);
            updateHeldTiles();
        }
        
        selectedComponent = null;
        availableTilesList.getSelectionModel().clearSelection();
        heldTilesList.getSelectionModel().clearSelection();
    }

    private void handleCellRightClick(int row, int col) {
        Position position = new Position(row, col);
        LocalGameState gameState = LocalGameState.getInstance();
        ComponentType component = gameState.getComponentAt(position);
        
        if (component != null) {
            // Remove component and return to available tiles
            gameState.removeTile(position);
            gameState.addAvailableTile(component);
            
            updateShipDisplay();
            updateAvailableTiles();
            updateShipStats();
        }
    }

    private void handleTakeRandomTile() {
        controller.takeTile();
    }

    private void handleTakeSelectedTile() {
        ComponentType selected = availableTilesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            controller.requestFaceUpTile(selected.name());
        } else {
            showAlert("No Selection", "Please select a tile from the available tiles list.");
        }
    }

    private void handleReturnTile() {
        ComponentType selected = heldTilesList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            controller.returnTile(selected.name());
            
            LocalGameState.getInstance().removeHeldTile(selected);
            LocalGameState.getInstance().addAvailableTile(selected);
            
            updateHeldTiles();
            updateAvailableTiles();
        } else {
            showAlert("No Selection", "Please select a tile from the held tiles list.");
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
            LocalGameState gameState = LocalGameState.getInstance();
            Map<Position, ComponentType> shipGridData = gameState.getShipGrid();
            
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 7; col++) {
                    Position pos = new Position(row, col);
                    ComponentType component = shipGridData.get(pos);
                    
                    Rectangle cell = gridCells[row][col];
                    Label label = gridLabels[row][col];
                    
                    if (component != null && !pos.equals(new Position(2, 3))) {
                        // Component placed
                        cell.setFill(getComponentColor(component));
                        label.setText(getComponentAbbreviation(component));
                        label.setStyle("-fx-font-size: 8px; -fx-font-weight: bold;");
                    } else if (pos.equals(new Position(2, 3))) {
                        // Keep starting cabin style
                        cell.setFill(Color.LIGHTBLUE);
                        label.setText("START");
                    } else if (gameState.canPlaceComponent(ComponentType.STRUCTURAL, pos)) {
                        // Empty valid position
                        cell.setFill(Color.WHITE);
                        label.setText("");
                    }
                }
            }
        });
    }

    private void updateTimerDisplay() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            long timeRemaining = gameState.getBuildingTimeRemaining();
            
            if (timeRemaining > 0) {
                long minutes = timeRemaining / 60000;
                long seconds = (timeRemaining % 60000) / 1000;
                timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds));
            } else {
                timerLabel.setText("Time Remaining: --:--");
            }
            
            if (gameState.isBuildingTimerFlipped()) {
                timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: red;");
            } else {
                timerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: black;");
            }
        });
    }

    private void updateShipStats() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            Map<LocalGameState.ComponentStatType, Integer> stats = gameState.getAllShipStats();
            
            shipStatsLabel.setText(String.format(
                "Ship Stats - Engines: %d, Cannons: %d, Crew: %d, Cargo: %d, Batteries: %d, Shields: %d",
                stats.get(LocalGameState.ComponentStatType.ENGINES),
                stats.get(LocalGameState.ComponentStatType.CANNONS),
                stats.get(LocalGameState.ComponentStatType.CREW),
                stats.get(LocalGameState.ComponentStatType.CARGO),
                stats.get(LocalGameState.ComponentStatType.BATTERIES),
                stats.get(LocalGameState.ComponentStatType.SHIELDS)
            ));
        });
    }

    private void updateAvailableTiles() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            availableTilesList.getItems().clear();
            availableTilesList.getItems().addAll(gameState.getAvailableTiles());
        });
    }

    private void updateHeldTiles() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            heldTilesList.getItems().clear();
            heldTilesList.getItems().addAll(gameState.getHeldTiles());
        });
    }

    private void updateValidationErrors() {
        Platform.runLater(() -> {
            LocalGameState gameState = LocalGameState.getInstance();
            List<String> errors = gameState.getValidationErrors();
            
            if (errors.isEmpty()) {
                validationErrorsArea.setText("No validation errors");
                validationErrorsArea.setStyle("-fx-text-fill: green;");
            } else {
                validationErrorsArea.setText(String.join("\n", errors));
                validationErrorsArea.setStyle("-fx-text-fill: red;");
            }
        });
    }

    private Color getComponentColor(ComponentType component) {
        return switch (component) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> Color.ORANGE;
            case CANNON_SINGLE, CANNON_DOUBLE -> Color.RED;
            case CABIN, CABIN_START -> Color.YELLOW;
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> Color.BROWN;
            case BATTERY -> Color.PURPLE;
            case SHIELD -> Color.CYAN;
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> Color.PINK;
            case STRUCTURAL -> Color.LIGHTGRAY;
        };
    }

    private String getComponentAbbreviation(ComponentType component) {
        return switch (component) {
            case ENGINE_SINGLE -> "E1";
            case ENGINE_DOUBLE -> "E2";
            case CANNON_SINGLE -> "C1";
            case CANNON_DOUBLE -> "C2";
            case CABIN -> "CAB";
            case CABIN_START -> "START";
            case CARGO_HOLD -> "CRG";
            case CARGO_HOLD_SPECIAL -> "CRG+";
            case BATTERY -> "BAT";
            case SHIELD -> "SHD";
            case LIFE_SUPPORT_BROWN -> "LS-B";
            case LIFE_SUPPORT_PURPLE -> "LS-P";
            case STRUCTURAL -> "STR";
        };
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
    protected void onHide() {
        // GUI views don't need special hiding logic
    }

    @Override
    protected void onRefresh() {
        updateShipDisplay();
        updateTimerDisplay();
        updateShipStats();
        updateAvailableTiles();
        updateHeldTiles();
        updateValidationErrors();
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        switch (propertyName) {
            case "shipGridUpdated" -> updateShipDisplay();
            case "buildingTimeRemaining" -> updateTimerDisplay();
            case "buildingTimerFlipped" -> updateTimerDisplay();
            case "shipValidated", "shipValidationErrors" -> updateValidationErrors();
            case "tileConfirmed" -> {
                updateShipDisplay();
                updateShipStats();
            }
            case "heldTiles" -> updateHeldTiles();
            case "availableTiles" -> updateAvailableTiles();
            case "phaseTransition" -> {
                if ("FLIGHT".equals(evt.getNewValue())) {
                    showAlert("Phase Transition", "Building phase complete! Transitioning to flight phase.");
                }
            }
        }
    }
}