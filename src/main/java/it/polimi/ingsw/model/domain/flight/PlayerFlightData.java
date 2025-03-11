package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.enums.flight.FlightStatus;

public class PlayerFlightData {
    private int position;
    private int lapsCompleted;
    private FlightStatus status;
    private int startPosition;

    /**
     * Constructor that sets the starting position.
     * @param startPosition representing the player's position on Route
     * @see it.polimi.ingsw.model.domain.flight.Route
     */
    public PlayerFlightData (int startPosition){
        this.position = startPosition;
        this.startPosition = startPosition;
    }

    /**
     * Return player's position on Route
     * @return player's position on Route
     * @see it.polimi.ingsw.model.domain.flight.Route
     */
    public int getPosition(){
        return position;
    }

    /**
     * Return player's start position on Route
     * @return player's start position on Route
     * @see it.polimi.ingsw.model.domain.flight.Route
     */
    public int getStartPosition(){
        return startPosition;
    }

    /**
     * Return the number of laps completed by the player.
     * @return the number of laps completed
     */
    public int getLapsCompleted() {
        return lapsCompleted;
    }

    /**
     *Returns the player's flight status: still in the race or abandoned.
     * @return player's flight status
     */
    public FlightStatus getStatus(){
        return status;
    }

    /**
     * Sets the normalized player's position.
     * @param position player's position on Route
     * @param routeLength Route's length
     * @see it.polimi.ingsw.model.domain.flight.Route
     */
    public void setPosition(int position, int routeLength){
        this.position = position%routeLength;
    }

    /**
     * Sets the laps completed by the player.
     * @param lapsCompleted Number of laps completed.
     */
    public void setLapsCompleted(int lapsCompleted) {
        this.lapsCompleted = lapsCompleted;
    }

    /**
     * Update the player's flight status
     * @param status new status to set
     */
    public void setStatus (FlightStatus status){
        this.status = status;
    }

}
