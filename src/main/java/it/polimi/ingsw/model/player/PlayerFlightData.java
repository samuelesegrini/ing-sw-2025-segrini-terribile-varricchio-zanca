package it.polimi.ingsw.model.player;

public class PlayerFlightData {
    private int position;
    private int lapsCompleted;
    private FlightStatus status;

    //cpnstructor defined by user
    public PlayerFlightData(int startPosition);
    public int getPosition (){ return position; }
    //normalize position based on number of laps completed
    public int getAbsolutePosition();
    public void setPosition(int position, int routeLength);
    public int getLapsCompleted() {return lapsCompleted;};
    public FlightStatus getStatus();
    public void setStatus (FlightStatus status);
}
