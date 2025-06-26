package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.client.core.UIRefreshable;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
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
 * Ship building grid using Simple Direct Model Architecture.
 */
public class ShipGridView extends StackPane implements UIRefreshable {
    
    public static final double DEFAULT_CELL_SIZE = 60;
    
    private GridPane cellGrid;
    private double cellSize;
    private StackPane[][] cellPanes;
    private ShipGridClickHandler clickHandler;
    private final UIContext uiContext;
    
    private int gridRows = 5; // Will be updated from server
    private int gridCols = 7; // Will be updated from server
    
    public interface ShipGridClickHandler {
        void onCellClicked(int row, int col);
        void onCellRightClicked(int row, int col);
    }
    
    public ShipGridView(UIContext uiContext, double cellSize, ShipGridClickHandler clickHandler) {
        this.uiContext = uiContext;
        this.cellSize = cellSize;
        this.clickHandler = clickHandler;
        
        getStyleClass().add("ship-grid-view");
        setAlignment(Pos.CENTER);
        
        updateGridDimensions();
        createGridUI();
    }
    
    public ShipGridView(UIContext uiContext, ShipGridClickHandler clickHandler) {
        this(uiContext, DEFAULT_CELL_SIZE, clickHandler);
    }
    
    // Constructor for mini-views that don't need UIContext functionality
    public ShipGridView(double cellSize, ShipGridClickHandler clickHandler) {
        this.uiContext = null; // Mini-views don't need full context
        this.cellSize = cellSize;
        this.clickHandler = clickHandler;
        
        getStyleClass().add("ship-grid-view");
        setAlignment(Pos.CENTER);
        
        updateGridDimensions();
        createGridUI();
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
        
        return true; // Basic validation - server will validate placement
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
    
    public void updateGridLayout() {
        cellGrid.getRowConstraints().clear();
        cellGrid.getColumnConstraints().clear();
        cellGrid.getChildren().clear();
        createGridUI();
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
     * Update forbidden positions for mini views (when not using LocalGameState)
     * This allows mini views to display forbidden positions from other players' perspectives
     */
    public void updateForbiddenPositions(Set<Position> forbiddenPositions) {
        if (cellPanes == null) return;
        
        // Reset all cells to valid state first
        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                StackPane cellPane = cellPanes[row][col];
                if (cellPane != null) {
                    cellPane.getStyleClass().removeAll("invalid-cell", "valid-cell");
                    cellPane.setDisable(false);
                    
                    // Re-apply appropriate styling
                    Position pos = new Position(row, col);
                    if (forbiddenPositions != null && forbiddenPositions.contains(pos)) {
                        cellPane.getStyleClass().add("invalid-cell");
                        cellPane.setDisable(true);
                        
                        // Add visual indicator for forbidden cells (especially useful in mini views)
                        boolean hasIndicator = cellPane.getChildren().stream()
                            .anyMatch(node -> node instanceof Label && "✕".equals(((Label) node).getText()));
                        
                        if (!hasIndicator) {
                            Label forbiddenIndicator = new Label("✕");
                            forbiddenIndicator.setStyle("-fx-text-fill: #666666; -fx-font-size: " + 
                                                       (cellSize < 40 ? "8px" : "12px") + "; -fx-font-weight: bold;");
                            forbiddenIndicator.setMouseTransparent(true);
                            cellPane.getChildren().add(forbiddenIndicator);
                            StackPane.setAlignment(forbiddenIndicator, Pos.CENTER);
                        }
                    } else {
                        cellPane.getStyleClass().add("valid-cell");
                        
                        // Remove any existing forbidden indicators
                        cellPane.getChildren().removeIf(node -> 
                            node instanceof Label && "✕".equals(((Label) node).getText()));
                    }
                }
            }
        }
    }
    
