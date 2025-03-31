package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.crew.AlienColor;

public class LifeSupportSystem extends Component {
    private AlienColor supportedAlienColor;


    public AlienColor getSupportedAlienColor() {
        return supportedAlienColor;
    }

    public void setSupportedAlienColor(AlienColor supportedAlienColor) {
        this.supportedAlienColor = supportedAlienColor;
    }
}
