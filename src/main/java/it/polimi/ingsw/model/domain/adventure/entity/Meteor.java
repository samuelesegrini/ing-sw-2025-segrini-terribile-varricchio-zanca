package it.polimi.ingsw.model.domain.adventure.entity;

import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Meteor {

    private ShotIntensity intensity;
    private Direction approach;

    /**
     * Constructs a new Meteor with the specified intensity and approach direction.
     *
     * @param intensity The intensity of the meteor.
     * @param approach The direction in which the meteor is approaching the ship.
     */
    public Meteor(ShotIntensity intensity, Direction approach) {
        this.intensity = intensity;
        this.approach = approach;
    }

    /**
     * Gets the intensity type of the meteor.
     *
     * @return The {@link ShotIntensity} value that represents the meteor's intensity.
     */
    public ShotIntensity getType(){
        return null;
    }

    /**
     * Gets the direction in which the meteor is approaching the ship.
     *
     * @return The {@link Direction} representing the meteor's approach.
     */
    public Direction getApproach(){
        return null;
    }

    /**
     * Determines if the meteor can be blocked by the ship's shield.
     * Depending on the meteor's intensity and direction, it may be possible for the player to block it using a shield.
     *
     * @return {@code true} if the meteor can be blocked by the shield, otherwise {@code false}.
     */
    public boolean canBeBlockedByShield(){
        return false;
    }

    /**
     * Determines if the meteor can be destroyed by the ship's cannon.
     * Depending on the meteor's intensity and direction, it may or may not be destroyable by the player's cannon.
     *
     * @return {@code true} if the meteor can be destroyed by the cannon, otherwise {@code false}.
     */
    public boolean canBeDestroyedByCannon(){
        return false;
    }
}
