package it.polimi.ingsw.model.adventure;

import javafx.scene.control.skin.TextInputControlSkin;

public class Meteor {

    private ShotIntensity intensity;
    private Direction approach;

    //constructor
    public Meteor(ShotIntensity intensity, Direction approach) {
        this.intensity = intensity;
        this.approach = approach;
    }


    public MeteorType getType(){}

    public Direction getApproach(){}

    public boolean canBeBlockedByShield(){}

    public boolean canBeDestroyedByCannon(){}
}
