package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.CargoHold;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.common.message.event.*;

import it.polimi.ingsw.server.model.domain.ship.components.Battery;
import it.polimi.ingsw.server.model.domain.ship.components.Shield;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.*;

public class Ship implements Serializable {
    private final static long serialVersionUID = 1L;

    private GameLevel level;
    private Component[][] board;
    public Set<Position> forbiddenPositions;
    private Set<Component> reservedComponents;
    private Set<Component> lostComponents;
    private final int maxReservedComponents; // Max reserved components from configuration
    
    // PropertyChangeSupport for event firing
    private transient PropertyChangeSupport propertyChangeSupport;
    private String gameId; // For event context
    private PlayerId playerId; // For event context
    private String playerNickname; // For event context

    // Ship stats
    private double cannons;
    private double engines;
    private int batteries;
    private int crew;
    private int shields;
    private int lifeSupport;
    private Map<GoodType, Integer> resources;
    private int specialGoods;
    private int normalGoods;

    private int specialGoodsCapacity;
    private int normalGoodsCapacity;




    /**
     * Creates a new ship with the specified level and ship grid configuration.
     * 
     * @param level The game level (TEST_FLIGHT, LEVEL_II, etc.)
     * @param shipGridConfig The ship grid configuration containing forbidden positions
     */
    public Ship(GameLevel level, it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig shipGridConfig) {
        this.level = level;
        this.board = new Component[shipGridConfig.rows()][shipGridConfig.cols()];
        reservedComponents = new HashSet<>();
        lostComponents = new HashSet<>();
        
        // Set maximum reserved components from configuration
        this.maxReservedComponents = shipGridConfig.reservedComponentsPositions().size();
        
        // Initialize PropertyChangeSupport
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.gameId = null; // Will be set when associated with game
        this.playerId = null; // Will be set when associated with player
        this.playerNickname = null;
        
        this.resources = new HashMap<>() {{
            put(GoodType.RED, 0);
            put(GoodType.BLUE, 0);
            put(GoodType.GREEN, 0);
            put(GoodType.YELLOW, 0);
        }};
        normalGoodsCapacity = 0;
        specialGoodsCapacity = 0;
        normalGoods = 0;
        specialGoods = 0;
        
        // Load forbidden positions from configuration
        forbiddenPositions = new HashSet<>();
        for (var posConfig : shipGridConfig.forbiddenPositions()) {
            forbiddenPositions.add(new Position(posConfig.x(), posConfig.y()));
        }
        
    }

