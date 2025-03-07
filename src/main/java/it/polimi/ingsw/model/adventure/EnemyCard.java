package it.polimi.ingsw.model.adventure;

public abstract class EnemyCard {

    private int powerLevel;
    private int movementPenalty;

    //empty constructor
    public EnemyCard(){}

    public int getPowerLevel(){}
    public int getMovementPenalty(){}
    public boolean canSkipMovementPenalty(){}
}
