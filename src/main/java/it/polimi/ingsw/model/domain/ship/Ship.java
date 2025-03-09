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

    public Ship(ShipBoardLayout layout) {}

    // adds component at the given position
    public void addComponent(Component component, Position position) {}

    // removes component at the given position
    public void removeComponent(Position position) {}

    // adds component to reservedComponents
    public void reserveComponent(Component component) {}

    // returns ship's total engine strength
    public double getEngineStrength() {return 0.0;}

    // returns ship's total cannon strength
    public double getCannonStrength() {return 0.0;}

    // returns ship's total crew number
    public int getCrewNumber() {return 0;}
}