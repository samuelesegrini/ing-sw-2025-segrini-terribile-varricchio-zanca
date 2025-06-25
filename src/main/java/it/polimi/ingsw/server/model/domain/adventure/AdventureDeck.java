package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.util.PileIdentifier;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedList;

/**
 * Represents a deck of adventure cards used in gameplay.
 * This class manages different sets of cards based on the game phase and level:
 * - During setup (for levels like LEVEL_II): Manages multiple 'uncovered' (predictable) piles
 *   that players can view, and one 'unknown' (initially covered) pile.
 * - For TEST_FLIGHT: Starts with a single combined pile.
 * - During flight phase: All initial piles are merged into one main 'coveredPile' and shuffled.
 *
 * It tracks which players are viewing which predictable piles during setup.
 */
public class AdventureDeck implements Serializable {
    private final static long serialVersionUID = 1L;

    private final GameLevel gameLevel;
    private List<List<AdventureCard>> uncoveredPiles; // For L2/L3 setup: the 3 predictable piles
    private LinkedList<AdventureCard> unknownOrTestFlightPile; // For L2/L3 setup: the 1 unknown pile; For TestFlight: all cards
    private LinkedList<AdventureCard> mainFlightDeck; // The combined, shuffled deck used during flight phase

    private int currentFlightDeckIndex;
    private Map<PlayerId, PileIdentifier> playerViewing; // Tracks which player is viewing which predictable pile
    private boolean isFlightPhaseActive;

    /**
     * Constructs a new AdventureDeck.
     *
     * @param gameLevel                    The difficulty level for this game.
     * @param initialUncoveredPiles        For LEVEL_II/III: The list of predictable piles (e.g., 3 piles).
     *                                     For TEST_FLIGHT: This should be an empty list.
     * @param initialCoveredOrUnknownPile For LEVEL_II/III: The single "unknown" top pile.
     *                                     For TEST_FLIGHT: All Test Flight cards, forming the main deck.
     */
    public AdventureDeck(GameLevel gameLevel, List<List<AdventureCard>> initialUncoveredPiles, List<AdventureCard> initialCoveredOrUnknownPile) {
        this.gameLevel = gameLevel;
        this.playerViewing = new HashMap<>();
        this.isFlightPhaseActive = false;
        this.currentFlightDeckIndex = 0;
        this.mainFlightDeck = new LinkedList<>();

        if (gameLevel == GameLevel.TEST_FLIGHT) {
            this.uncoveredPiles = new ArrayList<>(); // No uncovered piles for Test Flight setup
            this.unknownOrTestFlightPile = new LinkedList<>();
            this.mainFlightDeck.addAll(initialCoveredOrUnknownPile);
            System.out.println("[AdventureDeck Constructor for TF] Size of mainFlightDeck after addAll: " + this.mainFlightDeck.size());
            Collections.shuffle(this.mainFlightDeck);
        } else {
            this.uncoveredPiles = new ArrayList<>(initialUncoveredPiles);
            this.unknownOrTestFlightPile = new LinkedList<>(initialCoveredOrUnknownPile);
        }
    }

    /**
     * Checks if a player can view a specific predictable (uncovered) pile during setup.
     * A pile cannot be viewed by multiple players simultaneously.
     * This is only relevant before the flight phase begins and for game levels with predictable piles.
     *
     * @param playerId The ID of the player requesting to view.
     * @param pileId   The identifier of the predictable pile to view.
     * @return true if the player can view the pile, false otherwise.
     */
    public boolean canPlayerViewPile(PlayerId playerId, PileIdentifier pileId) {
        if (isFlightPhaseActive || gameLevel == GameLevel.TEST_FLIGHT) {
            return false; // Viewing specific setup piles is not applicable
        }
        if (playerId == null || pileId == null || pileId.getIndex() < 0 || pileId.getIndex() >= uncoveredPiles.size()) {
            return false; // Invalid arguments or pile index
        }

        // Check if the pile is being viewed by someone else (and it's not the current player already viewing it)
        for (Map.Entry<PlayerId, PileIdentifier> entry : playerViewing.entrySet()) {
            if (entry.getValue().equals(pileId) && !entry.getKey().equals(playerId)) {
                return false; // Pile is being viewed by another player
            }
        }
        return true; // Pile is available or already viewed by this player
    }

