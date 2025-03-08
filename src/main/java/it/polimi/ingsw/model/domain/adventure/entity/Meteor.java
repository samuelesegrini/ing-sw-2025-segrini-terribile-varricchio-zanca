package it.polimi.ingsw.model.domain.adventure.entity;

import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.ship.Direction;

public class Meteor {

    private ShotIntensity intensity;
    private Direction approach;

    //constructor
    public Meteor(ShotIntensity intensity, Direction approach) {
        this.intensity = intensity;
        this.approach = approach;
    }


    public ShotIntensity getType(){
        return null;
    }

    public Direction getApproach(){
        return null;
    }

    public boolean canBeBlockedByShield(){
        return false;
    }

    public boolean canBeDestroyedByCannon(){
        return false;
    }
}
