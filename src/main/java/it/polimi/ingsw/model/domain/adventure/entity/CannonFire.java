package it.polimi.ingsw.model.domain.adventure.entity;

import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.ship.Direction;

public class CannonFire {
    private Direction direction;
    private ShotIntensity intensity;

    public CannonFire() {};

    /**
     * Gets the direction from which the cannon fire originates.
     *
     * @return The direction of the shot.
     */
    public Direction getDirection(){
        return null;
    }

    /**
     * Gets the intensity of the cannon fire.
     *
     * @return The intensity level of the shot (e.g., light or heavy).
     */
    public ShotIntensity getIntensity(){
        return null;
    }

    /**
     * Checks if the cannon fire can be blocked by a shield.
     *
     * @return {@code true} if the shot is blockable, {@code false} otherwise.
     */
    public boolean isBlockable(){
        return false;
    }
}
