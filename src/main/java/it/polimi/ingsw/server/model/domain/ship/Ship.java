package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.CargoHold;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import it.polimi.ingsw.server.model.domain.ship.components.Battery;
import it.polimi.ingsw.server.model.domain.ship.components.Shield;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.*;

public class Ship {
    private final Player player;
    private Component[][] board;
    public Set<Position> forbiddenPositions;
    private Set<Component> reservedComponents;

    // Ship stats
    private double cannons;
    private double engines;
    private int batteries;
    private int crew;
    private Map<GoodType, Integer> resources;
    private int specialGoods;
    private int normalGoods;

    private int specialGoodsCapacity;
    private int normalGoodsCapacity;

    // Batteries set to be used by the player to charge cannons/engines/shields
    private int chargingBatteries;

    private int lostComponents;
    private GameLevel level;

    public Ship(Player player, GameLevel level) {
        this.player = player;
        this.board = new Component[5][7];
        reservedComponents = new HashSet<>();
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

        this.level = level;
        if (level == GameLevel.TEST_FLIGHT) {
            forbiddenPositions = new HashSet<>() {{
                add(new Position(0, 0));
                add(new Position(0, 1));
                add(new Position(0, 2));
                add(new Position(0, 4));
                add(new Position(0, 5));
                add(new Position(0, 6));
                add(new Position(1, 0));
                add(new Position(1, 1));
                add(new Position(1, 5));
                add(new Position(1, 6));
                add(new Position(2, 0));
                add(new Position(2, 6));
                add(new Position(3, 0));
                add(new Position(3, 6));
                add(new Position(4, 0));
                add(new Position(4, 3));
                add(new Position(4, 6));
            }};
        } else if (level == GameLevel.LEVEL_II) {
            forbiddenPositions = new HashSet<>() {{
                add(new Position(0, 0));
                add(new Position(0, 1));
                add(new Position(0, 3));
                add(new Position(0, 5));
                add(new Position(0, 6));
                add(new Position(1, 0));
                add(new Position(1, 6));
                add(new Position(4, 3));
            }};
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
        }
    }

    /**
     * Removes the component from the ship at the specified position on the board.
     *
     * @param position The position on the grid from which the component should be removed.
     */
    public void removeComponent(Position position) {
        int row = position.getRow();
        int col = position.getCol();

        // Checks if position is illegal or empty, otherwise removes Component and updates its position attribute
        if (forbiddenPositions.contains(position)) {
            throw new IllegalArgumentException("Forbidden position");
        } else if (board[row][col] == null) {
            throw new IllegalArgumentException("Empty position");
        } else {
            board[row][col].setPosition(null);    // Spostare il component in lista dei "rifiuti"?
            board[row][col] = null;
        }
    }

    /**
     * Reserves a component for future use, allowing it to be kept aside without attaching it to the ship.
     * If there are already 2 reserved components, the first one is removed to make room for the new component.
     *
     * @param component The component to reserve.
     */
    public void reserveComponent(Component component) {
        if (reservedComponents.size() < 2) {
            reservedComponents.add(component);
        } else {
            throw new IllegalArgumentException("Too many reserved components");
        }
    }

    public Player getPlayer() {
        return player;
    }

    public Component[][] getBoard() {
        return board;
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
    }

    public double getCannons() {
        return cannons;
    }

    public void setCannons(double cannons) {
        this.cannons = cannons;
    }

    public double getEngines() {
        return engines;
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

    public int getChargingBatteries() {
        return chargingBatteries;
    }

    public void setChargingBatteries(int chargingBatteries) {
        this.chargingBatteries = chargingBatteries;
    }

    public int getLostComponents() {
        return lostComponents;
    }

    public void setLostComponents(int lostComponents) {
        this.lostComponents = lostComponents;
    }

    public GameLevel getLevel() { return level; }

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

                        cargoHold.storeGoodsOfType(type, amountToAdd);
                        remainingAmount -= amountToAdd;
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
                        int amount = Math.min(battery.getCurrentBatteries(), remainingToDelete);
                        battery.setCurrentBatteries(battery.getCurrentBatteries() - amount);
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
                            cargoHold.removeGoodsOfType(goodType, toRemove);
                            remaining -= toRemove;
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
                    Position position = component.getPosition();
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
     * @return A list of components that are incorrectly placed
     */
    public List<Component> checkPlacingErrors (Component component, GamePhase phase) {
        Position position = component.getPosition();
        List<Component> wrongPlacing = new ArrayList<>();
        if(component == null){
            return wrongPlacing;
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
            getPlayer().subtractCredits(1);
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
            Ship newShip = new Ship(this.player, getLevel());

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

    //TODO :  pezzi scartati perchè rimasti nella pila dei componenti riservati al termine dell'assemblaggio
    //        o perchè eliminati per rendere la nave corretta  vanno inseriti nella pila degli scarti
}