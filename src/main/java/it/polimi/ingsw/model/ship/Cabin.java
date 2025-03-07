package it.polimi.ingsw.model.ship;

public class Cabin exthends Component{
    //if crewType is alien, then crewCount must be exactly one at the beginning
    private CrewType currentCrew;
    private int crewCount;
    public int getCrewCount();
    public CrewType getCurrentCrew();
    public boolean setCrew(CrewType type, int count);
    // In order for the life support system to have any effect, it must be joined to a cabin
    public boolean hasMatchingLifeSupport();
}

