package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.domain.flight.Route;
import it.polimi.ingsw.model.enums.flight.FlightStatus;

import java.util.*;

public class FlightBoard {
    private Route route;
    private int playerCount;
    private Map <Player, PlayerFlightData> playerDataMap;
    private List<Player> currentOrder;
    private List <Player> finishOrder;

    /**
     * Constructs a new FlightBoard for the given game level and player count.
     * Initializes the route based on the level and player count.
     * @param level The game level.
     */
    public FlightBoard(GameLevel level){
        playerCount=0;
        playerDataMap=new HashMap<>();
        currentOrder = new ArrayList<>();
        finishOrder = new ArrayList<>();
        List<Integer> startingPositions = new ArrayList<>();
        switch(level){
            case TEST_FLIGHT:
                startingPositions.add(0);
                startingPositions.add(1);
                startingPositions.add(2);
                startingPositions.add(4); //forse dovrei farlo in level
                this.route=new Route(level, 18, startingPositions, new RewardSystem(level) );
            case LEVEL_II:
                List<Integer> startingPositionII=new ArrayList<>();
                startingPositions.add(0);
                startingPositions.add(1);
                startingPositions.add(3);
                startingPositions.add(6);
                this.route=new Route(level, 24 , startingPositions, new RewardSystem(level) );
        }
    }

    /**
     * Returns a list of player names ordered from the first to the last based on their current position
     * on the flight route. The list is sorted such that the player furthest along the route comes first,
     * and the player furthest behind comes last.
     * @return A list of player names.
     */
    public List<Player> getCurrentOrder() {
        return currentOrder;
    }

    public List<Player> getFinishOrder() {
        return finishOrder;
    }

    /**
     * Returns the current position of the player on the flight route.
     * @param player The player whose position is being queried.
     * @return The position of the player on the flight route.
     */
    public int getPlayerPosition(Player player){
        return playerDataMap.get(player).getPosition();
    }

    /**
     * Returns the flight data of the specified player, including their current flight state, completed laps, and position.
     * @param player The player whose flight data is being requested.
     * @return The flight data of the player.
     */
    public PlayerFlightData getPlayerData(Player player){
        return playerDataMap.get(player);
    }

    //ordinati dal primo all'ultimo
    public void updateCurrentOrder() {
        boolean swapped;
        for (int i = 0; i < currentOrder.size() - 1; i++) {
            swapped = false;
            for (int j = 0; j < currentOrder.size() - i - 1; j++) {
                if (currentOrder.get(j).getFlightData().getPosition() < currentOrder.get(j + 1).getFlightData().getPosition()) {
                    Player temp = currentOrder.get(j);
                    currentOrder.set(j, currentOrder.get(j + 1));
                    currentOrder.set(j + 1, temp);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }

    public List<Player> getPlayersAhead(Player player){
        List<Player> playersAhead = new ArrayList<Player>();
        for(Player p: playerDataMap.keySet()){
            if(p.getFlightData().getPosition() > player.getFlightData().getPosition()){
                playersAhead.add(p);
            }
        }
        return playersAhead;
    }

    public List<Player> getPlayersBehind (Player player){
        List<Player> playersBehind = new ArrayList<Player>();
        for(Player p: playerDataMap.keySet()){
            if(p.getFlightData().getPosition() < player.getFlightData().getPosition()){
                playersBehind.add(p);
            }
        }
        return playersBehind;
    }

    public void movePlayer(Player player, Integer spaces, boolean forward){
        Integer playerPosition = player.getFlightData().getPosition();
        Integer newPlayerPosition;
        if(forward) {
            newPlayerPosition = playerPosition + spaces + getPlayersAhead(player).size();
            player.getFlightData().setPosition(newPlayerPosition, route.getLength());
        }
        else{
            newPlayerPosition = playerPosition - spaces - getPlayersBehind(player).size();
            player.getFlightData().setPosition(newPlayerPosition, route.getLength());
        }
    }

    /**
     * Indicates if the position is occupied by another player.
     * @param position player's position
     * @return {@code true} if the position is already occupied by another player, {@code false} otherwise.
     */
    public boolean isPositionOccupied (int position){
        for (Player p : playerDataMap.keySet()) {
            if(p.getFlightData().getPosition() == position) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a new player on the flight board, initializing PlayerFlightData
     * and adding a record to PlayerDataMap.
     * @param player The ID of the player to register.
     */
    public void registerPlayer(Player player){
        PlayerFlightData playerFlightData = new PlayerFlightData(route.getFirstAvailableStartingPosition());
        playerDataMap.put(player, playerFlightData);
        playerCount++;
        currentOrder.add(player);
        playerDataMap.get(player).setStatus(FlightStatus.RACING);
    }


    /**
     * Abandons a player from the flight, setting their flight status to ABANDONED.
     * @param player The ID of the player to abandon.
     */
    public void abandonPlayer(Player player){
        playerDataMap.remove(player);
        playerCount--;
        currentOrder.remove(player);
        playerDataMap.get(player).setStatus(FlightStatus.ABANDONED);
    }

    /**
     * Checks if the flight is complete, meaning all players have either finished the flight or abandoned the race.
     * @return {@code true} if the flight is complete, {@code false} otherwise.
     */
    public boolean isFlightComplete() {
        //quando aggiorno finishOrder? quando finsice il mazzo?
        return (finishOrder.size() == playerCount);
    }
}





