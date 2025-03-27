package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.crew.AlienColor;

public class LifeSupportSystem extends NewComponent {
    private AlienColor supportedAlienColor;


    public AlienColor getSupportedAlienColor() {
        return supportedAlienColor;
    }

    public void setSupportedAlienColor(AlienColor supportedAlienColor) {
        this.supportedAlienColor = supportedAlienColor;
    }
}
















/*
public class LifeSupportSystem extends Component {
    private AlienColor supportedAlienColor;

    /**
     * Returns the color of the alien that can be hosted in the cabin
     * when this life support system component is attached to it.
     *
     * @return the color of the alien that can be placed in the cabin.

public AlienColor getSupportedAlienColor() {
    return this.supportedAlienColor;
}

        // returns true if given crew type is supported, false otherwise
        /**
         * This method checks if the life support system is compatible with the given crew type in the cabin.
         *
         * @param crewType the type of crew to check for life support compatibility.
         * @return {@code true} if the life support system supports the given crew type, {@code false} otherwise.

        public boolean supportsCrewType(CrewType crewType) {
            return ((crewType.toString()).equals(this.supportedAlienColor.toString()));
        }
}
 */
