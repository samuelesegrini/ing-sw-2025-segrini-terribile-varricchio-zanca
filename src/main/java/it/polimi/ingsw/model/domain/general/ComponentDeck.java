package it.polimi.ingsw.model.domain.general;

import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.domain.ship.components.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a deck of ship components that players can draw from during the game
 */
public class ComponentDeck {
    private final List<Component> drawPile;
    private final List<Component> discardPile;

    /**
     * Creates a new component deck with the given list of components
     * @param components The initial list of components
     */
    public ComponentDeck(List<Component> components) {
        this.drawPile = new ArrayList<>(components);
        this.discardPile = new ArrayList<>();
        shuffle();
    }

    /**
     * Creates a new component deck for the specified game level
     * @param level The game level that determines the deck composition
     */
    public ComponentDeck(GameLevel level) {
        this.drawPile = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        initializeDeckForLevel(level);
    }

    /**
     * Draws a component from the deck
     * @return An Optional containing the drawn component, or empty if the deck is empty
     */
    public Optional<Component> draw() {
        if (drawPile.isEmpty()) {
            if (discardPile.isEmpty()) {
                return Optional.empty();
            }
            reshuffleDiscardPile();
        }
        return Optional.of(drawPile.removeLast());
    }

    /**
     * Discards a component, adding it to the discard pile
     * @param component The component to discard
     */
    public void discard(Component component) {
        discardPile.add(component);
    }

    /**
     * Shuffles the draw pile
     */
    public void shuffle() {
        Collections.shuffle(drawPile);
    }

    /**
     * Gets the number of cards remaining in the draw pile
     * @return The number of cards in the draw pile
     */
    public int getRemainingCards() {
        return drawPile.size();
    }

    /**
     * Gets the number of cards in the discard pile
     * @return The number of cards in the discard pile
     */
    public int getDiscardedCards() {
        return discardPile.size();
    }

    /**
     * Reshuffles the discard pile into the draw pile
     */
    private void reshuffleDiscardPile() {
        drawPile.addAll(discardPile);
        discardPile.clear();
        shuffle();
    }

    /**
     * Initializes the deck with components based on the game level
     * @param level The game level
     */
    private void initializeDeckForLevel(GameLevel level) {
        // This should be implemented based on your game's specific rules
        // for how different game levels should have different component distributions
        throw new UnsupportedOperationException("initializeDeckForLevel needs to be implemented based on game rules");
    }

    /**
     * Gets an unmodifiable view of the draw pile
     * @return The draw pile
     */
    public List<Component> getDrawPile() {
        return Collections.unmodifiableList(drawPile);
    }

    /**
     * Gets an unmodifiable view of the discard pile
     * @return The discard pile
     */
    public List<Component> getDiscardPile() {
        return Collections.unmodifiableList(discardPile);
    }
} 