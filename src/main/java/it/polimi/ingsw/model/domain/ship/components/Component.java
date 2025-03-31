package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.Map;

public abstract class Component {
    protected ComponentType type;
    protected Direction direction;
    protected Position position;
    protected Map<Direction, ConnectorType> connectors;
    protected Ship ship;


    public Component(ComponentType type, Map<Direction, ConnectorType> connectors) {
        this.type = type;
        this.direction = Direction.UP;
        this.position = null;
        this.connectors = connectors;
        this.ship = null;
    }


    public ComponentType getType() {
        return type;
    }

    /**
     * Returns the component's direction. If it hasn't been rotated
     * or has been rotated by multiples of 360 degrees, the direction will be UP.
     * @return direction of the component
     */
    public Direction getDirection() {
        return direction;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Map<Direction, ConnectorType> getConnectors() {
        return connectors;
    }

    /**
     * Given a direction it returns the connector type.
     * @param direction direction of interest
     * @return Returns the connector type at the specified direction
     */
    public ConnectorType getConnectorAt(Direction direction) {
        return connectors.get(direction);
    }


    /**
     * Rotates the component 90 degrees clockwise from the direction stored in the direction attribute.
     */
    public void rotate() {
        direction = direction.rotateClockwise();

        Map<Direction, ConnectorType> temp = new HashMap<>(connectors);
        for (Direction d : Direction.values()) {
            connectors.put(d, temp.get(d.rotateCounterClockwise()));
        }
    }


    public void accept(ComponentVisitor visitor) {}


    // These methods are overridden by the subclasses

    // Updates the ship's stats adding this component's contributions
    public void count(Ship s) {}

    // Checks if the component is still connected to at least one other component, and other specific conditions
    public boolean check(Ship s) {
        Component[][] board = s.getBoard();

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int x = neighbor.getX();
            int y = neighbor.getY();

            // Checks if there's a component nearby and if they're connected (assumes the connection is legal)
            if (board[x][y] != null && board[x][y].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                return true;
            }
        }
        return false;
    }
}
