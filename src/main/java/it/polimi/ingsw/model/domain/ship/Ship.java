package it.polimi.ingsw.model.domain.ship;
import java.util.List;
import java.util.Map;

import it.polimi.ingsw.model.domain.ship.ShipBoardLayout;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.domain.ship.Grid;
import it.polimi.ingsw.model.enums.resource.GoodType;

public class Ship {
    private Grid<Component> grid;
    private ShipBoardLayout layout;
    private int lostComponents;
    private List<Component> reservedComponents;
    private Map<GoodType, Integer> resources;

    public Ship(ShipBoardLayout layout) {};

    /**
     * Adds a component to the ship at the specified position on the grid.
     * @param component The component to add.
     * @param position The position on the grid where the component should be placed.
     */
    public void addComponent(Component component, Position position) {};

    /**
     * Removes the component from the ship at the specified position on the grid.
     * @param position The position on the grid from which the component should be removed.
     */
    public void removeComponent(Position position) {};

    /**
     * Reserves a component for future use, allowing it to be kept aside without attaching it to the ship.
     * @param component The component to reserve.
     */
    public void reserveComponent(Component component) {};

    /**
     * Calculates and returns the total engine strength of the ship.
     * @return The engine strength.
     */
    public double getEngineStrength() {return 0.0;}

    /**
     * Calculates and returns the total cannon strength of the ship.
     * @return The cannon strength.
     */
    public double getCannonStrength() {return 0.0;}

    /**
     * Returns the number of crew members assigned to the ship.
     * @return The crew number.
     */
    public int getCrewNumber() {return 0;}
}