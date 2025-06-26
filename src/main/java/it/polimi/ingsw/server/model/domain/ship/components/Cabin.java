package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.crew.CrewType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.Map;

public class Cabin extends Component {
    private CrewType currentCrewType;
    private int maxCrew;
    private int currentCrew;

    public Cabin(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
        this.maxCrew = 2;
        this.currentCrew = this.maxCrew;
        this.currentCrewType = CrewType.HUMAN;
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useCabin(this.getShip(), this, v.getQuantity());
    }

    @Override
    public void count(Ship ship) {
        ship.setCrew(ship.getCrew() + currentCrew);

        // Apply alien bonuses only if base stats > 0 (Galaxy Trucker rules)
        if (currentCrewType == CrewType.ALIEN_PURPLE && ship.getCannons() > 0) {
            ship.setCannons(ship.getCannons() + 2);
        }
        else if (currentCrewType == CrewType.ALIEN_BROWN && ship.getEngines() > 0) {
            ship.setEngines(ship.getEngines() + 2);
        }
    }

    /**
     * Enhanced crew placement validation with proper life support checking.
     * Validates that crew members have appropriate life support systems.
     */
    @Override
    public boolean check(Ship ship) {
        Component[][] board = ship.getBoard();
        boolean hasValidConnection = false;
        boolean hasCorrectLifeSupport = false;

        for (Direction d : Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                Component neighborComponent = board[row][col];
                if (neighborComponent != null) {
                    // Check if there's a valid connector connection
                    ConnectorType thisConnector = this.getConnectorAt(d);
                    ConnectorType neighborConnector = neighborComponent.getConnectorAt(d.getOpposite());
                    
                    if (thisConnector != ConnectorType.PLAIN && neighborConnector != ConnectorType.PLAIN
                        && thisConnector == neighborConnector) {
                        hasValidConnection = true;
                        
                        // Check life support requirements based on crew type
                        if (currentCrewType == CrewType.HUMAN) {
                            // Humans just need any life support or structural connection
                            if (neighborComponent.getType() == ComponentType.LIFE_SUPPORT_PURPLE
                                || neighborComponent.getType() == ComponentType.LIFE_SUPPORT_BROWN
                                || neighborComponent.getType() == ComponentType.STRUCTURAL) {
                                hasCorrectLifeSupport = true;
                            }
                        }
                        else if (currentCrewType == CrewType.ALIEN_PURPLE) {
                            // Purple aliens need purple life support
                            if (neighborComponent.getType() == ComponentType.LIFE_SUPPORT_PURPLE) {
                                hasCorrectLifeSupport = true;
                            }
                        }
                        else if (currentCrewType == CrewType.ALIEN_BROWN) {
                            // Brown aliens need brown life support
                            if (neighborComponent.getType() == ComponentType.LIFE_SUPPORT_BROWN) {
                                hasCorrectLifeSupport = true;
                            }
                        }
                    }
                }
            }
        }
        
        return hasValidConnection && (currentCrewType == CrewType.HUMAN || hasCorrectLifeSupport);
    }


    public CrewType getCurrentCrewType() {
        return currentCrewType;
    }

    public void setCurrentCrewType(CrewType currentCrewType) {
        this.currentCrewType = currentCrewType;

        // Update crew capacity based on type (Galaxy Trucker rules)
        if (currentCrewType == CrewType.HUMAN) {
            this.maxCrew = 2;  // Humans: 2 per cabin
            this.currentCrew = Math.min(this.currentCrew, this.maxCrew);
        }
        else {
            this.maxCrew = 1;  // Aliens: 1 per cabin
            this.currentCrew = Math.min(this.currentCrew, this.maxCrew);
        }
    }

    public int getMaxCrew() {
        return maxCrew;
    }

    public void setMaxCrew(int maxCrew) {
        this.maxCrew = maxCrew;
    }

    public int getCurrentCrew() {
        return currentCrew;
    }

    public void setCurrentCrew(int currentCrew) {
        this.currentCrew = currentCrew;
    }
}
