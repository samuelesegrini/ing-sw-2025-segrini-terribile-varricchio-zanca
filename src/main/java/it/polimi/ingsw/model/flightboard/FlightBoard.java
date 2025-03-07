package it.polimi.ingsw.model.flightboard;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlightBoard {
    private Route route;
    //connnects PlayerId with FlightData
    private Map<PlayerId, PlayerFlightData> playerDataMap;
    private List<PlayerId> finishOrder;

    //constructor defined by user
    public FlightBoard(GameLevel level, int PlayerCount) {}

    public void movePlayer(PlayerId playerId, int spaces);

    public void registerPlayer(PalyerId playerId);

    //thanks to the attribute playerDataMap i can get the position of the player on the flight route given the PlayerId
    public int getPlayerPosition(PlayerId playerId);

    public PlayerFlightData getPlayerData(PlayerId playerId);

    public List<String> getPlayerOrderByPosition();

    public void abandonPlayer(PlayerId playerId);

    public boolean isFlightComplete();
}








