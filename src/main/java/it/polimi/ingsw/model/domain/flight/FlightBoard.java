package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlightBoard {
    private Route route;
    //connects PlayerId with FlightData
    private Map<PlayerId, PlayerFlightData> playerDataMap;
    private List<PlayerId> finishOrder;

    //constructor defined by user
    public FlightBoard(GameLevel level, Integer PlayerCount){}

    public void movePlayer(PlayerId playerId, Integer spaces){}

    public void registerPlayer(PlayerId playerId){}

    //thanks to the attribute playerDataMap i can get the position of the player on the flight route given the PlayerId
    public int getPlayerPosition(PlayerId playerId){
        return 0;
    }

    public PlayerFlightData getPlayerData(PlayerId playerId){
        return null;
    }

    public List<String> getPlayerOrderByPosition(){
        return List.of();
    }

    public void abandonPlayer(PlayerId playerId){}

    public boolean isFlightComplete(){
        return false;
    }
}








