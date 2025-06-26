package it.polimi.ingsw.client.ui.gui.components;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure Component-based mini view for other players' ships.
 * Shows player avatar, name, and a scaled-down version of their ship with component details.
 * NO ComponentType support - Component only.
 */
public class PlayerMiniView extends VBox {
    
    public static final double MINI_AVATAR_SIZE = 40;
    public static final double MINI_CELL_SIZE = 25;
    public static final double MINI_VIEW_WIDTH = 230;
    public static final double MINI_VIEW_HEIGHT = 200;
    
    private final String playerId;
    private String playerName;
    private Circle playerAvatar;
    private Label playerNameLabel;
    private ShipGridView miniShipGrid;
    private Label shipStatsLabel;
    private UIContext context;
    
    public PlayerMiniView(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        
        getStyleClass().add("player-mini-view");
        setAlignment(Pos.CENTER);
        setSpacing(6);
        setPrefSize(MINI_VIEW_WIDTH, MINI_VIEW_HEIGHT);
        setMaxSize(MINI_VIEW_WIDTH, MINI_VIEW_HEIGHT);
        
        createUI();
    }
    
    /**
     * Sets the UI context for accessing client state
     * @param context The UI context
     */
    public void setContext(UIContext context) {
        this.context = context;
    }
    
