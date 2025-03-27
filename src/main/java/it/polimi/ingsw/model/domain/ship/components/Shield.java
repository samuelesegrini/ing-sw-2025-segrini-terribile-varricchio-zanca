package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.Set;

public class Shield extends NewComponent{
    private Set<Direction> protectedDirections;

    @Override
    public void accept(ComponentVisitor v) {
        v.useShield(this);
    }


    public Set<Direction> getProtectedDirections() {
        return protectedDirections;
    }

    public void setProtectedDirections(Set<Direction> protectedDirections) {
        this.protectedDirections = protectedDirections;
    }
}










/*
/**
     * Indicates the directions in which the shield provides protection.
     * @return the shielded directions

public Set<Direction> getProtectedDirections(){ return Set.of(); }

/**
 * Checks if the shield protects in a specific direction.
 * @param d direction
 * @return {@code true} if the shield cover the specified direction, {@code false} otherwise

public boolean protectsFromDirection(Direction d){ return false; }
 */