    /**
     * Mark specific cells as reservation areas (for mini views showing reserved components)
     */
    public void markReservationAreas(Set<Position> reservationPositions) {
        if (cellPanes == null || reservationPositions == null) return;
        
        for (Position pos : reservationPositions) {
            int row = pos.getRow();
            int col = pos.getCol();
            
            if (row >= 0 && row < gridRows && col >= 0 && col < gridCols) {
                StackPane cellPane = cellPanes[row][col];
                if (cellPane != null) {
                    cellPane.getStyleClass().add("reservation-area");
                    
                    // Add visual indicator for reservation areas
                    boolean hasIndicator = cellPane.getChildren().stream()
                        .anyMatch(node -> node instanceof Label && "R".equals(((Label) node).getText()));
                    
                    if (!hasIndicator) {
                        Label reservationIndicator = new Label("R");
                        reservationIndicator.setStyle("-fx-text-fill: #FFD700; -fx-font-size: " + 
                                                     (cellSize < 40 ? "6px" : "10px") + "; -fx-font-weight: bold;");
                        reservationIndicator.setMouseTransparent(true);
                        cellPane.getChildren().add(reservationIndicator);
                        StackPane.setAlignment(reservationIndicator, Pos.BOTTOM_RIGHT);
                        reservationIndicator.setTranslateX(-2);
                        reservationIndicator.setTranslateY(-2);
                    }
                }
            }
        }
    }
    
    /**
     * Get the starting cabin position (always at 2,3)
     */
    public Position getStartingCabinPosition() {
        return new Position(2, 3);
    }
    
    @Override
    public void refresh() {
        // Mini-views (uiContext == null) don't auto-refresh from client state
        if (uiContext != null && uiContext.getClientState().isInGame()) {
            Ship ship = uiContext.getClientState().getLocalPlayerShip();
            if (ship != null) {
                updateFromShip(ship);
            }
        }
    }
    
    public void updateFromShip(Ship ship) {
        if (ship == null) return;
        
        clearComponents();
        
        int shipRows = ship.getRows();
        int shipCols = ship.getCols();
        
        if (shipRows != gridRows || shipCols != gridCols) {
            gridRows = shipRows;
            gridCols = shipCols;
            updateGridLayout();
        }
        
        for (int row = 0; row < shipRows; row++) {
            for (int col = 0; col < shipCols; col++) {
                Component component = ship.getComponentAt(row, col);
                if (component != null) {
                    ComponentTileView componentView = createComponentTileView(component);
                    placeComponent(componentView, row, col);
                }
            }
        }
        
        updateForbiddenPositions(ship.getForbiddenPositions());
    }
    
    private ComponentTileView createComponentTileView(Component component) {
        return new ComponentTileView(component);
    }

    
    private void updateGridDimensions() {
        // Mini-views (uiContext == null) use default dimensions
        if (uiContext != null && uiContext.getClientState().isInGame()) {
            Ship ship = uiContext.getClientState().getLocalPlayerShip();
            if (ship != null) {
                gridRows = ship.getRows();
                gridCols = ship.getCols();
            }
        }
        // If uiContext is null (mini-view), keep default dimensions (5x7)
    }
    
    /**
     * Update connector visual feedback for placed components
     */
    private void updateConnectorFeedback(int row, int col) {
        ComponentTileView placedComponent = getComponentAt(row, col);
        if (placedComponent == null || placedComponent.getComponent() == null) {
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
            adjacentComponent.getComponent() != null && 
            sourceComponent.getComponent() != null) {
            
            // Visual feedback could be added here to show valid/invalid connections
            // For now, this serves as a hook for future connector visualization
        }
    }
    
    /**
     * Get all adjacent components for connection validation
     */
    public Component[] getAdjacentComponents(int row, int col) {
        Component[] adjacent = new Component[4];
        
        // UP, DOWN, LEFT, RIGHT
        ComponentTileView upComponent = getComponentAt(row - 1, col);
        ComponentTileView downComponent = getComponentAt(row + 1, col);
        ComponentTileView leftComponent = getComponentAt(row, col - 1);
        ComponentTileView rightComponent = getComponentAt(row, col + 1);
        
        adjacent[0] = upComponent != null ? upComponent.getComponent() : null;
        adjacent[1] = downComponent != null ? downComponent.getComponent() : null;
        adjacent[2] = leftComponent != null ? leftComponent.getComponent() : null;
        adjacent[3] = rightComponent != null ? rightComponent.getComponent() : null;
        
        return adjacent;
    }
    
    public boolean canPlaceComponentAt(Component component, int row, int col) {
        return isValidPosition(row, col) && getComponentAt(row, col) == null;
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
