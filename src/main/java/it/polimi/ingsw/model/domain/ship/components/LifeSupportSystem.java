package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.crew.AlienColor;
import it.polimi.ingsw.model.enums.crew.CrewType;

public class LifeSupportSystem extends Component {
    private AlienColor supportedAlienColor;

    // returns supported alien color
    public AlienColor getSupportedAlienColor() {return null;}

    // returns true if given crew type is supported, false otherwise
    public boolean supportsCrewType(CrewType crewType) {return false;}
}
