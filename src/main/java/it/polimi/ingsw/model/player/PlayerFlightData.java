package it.polimi.ingsw.model.player;

public class PlayerFlightData {
    private int position;
    private int lapsCompleted;
    private FlightStatus status;

    /**
     * Constructor that sets the starting position.
     * @param startPosition representing the player's position on Route
     * @see it.polimi.ingsw.model.flightboard.Route
     */
    public PlayerFlightData (int startPosition){this.position=startPosition};

    /**
     * Return player's position on Route
     * @return player's position on Route
     * @see it.polimi.ingsw.model.flightboard.Route
     */
    public int getPosition(){ return position; }

    /**
     * Return the normalized player's position based on number of laps completed
     * @return normalized player's position
     */
    public int getAbsolutePosition(){};

    /**
     * Sets the normalized player's position, based on number of laps completed.
     * @param position player's position on Route
     * @param routeLength Route's lenght
     * @see it.polimi.ingsw.model.flightboard.Route
     */
    public void setPosition(int position, int routeLength){};

    /**
     * Return the number of laps completed by the player.
     * @return the number of laps completed
     */
    public int getLapsCompleted() {return lapsCompleted;}

    /**
     *Returns the player's flight status: still in the race or abandoned.
     * @return player's flight status
     */
    public FlightStatus getStatus(){};

    /**
     * Update the player's flight status
     * @param status new status to set
     */
    public void setStatus (FlightStatus status){};
}
