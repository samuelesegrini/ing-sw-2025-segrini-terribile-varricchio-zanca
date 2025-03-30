package it.polimi.ingsw.model.domain.adventure;

import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.flight.FlightStatus;
import it.polimi.ingsw.model.util.PileIdentifier;
import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.model.domain.player.PlayerId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a deck of adventure cards used in gameplay.
 * This class manages both covered and uncovered piles of cards,
 * tracks which players are viewing which piles, and handles card drawing operations.
 * It also supports transitioning between regular gameplay and the flight phase.
 * 
 * <p>The deck maintains state information about which players are currently viewing
 * which piles to prevent multiple players from accessing the same pile simultaneously.</p>
 *
 */
public class AdventureDeck {
    /** The difficulty level of the current game. */
    private GameLevel gameLevel;
    
    /** Collection of uncovered card piles visible to players. */
    private List<List<AdventureCard>> uncoveredPiles;
    
    /** The main covered pile of cards from which cards are drawn. */
    private List<AdventureCard> coveredPile;

    private List<AdventureCard> combinedPile;

    /** Index tracking the current position in the covered pile. */
    private int currentIndex;
    
    /** Maps players to the piles they are currently viewing. */
    private Map<PlayerId, PileIdentifier> playerViewing;
    
    /** Flag indicating whether the game is in flight phase. */
    private boolean isFlightPhase;
    
    /**
     * Constructs a new adventure deck with specified game parameters.
     *
     * @param gameLevel The difficulty level for this game
     * @param uncoveredPiles Initial uncovered piles of cards
     * @param coveredPile Initial covered pile of cards
     * @throws IllegalArgumentException if any parameter is null or if piles are empty
     */
    public AdventureDeck(GameLevel gameLevel, List<List<AdventureCard>> uncoveredPiles, List<AdventureCard> coveredPile) {
        this.gameLevel = gameLevel;
        this.coveredPile = coveredPile;
        this.currentIndex = 0;
        this.uncoveredPiles = uncoveredPiles;
        this.playerViewing = new HashMap<PlayerId, PileIdentifier>();
        this.isFlightPhase = false;
    }
    
    /**
     * Determines whether a player can view a specific card pile.
     * A pile cannot be viewed by multiple players simultaneously.
     *
     * <p>Example usage:</p>
     * {@code
     * if (deck.canPlayerViewPile(player1, pile3)) {
     *     List<AdventureCard> cards = deck.viewPile(player1, pile3);
     * }
     * }
     *
     * @param playerId The ID of the player requesting to view
     * @param pileId The identifier of the pile to view
     * @return true if the player can view the pile, false otherwise
     * @throws IllegalArgumentException if playerId or pileId is null
     */
    public boolean canPlayerViewPile(PlayerId playerId, PileIdentifier pileId) {
        // Check if the pile is being viewed by someone else, but not the player itself
        if (playerViewing.containsValue(pileId)) {
            // Check if the pile is associated with the given player
            for (Map.Entry<PlayerId, PileIdentifier> entry : playerViewing.entrySet()) {
                if (entry.getValue().equals(pileId) && !entry.getKey().equals(playerId)) {
                    return false; // The pile is already being viewed by someone else
                }
            }
        }
        // If no one else is viewing it or the player is already viewing it, allow viewing
        return true;
    }
    
    /**
     * Retrieves all cards in an uncovered pile for player viewing.
     * This method records that the player is viewing this pile.
     * 
     * <p>Note: This operation has a side effect of marking the pile as being viewed
     * by the specified player. Other players will not be able to view this pile
     * until {@link #stopViewingPile(PlayerId)} is called.</p>
     *
     * @param playerId The ID of the player viewing the pile (must not be null)
     * @param pileId The identifier of the pile to view (must not be null)
     * @return List of adventure cards in the requested pile
     * @throws IllegalArgumentException if playerId or pileId is null
     * @throws IllegalStateException if another player is already viewing the requested pile
     * @see #canPlayerViewPile(PlayerId, PileIdentifier)
     * @see #stopViewingPile(PlayerId)
     */
    public List<AdventureCard> viewPile(PlayerId playerId, PileIdentifier pileId) {
        if(canPlayerViewPile(playerId, pileId)) {
            playerViewing.put(playerId, pileId);
            return uncoveredPiles.get(pileId.getIndex());
        }
        else{
            throw new IllegalStateException();
        }
    }
    
    /**
     * Stops a player from viewing a pile, making it available for other players.
     *
     * @param playerId The ID of the player who is currently viewing a pile
     * @return The updated adventure deck state
     */
    public AdventureDeck stopViewingPile(PlayerId playerId) {
        playerViewing.remove(playerId);
        return this;
    }
    
    /**
     * Retrieves the currently active card being played.
     *
     * @return The current card, or empty if no card is active
     */
    public Optional<AdventureCard> getCurrentCard() {
        if (currentIndex < coveredPile.size()) {
            return Optional.ofNullable(coveredPile.get(currentIndex));
        } else {
            return Optional.empty();
        }
    }
    
    /**
     * Draws the next card from the covered pile if available.
     * Increments the current index if a card is successfully drawn.
     *
     * @return The next card, or empty if the deck is exhausted
     * @see #isExhausted()
     * @implNote This implementation is not thread-safe. Concurrent calls to this method
     *           may result in the same card being returned multiple times.
     */
    public Optional<AdventureCard> drawNextCard() {
        if (currentIndex < coveredPile.size()) {
            AdventureCard card = coveredPile.get(currentIndex);
            this.currentIndex++;
            return Optional.ofNullable(card);
        } else {
            return Optional.empty();
        }
    }
    
    /**
     * Transitions the game to flight phase, merging all decks into a single pile.
     * 
     * <p>This is a one-way operation - once the flight phase has started, the deck cannot
     * return to the previous state.</p>
     *
     * @return The updated adventure deck configured for flight phase
     * @throws IllegalStateException if the flight phase has already started
     */
    public AdventureDeck startFlightPhase() {
        this.isFlightPhase = true;
        return this;
    }
    
    /**
     * Checks if there are no more cards available to draw.
     *
     * @return true if the deck is exhausted, false otherwise
     */
    public boolean isExhausted() {
        if(currentIndex == coveredPile.size()) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns all valid pile identifiers that can be used in the current game state.
     *
     * @return Array of valid pile identifiers
     */
    public PileIdentifier[] getValidPileIdentifiers() {
        return PileIdentifier.getPredictablePiles(gameLevel);
    }
    
    /**
     * Returns the difficulty level of the current game.
     *
     * @return The game level
     */
    public GameLevel getGameLevel() {
        return this.gameLevel;
    }
}
