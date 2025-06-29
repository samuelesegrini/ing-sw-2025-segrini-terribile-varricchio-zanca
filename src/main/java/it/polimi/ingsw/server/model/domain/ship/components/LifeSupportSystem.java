package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.crew.AlienColor;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Life Support System component that provides life support for crew members.
 * Different types support different alien colors according to Galaxy Trucker rules.
 */
public class LifeSupportSystem extends Component {
    private static final long serialVersionUID = 1L;
    
    private AlienColor supportedAlienColor;
    private int lifeSupportCapacity;

    public LifeSupportSystem(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
        
        // Set supported alien color and capacity based on component type
        switch (type) {
            case LIFE_SUPPORT_PURPLE -> {
                this.supportedAlienColor = AlienColor.ALIEN_PURPLE;
                this.lifeSupportCapacity = 2;
            }
            case LIFE_SUPPORT_BROWN -> {
                this.supportedAlienColor = AlienColor.ALIEN_BROWN;
                this.lifeSupportCapacity = 2;
            }
            default -> {
                this.supportedAlienColor = null;
                this.lifeSupportCapacity = 1;
            }
        }
    }

    @Override
    public void use(UseComponentVisitor v) {
        // Life support systems don't have direct use actions
    }

    @Override
    public void count(Ship ship) {
        // Life support systems contribute to ship's life support capacity
        // Note: Ship class handles life support counting internally
    }

    @Override
    public boolean check(Ship ship) {
        // Life support systems need proper connection to ship structure
        return hasValidStructuralConnection(ship);
    }

    /**
     * Checks if this life support system has a valid connection to ship structure.
     */
    private boolean hasValidStructuralConnection(Ship ship) {
        Component[][] board = ship.getBoard();
        
        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                Component neighborComponent = board[row][col];
                if (neighborComponent != null) {
                    ConnectorType thisConnector = this.getConnectorAt(d);
                    ConnectorType neighborConnector = neighborComponent.getConnectorAt(d.getOpposite());
                    
                    // Valid connection if connectors match and are not plain
                    if (thisConnector != ConnectorType.PLAIN && neighborConnector != ConnectorType.PLAIN
                        && thisConnector == neighborConnector) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Gets all connected cabins that this life support can serve.
     */
    public List<Component> getConnectedCabins(Ship ship) {
        List<Component> connectedCabins = new ArrayList<>();
        Component[][] board = ship.getBoard();
        
        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                Component neighborComponent = board[row][col];
                if (neighborComponent != null && (neighborComponent.getType() == ComponentType.CABIN || 
                                                    neighborComponent.getType() == ComponentType.CABIN_START)) {
                    ConnectorType thisConnector = this.getConnectorAt(d);
                    ConnectorType neighborConnector = neighborComponent.getConnectorAt(d.getOpposite());
                    
                    if (thisConnector != ConnectorType.PLAIN && neighborConnector != ConnectorType.PLAIN
                        && thisConnector == neighborConnector) {
                        connectedCabins.add(neighborComponent);
                    }
                }
            }
        }
        return connectedCabins;
    }

    /**
     * Checks if this life support system can support the given alien color.
     */
    public boolean canSupport(AlienColor alienColor) {
        // Universal life support can support any alien, specific ones only support their color
        return supportedAlienColor == null || supportedAlienColor == alienColor;
    }

    public AlienColor getSupportedAlienColor() {
        return supportedAlienColor;
    }

    public void setSupportedAlienColor(AlienColor supportedAlienColor) {
        this.supportedAlienColor = supportedAlienColor;
    }

    public int getLifeSupportCapacity() {
        return lifeSupportCapacity;
    }

    public void setLifeSupportCapacity(int lifeSupportCapacity) {
        this.lifeSupportCapacity = lifeSupportCapacity;
    }
}
