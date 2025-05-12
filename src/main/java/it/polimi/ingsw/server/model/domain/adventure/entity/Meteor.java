package it.polimi.ingsw.server.model.domain.adventure.entity;

import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;

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
        return intensity;
    }

    /**
     * Gets the direction in which the meteor is approaching the ship.
     *
     * @return The {@link Direction} representing the meteor's approach.
     */
    public Direction getApproach(){return approach;}
    public ShotIntensity getShotIntensity() { return intensity; }
}
