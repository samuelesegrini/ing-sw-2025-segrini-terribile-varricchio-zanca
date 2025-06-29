package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.common.message.event.*;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.*;

/**
 * Represents a deck of ship components that players can draw from during the game.
 * Supports both face-down drawing and face-up returned components.
 */
public class ComponentDeck implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Component> drawPile; // Face-down components
    private final List<Component> discardPile; // Permanently discarded
    private final List<Component> faceUpPile; // Face-up returned components
    private final Map<String, Component> reservedComponents; // Components reserved by players
    private final GameLevel gameLevel; // Track level for feature gating
    
    // PropertyChangeSupport for event firing
    private transient PropertyChangeSupport propertyChangeSupport;
    private String gameId; // For event context

    /**
     * Creates a new component deck with the given list of components
     * @param components The initial list of components
     */
    public ComponentDeck(List<Component> components) {
        this.drawPile = new ArrayList<>(components);
        this.discardPile = new ArrayList<>();
        this.faceUpPile = new ArrayList<>();
        this.reservedComponents = new HashMap<>();
        this.gameLevel = GameLevel.TEST_FLIGHT; // Default level
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.gameId = null; // Will be set when associated with a game
        shuffle();
    }
    
    /**
     * Creates a new component deck with the given list of components and game level
     * @param components The initial list of components
     * @param gameLevel The game level for feature gating
     */
    public ComponentDeck(List<Component> components, GameLevel gameLevel) {
        this.drawPile = new ArrayList<>(components);
        this.discardPile = new ArrayList<>();
        this.faceUpPile = new ArrayList<>();
        this.reservedComponents = new HashMap<>();
        this.gameLevel = gameLevel;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.gameId = null; // Will be set when associated with a game
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
        this.gameLevel = level;
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.gameId = null; // Will be set when associated with a game
        initializeDeckForLevel(level);
    }

    /**
     * Sets the game ID for event context.
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    /**
     * Adds a PropertyChangeListener to this deck.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener from this deck.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Fires a PropertyChangeEvent with the given property name and new event.
     */
    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
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
        Component component = drawPile.removeLast();
        
        // Fire ComponentTakenEvent - use null for player since this is anonymous draw
        ComponentTakenEvent event = new ComponentTakenEvent(gameId, null, null, this);
        firePropertyChange("eventPublished", null, event);
        
        return Optional.of(component);
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
            
            // Fire ComponentOfferedEvent for components returned to face-up pile
            ComponentOfferedEvent event = new ComponentOfferedEvent(gameId, null, null, this);
            firePropertyChange("eventPublished", null, event);
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
                
                // Fire ComponentTakenEvent - use null for player since player context handled at higher level
                ComponentTakenEvent event = new ComponentTakenEvent(gameId, null, null, this);
                firePropertyChange("eventPublished", null, event);
                
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
     * @return true if successfully reserved, false if player already has max reservations or feature not supported
     */
    public boolean reserveComponent(String playerId, Component component) {
        if (component == null || playerId == null) {
            return false;
        }
        
        // ENHANCED: Level-specific feature gating
        if (!supportsComponentReservation()) {
            System.out.println("[ComponentDeck] Component reservation not supported in " + gameLevel);
            return false;
        }
        
        // Check if player already has 2 reserved components (max limit)
        long playerReservations = reservedComponents.values().stream()
            .mapToLong(c -> c.getReservedBy() != null && c.getReservedBy().equals(playerId) ? 1 : 0)
            .sum();
            
        if (playerReservations >= getMaxReservationsPerPlayer()) {
            System.out.println("[ComponentDeck] Player " + playerId + " already has " + playerReservations + " reservations (max: " + getMaxReservationsPerPlayer() + ")");
            return false;
        }
        
        reservedComponents.put(component.getId(), component);
        component.setReservedBy(playerId);
        System.out.println("[ComponentDeck] Component " + component.getId() + " reserved by player " + playerId);
        
        // Fire ComponentReservedEvent - need Player object, so comment out for now
        // TODO: Need to get Player object to create ComponentReservedEvent properly
        // PlayerId playerIdObj = PlayerId.fromString(playerId);
        // ComponentReservedEvent event = new ComponentReservedEvent(gameId, component, player, this);
        // firePropertyChange("eventPublished", null, event);
        
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
            
            // Note: ComponentOfferedEvent should be fired by the calling code (like GameSession)
            // that has access to the full Player object
            
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
     * Gets all available components that can be taken (draw pile + face-up)
     * @return List of available components
     */
    public List<Component> getAvailableComponents() {
        List<Component> available = new ArrayList<>();
        available.addAll(drawPile);
        available.addAll(faceUpPile);
        return Collections.unmodifiableList(available);
    }
    
    /**
     * Gets the face-up components (alias for getFaceUpPile for compatibility)
     * @return The face-up components
     */
    public List<Component> getFaceUpComponents() {
        return getFaceUpPile();
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
    
    // ENHANCED: Level-specific feature methods
    
    /**
     * Checks if component reservation is supported in the current game level
     * @return true if reservations are supported, false otherwise
     */
    public boolean supportsComponentReservation() {
        return gameLevel != GameLevel.TEST_FLIGHT;
    }
    
    /**
     * Gets the maximum number of components a player can reserve
     * @return Maximum reservations per player
     */
    public int getMaxReservationsPerPlayer() {
        return supportsComponentReservation() ? 2 : 0;
    }
    
    /**
     * Checks if predictable pile preview is supported in the current game level
     * @return true if predictable piles are supported, false otherwise
     */
    public boolean supportsPredictablePiles() {
        return gameLevel.getPredictablePileCount() > 0;
    }
    
    /**
     * Gets the current game level
     * @return The game level
     */
    public GameLevel getGameLevel() {
        return gameLevel;
    }
    
    /**
     * Gets the maximum number of predictable piles for this level
     * @return Number of predictable piles
     */
    public int getMaxPredictablePiles() {
        return gameLevel.getPredictablePileCount();
    }

    /**
     * Finds a component by ID in the available piles (draw pile + face-up pile)
     * @param componentId The component ID to search for
     * @return The component if found in available piles, null otherwise
     */
    public Component findComponentById(String componentId) {
        // Search in draw pile
        for (Component component : drawPile) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        
        // Search in face-up pile
        for (Component component : faceUpPile) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        
        return null;
    }

    /**
     * Gets all available components from draw and face-up piles for UI sync
     * @return Map of componentId to Component
     */
    public Map<String, Component> getAvailableComponentsMap() {
        Map<String, Component> availableComponents = new HashMap<>();
        
        // Include draw pile components
        for (Component component : drawPile) {
            availableComponents.put(component.getId(), component);
        }
        
        // Include face-up pile components  
        for (Component component : faceUpPile) {
            availableComponents.put(component.getId(), component);
        }
        
        return availableComponents;
    }

    /**
     * Custom serialization to handle transient PropertyChangeSupport.
     */
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    /**
     * Custom deserialization to restore transient PropertyChangeSupport.
     */
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

} 