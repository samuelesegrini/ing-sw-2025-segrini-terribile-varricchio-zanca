package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.flight.FlightStatus;

import java.util.*;

public class FlightBoard {
    private final Route route;
    private int playerCount;
    private Map <Player, PlayerFlightData> playerDataMap;
    private List<Player> currentOrder;


    /**
     * Constructs a new FlightBoard for the given game level and player count.
     * Initializes the route based on the level and player count.
     * @param level The game level.
     */
    public FlightBoard(GameLevel level, Route route, int playerCount) {
        this.route = route;
        this.playerCount = playerCount;
        playerDataMap = new HashMap<>();
        currentOrder = new ArrayList<>();
    }

    public Route getRoute() { return route; }
    public int getPlayerCount() { return playerCount; }
    public List<Player> getCurrentOrder() {
        return currentOrder;
    }
    public PlayerFlightData getPlayerData(Player player){
        return playerDataMap.get(player);
    }

   /**
     * Returns the leading player in the current order of players.
     * The leading player is the one with the highest position in the flight data.
     * @return The leading player, or null if no players are registered.
     */
    public Player getLeadingPlayer(){
        if (currentOrder.isEmpty()) {
            return null; // No players registered
        }
        return currentOrder.get(0); // The first player in the current order is the leading player
    }

    /**
     * Updates the current order of players based on their flight data positions.
     * This method sorts the players in descending order of their positions.
     */
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

    /**
     * Returns a list of players who are ahead of the specified player within a given distance.
     * @param player The player to check against.
     * @param distance The distance within which to find players ahead.
     * @return A list of players who are ahead of the specified player within the given distance.
     */
    public List<Player> getPlayersAhead(Player player, int distance) {
        List<Player> playersAhead = new ArrayList<Player>();
        int playerPosition = player.getFlightData().getPosition();
        for(Player p: playerDataMap.keySet()){
            int aheadPosition = p.getFlightData().getPosition();
            if(aheadPosition > playerPosition && (playerPosition + distance) >= aheadPosition){
                playersAhead.add(p);
            }
        }
        return playersAhead;
    }

    /**
     * Returns a list of players who are behind the specified player within a given distance.
     * @param player The player to check against.
     * @param distance The distance within which to find players behind.
     * @return A list of players who are behind the specified player within the given distance.
     */
    public List<Player> getPlayersBehind (Player player, int distance){
        List<Player> playersBehind = new ArrayList<Player>();
        int playerPosition = player.getFlightData().getPosition();
        for(Player p: playerDataMap.keySet()){
            int behindPosition = p.getFlightData().getPosition();
            if(behindPosition < playerPosition && (playerPosition - distance) <= behindPosition){
                playersBehind.add(p);
            }
        }
        return playersBehind;
    }

    /**
     * Moves the player forward or backward by the specified number of spaces,
     * adjusting for the number of players ahead or behind.
     * @param player The player to move.
     * @param spaces The number of spaces to move.
     * @param forward {@code true} to move forward, {@code false} to move backward.
     */
    public void movePlayer(Player player, int spaces, boolean forward){
        Integer playerPosition = player.getFlightData().getPosition();
        Integer newPosition;
        if(forward) {
            newPosition = playerPosition + spaces + getPlayersAhead(player, spaces).size();
            player.getFlightData().setPosition(newPosition, route.getLength());
        }
        else{
            newPosition = playerPosition - spaces - getPlayersBehind(player, spaces).size();
            player.getFlightData().setPosition(newPosition, route.getLength());
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
        playerDataMap.get(player).setStatus(FlightStatus.RACING);
        playerCount++;
        currentOrder.add(player);
    }

    /**
     * Abandons a player from the flight, setting their flight status to ABANDONED.
     * @param player The ID of the player to abandon.
     */
    public void abandonPlayer(Player player){
        playerDataMap.get(player).setStatus(FlightStatus.ABANDONED);
        playerCount--;
        currentOrder.remove(player);
    }
}





