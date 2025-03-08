package it.polimi.ingsw.model.adventure;

public abstract class EnemyCard {

    private int powerLevel;
    private int movementPenalty;

    //empty constructor
    public EnemyCard(){}

    public int getPowerLevel(){
        return 0;
    }
    public int getMovementPenalty(){
        return 0;
    }
    public boolean canSkipMovementPenalty(){
        return false;
    }
}
