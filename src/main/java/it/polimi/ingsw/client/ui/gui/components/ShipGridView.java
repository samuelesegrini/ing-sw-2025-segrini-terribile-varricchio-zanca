package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import java.util.Set;

/**
 * Pure ComponentInstance-based ship building grid.
 * Displays components with connectors, rotation, and placement validation.
 * NO ComponentType support - ComponentInstance only.
 */
public class ShipGridView extends StackPane {
    
    public static final double DEFAULT_CELL_SIZE = 60;
    
    private GridPane cellGrid;
    private double cellSize;
    private StackPane[][] cellPanes;
    private ShipGridClickHandler clickHandler;
    private LocalGameState gameState;
    private int gridRows = 5; // Will be updated from server
    private int gridCols = 7; // Will be updated from server
    
    public interface ShipGridClickHandler {
        void onCellClicked(int row, int col);
        void onCellRightClicked(int row, int col);
    }
    
    public ShipGridView(double cellSize, ShipGridClickHandler clickHandler) {
        this.cellSize = cellSize;
        this.clickHandler = clickHandler;
        this.gameState = LocalGameState.getInstance();
        
        getStyleClass().add("ship-grid-view");
        setAlignment(Pos.CENTER);
        
        updateGridDimensions();
        createGridUI();
    }
    
    public ShipGridView(ShipGridClickHandler clickHandler) {
        this(DEFAULT_CELL_SIZE, clickHandler);
    }
    
    private void createGridUI() {
        cellGrid = new GridPane();
        cellGrid.getStyleClass().add("ship-grid");
        cellGrid.setAlignment(Pos.CENTER);
        cellGrid.setHgap(2);
        cellGrid.setVgap(2);
        
        // Set up grid constraints using server dimensions
        for (int i = 0; i < gridRows; i++) {
            cellGrid.getRowConstraints().add(
                new RowConstraints(cellSize, cellSize, cellSize, Priority.NEVER, VPos.CENTER, true)
            );
        }
        
        for (int i = 0; i < gridCols; i++) {
            cellGrid.getColumnConstraints().add(
                new ColumnConstraints(cellSize, cellSize, cellSize, Priority.NEVER, HPos.CENTER, true)
            );
        }
        
        // Create cells
        createCells();
        
        getChildren().add(cellGrid);
    }
    
