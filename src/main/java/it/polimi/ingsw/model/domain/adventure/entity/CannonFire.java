package it.polimi.ingsw.model.domain.adventure.entity;

import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.ship.Direction;

public class CannonFire {
    private Direction approach;
    private ShotIntensity intensity;

    public CannonFire(Direction approach, ShotIntensity intensity) {
        this.approach= approach;
        this.intensity = intensity;
    }

    /**
     * Gets the direction from which the cannon fire originates.
     * @return The direction of the shot.
     */
    public Direction getApproach() {return this.approach;}

    /**
     * Gets the intensity of the cannon fire.
     * @return The intensity level of the shot (e.g., light or heavy).
     */
    public ShotIntensity getIntensity(){
        return this.intensity;
    }

    /**
     * Checks if the cannon fire can be blocked by a shield.
     * @return {@code true} if the shot is blockable, {@code false} otherwise.
     */
    public boolean isBlockable(){
        if (intensity == ShotIntensity.LIGHT)
            return true;
        return false;
    }
}
