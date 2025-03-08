package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.crew.CrewType;

public class Cabin extends Component{
    //if crewType is alien, then crewCount must be exactly one at the beginning
    private CrewType currentCrew;
    private int crewCount;

    public int getCrewCount() {
        return 0;
    }

    public CrewType getCurrentCrew() {
        return null;
    }

    public boolean setCrew(CrewType type, int count) {
        return false;
    }

    // In order for the life support system to have any effect, it must be joined to a cabin
    public boolean hasMatchingLifeSupport() {
        return false;
    }
}

