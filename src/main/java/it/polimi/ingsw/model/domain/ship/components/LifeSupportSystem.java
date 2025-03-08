package it.polimi.ingsw.model.ship.components;

import it.polimi.ingsw.model.enums.crew.AlienColor;

public class LifeSupportSystem {
    private AlienColor supportedAlienColor;

    public AlienColor getSupportedAlienColor() {return null;}
    public boolean supportsCrewType(CrewType crewType) {return false;}
}
