package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.crew.CrewType;

public class Cabin extends Component{
    private CrewType currentCrew;
    private int crewCount;

    /**
    * Retrieves the number of crew members currently in the cabin.
    * @return the number of crew members in the cabin
    */
    public int getCrewCount(){ return 0; }

    /**
     * Indicates the crew type in the cabin.
     * If they are aliens, there can be at most one. If they are people, the cabin can hold up to two
     * @return crew's type
     */
    public CrewType getCurrentCrew(){ return null; }

    /**
     * Update the number of crew members currently in the cabin.
     * @param type crew type (purple alien, brown alien or human)
     * @param count number of crew members to subtract
     * @return true if the number of crew members in the cabin is at least equal to
     * the number of crew members to subtract, false otherwise. If true, proceeds to subtract the amount passed as a parameter
     */
    public boolean setCrew(CrewType type, int count){ return false; }

    /**
     * Indicates if the cabin is connected to a life support system,
     * meaning it can have aliens as crew members.
     * @return true if the cabin is connected to a life support system, false otherwise
     */
    public boolean hasMatchingLifeSupport(){ return false; }
}

