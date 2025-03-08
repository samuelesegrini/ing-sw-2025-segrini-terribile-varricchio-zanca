package it.polimi.ingsw.model.adventure;

public enum PileIdentifier {
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    UNKNOWN;
    private int index;
    private boolean predictable;

    //c'è da fare un costruttore????

    //mancano tutti i commenti ma non ho capito molto di questa cosa che sembra una mezza enumerazione mezza classe normale :))

    public int getIndex(){
        return 0;
    }
    public boolean isPredictable(){
        return false;
    }
    public static void PileIdentifier fromIndex(int index){ }
    public static void PileIdentifier[] getPredictablePiles(GameLevel){}
}
