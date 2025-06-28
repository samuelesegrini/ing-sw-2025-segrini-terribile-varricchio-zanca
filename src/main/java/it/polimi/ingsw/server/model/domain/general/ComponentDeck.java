package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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
    private final Map<String, Component> allComponents; // Master registry of all components by ID
    private final Map<String, String> componentOwnership; // componentId -> playerId mapping
    private final GameLevel gameLevel; // Track level for feature gating

    /**
     * Creates a new component deck with the given list of components
     * @param components The initial list of components
     */
    public ComponentDeck(List<Component> components) {
        this.drawPile = new ArrayList<>(components);
        this.discardPile = new ArrayList<>();
        this.faceUpPile = new ArrayList<>();
        this.reservedComponents = new HashMap<>();
        this.allComponents = new HashMap<>();
        this.componentOwnership = new HashMap<>();
        this.gameLevel = GameLevel.TEST_FLIGHT; // Default level
        
        // Build master registry of all components
        for (Component component : components) {
            allComponents.put(component.getId(), component);
        }
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
        this.allComponents = new HashMap<>();
        this.componentOwnership = new HashMap<>();
        this.gameLevel = gameLevel;
        
        // Build master registry of all components
        for (Component component : components) {
            allComponents.put(component.getId(), component);
        }
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
        this.allComponents = new HashMap<>();
        this.componentOwnership = new HashMap<>();
        this.gameLevel = level;
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
     * Gets a component by its ID from all components (regardless of location)
     * @param componentId The component ID to search for
     * @return The component if found, null otherwise
     */
    public Component getComponentById(String componentId) {
        return allComponents.get(componentId);
    }

    /**
     * Assigns ownership of a component to a player
     * @param componentId The component ID
     * @param playerId The player ID who owns the component
     */
    public void setComponentOwnership(String componentId, String playerId) {
        componentOwnership.put(componentId, playerId);
    }

    /**
     * Gets the owner of a component
     * @param componentId The component ID
     * @return The player ID who owns the component, null if unowned
     */
    public String getComponentOwner(String componentId) {
        return componentOwnership.get(componentId);
    }

    /**
     * Removes ownership of a component
     * @param componentId The component ID
     */
    public void clearComponentOwnership(String componentId) {
        componentOwnership.remove(componentId);
    }

    /**
     * Gets all components currently owned by a player
     * @param playerId The player ID
     * @return List of components owned by the player
     */
    public List<Component> getPlayerOwnedComponents(String playerId) {
        return componentOwnership.entrySet().stream()
                .filter(entry -> playerId.equals(entry.getValue()))
                .map(entry -> allComponents.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets a map of all components for UI synchronization
     * @return Map of componentId to Component for all available components
     */
    public Map<String, Component> getAllComponentsMap() {
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
     * Checks if a component is currently available (not owned by a player)
     * @param componentId The component ID to check
     * @return true if the component is available, false if owned or not found
     */
    public boolean isComponentAvailable(String componentId) {
        return allComponents.containsKey(componentId) && 
               !componentOwnership.containsKey(componentId);
    }
} 