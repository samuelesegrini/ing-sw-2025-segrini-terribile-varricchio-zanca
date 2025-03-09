package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;
import it.polimi.ingsw.model.domain.ship.Ship;
import java.util.Map;

public abstract class Component {
    private ComponentType type;
    private Direction facing;
    private Position position;
    private Map<Direction, ConnectorType> connectors;

    // rotates component
    public void rotate() {}

    // places component at the given position for the given ship
    public void place(Position p, Ship s) {}

    // returns component type
    public ComponentType getType() {return null;}

    // returns what direction the component is facing
    public Direction getFacing() {return null;}

    // returns connector at the given direction
    public ConnectorType getConnectorAt(Direction direction) {return null;}

    // returns a map with all directions and corresponding connector type
    public Map<Direction, ConnectorType> getConnectors() {return null;}

    // returns true if component can be placed at the given position, false otherwise
    public boolean canBePlacedAt(Position position) {return false;}
}