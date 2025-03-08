package it.polimi.ingsw.model.ship;

import it.polimi.ingsw.model.ship.components.ComponentType;
import it.polimi.ingsw.model.ship.components.ConnectorType;

import java.util.Map;

public abstract class Component {
    private ComponentType type;
    //indicates the direction the component is facing, useful for tracking any rotations of the component.
    private Direction facing;
    private Position position;
    private Map<Direction, ConnectorType> connectors;

    public void rotate() { }

    public void place(Position position, Ship ship) {}

    public boolean canBePlacedAt(Position position) {
        return false;
    }

    public ComponentType getType() {
        return null;
    }

    public Direction getFacing() {
        return null;
    }

    public ConnectorType getConnectorAt(Direction direction) {
        return null;
    }

    public Map<Direction, ConnectorType> getConnectors() {
        return null;
    }
}
