package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.crew.AlienColor;
import it.polimi.ingsw.model.enums.crew.CrewType;

public class LifeSupportSystem {
    private AlienColor supportedAlienColor;

    public AlienColor getSupportedAlienColor() {return null;}
    public boolean supportsCrewType(CrewType crewType) {return false;}
}
