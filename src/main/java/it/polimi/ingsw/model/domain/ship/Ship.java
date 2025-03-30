package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.enums.ship.ComponentType;

import it.polimi.ingsw.model.domain.ship.components.Battery;
import it.polimi.ingsw.model.domain.ship.components.Shield;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Set;
import java.util.Map;

public class Ship {
    private Player player;

    private Component[][] board;
    private static Set<Position> forbiddenPositions;

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

    private int lostComponents;


    public Ship(Player player) {
        this.player = player;
    }


    /**
     * Adds a component to the ship at the specified position on the board.
     * @param component The component to add.
     * @param position  The position on the grid where the component should be placed.
     */
    public void addComponent(Component component, Position position) {
        int x = position.getX();
        int y = position.getY();

        // Checks if position is illegal, otherwise adds Component and updates its position attribute
        if (forbiddenPositions.contains(position)) {
            throw new IllegalArgumentException("Forbidden position");
        }
        else if (board[y][x] != null) {
            throw new IllegalArgumentException("Occupied position");
        }
        else {
            board[y][x] = component;
            component.setPosition(position);
        }

    }

    /**
     * Removes the component from the ship at the specified position on the board.
     * @param position The position on the grid from which the component should be removed.
     */
    public void removeComponent(Position position) {
        int x = position.getX();
        int y = position.getY();

        if (forbiddenPositions.contains(position)) {
            throw new IllegalArgumentException("Forbidden position");
        }
        else if (board[y][x] != null) {
            throw new IllegalArgumentException("Occupied position");
        }
        else {
            board[y][x].setPosition(null);    // Spostare il component in lista dei "rifiuti"?
            board[y][x] = null;
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
        }
        else {
            throw new IllegalArgumentException("Too many reserved components");
        }
    }


    public Component[][] getBoard() {
        return board;
    }


