package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.*;

/**
 * Represents a deck of ship components that players can draw from during the game.
 * Supports both face-down drawing and face-up returned components.
 */
public class ComponentDeck {
    private final List<Component> drawPile; // Face-down components
    private final List<Component> discardPile; // Permanently discarded
    private final List<Component> faceUpPile; // Face-up returned components
    private final Map<String, Component> reservedComponents; // Components reserved by players

    /**
     * Creates a new component deck with the given list of components
     * @param components The initial list of components
     */
    public ComponentDeck(List<Component> components) {
        this.drawPile = new ArrayList<>(components);
        this.discardPile = new ArrayList<>();
        this.faceUpPile = new ArrayList<>();
        this.reservedComponents = new HashMap<>();
        shuffle();
    }

    /**
     * Creates a new component deck for the specified game level
     * @param level The game level that determines the deck composition
     */
    public ComponentDeck(GameLevel level) {
        this.drawPile = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.faceUpPile = new ArrayList<>();
        this.reservedComponents = new HashMap<>();
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
        // Components are loaded from the configuration manager
        // This method is called when creating a deck for a specific level
        // The actual component loading is handled by GameConfigurationManager
        shuffle();
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
    
    // Face-up pile management (for returned components)
    
    /**
     * Returns a component to the face-up pile where all players can see it
     * @param component The component to return
     */
    public void returnToFaceUp(Component component) {
        if (component != null) {
            faceUpPile.add(component);
        }
    }
    
    /**
     * Takes a specific component from the face-up pile
     * @param component The component to take
     * @return true if the component was successfully taken
     */
    public boolean takeFaceUpComponent(Component component) {
        return faceUpPile.remove(component);
    }
    
    /**
     * Takes a component from the face-up pile by ID
     * @param componentId The ID of the component to take
     * @return The component if found and taken, null otherwise
     */
    public Component takeFaceUpComponentById(String componentId) {
        for (Component component : faceUpPile) {
            if (component.getId().equals(componentId)) {
                faceUpPile.remove(component);
                return component;
            }
        }
        return null;
    }
    
    /**
     * Gets an unmodifiable view of the face-up pile
     * @return The face-up pile
     */
    public List<Component> getFaceUpPile() {
        return Collections.unmodifiableList(faceUpPile);
    }
    
    /**
     * Gets the number of face-up components
     * @return The number of face-up components
     */
    public int getFaceUpCount() {
        return faceUpPile.size();
    }
    
    // Component reservation system
    
    /**
     * Reserves a component for a specific player
     * @param playerId The player reserving the component
     * @param component The component to reserve
     * @return true if successfully reserved, false if player already has max reservations
     */
    public boolean reserveComponent(String playerId, Component component) {
        if (component == null || playerId == null) {
            return false;
        }
        
        // Check if player already has 2 reserved components (max limit)
        long playerReservations = reservedComponents.values().stream()
            .mapToLong(c -> c.getReservedBy() != null && c.getReservedBy().equals(playerId) ? 1 : 0)
            .sum();
            
        if (playerReservations >= 2) {
            return false;
        }
        
        reservedComponents.put(component.getId(), component);
        component.setReservedBy(playerId);
        return true;
    }
    
    /**
     * Releases a reserved component back to the general pool
     * @param playerId The player releasing the component
     * @param componentId The component to release
     * @return The released component, or null if not found
     */
    public Component releaseReservedComponent(String playerId, String componentId) {
        Component component = reservedComponents.get(componentId);
        if (component != null && playerId.equals(component.getReservedBy())) {
            reservedComponents.remove(componentId);
            component.setReservedBy(null);
            return component;
        }
        return null;
    }
    
    /**
     * Gets all components reserved by a specific player
     * @param playerId The player ID
     * @return List of reserved components
     */
    public List<Component> getReservedComponents(String playerId) {
        return reservedComponents.values().stream()
            .filter(c -> playerId.equals(c.getReservedBy()))
            .toList();
    }
    
    /**
     * Gets the total number of available components (draw pile + face-up + reserved)
     * @return Total available components
     */
    public int getTotalAvailableComponents() {
        return drawPile.size() + faceUpPile.size() + reservedComponents.size();
    }
    
    /**
     * Checks if the deck is completely empty (no more components to draw or take)
     * @return true if no components are available
     */
    public boolean isEmpty() {
        return drawPile.isEmpty() && faceUpPile.isEmpty() && discardPile.isEmpty();
    }
    
    /**
     * Discards all reserved components at the end of building phase
     * These become waste components with negative points
     * @param playerId The player whose reserved components to discard
     * @return List of discarded components (for tracking waste)
     */
    public List<Component> discardReservedComponents(String playerId) {
        List<Component> discarded = new ArrayList<>();
        Iterator<Map.Entry<String, Component>> iterator = reservedComponents.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Component> entry = iterator.next();
            Component component = entry.getValue();
            
            if (playerId.equals(component.getReservedBy())) {
                discarded.add(component);
                discard(component);
                iterator.remove();
            }
        }
        
        return discarded;
    }
} 