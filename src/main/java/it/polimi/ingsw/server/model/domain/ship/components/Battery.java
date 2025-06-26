package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

/**
 * Battery component that stores battery tokens according to Galaxy Trucker rules.
 * Batteries are consumed (trashed) when used for power - no complex state management.
 */
public class Battery extends Component {
    private int maxBatteries;
    private int availableBatteries;

    public Battery(ComponentType type, Map<Direction, ConnectorType> connectors, int maxBatteries, String id) {
        super(type, connectors, id);
        this.maxBatteries = maxBatteries;
        this.availableBatteries = maxBatteries;
    }

    @Override
    public void use(UseComponentVisitor v) {
        v.useBattery(this.getShip(), this, v.getQuantity());
    }

    @Override
    public void count(Ship ship) {
        ship.setBatteries(ship.getBatteries() + availableBatteries);
    }

    @Override
    public boolean check(Ship ship) {
        // Batteries need proper connection to ship's power grid
        return hasValidPowerConnection(ship);
    }

    /**
     * Checks if battery has valid connection to ship's power systems.
     */
    private boolean hasValidPowerConnection(Ship ship) {
        Component[][] board = ship.getBoard();
        
        for (Direction d : Direction.values()) {
            if (this.getPosition() != null) {
                var neighbor = this.getPosition().offsetBy(d);
                int row = neighbor.getRow();
                int col = neighbor.getCol();

                if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                    Component neighborComponent = board[row][col];
                    if (neighborComponent != null) {
                        ConnectorType thisConnector = this.getConnectorAt(d);
                        ConnectorType neighborConnector = neighborComponent.getConnectorAt(d.getOpposite());
                        
                        if (thisConnector != ConnectorType.PLAIN && neighborConnector != ConnectorType.PLAIN
                            && thisConnector == neighborConnector) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Consumes battery tokens according to Galaxy Trucker rules.
     * Batteries are permanently consumed (returned to bank) when used.
     * @param amount The number of batteries to consume
     * @return The actual number of batteries consumed
     */
    public int consumeBatteries(int amount) {
        int consumed = Math.min(amount, availableBatteries);
        availableBatteries -= consumed;
        return consumed; // These batteries are gone forever
    }

    /**
     * Checks if this battery component has enough batteries available.
     * @param amount The number of batteries needed
     * @return true if enough batteries are available
     */
    public boolean hasEnoughBatteries(int amount) {
        return availableBatteries >= amount;
    }

    // Simple getters for Galaxy Trucker rules
    public int getMaxBatteries() {
        return maxBatteries;
    }

    public int getAvailableBatteries() {
        return availableBatteries;
    }

    public boolean isEmpty() {
        return availableBatteries <= 0;
    }

    @Override
    public String toString() {
        return String.format("Battery{available=%d, max=%d}", 
                           availableBatteries, maxBatteries);
    }
}
