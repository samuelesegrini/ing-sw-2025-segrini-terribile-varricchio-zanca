package it.polimi.ingsw.model.ship;

import java.util.Map;

public abstract class Component {
    private ComponentType type;
    //indicates the direction the component is facing, useful for tracking any rotations of the component.
    private Direction facing;
    private Position position;
    private Map<Direction,ConnectorType> connectors;

    public void rotate(){};
    public void place(Position p,Ship s){};
    public boolean canBePlacedAt(Position ){};
    public ComponentType getType(){};
    public Direction getFacing(){};
    public ConnectorType getConnectorAt(Direction){};
    public Map<Direction, ConnectorType> getConnectors(){};
}