    /**
     * Allows a player to view the contents of a predictable (uncovered) pile during setup.
     * Records that the player is viewing this pile.
     *
     * @param playerId The ID of the player.
     * @param pileId   The identifier of the pile.
     * @return A list of cards in the pile, or an empty list if viewing is not allowed or pile is invalid.
     * @throws IllegalStateException if another player is already viewing the requested pile.
     */
    public List<AdventureCard> viewPile(PlayerId playerId, PileIdentifier pileId) {
        if (!canPlayerViewPile(playerId, pileId)) {
            if (isFlightPhaseActive || gameLevel == GameLevel.TEST_FLIGHT) {
                System.err.println("Cannot view setup piles during flight phase or for Test Flight level.");
                return Collections.emptyList();
            }
            // If canPlayerViewPile returned false due to another player viewing it
            throw new IllegalStateException("Pile " + pileId + " is currently being viewed by another player.");
        }
        // If canPlayerViewPile was true, it means either the pile is free, or this player is already viewing it.
        // In either case, (re-)assign the view.
        playerViewing.put(playerId, pileId);
        return Collections.unmodifiableList(uncoveredPiles.get(pileId.getIndex()));
    }

    /**
     * Stops a player from viewing a pile, making it available for others during setup.
     *
     * @param playerId The ID of the player who was viewing a pile.
     */
    public void stopViewingPile(PlayerId playerId) {
        if (isFlightPhaseActive) return; // Not applicable
        playerViewing.remove(playerId);
    }

    /**
     * Transitions the game to the flight phase.
     * - For levels like LEVEL_II: Combines all setup piles (uncovered and unknown) into the main flight deck and shuffles it.
     * - For TEST_FLIGHT: Ensures the main deck (already prepared) is ready.
     * Clears any player viewing states for setup piles.
     */
    public void startFlightPhase() {
        if (isFlightPhaseActive) {
            System.out.println("[AdventureDeck.startFlightPhase] Already active.");
            return;
        }
        isFlightPhaseActive = true;
        System.out.println("[AdventureDeck.startFlightPhase] Called. GameLevel: " + gameLevel);

        if (gameLevel == GameLevel.TEST_FLIGHT) {
            System.out.println("[AdventureDeck.startFlightPhase] TestFlight: mainFlightDeck size BEFORE playerViewing.clear: " + mainFlightDeck.size());

            // For Test Flight, unknownOrTestFlightPile was used to initialize mainFlightDeck in constructor
            // If it wasn't shuffled there, shuffle now. If it was, this is redundant but safe.
            if(this.mainFlightDeck.isEmpty() && this.unknownOrTestFlightPile != null && !this.unknownOrTestFlightPile.isEmpty()){
                // This case implies constructor logic might have changed or was for a different setup path
                this.mainFlightDeck.addAll(this.unknownOrTestFlightPile);
                Collections.shuffle(this.mainFlightDeck);
            }
        } else { // For LEVEL_II
            System.out.println("[AdventureDeck.startFlightPhase L2] Size of this.uncoveredPiles: " + this.uncoveredPiles.stream().mapToLong(List::size).sum());
            System.out.println("[AdventureDeck.startFlightPhase L2] Size of this.unknownOrTestFlightPile: " + (this.unknownOrTestFlightPile != null ? this.unknownOrTestFlightPile.size() : "null"));
            mainFlightDeck.clear();
            for (List<AdventureCard> pile : uncoveredPiles) {
                mainFlightDeck.addAll(pile);
            }
            if (unknownOrTestFlightPile != null) {
                mainFlightDeck.addAll(unknownOrTestFlightPile);
            }
            System.out.println("[AdventureDeck.startFlightPhase L2] Final mainFlightDeck size: " + mainFlightDeck.size());
            Collections.shuffle(mainFlightDeck);
        }

        uncoveredPiles.clear(); // No longer accessible as separate piles
        if (unknownOrTestFlightPile != null) unknownOrTestFlightPile.clear();
        playerViewing.clear();    // Viewing of setup piles ends
        currentFlightDeckIndex = 0;
        System.out.println("[AdventureDeck.startFlightPhase] TestFlight: mainFlightDeck size AFTER playerViewing.clear and index reset: " + mainFlightDeck.size());


        // Optional: Implement the rule about the top card matching flight level.
        // This is complex and might require re-shuffling or specific card placement.
        // For simplicity, a standard shuffle is performed.
        if (!mainFlightDeck.isEmpty() && mainFlightDeck.peekFirst() != null) {
            if (mainFlightDeck.peekFirst().getLevel() != gameLevel.getPrimaryCardLevel()) {
                // System.out.println("Debug: Top card after shuffle (" + mainFlightDeck.peekFirst().getId() + " - " +
                //                    mainFlightDeck.peekFirst().getLevel() +
                //                    ") does not match primary flight level (" + gameLevel.getPrimaryCardLevel() + ").");
                // Consider if re-shuffling or other logic is needed to meet this rule.
            }
        }
    }

