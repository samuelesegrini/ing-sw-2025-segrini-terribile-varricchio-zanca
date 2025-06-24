package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Component {
    protected String id;
    protected ComponentType type;
    protected Direction direction;
    protected Position position;
    protected Map<Direction, ConnectorType> connectors;
    protected Ship ship;
    protected String reservedBy; // Player ID who reserved this component


    public Component(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        this.type = type;
        this.direction = Direction.UP;
        this.position = null;
        this.connectors = connectors;
        this.ship = null;
        this.id = id;
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

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
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


    public void use(UseComponentVisitor v) {}


    // These methods are overridden by the subclasses

    // Updates the ship's stats adding this component's contributions
    public void count(Ship ship) {}

    // Checks if the component is still connected to at least one other component, and other specific conditions
    public boolean check(Ship ship) {
        Component[][] board = ship.getBoard();

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            // Checks if there's a component nearby and if they're connected (assumes the connection is legal)
            if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                if (board[row][col] != null && board[row][col].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }
    
    /**
     * Gets the ID of the player who reserved this component
     * @return Player ID or null if not reserved
     */
    public String getReservedBy() {
        return reservedBy;
    }
    
    /**
     * Sets the player who reserved this component
     * @param playerId Player ID or null to unreserve
     */
    public void setReservedBy(String playerId) {
        this.reservedBy = playerId;
    }
    
    /**
     * Checks if this component is reserved by any player
     * @return true if reserved, false otherwise
     */
    public boolean isReserved() {
        return reservedBy != null;
    }
}
