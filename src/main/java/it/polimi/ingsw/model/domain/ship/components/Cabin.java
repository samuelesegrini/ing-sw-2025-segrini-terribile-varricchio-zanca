package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.enums.crew.CrewType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Cabin extends Component {
    private CrewType currentCrew;
    private int crewCount;

    @Override
    public void accept(ComponentVisitor v) {
        int quantity = 0;
        v.useCabin(this, quantity);
    }

    @Override
    public void count(Ship s) {
        s.setCrew(s.getCrew() + crewCount);

        // TODO: SE CANNONS È ZERO, IL BONUS DELL'ALIENO NON CONTA
        if (currentCrew == CrewType.ALIEN_PURPLE) {
            s.setCannons(s.getCannons() + 2);
        }
        else if (currentCrew == CrewType.ALIEN_BROWN) {
            s.setEngines(s.getEngines() + 2);
        }
    }

    // Also checks in the case of an alien if there is the corresponding life support system
    @Override
    public boolean check(Ship s) {
        Component[][] board = s.getBoard();

        for (Direction d :  Direction.values()) {
            Position neighbor = this.getPosition().offsetBy(d);
            int x = neighbor.getX();
            int y = neighbor.getY();

            if (board[x][y] != null && board[x][y].getConnectorAt(d.getOpposite()) != ConnectorType.PLAIN) {
                if (currentCrew == CrewType.HUMAN) {
                    return true;
                }
                else if (currentCrew == CrewType.ALIEN_PURPLE && board[x][y].getType() == ComponentType.LIFE_SUPPORT_PURPLE) {
                    return true;
                }
                else if (currentCrew == CrewType.ALIEN_BROWN && board[x][y].getType() == ComponentType.LIFE_SUPPORT_BROWN) {
                    return true;
                }
            }
        }
        return false;
    }


    public CrewType getCurrentCrew() {
        return currentCrew;
    }

    public void setCurrentCrew(CrewType currentCrew) {
        this.currentCrew = currentCrew;
    }

    public int getCrewCount() {
        return crewCount;
    }

    public void setCrewCount(int crewCount) {
        this.crewCount = crewCount;
    }
}





























/*
public class Cabin extends Component{
    private CrewType currentCrew;
    private int crewCount;

    /**
    * Retrieves the number of crew members currently in the cabin.
    * @return the number of crew members in the cabin

public int getCrewCount(){ return this.crewCount; }

        /**
         * Indicates the crew type in the cabin.
         * If they are aliens, there can be at most one. If they are people, the cabin can hold up to two
         * @return crew's type

        public CrewType getCurrentCrew(){ return this.currentCrew; }

        /**
         * Update the number of crew members currently in the cabin.
         * @param type crew type (purple alien, brown alien or human)
         * @param count number of crew members to subtract
         * @return {@code true} if the number of crew members in the cabin is at least equal to
         * the number of crew members to subtract, {@code false} otherwise. If {@code true}, proceeds to subtract the amount passed as a parameter

        public boolean setCrew(CrewType type, int count){ return false; }

        /**
         * Indicates if the cabin is connected to a life support system,
         * meaning it can have aliens as crew members.
         * @return {@code true} if the cabin is connected to a life support system, {@code false} otherwise

        public boolean hasMatchingLifeSupport(){ return false; }
}
 */