    private void createCells() {
        cellPanes = new StackPane[gridRows][gridCols];
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                StackPane cellPane = createCellPane(row, col);
                cellPanes[row][col] = cellPane;
                cellGrid.add(cellPane, col, row);
            }
        }
    }
    
    private StackPane createCellPane(int row, int col) {
        StackPane cellPane = new StackPane();
        cellPane.setPrefSize(cellSize, cellSize);
        cellPane.setMinSize(cellSize, cellSize);
        cellPane.getStyleClass().add("ship-grid-cell");
        
        Position position = new Position(row, col);
        
        // Starting cabin position (2,3)
        if (row == 2 && col == 3) {
            cellPane.getStyleClass().add("starting-cabin");
            Label cabinLabel = new Label("START");
            cabinLabel.getStyleClass().add("cabin-label");
            cabinLabel.setFont(Font.font("Arial", FontWeight.BOLD, cellSize * 0.2));
            cabinLabel.setTextFill(Color.WHITE);
            cellPane.getChildren().add(cabinLabel);
        }
        // Check if position is valid for placement
        else if (isValidPosition(row, col)) {
            cellPane.getStyleClass().add("valid-cell");
        } else {
            cellPane.getStyleClass().add("invalid-cell");
            cellPane.setDisable(true);
        }
        
        // Add click handlers
        if (clickHandler != null && isValidPosition(row, col)) {
            cellPane.setOnMouseClicked(event -> {
                if (event.getButton().toString().equals("PRIMARY")) {
                    clickHandler.onCellClicked(row, col);
                } else if (event.getButton().toString().equals("SECONDARY")) {
                    clickHandler.onCellRightClicked(row, col);
                }
                event.consume();
            });
        }
        
        return cellPane;
    }
    
    /**
     * Check if a position is valid for component placement using server configuration
     */
    private boolean isValidPosition(int row, int col) {
        // Check basic bounds with server dimensions
        if (row < 0 || row >= gridRows || col < 0 || col >= gridCols) {
            return false;
        }
        
        // Use server-provided forbidden positions
        if (gameState != null) {
            Position position = new Position(row, col);
            return !gameState.getForbiddenPositions().contains(position);
        }
        
        return true; // Default to valid if no server configuration
    }
    
    /**
     * Place a component at the specified position with connector validation
     */
    public boolean placeComponent(ComponentTileView componentView, int row, int col) {
        if (!isValidPosition(row, col)) {
            return false;
        }
        
        StackPane cellPane = cellPanes[row][col];
        
        // Check if cell is already occupied
        boolean hasComponent = cellPane.getChildren().stream()
            .anyMatch(node -> node instanceof ComponentTileView);
            
        if (hasComponent) {
            return false;
        }
        
        // Mark component as placed
        componentView.setPlaced(true);
        
        cellPane.getChildren().add(componentView);
        StackPane.setAlignment(componentView, Pos.CENTER);
        
        // Update connector connections visual feedback
        updateConnectorFeedback(row, col);
        
        return true;
    }
    
    /**
     * Remove component from the specified position
     */
    public ComponentTileView removeComponent(int row, int col) {
        if (!isValidPosition(row, col)) {
            return null;
        }
        
        StackPane cellPane = cellPanes[row][col];
        
        for (Node node : cellPane.getChildren()) {
            if (node instanceof ComponentTileView) {
                cellPane.getChildren().remove(node);
                return (ComponentTileView) node;
            }
        }
        
        return null;
    }
    
    /**
     * Get component at the specified position
     */
    public ComponentTileView getComponentAt(int row, int col) {
        if (!isValidPosition(row, col)) {
            return null;
        }
        
        StackPane cellPane = cellPanes[row][col];
        
        for (Node node : cellPane.getChildren()) {
            if (node instanceof ComponentTileView) {
                return (ComponentTileView) node;
            }
        }
        
        return null;
    }
    
    /**
     * Clear all components from the grid
     */
    public void clearComponents() {
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                removeComponent(row, col);
            }
        }
    }
    
    /**
     * Efficiently update grid from component data without full clear/rebuild
     */
    public void updateFromGridData(ComponentInstance[][] gridData) {
        if (gridData == null) return;
        
        // Track which positions need updates
        for (int row = 0; row < Math.min(gridRows, gridData.length); row++) {
            for (int col = 0; col < Math.min(gridCols, gridData[row].length); col++) {
                ComponentInstance newComponent = gridData[row][col];
                ComponentTileView existingView = getComponentAt(row, col);
                
                // Check if we need to update this position
                if (newComponent == null && existingView != null) {
                    // Remove component that's no longer there
                    removeComponent(row, col);
                } else if (newComponent != null) {
                    // Check if component changed
                    if (existingView == null || 
                        !newComponent.getId().equals(existingView.getComponentId()) ||
                        !newComponent.getCurrentDirection().equals(existingView.getComponentInstance().getCurrentDirection())) {
                        
                        // Remove old component if exists
                        if (existingView != null) {
                            removeComponent(row, col);
                        }
                        
                        // Add new component
                        ComponentTileView newView = new ComponentTileView(newComponent);
                        newView.setPlaced(true);
                        placeComponent(newView, row, col);
                    }
                }
            }
        }
    }
    
    /**
     * Highlight a cell (for drag and drop feedback)
     */
    public void highlightCell(int row, int col, boolean highlight) {
        if (!isValidPosition(row, col)) {
            return;
        }
        
        StackPane cellPane = cellPanes[row][col];
        if (highlight) {
            cellPane.getStyleClass().add("highlighted");
        } else {
            cellPane.getStyleClass().remove("highlighted");
        }
    }
    
    /**
     * Clear all cell highlights
     */
    public void clearHighlights() {
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                highlightCell(row, col, false);
            }
        }
    }
    
    /**
     * Update the grid layout based on server configuration
     * Call this when ship grid configuration is received from server
     */
    public void updateGridLayout() {
        // Update dimensions from server
        int newRows = gameState != null ? gameState.getGridRows() : 5;
        int newCols = gameState != null ? gameState.getGridCols() : 7;
        
        // Only rebuild if dimensions actually changed
        if (newRows != gridRows || newCols != gridCols) {
            gridRows = newRows;
            gridCols = newCols;
            
            // Clear constraints and recreate with new dimensions
            cellGrid.getRowConstraints().clear();
            cellGrid.getColumnConstraints().clear();
            cellGrid.getChildren().clear();
            
            // Recreate UI with new dimensions
            createGridUI();
        }
    }
    
    /**
     * Refresh grid cell styling to reflect updated forbidden positions
     */
    public void refreshCellStyling() {
        // Force recreate cells to apply updated forbidden positions
        cellGrid.getChildren().clear();
        createCells();
    }
    
    /**
     * Get the starting cabin position (always at 2,3)
     */
    public Position getStartingCabinPosition() {
        return new Position(2, 3);
    }
    
    /**
     * Update grid from server-provided game state
     * This is the key method for the perfect initialization flow
     */
    public void updateFromGameState(LocalGameState gameState) {
        if (gameState == null) return;
        
        // 1. Apply server-provided ship grid configuration
        ShipGridConfig config = gameState.getShipGridConfig();
        if (config != null) {
            updateGridLayoutFromConfig(config);
        }
        
        // 2. Apply forbidden positions from server
        updateGridLayout(); // Recreate cells with new forbidden positions
        
        // 3. Place existing components from server state
        updateComponentPlacements(gameState);
        
        // 4. Update background image if provided
        updateBackgroundImage(gameState);
    }
    
    private void updateGridLayoutFromConfig(ShipGridConfig config) {
        // Update forbidden positions are already handled in LocalGameState
        // This method can be extended for other config-specific updates
    }
    
    private void updateComponentPlacements(LocalGameState gameState) {
        // Clear existing components first
        clearComponents();
        
        // Get ship grid as 2D array of ComponentInstances
        ComponentInstance[][] shipGrid = gameState.getShipGrid();
        
        for (int row = 0; row < gameState.getGridRows(); row++) {
            for (int col = 0; col < gameState.getGridCols(); col++) {
                ComponentInstance component = shipGrid[row][col];
                if (component != null) {
                    // Create component view with unique instance and place it
                    ComponentTileView componentView = new ComponentTileView(component);
                    placeComponent(componentView, row, col);
                }
            }
        }
    }
    
    private void updateBackgroundImage(LocalGameState gameState) {
        String backgroundImage = gameState.getShipGridBackgroundImage();
        if (backgroundImage != null) {
            // Apply CSS background image based on game level
            String imageUrl = getClass().getResource("/images/" + backgroundImage).toExternalForm();
            setStyle("-fx-background-image: url('" + imageUrl + "'); " +
                    "-fx-background-size: contain; " +
                    "-fx-background-repeat: no-repeat; " +
                    "-fx-background-position: center;");
        }
    }
    
    private void updateGridDimensions() {
        if (gameState != null) {
            gridRows = gameState.getGridRows();
            gridCols = gameState.getGridCols();
        }
    }
    
    /**
     * Update connector visual feedback for placed components
     */
    private void updateConnectorFeedback(int row, int col) {
        ComponentTileView placedComponent = getComponentAt(row, col);
        if (placedComponent == null || placedComponent.getComponentInstance() == null) {
            return;
        }
        
        // Check all adjacent cells for connection compatibility
        checkAndUpdateConnection(row - 1, col, row, col); // UP
        checkAndUpdateConnection(row + 1, col, row, col); // DOWN
        checkAndUpdateConnection(row, col - 1, row, col); // LEFT
        checkAndUpdateConnection(row, col + 1, row, col); // RIGHT
    }
    
    private void checkAndUpdateConnection(int adjRow, int adjCol, int sourceRow, int sourceCol) {
        ComponentTileView adjacentComponent = getComponentAt(adjRow, adjCol);
        ComponentTileView sourceComponent = getComponentAt(sourceRow, sourceCol);
        
        if (adjacentComponent != null && sourceComponent != null && 
            adjacentComponent.getComponentInstance() != null && 
            sourceComponent.getComponentInstance() != null) {
            
            // Visual feedback could be added here to show valid/invalid connections
            // For now, this serves as a hook for future connector visualization
        }
    }
    
    /**
     * Get all adjacent components for connection validation
     */
    public ComponentInstance[] getAdjacentComponents(int row, int col) {
        ComponentInstance[] adjacent = new ComponentInstance[4];
        
        // UP, DOWN, LEFT, RIGHT
        ComponentTileView upComponent = getComponentAt(row - 1, col);
        ComponentTileView downComponent = getComponentAt(row + 1, col);
        ComponentTileView leftComponent = getComponentAt(row, col - 1);
        ComponentTileView rightComponent = getComponentAt(row, col + 1);
        
        adjacent[0] = upComponent != null ? upComponent.getComponentInstance() : null;
        adjacent[1] = downComponent != null ? downComponent.getComponentInstance() : null;
        adjacent[2] = leftComponent != null ? leftComponent.getComponentInstance() : null;
        adjacent[3] = rightComponent != null ? rightComponent.getComponentInstance() : null;
        
        return adjacent;
    }
    
    /**
     * Validate if a component can be placed at the given position based on connector rules
     */
    public boolean canPlaceComponentAt(ComponentInstance component, int row, int col) {
        if (!isValidPosition(row, col) || getComponentAt(row, col) != null) {
            return false;
        }
        
        // Use game state for advanced placement validation
        if (gameState != null) {
            Position position = new Position(row, col);
            return gameState.canPlaceComponent(component, position);
        }
        
        return true; // Basic validation passed
    }
    
    /**
     * Get grid dimensions
     */
    public int getGridRows() {
        return gridRows;
    }
    
    public int getGridCols() {
        return gridCols;
    }
}
