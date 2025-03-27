package it.polimi.ingsw.model.domain.ship;

import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.ship.components.NewComponent;
import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Set;
import java.util.Map;

public class NewShip {
    private Player player;

    private NewComponent[][] board;
    private static Set<Position> forbiddenPositions;

    private Set<NewComponent> reservedComponents;

    // Ship stats
    private double cannons;
    private double engines;
    private int batteries;
    private int crew;
    private Map<GoodType, Integer> resources;
    private int specialGoods;
    private int normalGoods;

    private int lostComponents;


    public NewShip(Player player) {
        this.player = player;
    }


    /**
     * Adds a component to the ship at the specified position on the board.
     * @param component The component to add.
     * @param position  The position on the grid where the component should be placed.
     */
    public void addComponent(NewComponent component, Position position) {
        int x = position.getX();
        int y = position.getY();

        // Checks if position is illegal, otherwise adds Component and updates its position attribute
        if (forbiddenPositions.contains(position)) {
            throw new IllegalArgumentException("Forbidden position");
        }
        else if (board[x][y] != null) {
            throw new IllegalArgumentException("Occupied position");
        }
        else {
            board[x][y] = component;
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
        else if (board[x][y] != null) {
            throw new IllegalArgumentException("Occupied position");
        }
        else {
            board[x][y].setPosition(null);    // Spostare il component in lista dei "rifiuti"?
            board[x][y] = null;
        }
    }

    /**
     * Reserves a component for future use, allowing it to be kept aside without attaching it to the ship.
     * If there are already 2 reserved components, the first one is removed to make room for the new component.
     *
     * @param component The component to reserve.
     */
    public void reserveComponent(NewComponent component) {
        if (reservedComponents.size() < 2) {
            reservedComponents.add(component);
        }
        else {
            throw new IllegalArgumentException("Too many reserved components");
        }
    }


    public NewComponent[][] getBoard() {
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

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                if (board[x][y] != null) {
                    board[x][y].count(this);
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
}