package it.polimi.ingsw.model.domain.ship.components;

public class Shield extends NewComponent{
    @Override
    public void accept(ComponentVisitor v) {
        v.useShield(this);
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