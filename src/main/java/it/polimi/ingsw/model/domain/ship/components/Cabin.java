package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Map;

public class Cabin extends Component {
    private CrewType currentCrewType;
    private int maxCrew;
    private int currentCrew;


    public Cabin(ComponentType type, Map<Direction, ConnectorType> connectors) {
        super(type, connectors);
        this.maxCrew = 2;
        this.currentCrew = this.maxCrew;
        this.currentCrewType = CrewType.HUMAN;
    }


    @Override
    public void accept(ComponentVisitor v) {
        int quantity = 0;
        v.useCabin(super.ship, this, quantity);
    }

    @Override
    public void count(Ship ship) {
        ship.setCrew(ship.getCrew() + currentCrew);

        // TODO: SE CANNONS È ZERO, IL BONUS DELL'ALIENO NON CONTA
        if (currentCrewType == CrewType.ALIEN_PURPLE) {
            ship.setCannons(ship.getCannons() + 2);
        }
        else if (currentCrewType == CrewType.ALIEN_BROWN) {
            ship.setEngines(ship.getEngines() + 2);
        }
    }

    // Also checks in the case of an alien if there is the corresponding life support system
    @Override
    public boolean check(Ship ship) {
        Component[][] board = ship.getBoard();

        for (Direction d :  Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int row = neighbor.getRow();
            int col = neighbor.getCol();

            if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                if (board[row][col] != null && board[row][col].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                    if (currentCrewType == CrewType.HUMAN) {
                        return true;
                    }
                    else if (currentCrewType == CrewType.ALIEN_PURPLE && board[row][col].getType() == ComponentType.LIFE_SUPPORT_PURPLE) {
                        return true;
                    }
                    else if (currentCrewType == CrewType.ALIEN_BROWN && board[row][col].getType() == ComponentType.LIFE_SUPPORT_BROWN) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public CrewType getCurrentCrewType() {
        return currentCrewType;
    }

    public void setCurrentCrewType(CrewType currentCrewType) {
        this.currentCrewType = currentCrewType;

        if (currentCrewType == CrewType.HUMAN) {
            this.maxCrew = 2;
        }
        else {
            this.maxCrew = 1;
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