    private void createUI() {
        // Player avatar and name section
        VBox playerInfo = new VBox(4);
        playerInfo.setAlignment(Pos.CENTER);
        playerInfo.getStyleClass().add("player-info-section");
        
        // Create avatar circle
        playerAvatar = new Circle(MINI_AVATAR_SIZE / 2);
        playerAvatar.setFill(generatePlayerColor(playerId));
        playerAvatar.setStroke(Color.WHITE);
        playerAvatar.setStrokeWidth(2);
        playerAvatar.getStyleClass().add("player-avatar");
        
        // Player name label
        playerNameLabel = new Label(playerName);
        playerNameLabel.getStyleClass().addAll("player-name-label", "mini-view-text");
        playerNameLabel.setStyle("-fx-font-size: 11pt; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Ship stats label
        shipStatsLabel = new Label("E:0 C:0 R:0");
        shipStatsLabel.getStyleClass().add("ship-stats-mini");
        shipStatsLabel.setStyle("-fx-font-size: 8pt; -fx-text-fill: #ccc;");
        
        playerInfo.getChildren().addAll(playerAvatar, playerNameLabel, shipStatsLabel);
        
        // Mini ship grid (non-interactive)
        miniShipGrid = new ShipGridView(MINI_CELL_SIZE, null); // No click handler for mini-views
        miniShipGrid.getStyleClass().add("mini-ship-grid");
        miniShipGrid.setDisable(true); // Make it non-interactive
        
        getChildren().addAll(playerInfo, miniShipGrid);
        
        // Set growth priorities
        VBox.setVgrow(playerInfo, javafx.scene.layout.Priority.NEVER);
        VBox.setVgrow(miniShipGrid, javafx.scene.layout.Priority.ALWAYS);
    }
    
    /**
     * Update the mini ship display with ship state only (no reserved components)
     */
    public void updateShipDisplay(Map<Position, Component> shipGrid) {
        updateShipDisplay(shipGrid, new ArrayList<>());
    }
    
    /**
     * Update the mini ship display with ship state and reserved components
     */
    public void updateShipDisplay(Map<Position, Component> shipGrid, List<Component> reservedComponents) {
        if (miniShipGrid == null || shipGrid == null) return;
        
        // Clear current ship
        miniShipGrid.clearComponents();
        
        // Update forbidden positions in the mini grid (same for all players in the game)
        Set<Position> forbiddenPositions = Collections.emptySet();
        if (context != null) {
            ClientState clientState = context.getClientState();
            forbiddenPositions = clientState.getLocalPlayerShip() != null ? 
                clientState.getLocalPlayerShip().getForbiddenPositions() : Collections.emptySet();
        }
        miniShipGrid.updateForbiddenPositions(forbiddenPositions);
        
        // Setup reservation areas in the first row (last two columns)
        setupReservationAreas(reservedComponents);
        
        // Component counters for stats
        int engines = 0, cannons = 0, crew = 0, cargo = 0, batteries = 0, shields = 0;
        
        // Place components according to ship state
        for (Map.Entry<Position, Component> entry : shipGrid.entrySet()) {
            Position pos = entry.getKey();
            Component component = entry.getValue();
            
            if (component != null) {
                ComponentTileView tileView = new ComponentTileView(component);
                tileView.setPlaced(true);
                
                // Scale down the tile for mini-view
                tileView.setPrefSize(MINI_CELL_SIZE * 0.9, MINI_CELL_SIZE * 0.9);
                tileView.setMaxSize(MINI_CELL_SIZE * 0.9, MINI_CELL_SIZE * 0.9);
                tileView.getStyleClass().add("mini-tile");
                
                miniShipGrid.placeComponent(tileView, pos.getRow(), pos.getCol());
                
                // Count components for stats display
                switch (component.getType()) {
                    case ENGINE_SINGLE -> engines += 1;
                    case ENGINE_DOUBLE -> engines += 2;
                    case CANNON_SINGLE -> cannons += 1;
                    case CANNON_DOUBLE -> cannons += 2;
                    case CABIN, CABIN_START -> crew += 1;
                    case CARGO_HOLD -> cargo += 1;
                    case CARGO_HOLD_SPECIAL -> cargo += 2;
                    case BATTERY -> batteries += 1;
                    case SHIELD -> shields += 1;
                }
            }
        }
        
        // Update stats display
        shipStatsLabel.setText(String.format("E:%d C:%d R:%d G:%d B:%d S:%d", 
                                            engines, cannons, crew, cargo, batteries, shields));
    }
    
    /**
     * Setup reservation areas and display reserved components
     */
    private void setupReservationAreas(List<Component> reservedComponents) {
        int gridCols = 8; // Default grid cols, will be updated if context is available
        if (context != null) {
            ClientState clientState = context.getClientState();
            if (clientState.getCurrentGame() != null && clientState.getLocalPlayerShip() != null) {
                gridCols = clientState.getLocalPlayerShip().getCols();
            }
        }
        
        // Define reservation positions (last two columns of first row)
        Position[] reservationPositions = {
            new Position(0, gridCols - 2), // Second to last column, first row
            new Position(0, gridCols - 1)  // Last column, first row
        };
        
        // Mark reservation areas in the ship grid
        miniShipGrid.markReservationAreas(Set.of(reservationPositions));
        
        // Place reserved components in reservation areas
        for (int i = 0; i < Math.min(reservedComponents.size(), reservationPositions.length); i++) {
            Component component = reservedComponents.get(i);
            Position reservationPos = reservationPositions[i];
            
            ComponentTileView tileView = new ComponentTileView(component);
            tileView.setPlaced(false); // Not actually placed on ship, just reserved
            
            // Scale down for mini-view and add special styling
            tileView.setPrefSize(MINI_CELL_SIZE * 0.8, MINI_CELL_SIZE * 0.8);
            tileView.setMaxSize(MINI_CELL_SIZE * 0.8, MINI_CELL_SIZE * 0.8);
            tileView.getStyleClass().addAll("mini-tile", "reserved-component");
            
            // Set semi-transparent to indicate it's reserved, not placed
            tileView.setOpacity(0.8);
            
            miniShipGrid.placeComponent(tileView, reservationPos.getRow(), reservationPos.getCol());
        }
    }
    
    
    /**
     * Complete update method with ship grid, forbidden positions, and reserved components
     */
    public void updateShipDisplay(Map<Position, Component> shipGrid, Set<Position> forbiddenPositions, List<Component> reservedComponents) {
        if (miniShipGrid == null || shipGrid == null) return;
        
        // Clear current ship
        miniShipGrid.clearComponents();
        
        // Update forbidden positions in the mini grid
        miniShipGrid.updateForbiddenPositions(forbiddenPositions);
        
        // Setup reservation areas and components
        setupReservationAreas(reservedComponents);
        
        // Component counters for stats
        int engines = 0, cannons = 0, crew = 0, cargo = 0, batteries = 0, shields = 0;
        
        // Place components according to ship state
        for (Map.Entry<Position, Component> entry : shipGrid.entrySet()) {
            Position pos = entry.getKey();
            Component component = entry.getValue();
            
            if (component != null) {
                ComponentTileView tileView = new ComponentTileView(component);
                tileView.setPlaced(true);
                
                // Scale down the tile for mini-view
                tileView.setPrefSize(MINI_CELL_SIZE * 0.9, MINI_CELL_SIZE * 0.9);
                tileView.setMaxSize(MINI_CELL_SIZE * 0.9, MINI_CELL_SIZE * 0.9);
                tileView.getStyleClass().add("mini-tile");
                
                miniShipGrid.placeComponent(tileView, pos.getRow(), pos.getCol());
                
                // Count components for stats display
                switch (component.getType()) {
                    case ENGINE_SINGLE -> engines += 1;
                    case ENGINE_DOUBLE -> engines += 2;
                    case CANNON_SINGLE -> cannons += 1;
                    case CANNON_DOUBLE -> cannons += 2;
                    case CABIN, CABIN_START -> crew += 1;
                    case CARGO_HOLD -> cargo += 1;
                    case CARGO_HOLD_SPECIAL -> cargo += 2;
                    case BATTERY -> batteries += 1;
                    case SHIELD -> shields += 1;
                }
            }
        }
        
        // Update stats display
        shipStatsLabel.setText(String.format("E:%d C:%d R:%d G:%d B:%d S:%d", 
                                            engines, cannons, crew, cargo, batteries, shields));
    }
    
    /**
     * Update player information (name, avatar color)
     */
    public void updatePlayerInfo(String newPlayerName) {
        if (newPlayerName != null && !newPlayerName.equals(this.playerName)) {
            this.playerName = newPlayerName;
            playerNameLabel.setText(newPlayerName);
        }
    }
    
    /**
     * Generate a consistent color for a player based on their ID
     */
    private Color generatePlayerColor(String playerId) {
        if (playerId == null) return Color.GRAY;
        
        // Generate consistent color based on player ID hash
        int hash = Math.abs(playerId.hashCode());
        
        // Predefined set of appealing colors for players
        Color[] playerColors = {
            Color.LIGHTBLUE,
            Color.LIGHTGREEN,
            Color.ORANGE,
            Color.MEDIUMPURPLE,
            Color.PINK,
            Color.LIGHTYELLOW,
            Color.LIGHTCORAL,
            Color.LIGHTCYAN,
            Color.GOLD,
            Color.LIGHTSTEELBLUE
        };
        
        return playerColors[hash % playerColors.length];
    }
    
    /**
     * Highlight this mini-view (e.g., when it's the player's turn)
     */
    public void setHighlighted(boolean highlighted) {
        if (highlighted) {
            getStyleClass().add("highlighted-mini-view");
            playerAvatar.setStroke(Color.GOLD);
            playerAvatar.setStrokeWidth(3);
            setStyle("-fx-effect: dropshadow(gaussian, gold, 10, 0.6, 0, 0);");
        } else {
            getStyleClass().remove("highlighted-mini-view");
            playerAvatar.setStroke(Color.WHITE);
            playerAvatar.setStrokeWidth(2);
            setStyle("");
        }
    }
    
    /**
     * Set player status (building, waiting, ready, etc.)
     */
    public void setPlayerStatus(String status) {
        if (status != null && !status.isEmpty()) {
            playerNameLabel.setText(playerName + " (" + status + ")");
        } else {
            playerNameLabel.setText(playerName);
        }
    }
    
    /**
     * Show validation status of the player's ship
     */
    public void setShipValidated(boolean isValid) {
        if (isValid) {
            shipStatsLabel.setStyle("-fx-font-size: 8pt; -fx-text-fill: #90EE90;"); // Light green
            shipStatsLabel.setText(shipStatsLabel.getText() + " ✓");
        } else {
            shipStatsLabel.setStyle("-fx-font-size: 8pt; -fx-text-fill: #ccc;");
        }
    }
    
    /**
     * Get the player ID this mini-view represents
     */
    public String getPlayerId() {
        return playerId;
    }
    
    /**
     * Get the player name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Get the mini ship grid for advanced operations
     */
    public ShipGridView getMiniShipGrid() {
        return miniShipGrid;
    }
    
    /**
     * Update the mini-view with all available data
     */
    public void updatePlayerData(String name, Map<Position, Component> shipGrid, List<Component> reservedComponents, String status, boolean isValidated) {
        updatePlayerInfo(name);
        updateShipDisplay(shipGrid, reservedComponents);
        setPlayerStatus(status);
        setShipValidated(isValidated);
    }
    
    /**
     * Set the size of the mini-view (useful for different layouts)
     */
    public void setMiniViewSize(double width, double height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
    }
    
    /**
     * Update player information from Player object
     * @param player The player to update from
     */
    public void updatePlayer(it.polimi.ingsw.server.model.domain.player.Player player) {
        if (player != null) {
            updatePlayerInfo(player.getNickname()); // Use getNickname() method
            setPlayerStatus(player.isReady() ? "Ready" : "Not Ready");
        }
    }
    
    /**
     * Update ship display from Ship object
     * @param ship The ship to display
     */
    public void updateShip(it.polimi.ingsw.server.model.domain.ship.Ship ship) {
        if (ship != null) {
            // Convert ship board to position map
            Map<Position, Component> shipGrid = convertBoardToPositionMap(ship);
            
            // Get reserved components as list (convert Set to List)
            List<Component> reservedComponents = ship.getReservedComponents() != null ? 
                new ArrayList<>(ship.getReservedComponents()) : new ArrayList<>();
            
            // Update the ship display
            updateShipDisplay(shipGrid, reservedComponents);
        }
    }
    
    /**
     * Convert ship board array to Position-Component map
     */
    private Map<Position, Component> convertBoardToPositionMap(it.polimi.ingsw.server.model.domain.ship.Ship ship) {
        Map<Position, Component> shipGrid = new HashMap<>();
        Component[][] board = ship.getBoard();
        
        if (board != null) {
            for (int row = 0; row < ship.getRows(); row++) {
                for (int col = 0; col < ship.getCols(); col++) {
                    Component component = board[row][col];
                    if (component != null) {
                        shipGrid.put(new Position(row, col), component);
                    }
                }
            }
        }
        
        return shipGrid;
    }
    
    @Override
    public String toString() {
        return "PlayerMiniView{" +
                "playerId='" + playerId + '\'' +
                ", playerName='" + playerName + '\'' +
                '}';
    }
}