    // Calls the count() for each component on the board
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
        return specialGoods;
    }

    public void setSpecialGoods(int specialGoods) {
        this.specialGoods = specialGoods;
    }

    public int getNormalGoods() {
        return normalGoods;
    }

    public void setNormalGoods(int normalGoods) {
        this.normalGoods = normalGoods;
    }

    public int getLostComponents() {
        return lostComponents;
    }

    public void setLostComponents(int lostComponents) {
        this.lostComponents = lostComponents;
    }


    /**
     * Adds resources to the ship's cargo holds, following these rules:
     * 1. If there is enough free space in the cargo holds, the resources are added.
     * 2. If the total cargo capacity is sufficient but some space is occupied,
     *    old resources are removed (as needed) to make space for the new ones.
     * 3. If there is not enough space, the addition fails.
     *
     * @param newResources A map containing the resources (GoodType) and their respective quantities to add.
     * @return {@code true} if resources were successfully added, {@code false} if there was insufficient space.
     */
    public boolean addResources(Map<GoodType, Integer> newResources) {
        // Good capacities and good quantities are updated by updateStats

        // Available space in cargo holds
        int freeSpecialGoodsCapacity = specialGoodsCapacity - specialGoods;
        int freeNormalGoodsCapacity = normalGoodsCapacity - normalGoods;

        // Adding new resources
        for (Map.Entry<GoodType, Integer> entry : newResources.entrySet()) {
            GoodType type = entry.getKey();
            int amount = entry.getValue();

            if (type == GoodType.RED) {
                if (freeSpecialGoodsCapacity >= amount) {
                    resources.put(type, resources.getOrDefault(type, 0) + amount);
                    freeSpecialGoodsCapacity -= amount;
                }
                // Not enough space for special goods
                else {
                    return false;
                }
            }
            else {
                if (freeNormalGoodsCapacity >= amount) {
                    resources.put(type, resources.getOrDefault(type, 0) + amount);
                    freeNormalGoodsCapacity -= amount;
                }
                // If no space in normal cargo, try special cargo
                else if (specialGoodsCapacity - specialGoods >= amount) {
                    resources.put(type, resources.getOrDefault(type, 0) + amount);
                    freeSpecialGoodsCapacity -= amount;
                }
                // Not enough space for normal goods
                else {
                    return false;
                }
            }
        }
        return true;    // Resources added successfully
    }

    public boolean removeValuableResources(int deletingNumber) {
        // Good capacities and good quantities are updated by updateStats

        // Elimino prima tutte le merci rosse
        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.RED)) {
                while (specialGoods > 0 && deletingNumber > 0 && resources.get(GoodType.RED) > 0) {
                    resources.put(GoodType.RED, resources.get(GoodType.RED) - 1);
                    deletingNumber--;
                    specialGoods--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.RED) == 0) {
                        resources.remove(GoodType.RED);
                    }

                    if (deletingNumber == 0) {
                        return true;
                    }
                }
            }
        }
        // Elimino altre merci in ordine decrescente di valore (BLUE, GREEN, YELLOW)
        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.BLUE)) {
                while (normalGoods > 0 && deletingNumber > 0 && resources.get(GoodType.BLUE) > 0) {
                    resources.put(GoodType.BLUE, resources.get(GoodType.BLUE) - 1);
                    deletingNumber--;
                    normalGoods--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.BLUE) == 0) {
                        resources.remove(GoodType.BLUE);
                    }

                    if (deletingNumber == 0) {
                        return true;
                    }
                }
            }
        }
        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.GREEN)) {
                while (normalGoods > 0 && deletingNumber > 0 && resources.get(GoodType.GREEN) > 0) {
                    resources.put(GoodType.GREEN, resources.get(GoodType.GREEN) - 1);
                    deletingNumber--;
                    normalGoods--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.GREEN) == 0) {
                        resources.remove(GoodType.GREEN);
                    }

                    if (deletingNumber == 0) {
                        return true;
                    }
                }
            }
        }
        if (deletingNumber > 0) {
            if (resources.containsKey(GoodType.YELLOW)) {
                while (normalGoods > 0 && resources.get(GoodType.YELLOW) > 0) {
                    resources.put(GoodType.YELLOW, resources.get(GoodType.YELLOW) - 1);
                    deletingNumber--;
                    normalGoods--;

                    // Rimuovo il record se il valore diventa 0
                    if (resources.get(GoodType.YELLOW) == 0) {
                        resources.remove(GoodType.YELLOW);
                    }

                    if (deletingNumber == 0) {
                        return true;
                    }
                }
            }
        }

        // Removes batteries if there are no more goods (TODO: Dovrebbe scegliere il giocatore da dove?)
        if (deletingNumber > 0) {
            for (Component[] components : board) {
                for (Component component : components) {
                    if (component != null && component.getType() == ComponentType.BATTERY) {
                        while (((Battery) component).getCurrentBatteries() > 0) {
                            ((Battery) component).setCurrentBatteries(((Battery) component).getCurrentBatteries() - 1);    // TODO: Chi aggiorna Ship.batteries?
                            deletingNumber--;

                            if (deletingNumber == 0) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // In teoria se si arriva a questo punto deletingNumber > 0 ma per ora lascio la condizione per sicurezza
        return deletingNumber <= 0;
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
        Position result = null;

        switch (direction) {
            // For a shot from the top, fixedIndex is the column.
            // Iterate rows from top (0) to bottom.
            case Direction.UP:
                for (int row = 0; row < board.length; row++) {
                    if (board[row][fixedIndex] != null) {
                        return new Position(row, fixedIndex);
                    }
                }
            // For a shot from the bottom, fixedIndex is the column.
            // Iterate rows from bottom to top.
            case Direction.DOWN:
                for (int row = board.length - 1; row >= 0; row--) {
                    if (board[row][fixedIndex] != null) {
                        return new Position(row, fixedIndex);
                    }
                }
            // For a shot from the left, fixedIndex is the row.
            // Iterate columns from left (0) to right.
            case Direction.LEFT:
                for (int col = 0; col < board[0].length; col++) {
                    if (board[fixedIndex][col] != null) {
                        return new Position(fixedIndex, col);
                    }
                }
            // For a shot from the left, fixedIndex is the row.
            // Iterate columns from left (0) to right.
            case Direction.RIGHT:
                for (int col = board[0].length - 1; col >= 0; col--) {
                    if (board[fixedIndex][col] != null) {
                        return new Position(fixedIndex, col);
                    }
                }
        }

        throw new NullPointerException("No element found");
    }

    public boolean protectedByShield(Direction direction) {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                if (board[row][col] != null && board[row][col].getType() == ComponentType.SHIELD) {
                    if (((Shield) board[row][col]).getProtectedDirections().contains(direction)) {
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

        if ((direction == Direction.UP) || (direction == Direction.DOWN)) {
            for (int row = 0; row < board.length; row++) {
                if (board[row][fixedIndex] != null) {
                    if (board[row][fixedIndex].getType() == ComponentType.CANNON_SINGLE && board[row][fixedIndex].getDirection() == direction) {
                        return true;
                    }
                    else if (board[row][fixedIndex].getType() == ComponentType.CANNON_DOUBLE && board[row][fixedIndex].getDirection() == direction) {
                        foundDouble = true;
                    }
                }
            }
        }
        else if ((direction == Direction.LEFT) || (direction == Direction.RIGHT)) {
            int[] rowsToCheck = {fixedIndex, fixedIndex - 1, fixedIndex + 1};
            for (int row : rowsToCheck) {
                if (row >= 0 && row < board.length) {
                    for (int col = 0; col < board[0].length; col++) {
                        if (board[row][col] != null) {
                            if (board[row][fixedIndex].getType() == ComponentType.CANNON_SINGLE && board[row][fixedIndex].getDirection() == direction) {
                                return true;
                            }
                            else if (board[row][fixedIndex].getType() == ComponentType.CANNON_DOUBLE && board[row][fixedIndex].getDirection() == direction) {
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
        }
        else {
            return false;
        }
    }
}