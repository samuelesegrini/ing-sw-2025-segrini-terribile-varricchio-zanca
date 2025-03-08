package it.polimi.ingsw.model.domain.adventure.entity;

import it.polimi.ingsw.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.model.enums.ship.Direction;

public class CannonFire {
    private Direction direction;
    private ShotIntensity intensity;

    //constructor da finire
    public CannonFire() {}

    public Direction getDirection(){
        return null;
    }
    public ShotIntensity getIntensity(){
        return null;
    }
    public boolean isBlockable(){
        return false;
    }
}