    /**
     * Sets the game context for event firing.
     */
    public void setGameContext(String gameId, PlayerId playerId, String playerNickname) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.playerNickname = playerNickname;
    }

    /**
     * Adds a PropertyChangeListener to this ship.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from this ship.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Fires a PropertyChangeEvent with the given property name and new event.
     */
    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Adds a component to the ship at the specified position on the board.
     *
     * @param component The component to add.
     * @param position  The position on the grid where the component should be placed.
     */
    public void addComponent(Component component, Position position) {
        int row = position.getRow();
        int col = position.getCol();

        // Checks if position is illegal, otherwise adds Component and updates its position attribute
        if (forbiddenPositions.contains(position)) {
            throw new IllegalArgumentException("Forbidden position");
        } else if (board[row][col] != null) {
            throw new IllegalArgumentException("Occupied position");
        } else {
            board[row][col] = component;
            component.setPosition(position);
            component.setShip(this);
            
            // Note: ComponentPlacedEvent should be fired by the calling code (like GameSession)
            // that has access to the full Player object and ComponentDeck
        }
    }

    /**
     * Removes the component from the ship at the specified position on the board.
     *
     * @param position The position on the grid from which the component should be removed.
     */
    public void removeComponent(Position position, GamePhase phase) {
        int row = position.getRow();
        int col = position.getCol();

        if (forbiddenPositions.contains(position)) {
            throw new IllegalArgumentException("Forbidden position");
        } else if (board[row][col] == null) {
            throw new IllegalArgumentException("Empty position");
        } else {
            Component componentToRemove = board[row][col];
            
            // Galaxy Trucker Rule: "Any playing pieces (crew, battery tokens, goods) on lost components are returned to the bank"
            handleComponentDestruction(componentToRemove);
            
            componentToRemove.setPosition(null);
            board[row][col] = null;
            
            // Fire ComponentRemovedEvent when component is removed from ship
            String reason = (phase == GamePhase.FLIGHT) ? "Combat damage" : "Manual removal";
            ComponentRemovedEvent event = new ComponentRemovedEvent(gameId, playerId, playerNickname, componentToRemove, position, reason);
            firePropertyChange("eventPublished", null, event);
        }
    }
    
    /**
     * Handles the destruction of a component according to Galaxy Trucker rules.
     * When components are destroyed, any resources (goods, batteries, crew) on them are lost.
     * 
     * @param component The component being destroyed
     */
    private void handleComponentDestruction(Component component) {
        if (component == null) return;
        
        switch (component.getType()) {
            case CARGO_HOLD:
            case CARGO_HOLD_SPECIAL:
                // Cargo holds lose all stored goods when destroyed
                CargoHold cargoHold = (CargoHold) component;
                Map<GoodType, Integer> lostGoods = cargoHold.getStoredGoods();
                
                int totalLost = 0;
                for (Map.Entry<GoodType, Integer> entry : lostGoods.entrySet()) {
                    totalLost += entry.getValue();
                }
                
                if (totalLost > 0) {
                    System.out.println("CARGO DESTROYED: Lost " + totalLost + " goods when cargo hold was destroyed!");
                    
                    // Clear all goods from the cargo hold (they're returned to the bank per rules)
                    cargoHold.setStoredGoods(new HashMap<>());
                    
                    // Update ship's total resource counts
                    updateStats();
                }
                break;
                
            case BATTERY:
                // Batteries lose stored energy when destroyed - return to bank
                Battery battery = (Battery) component;
                int lostBatteries = battery.getAvailableBatteries();
                if (lostBatteries > 0) {
                    System.out.println("BATTERY DESTROYED: Lost " + lostBatteries + " battery tokens!");
                    battery.consumeBatteries(lostBatteries); // Permanently remove all batteries
                }
                break;
                
            case CABIN:
                // Cabins lose crew when destroyed (handled elsewhere in crew management)
                System.out.println("CABIN DESTROYED: Any crew in this cabin are lost!");
                break;
                
            default:
                // Other components don't store resources
                break;
        }
    }

    /**
     * Reserves a component for future use, allowing it to be kept aside without attaching it to the ship.
     * The maximum number of reserved components is determined by the game level configuration.
     * If the maximum is already reached, the reservation fails.
     *
     * @param component The component to reserve.
     * @return true if the component was successfully reserved, false if maximum limit reached
     */
    public boolean reserveComponent(Component component) {
        if (component == null) {
            return false;
        }
        
        if (reservedComponents.size() < maxReservedComponents) {
            boolean added = reservedComponents.add(component);
            if (added) {
                // Note: ComponentReservedEvent should be fired by the calling code (like GameSession)
                // that has access to the full Player object and ComponentDeck
            }
            return added;
        } else {
            // Maximum reserved components reached - cannot add more
            return false;
        }
    }


    // Calls count() for each component on the board
    public void updateStats() {
        // Set all stats to zero
        cannons = 0.0;
        engines = 0.0;
        batteries = 0;
        crew = 0;
        specialGoods = 0;
        normalGoods = 0;
        resources.replaceAll((t, v) -> 0);

        for (Component[] components : board) {
            for (Component component : components) {
                if (component != null) {
                    component.count(this);
                }
            }
        }
        
        // Fire ShipStatsUpdatedEvent when stats are recalculated
        ShipStatsUpdatedEvent event = new ShipStatsUpdatedEvent(gameId, playerId, playerNickname, this);
        firePropertyChange("eventPublished", null, event);
    }
    public GameLevel getLevel() { return level; }

    public void setLevel(GameLevel level) {
        this.level = level;
    }

    public Component[][] getBoard() {
        return board;
    }
    
    /**
     * Gets the number of rows in the ship grid
     * @return The number of rows
     */
    public int getRows() {
        return board.length;
    }
    
    /**
     * Gets the number of columns in the ship grid
     * @return The number of columns
     */
    public int getCols() {
        return board[0].length;
    }
    
    /**
     * Gets the component at the specified position
     * @param row The row index
     * @param col The column index
     * @return The component at that position, or null if empty
     */
    public Component getComponentAt(int row, int col) {
        if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
            return board[row][col];
        }
        return null;
    }
    
    /**
     * Gets the set of forbidden positions on this ship
     * @return The set of forbidden positions
     */
    public Set<Position> getForbiddenPositions() {
        return Collections.unmodifiableSet(forbiddenPositions);
    }

    public Set<Component> getReservedComponents() {
        return reservedComponents;
    }
    
    /**
     * Gets the maximum number of components that can be reserved based on game configuration
     * @return The maximum number of reserved components allowed
     */
    public int getMaxReservedComponents() {
        return maxReservedComponents;
    }
    
    /**
     * Checks if component reservation is supported for this ship based on configuration
     * @return true if reservations are supported (max > 0), false otherwise
     */
    public boolean supportsComponentReservation() {
        return maxReservedComponents > 0;
    }
    
    /**
     * Releases a reserved component by its ID and returns it
     * @param componentId The ID of the component to release
     * @return The released component, or null if not found
     */
    public Component releaseReservedComponent(String componentId) {
        for (Component component : reservedComponents) {
            if (component.getId().equals(componentId)) {
                reservedComponents.remove(component);
                return component;
            }
        }
        return null;
    }

    public Set<Component> getLostComponents() {
        return lostComponents;
    }

    public double getCannons() {
        return cannons + getPurpleAlienCombatBonus();
    }

    public void setCannons(double cannons) {
        this.cannons = cannons;
    }

    public double getEngines() {
        return engines + getBrownAlienEngineBonus();
    }

    public void setEngines(double engines) {
        this.engines = engines;
    }

    public int getBatteries() {
        return batteries;
    }

    public void setBatteries(int batteries) {
        this.batteries = batteries;
    }

    public int getCrew() {
        return crew;
    }

    public void setCrew(int crew) {
        this.crew = crew;
    }

    public Map<GoodType, Integer> getResources() {
        return resources;
    }

    public void setResources(Map<GoodType, Integer> resources) {
        this.resources = resources;
    }

    public int getSpecialGoods() {
        return this.specialGoods;
    }

    public void setSpecialGoods(int specialGoods) {
        this.specialGoods = specialGoods;
    }

    public int getNormalGoods() {
        return this.normalGoods;
    }

    public void setNormalGoods(int normalGoods) {
        this.normalGoods = normalGoods;
    }

    /**
     * Consumes batteries from battery components according to Galaxy Trucker rules.
     * Batteries are permanently consumed (returned to bank) when used.
     * @param amount The number of batteries to consume
     * @return The actual number of batteries consumed
     */
    public int consumeBatteries(int amount) {
        int remainingToConsume = amount;
        int totalConsumed = 0;

        for (Component[] components : board) {
            for (Component component : components) {
                if (component instanceof Battery battery && remainingToConsume > 0) {
                    int consumed = battery.consumeBatteries(remainingToConsume);
                    totalConsumed += consumed;
                    remainingToConsume -= consumed;
                }
            }
        }

        // Update ship's total battery count
        updateStats();
        return totalConsumed;
    }



    public int calculateSpecialGoodsCapacity() {
        this.specialGoodsCapacity = 0;
        for (Component[] components : board) {
            for (Component component : components) {
                if (component != null && component.getType() == ComponentType.CARGO_HOLD_SPECIAL) {
                    this.specialGoodsCapacity += ((CargoHold) component).getCapacity();
                }
            }
        }
        return this.specialGoodsCapacity;
    }

    public int calculateNormalGoodsCapacity() {
        this.normalGoodsCapacity = 0;
        for (Component[] components : board) {
            for (Component component : components) {
                if (component != null && component.getType() == ComponentType.CARGO_HOLD) {
                    this.normalGoodsCapacity += ((CargoHold) component).getCapacity();
                }
            }
        }
        return this.normalGoodsCapacity;
    }

    public void setNormalGoodsCapacity(int normalGoodsCapacity) {
            this.normalGoodsCapacity = normalGoodsCapacity;
    }

    public void setSpecialGoodsCapacity(int specialGoodsCapacity) {
            this.specialGoodsCapacity = specialGoodsCapacity;
    }

    /**
     * Adds resources to the ship's cargo holds, following these rules:
     * 1. If there is enough free space in the cargo holds, the resources are added.
     * 2. If the total cargo capacity is sufficient but some space is occupied,
     * old resources are removed (as needed) to make space for the new ones.
     * 3. If there is not enough space, the addition fails.
     *
     * @param newResources A map containing the resources (GoodType) and their respective quantities to add.
     * @return {@code true} if resources were successfully added, {@code false} if there was insufficient space.
     */
    public boolean addResources(Map<GoodType, Integer> newResources) {
        updateStats();
        boolean allResourcesAdded = true;

    // Calcola lo spazio disponibile prima di iniziare
    int freeSpecialGoodsCapacity = calculateSpecialGoodsCapacity() - specialGoods;
    int freeNormalGoodsCapacity = calculateNormalGoodsCapacity() - normalGoods;

    int totalRedGoods = 0;
    int totalNormalGoods = 0;

        for (Map.Entry<GoodType, Integer> entry : newResources.entrySet()) {
        GoodType type = entry.getKey();
        int amount = entry.getValue();

        if (type == GoodType.RED) {
            totalRedGoods += amount;
        } else {
            totalNormalGoods += amount;
        }
    }

        if (totalRedGoods > freeSpecialGoodsCapacity) {
        System.out.println("Not enough special cargo space for red goods");
        return false;
    }

    // Verifica per merci normali
        if (totalNormalGoods > freeNormalGoodsCapacity + (freeSpecialGoodsCapacity - totalRedGoods)) {
        System.out.println("Not enough total cargo space for normal goods");
        return false;
    }

    // Se arriviamo qui, abbiamo verificato che c'è abbastanza spazio per tutte le merci
    // Procediamo con l'aggiunta
        for (Map.Entry<GoodType, Integer> entry : newResources.entrySet()) {
        GoodType type = entry.getKey();
        int amount = entry.getValue();

        if (type == GoodType.RED) {
            // Le merci rosse vanno solo nei cargo speciali
            int remaining = addToSpecificCargoType(type, amount, ComponentType.CARGO_HOLD_SPECIAL);
            if (remaining > 0) {
                allResourcesAdded = false;
            }
        } else {
            // Per le merci normali (blu, gialle, verdi)
            // Prima prova a riempire i cargo normali
            int remainingAmount = addToSpecificCargoType(type, amount, ComponentType.CARGO_HOLD);

            // Se non c'è abbastanza spazio nei cargo normali, usa anche quelli speciali
            if (remainingAmount > 0) {
                remainingAmount = addToSpecificCargoType(type, remainingAmount, ComponentType.CARGO_HOLD_SPECIAL);
                if (remainingAmount > 0) {
                    allResourcesAdded = false;
                }
            }
        }
    }

    updateStats();
    return allResourcesAdded;
}

    /**
     * Adds resources to a specific type of cargo hold
     * @param type The type of goods
     * @param amount The quantity to add
     * @param cargoType The type of cargo hold (normal or special)
     * @return The remaining quantity that could not be added
     */
    public int addToSpecificCargoType(GoodType type, int amount, ComponentType cargoType) {
        int remainingAmount = amount;

        // Cerca i cargo hold del tipo specificato
        for (int x = 0; x < board.length && remainingAmount > 0; x++) {
            for (int y = 0; y < board[x].length && remainingAmount > 0; y++) {
                if (board[x][y] != null && board[x][y].getType() == cargoType) {
                    CargoHold cargoHold = (CargoHold) board[x][y];
                    int freeSpace = cargoHold.getCapacity() - cargoHold.getOccupiedCapacity();

                    if (freeSpace > 0) {
                        int amountToAdd = Math.min(freeSpace, remainingAmount);

                        if (cargoHold.storeGoodsOfType(type, amountToAdd)) {
                            remainingAmount -= amountToAdd;
                        }
                    }
                }
            }
        }

        // Se non siamo riusciti ad aggiungere tutte le risorse, mostra un messaggio
        if (remainingAmount > 0) {
            System.out.println("Could not add " + remainingAmount + " units of " + type + " to cargo holds of type " + cargoType);
        }

        return remainingAmount;
    }

    /**
     * Removes a specified number of resources from the ship, starting with the most valuable ones.
     * The order of removal is: RED, YELLOW, GREEN, BLUE, and finally batteries.
     *
     * @param deletingNumber The number of resources to remove
     * @return true if all requested resources were successfully removed, false otherwise
     */
    public boolean removeValuableResources (int deletingNumber) {
        updateStats();
        int remainingToDelete = deletingNumber;

        // List of goods sorted by decreasing value
        GoodType[] goodsByValue = {GoodType.RED, GoodType.YELLOW, GoodType.GREEN, GoodType.BLUE};

        // Remove goods in priority order
        for (GoodType goodType : goodsByValue) {
            if (resources.containsKey(goodType) && remainingToDelete > 0) {
                // Determine which cargo holds to check
                ComponentType[] cargoTypes = (goodType == GoodType.RED)
                        ? new ComponentType[]{ComponentType.CARGO_HOLD_SPECIAL}  // Only special cargo for RED
                        : new ComponentType[]{ComponentType.CARGO_HOLD, ComponentType.CARGO_HOLD_SPECIAL};  // All cargo for others

                // Remove from all appropriate cargo holds
                for (ComponentType cargoType : cargoTypes) {
                    if (remainingToDelete <= 0) break;

                    remainingToDelete = removeFromCargoHoldType(goodType, remainingToDelete, cargoType);
                }
            }
        }

        // If there are still resources to remove, remove batteries
        if (remainingToDelete > 0) {
            for (int x = 0; x < board.length && remainingToDelete > 0; x++) {
                for (int y = 0; y < board[x].length && remainingToDelete > 0; y++) {
                    if (board[x][y] != null && board[x][y].getType() == ComponentType.BATTERY) {
                        Battery battery = (Battery) board[x][y];
                        int amount = Math.min(battery.getAvailableBatteries(), remainingToDelete);
                        battery.consumeBatteries(amount);
                        remainingToDelete -= amount;
                    }
                }
            }
        }
        updateStats();
        return remainingToDelete <= 0;
    }

    /**
     * Removes resources of a specific type from a specific type of cargo hold.
     *
     * @param goodType Type of good to remove
     * @param amountToRemove Amount to remove
     * @param cargoType Type of cargo hold to remove from
     * @return Remaining amount that could not be removed
     */
    public int removeFromCargoHoldType(GoodType goodType, int amountToRemove, ComponentType cargoType) {
        int remaining = amountToRemove;

        for (int x = 0; x < board.length && remaining > 0; x++) {
            for (int y = 0; y < board[x].length && remaining > 0; y++) {
                if (board[x][y] != null && board[x][y].getType() == cargoType) {
                    CargoHold cargoHold = (CargoHold) board[x][y];

                    // Check if this cargo hold contains goods of the requested type
                    if (cargoHold.getStoredGoods().containsKey(goodType)) {
                        int available = cargoHold.getStoredGoods().get(goodType);
                        int toRemove = Math.min(remaining, available);

                        if (toRemove > 0) {
                            int actuallyRemoved = cargoHold.removeAvailableGoods(goodType, toRemove);
                            remaining -= actuallyRemoved;
                        }
                    }
                }
            }
        }

        return remaining;
    }

    /**
     * Finds the first non-empty component along a line specified by a fixed coordinate.
     * <p>
     * For UP and DOWN directions, the fixed coordinate represents the column index.
     * The method iterates through the rows (starting from the top for UP or the bottom for DOWN)
     * until it finds a non-empty component in that column.
     * </p>
     * <p>
     * For LEFT and RIGHT directions, the fixed coordinate represents the row index.
     * The method iterates through the columns (starting from the left for LEFT or the right for RIGHT)
     * until it finds a non-empty component in that row.
     * </p>
     *
     * @param direction  The direction.
     * @param fixedIndex The column index if the direction is UP or DOWN, or the row index if the direction is LEFT or RIGHT.
     * @return A map containing the position and the component that is hit, or an empty map if no component is found.
     */
    public Position findFirstComponent(Direction direction, int fixedIndex) {

        if (board == null) {
            System.out.println("Board is null");
            return null;
        }

        if (direction == Direction.UP || direction == Direction.DOWN) {
            // For UP and DOWN directions, fixedIndex represents a column
            if (fixedIndex < 0 || fixedIndex >= board[0].length) {
                System.out.println("Column index out of bounds: " + fixedIndex);
                return null;
            }
        } else {
            // For LEFT and RIGHT directions, fixedIndex represents a row
            if (fixedIndex < 0 || fixedIndex >= board.length) {
                System.out.println("Row index out of bounds: " + fixedIndex);
                return null;
            }
        }

        switch (direction) {
            // For a shot from the top, fixedIndex is the column.
            // Iterate rows from top (0) to bottom.
            case UP:
                for (int row = 0; row < board.length; row++) {
                    if (board[row][fixedIndex] != null) {
                        return new Position(row, fixedIndex);
                    }
                }
                break;
            // For a shot from the bottom, fixedIndex is the column.
            // Iterate rows from bottom to top.
            case DOWN:
                for (int row = board.length - 1; row >= 0; row--) {
                    if (board[row][fixedIndex] != null) {
                        return new Position(row, fixedIndex);
                    }
                }
                break;
            // For a shot from the left, fixedIndex is the row.
            // Iterate columns from left (0) to right.
            case LEFT:
                for (int col = 0; col < board[0].length; col++) {
                    if (board[fixedIndex][col] != null) {
                        return new Position(fixedIndex, col);
                    }
                }
                break;
            // For a shot from the left, fixedIndex is the row.
            // Iterate columns from left (0) to right.
            case RIGHT:
                for (int col = board[0].length - 1; col >= 0; col--) {
                    if (board[fixedIndex][col] != null) {
                        return new Position(fixedIndex, col);
                    }
                }
                break;
        }
        return null;
    }

    public boolean protectedByShield(Direction direction) {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Component component = board[row][col];
                if (component != null && component.getType() == ComponentType.SHIELD) {
                    Shield shield = (Shield) component;
                    if (shield.getProtectedDirections().contains(direction)) {
                        System.out.println("Shield found, check battery presence");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if there is a cannon protecting the specified row or column.
     * <p>
     * For the directions {@code UP} or {@code DOWN}, the method checks the entire column (fixedIndex).
     * For the directions {@code LEFT} or {@code RIGHT}, the method checks the specified row (fixedIndex)
     * as well as the adjacent rows.
     * <p>
     * The method returns {@code true} as soon as a single cannon is found.
     * If no single cannon is found but a double cannon is detected,
     * it prints "Double cannon found, check battery presence" and returns {@code true}.
     * If no cannon is found, it returns {@code false}.
     *
     * @param direction  the direction of the shot (UP, DOWN, LEFT, or RIGHT)
     * @param fixedIndex the fixed index representing the column (for UP/DOWN) or row (for LEFT/RIGHT)
     * @return {@code true} if a cannon (single or double) is found; {@code false} otherwise
     */
    public boolean protectedByCannon(Direction direction, int fixedIndex) {
        boolean foundDouble = false;

        if (direction == Direction.UP || direction == Direction.DOWN) {
            // For UP and DOWN directions, fixedIndex represents a column
            if (fixedIndex < 0 || fixedIndex >= board[0].length) {
                System.out.println("Column index out of bounds: " + fixedIndex);
                return true;
            }
        } else {
            // For LEFT and RIGHT directions, fixedIndex represents a row
            if (fixedIndex < 0 || fixedIndex >= board.length) {
                System.out.println("Row index out of bounds: " + fixedIndex);
                return true;
            }
        }

        if ((direction == Direction.UP) || (direction == Direction.DOWN)) {
            for (int row = 0; row < board.length; row++) {
                if (board[row][fixedIndex] != null) {
                    if (board[row][fixedIndex].getType() == ComponentType.CANNON_SINGLE
                            && board[row][fixedIndex].getDirection() == direction) {
                        return true;
                    } else if (board[row][fixedIndex].getType() == ComponentType.CANNON_DOUBLE
                            && board[row][fixedIndex].getDirection() == direction) {
                        foundDouble = true;
                    }
                }
            }
        } else if ((direction == Direction.LEFT) || (direction == Direction.RIGHT)) {
            int[] rowsToCheck = {fixedIndex, fixedIndex - 1, fixedIndex + 1};
            for (int row : rowsToCheck) {
                if (row >= 0 && row < board.length) {
                    for (int col = 0; col < board[0].length; col++) {
                        if (board[row][col] != null) {
                            if (board[row][fixedIndex].getType() == ComponentType.CANNON_SINGLE
                                    && board[row][fixedIndex].getDirection() == direction) {
                                return true;
                            } else if (board[row][fixedIndex].getType() == ComponentType.CANNON_DOUBLE
                                    && board[row][fixedIndex].getDirection() == direction) {
                                foundDouble = true;
                            }
                        }
                    }
                }
            }
        }

        if (foundDouble) {
            System.out.println("Double cannon found, check battery presence");
            return true;
        } else {
            return false;
        }
    }
    public int getExposedConnectors(){
        int counter = 0;
        for(int row = 0; row < board.length; row++) {
            for(int col = 0; col < board[0].length; col++) {
                if (board[row][col] != null) {
                    for(Direction direction : Direction.values()) {
                        Position position = new Position(row, col);
                        Position offset = position.offsetBy(direction);
                        if((board[offset.getRow()][offset.getCol()] == null) &&
                                (board[position.getRow()][position.getCol()].getConnectorAt(direction)
                                        != ConnectorType.PLAIN)){
                            counter++;
                        }
                    }
                }
            }
        }
        return counter;
    }


    /**
     * Counts all connected cabins in the ship's grid, ensuring that each group of connected cabins
     * is counted exactly once.
     *
     * @return The total number of connected cabins across all groups
     *
     */
    public int countAllAdjacentCabins() {
        boolean[][] visited = new boolean[board.length][board[0].length];
        int cabins = 0;
        int totCabins = 0;

        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Position pos = new Position(row, col);
                Component component = board[row][col];

                // Controlla solo celle non visitate e di tipo CABIN/CABIN_START
                if (component != null && !visited[row][col] && (component.getType() == ComponentType.CABIN || component.getType() == ComponentType.CABIN_START)) {
                    cabins = checkAdjacentCabin(pos, board, visited);
                    if(cabins>=2){
                        totCabins += cabins;
                    }
                }
            }
        }
        return totCabins;
    }

    /**
     * Recursively counts all cabins connected to a starting position,
     * using a shared visited matrix to avoid reprocessing.
     *
     * @param position The starting position for traversal
     * @param board The 2D grid of ship components
     * @param visited A boolean matrix tracking processed positions
     * @return The number of cabins connected to the starting position (including the starting cabin itself)
     *
     */
    private int checkAdjacentCabin(Position position, Component[][] board, boolean[][] visited) {
        int row = position.getRow();
        int col = position.getCol();

        // Controllo dei limiti o già visitato
        if (row < 0 || row >= board.length || col < 0 || col >= board[0].length || visited[row][col]) {
            return 0;
        }

        Component current = board[row][col];
        if (current.getType() != ComponentType.CABIN && current.getType() != ComponentType.CABIN_START) {
            return 0;
        }

        visited[row][col] = true; // Segna come visitato
        int count = 1; // Conta questa cabina

        for (Direction direction : Direction.values()) {
            Position neighbor = position.offsetBy(direction);
            int rown = neighbor.getRow();
            int coln = neighbor.getCol();

            if (rown < 0 || rown >= board.length || coln < 0 || coln >= board[0].length) {
                continue;
            }

            Component neighborCell = board[rown][coln];
            if (neighborCell == null) {
                continue;
            }

            // Controlla connettore nella direzione opposta
            if ((neighborCell.getType() == ComponentType.CABIN || neighborCell.getType() == ComponentType.CABIN_START) &&
                    neighborCell.getConnectorAt(direction.getOpposite()) != ConnectorType.PLAIN) {
                count += checkAdjacentCabin(neighbor, board, visited);
            }
        }
        return count;
    }

    /**
     * Checks if the component at the given position is correctly linked to its neighbors.
     * A component is considered correctly linked if all its connectors are connected to compatible connectors
     * of neighboring components.
     *
     * @param component The component to check
     * @return {@code true} if the component is correctly linked, {@code false} otherwise
     */
    public boolean isCorrectlyConnected(Component component) {
        // Se non c'è componente in questa posizione, non ci sono connessioni da verificare
        if (component == null) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            Position position = component.getPosition();
            Position neighbour = position.offsetBy(direction);

            // Se la posizione del vicino è fuori dai limiti della griglia, non c'è connessione da controllare
            if (neighbour.getRow() < 0 || neighbour.getRow() >= board.length ||
                    neighbour.getCol() < 0 || neighbour.getCol() >= board[0].length) {
                continue;
            }

            // Se la posizione del vicino è vuota, non c'è connessione da controllare
            Component neighbourComponent = board[neighbour.getRow()][neighbour.getCol()];
            if (neighbourComponent == null) {
                continue;
            }

            // Verifica la compatibilità dei connettori
            boolean isCorrect = component.getConnectorAt(direction).canConnectTo(
                    neighbourComponent.getConnectorAt(direction.getOpposite())
            );

            if (!isCorrect) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks for connection errors in the ship's components.
     * A connection error occurs when a component is not correctly linked to its neighbors.
     *
     * @return A list of components that have connection errors
     */
    public List<Component> checkConnectingErrors() {
        List<Component> wrongConnections = new ArrayList<>();

        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                if (board[row][col] != null) {
                    Component component = board[row][col];
                    if (!isCorrectlyConnected(component)) {
                        wrongConnections.add(component);
                    }
                }
            }
        }
        return wrongConnections;
    }

    /**
     * Checks if the component is correctly placed on the board.
     * A component is considered correctly placed if its exhaust direction is DOWN and
     * it is not adjacent to another component in the direction of its exhaust.
     *
     * @param component The component to check
     * @param phase The current game phase
     * @return {@code true} if the component is correctly placed, {@code false} otherwise
     */
    public boolean isCorrectlyPlaced (Component component, GamePhase phase) {
        Position position = component.getPosition();
        List<Component> wrongPlacing = new ArrayList<>();
        if(component == null){
            return true;
        }

        switch (component.getType()) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> {
                Direction exhaust = component.getDirection().getOpposite();
                if(exhaust != Direction.DOWN){
                    wrongPlacing.add(component);
                    break;
                }
                Position neighbour = position.offsetBy(exhaust);
                if (neighbour.getRow() < 0 || neighbour.getRow() >= board.length ||
                        neighbour.getCol() < 0 || neighbour.getCol() >= board[0].length) {
                    break;
                }
                Component neighbourComponent = board[neighbour.getRow()][neighbour.getCol()];
                if (neighbourComponent!= null) {
                    wrongPlacing.add(component);
                }
            }
            case CANNON_SINGLE, CANNON_DOUBLE -> {
                Direction cannonDirection = component.getDirection();
                Position neighbour = position.offsetBy(cannonDirection);
                if (neighbour.getRow() < 0 || neighbour.getRow() >= board.length ||
                        neighbour.getCol() < 0 || neighbour.getCol() >= board[0].length) {
                    break;
                }
                Component neighbourComponent = board[neighbour.getRow()][neighbour.getCol()];
                if (neighbourComponent!= null) {
                    wrongPlacing.add(component);
                }
            }
        }
        // Se un errore viene scoperto quando la nave è già in volo, il giocatore, oltre a correggere l’errore
        // deve pagare alla banca 1 credito cosmico
        if(phase == GamePhase.FLIGHT) {
            // TODO: Credit penalty should be handled by calling code
        }
        return (wrongPlacing.isEmpty());
    }

    /**
     * Checks if the ship is correctly placed on the board.
     * A ship is considered correctly placed if all its components are correctly connected
     * and there are no connection errors.
     *
     * @return {@code true} if the ship is correctly placed, {@code false} otherwise
     */
    public List<Component> checkPlacingErrors() {

        List<Component> wrongPlacing = new ArrayList<>();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Component component = board[row][col];
                Position position = new Position(row, col);
                if (component != null) {
                    if (!isCorrectlyPlaced(component, GamePhase.BUILDING)) {
                        wrongPlacing.add(component);
                    }
                }
            }
        }
        return wrongPlacing;
    }


    /**
     * Performs a flood fill algorithm to find all connected components in the ship's grid.
     * It marks visited positions and adds them to the connected group.
     *
     * @param position The starting position for the flood fill
     * @param visited  A boolean matrix tracking processed positions
     * @param connectedGroup A set to store all positions in the connected group
     */
    private void floodFill(Position position, boolean[][] visited, Set<Position> connectedGroup) {
    int row = position.getRow();
    int col = position.getCol();

    // Verifica se la posizione è valida
    if (row < 0 || row >= board.length || col < 0 || col >= board[0].length ||
            visited[row][col] || board[row][col] == null) {
        return;
    }

    // Marca come visitata e aggiungi al gruppo connesso
    visited[row][col] = true;
    connectedGroup.add(position);

    // Esplora nelle quattro direzioni, verificando la compatibilità dei connettori
     for (Direction direction : Direction.values()) {
        Position neighborPos = position.offsetBy(direction);
        int nRow = neighborPos.getRow();
        int nCol = neighborPos.getCol();

        // Verifica se la posizione del vicino è valida
        if (nRow < 0 || nRow >= board.length || nCol < 0 || nCol >= board[0].length ||
                visited[nRow][nCol] || board[nRow][nCol] == null) {
            continue;
        }

        Component currentComponent = board[row][col];
        Component neighborComponent = board[nRow][nCol];

        boolean cantConnect = currentComponent.getConnectorAt(direction) == ConnectorType.PLAIN &&
                neighborComponent.getConnectorAt(direction.getOpposite()) == ConnectorType.PLAIN;

        // Se i connettori sono compatibili, esplora da quella posizione
        if (!cantConnect) {
            floodFill(neighborPos, visited, connectedGroup);
            }
        }
    }

    /**
     * Splits the ship into multiple ships based on the connected components.
     * If the specified component is not on the board, it returns a list containing only this ship.
     *
     * @param component The component to check for splitting
     * @return A list of ships created from the connected components
     */
    public List<Ship> splitBoard(Component component) {
        Position componentPosition = component.getPosition();

        // Se il componente non è nella board, restituisci solo questa nave
        if (componentPosition == null ||
                componentPosition.getRow() < 0 || componentPosition.getRow() >= board.length ||
                componentPosition.getCol() < 0 || componentPosition.getCol() >= board[0].length) {
            return List.of(this);
        }

        // Rimuovi temporaneamente il componente
        board[componentPosition.getRow()][componentPosition.getCol()] = null;

        // Mappa per tenere traccia delle celle visitate durante il flood fill
        boolean[][] visited = new boolean[board.length][board[0].length];

        // Lista per memorizzare tutti i gruppi di componenti connessi trovati
        List<Set<Position>> connectedGroups = new ArrayList<>();

        // Cerca gruppi di componenti connessi
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                if (board[row][col] != null && !visited[row][col]) {
                    // Trovato un nuovo gruppo connesso
                    Set<Position> connectedGroup = new HashSet<>();
                    floodFill(new Position(row, col), visited, connectedGroup);
                    connectedGroups.add(connectedGroup);
                }
            }
        }

        // Se c'è solo un gruppo connesso o nessun gruppo, non c'è divisione
        if (connectedGroups.size() <= 1) {
            // Ripristina il componente rimosso
            board[componentPosition.getRow()][componentPosition.getCol()] = component;
            return List.of(this);
        }

        // Altrimenti, crea nuove navi per ogni gruppo connesso
        List<Ship> ships = new ArrayList<>();

        for (Set<Position> group : connectedGroups) {
            // Crea una nuova nave con le stesse caratteristiche di questa
            // Note: This creates a basic ship structure for split ships after damage
            // For split ships, use the same grid configuration as the original
            var defaultShipConfig = new it.polimi.ingsw.server.model.domain.general.config.ShipGridConfig(
                "",  // image not needed for split ships
                this.board.length,  // same rows
                this.board[0].length,  // same cols  
                new java.util.ArrayList<>(),  // no reserved positions for split ships
                new java.util.ArrayList<>(
                    this.forbiddenPositions.stream()
                        .map(pos -> new it.polimi.ingsw.server.model.domain.general.config.PositionConfig(pos.getRow(), pos.getCol()))
                        .toList()
                )  // same forbidden positions
            );
            Ship newShip = new Ship(getLevel(), defaultShipConfig);

            // Copia i componenti rilevanti nella nuova nave
            for (Position pos : group) {
                Component comp = board[pos.getRow()][pos.getCol()];
                newShip.board[pos.getRow()][pos.getCol()] = comp;
            }

            // Aggiorna le statistiche della nuova nave
            newShip.updateStats();

            ships.add(newShip);
        }

        // Il componente rimosso non fa parte di nessuna delle nuove navi
        return ships;
    }

    /**
     * Calculates the combat bonus provided by purple alien passengers.
     * According to Galaxy Trucker rules, purple aliens provide +2 combat strength.
     * Bonus only applies if the ship has basic cannon strength > 0.
     * 
     * @return The total combat bonus from purple aliens
     */
    public int getPurpleAlienCombatBonus() {
        // Galaxy Trucker rule: "If cannon strength without alien is 0, don't get this bonus. 
        // Not going to fight space battle with bare tentacles."
        if (cannons <= 0) {
            return 0;
        }
        
        try {
            // Count purple aliens on the ship
            return countAlienPassengers(it.polimi.ingsw.server.model.enums.crew.AlienColor.ALIEN_PURPLE) * 2;
        } catch (Exception e) {
            // Fallback - return 0 if alien counting fails
            return 0;
        }
    }

    /**
     * Calculates the engine bonus provided by brown alien passengers.
     * According to Galaxy Trucker rules, brown aliens provide +2 engine strength.
     * Bonus only applies if the ship has basic engine strength > 0.
     * 
     * @return The total engine bonus from brown aliens
     */
    public int getBrownAlienEngineBonus() {
        // Galaxy Trucker rule: "If engine strength without alien is 0, don't get this bonus. 
        // Not going to get out and push."
        if (engines <= 0) {
            return 0;
        }
        
        try {
            // Count brown aliens on the ship
            return countAlienPassengers(it.polimi.ingsw.server.model.enums.crew.AlienColor.ALIEN_BROWN) * 2;
        } catch (Exception e) {
            // Fallback - return 0 if alien counting fails
            return 0;
        }
    }

    /**
     * Counts the number of alien passengers of a specific color on the ship.
     * This method searches for cabins with life support systems and counts aliens.
     * 
     * @param alienColor The color of aliens to count (PURPLE or BROWN)
     * @return The number of aliens of the specified color
     */
    private int countAlienPassengers(it.polimi.ingsw.server.model.enums.crew.AlienColor alienColor) {
        int alienCount = 0;
        
        // Search the ship board for life support systems matching the alien color
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Component component = board[row][col];
                if (component == null) continue;
                
                // Check for life support systems that correspond to the alien color
                boolean hasMatchingLifeSupport = false;
                switch (alienColor) {
                    case ALIEN_PURPLE:
                        hasMatchingLifeSupport = (component.getType() == ComponentType.LIFE_SUPPORT_PURPLE);
                        break;
                    case ALIEN_BROWN:
                        hasMatchingLifeSupport = (component.getType() == ComponentType.LIFE_SUPPORT_BROWN);
                        break;
                }
                
                if (hasMatchingLifeSupport) {
                    // Check if this life support is connected to a cabin with aliens
                    // For now, assume each life support system supports 1 alien
                    // This is a simplified implementation until full crew management is integrated
                    if (isConnectedToCabin(new Position(row, col))) {
                        alienCount++;
                    }
                }
            }
        }
        
        return alienCount;
    }

    /**
     * Checks if a life support system is properly connected to a cabin.
     * According to Galaxy Trucker rules, life support must be joined to a cabin to have effect.
     * 
     * @param lifeSupportPosition The position of the life support system
     * @return true if connected to a cabin, false otherwise
     */
    private boolean isConnectedToCabin(Position lifeSupportPosition) {
        Component lifeSupport = board[lifeSupportPosition.getRow()][lifeSupportPosition.getCol()];
        if (lifeSupport == null) return false;
        
        // Check all four directions for connected cabins
        for (Direction direction : Direction.values()) {
            Position neighborPos = lifeSupportPosition.offsetBy(direction);
            
            // Check bounds
            if (neighborPos.getRow() < 0 || neighborPos.getRow() >= board.length ||
                neighborPos.getCol() < 0 || neighborPos.getCol() >= board[0].length) {
                continue;
            }
            
            Component neighbor = board[neighborPos.getRow()][neighborPos.getCol()];
            if (neighbor == null) continue;
            
            // Check if neighbor is a cabin and if they're properly connected
            if (neighbor.getType() == ComponentType.CABIN || neighbor.getType() == ComponentType.CABIN_START) {
                // Verify the connectors are compatible
                ConnectorType lifeSupportConnector = lifeSupport.getConnectorAt(direction);
                ConnectorType cabinConnector = neighbor.getConnectorAt(direction.getOpposite());
                
                if (areConnectorsCompatible(lifeSupportConnector, cabinConnector)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Checks if two connectors are compatible for joining components.
     * 
     * @param connector1 The first connector type
     * @param connector2 The second connector type
     * @return true if the connectors can be joined, false otherwise
     */
    private boolean areConnectorsCompatible(ConnectorType connector1, ConnectorType connector2) {
        // Plain connectors can't connect to anything
        if (connector1 == ConnectorType.PLAIN || connector2 == ConnectorType.PLAIN) {
            return false;
        }
        
        // Universal connectors can connect to anything except plain
        if (connector1 == ConnectorType.UNIVERSAL || connector2 == ConnectorType.UNIVERSAL) {
            return true;
        }
        
        // Same types can connect to each other
        return connector1 == connector2;
    }

    /**
     * Gets the base cannon strength without alien bonuses.
     * Useful for combat calculations where you need to know the raw ship power.
     * 
     * @return The base cannon strength from ship components only
     */
    public double getBaseCannons() {
        return cannons;
    }

    /**
     * Gets the base engine strength without alien bonuses.
     * Useful for engine calculations where you need to know the raw ship power.
     * 
     * @return The base engine strength from ship components only
     */
    public double getBaseEngines() {
        return engines;
    }
    
    /**
     * Checks if the ship is structurally valid.
     * A ship is considered structurally valid if all components are connected
     * to at least one other component through proper connectors.
     * 
     * @return true if the ship is structurally valid, false otherwise
     */
    public boolean isStructurallyValid() {
        if (board == null) {
            return false;
        }
        
        // Find all components on the board
        Component[][] components = getBoard();
        boolean hasComponents = false;
        
        for (int row = 0; row < components.length; row++) {
            for (int col = 0; col < components[row].length; col++) {
                Component component = components[row][col];
                if (component != null) {
                    hasComponents = true;
                    // Check if this component is properly connected
                    if (!component.check(this)) {
                        return false;
                    }
                }
            }
        }
        
        return hasComponents; // Must have at least one component to be valid
    }
    
    /**
     * Gets the number of shield components on the ship.
     * 
     * @return The number of shields
     */
    public int getShields() {
        return shields;
    }
    
    /**
     * Gets the life support capacity of the ship.
     * 
     * @return The life support capacity
     */
    public int getLifeSupport() {
        return lifeSupport;
    }
    
    /**
     * Gets the number of components placed on the ship.
     * 
     * @return The count of placed components
     */
    public int getPlacedComponentsCount() {
        int count = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (board[row][col] != null) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Gets the component at the specified position.
     * 
     * @param position The position to check
     * @return The component at that position, or null if none
     */
    public Component getComponent(Position position) {
        if (position == null || board == null) {
            return null;
        }
        int row = position.getRow();
        int col = position.getCol();
        if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
            return board[row][col];
        }
        return null;
    }
    
    /**
     * Checks if a position is forbidden for component placement.
     * 
     * @param position The position to check
     * @return true if the position is forbidden
     */
    public boolean isForbiddenPosition(Position position) {
        // For now, no positions are forbidden
        // This could be extended to include specific forbidden areas
        return false;
    }
    
    /**
     * Gets the count of engine components on the ship.
     * 
     * @return The number of engine components
     */
    public int getEngineCount() {
        return countComponentsByType(ComponentType.ENGINE_SINGLE) + 
               countComponentsByType(ComponentType.ENGINE_DOUBLE);
    }
    
    /**
     * Gets the count of cannon components on the ship.
     * 
     * @return The number of cannon components
     */
    public int getCannonCount() {
        return countComponentsByType(ComponentType.CANNON_SINGLE) + 
               countComponentsByType(ComponentType.CANNON_DOUBLE);
    }
    
    /**
     * Gets the count of battery components on the ship.
     * 
     * @return The number of battery components
     */
    public int getBatteryCount() {
        return countComponentsByType(ComponentType.BATTERY);
    }
    
    /**
     * Gets the count of shield components on the ship.
     * 
     * @return The number of shield components
     */
    public int getShieldCount() {
        return countComponentsByType(ComponentType.SHIELD);
    }
    
    /**
     * Gets the crew capacity of the ship (number of cabins).
     * 
     * @return The crew capacity
     */
    public int getCrewCapacity() {
        return countComponentsByType(ComponentType.CABIN) + 
               countComponentsByType(ComponentType.CABIN_START);
    }
    
    /**
     * Gets the total cargo capacity of the ship.
     * 
     * @return The total cargo capacity
     */
    public int getCargoCapacity() {
        return normalGoodsCapacity + specialGoodsCapacity;
    }
    
    /**
     * Gets the number of cargo holds on the ship.
     * 
     * @return The number of cargo holds (both normal and special)
     */
    public int getCargoHolds() {
        return countComponentsByType(ComponentType.CARGO_HOLD) + 
               countComponentsByType(ComponentType.CARGO_HOLD_SPECIAL);
    }
    
    /**
     * Helper method to count components of a specific type.
     * 
     * @param type The component type to count
     * @return The number of components of that type
     */
    private int countComponentsByType(ComponentType type) {
        int count = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                Component component = board[row][col];
                if (component != null && component.getType() == type) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Custom serialization to handle transient PropertyChangeSupport.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    /**
     * Custom deserialization to restore transient PropertyChangeSupport.
     */
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

}