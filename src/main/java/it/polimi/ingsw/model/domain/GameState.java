package it.polimi.ingsw.model.domain;

import it.polimi.ingsw.model.domain.adventure.AdventureDeck;
import it.polimi.ingsw.model.domain.flight.FlightBoard;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.enums.GamePhase;

import java.util.List;

/**
 * Represents the current state of the game, including the current player, the flight board,
 * the adventure deck, the list of players, and the current game phase.
 */
public class GameState {
    private Player currentPlayer;
    private FlightBoard flightBoard;
    private AdventureDeck adventureDeck;
    private List<Player> players;
    private GamePhase gamePhase;

    /**
     * Returns the current player in the game.
     * @return The player who is currently taking their turn.
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Moves to the next player in the game, transitioning the turn to the next player.
     * @return The player who will take their turn next.
     */
    public Player nextPlayer(){
        int temp = players.indexOf(currentPlayer)+1;
        if (temp = players.size()){
            temp = 0;
        }
        return players.get(temp);
    }


    /**
     * Returns the flight board that tracks the players' progress in the game.
     * @return The flight board.
     */
    public FlightBoard getFlightBoard() {
        return flightBoard;
    }

    /**
     * Returns the adventure deck.
     * @return The adventure deck.
     */
    public AdventureDeck getAdventureDeck() {
        return adventureDeck;
    }

    /**
     * Returns a list of all the players participating in the game.
     * @return A list of players.
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Returns the current phase of the game (setup, building, flight, end).
     * @return The current game phase.
     */
    public GamePhase getGamePhase() {
        return gamePhase;
    }

    /**
     * Sets the current phase of the game.
     * This method allows updating the game phase.
     * @param gamePhase The new game phase to set.
     */
    public void setGamePhase(GamePhase gamePhase) {
        this.gamePhase = gamePhase;
    }

}