    /**
     * Draws the next card from the main flight deck.
     *
     * @return An Optional containing the next AdventureCard, or empty if the deck is exhausted or not in flight phase.
     */
    public Optional<AdventureCard> drawNextCard() {
        if (!isFlightPhaseActive || mainFlightDeck.isEmpty() || currentFlightDeckIndex >= mainFlightDeck.size()) {
            return Optional.empty();
        }
        AdventureCard card = mainFlightDeck.get(currentFlightDeckIndex);
        currentFlightDeckIndex++;
        return Optional.of(card);
    }

    /**
     * Gets the current card at the top of the main flight deck without drawing it (advancing the index).
     *
     * @return An Optional containing the current AdventureCard, or empty if not applicable.
     */
    public Optional<AdventureCard> getCurrentCard() {
        if (!isFlightPhaseActive || mainFlightDeck.isEmpty() || currentFlightDeckIndex >= mainFlightDeck.size()) {
            return Optional.empty();
        }
        return Optional.of(mainFlightDeck.get(currentFlightDeckIndex));
    }

    /**
     * Checks if the main flight deck is exhausted (all cards have been drawn).
     *
     * @return true if exhausted, false otherwise.
     */
    public boolean isExhausted() {
        return !isFlightPhaseActive || currentFlightDeckIndex >= mainFlightDeck.size();
    }


    // --- Getters for state inspection or testing ---

    public GameLevel getGameLevel() {
        return gameLevel;
    }

    public boolean isFlightPhaseActive() {
        return isFlightPhaseActive;
    }

    /**
     * Gets an unmodifiable view of the uncovered piles (for testing or specific display logic during setup).
     * Returns an empty list if in flight phase or for Test Flight.
     */
    public List<List<AdventureCard>> getUncoveredPilesView() {
        if (isFlightPhaseActive || gameLevel == GameLevel.TEST_FLIGHT) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        List<List<AdventureCard>> unmodifiableView = new ArrayList<>();
        for (List<AdventureCard> pile : uncoveredPiles) {
            unmodifiableView.add(Collections.unmodifiableList(pile));
        }
        return Collections.unmodifiableList(unmodifiableView);
    }

    /**
     * Gets an unmodifiable view of the main flight deck (for testing).
     */
    public List<AdventureCard> getMainFlightDeckView() {
        return Collections.unmodifiableList(mainFlightDeck);
    }

    /**
     * Gets the number of cards remaining in the main flight deck.
     */
    public int getRemainingCardsInFlightDeck() {
        if (!isFlightPhaseActive) return 0;
        return mainFlightDeck.size() - currentFlightDeckIndex;
    }

    /**
     * Gets the pile identifiers for the predictable piles available in the current game level's setup.
     * This depends on how PileIdentifier is defined and related to GameLevel rules.
     * @return An array of PileIdentifier for predictable piles.
     */
    public PileIdentifier[] getPredictablePileIdentifiers() {
        if (gameLevel == GameLevel.TEST_FLIGHT || uncoveredPiles.isEmpty()) {
            return new PileIdentifier[0];
        }
        int count = uncoveredPiles.size();
        PileIdentifier[] ids = new PileIdentifier[count];
        for (int i = 0; i < count; i++) {
            // This requires PileIdentifier.fromIndex or similar to map 0, 1, 2 to specific enums
            if (i == 0) ids[i] = PileIdentifier.BOTTOM_LEFT;
            else if (i == 1) ids[i] = PileIdentifier.BOTTOM_CENTER;
            else if (i == 2) ids[i] = PileIdentifier.BOTTOM_RIGHT;
            else ids[i] = PileIdentifier.UNKNOWN;
        }
        return ids;
    }
}