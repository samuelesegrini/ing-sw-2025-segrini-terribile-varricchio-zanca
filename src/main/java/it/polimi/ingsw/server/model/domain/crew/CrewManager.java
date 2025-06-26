package it.polimi.ingsw.server.model.domain.crew;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.components.Cabin;
import it.polimi.ingsw.server.model.enums.crew.CrewType;
import it.polimi.ingsw.server.model.enums.crew.AlienColor;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Manages crew members and alien passengers aboard a ship.
 * Handles crew assignment, alien bonuses, and life support requirements.
 */
public class CrewManager {
    private final Ship ship;
    private final Map<Component, List<CrewMember>> cabinAssignments;
    private final List<AlienPassenger> aliens;
    
    public CrewManager(Ship ship) {
        this.ship = ship;
        this.cabinAssignments = new HashMap<>();
        this.aliens = new ArrayList<>();
    }
    
    /**
     * Represents a crew member (human or alien) in a cabin.
     */
    public static class CrewMember {
        private final CrewType type;
        private final String name;
        private boolean active;
        
        public CrewMember(CrewType type, String name) {
            this.type = type;
            this.name = name;
            this.active = true;
        }
        
        public CrewType getType() { return type; }
        public String getName() { return name; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public boolean isAlien() { return type.isAlien(); }
        public AlienColor getAlienColor() { return type.getAlienColor(); }
    }
    
    /**
     * Adds a crew member to the ship if there's space.
     */
    public boolean addCrewMember(CrewType type, String name) {
        // Find an available cabin with space
        Component[][] board = ship.getBoard();
        
        for (Component[] row : board) {
            for (Component component : row) {
                if (component != null && (component.getType() == ComponentType.CABIN || component.getType() == ComponentType.CABIN_START)) {
                    List<CrewMember> currentCrew = cabinAssignments.getOrDefault(component, new ArrayList<>());
                    
                    // Check if cabin has space
                    long activeCrewCount = currentCrew.stream().filter(CrewMember::isActive).count();
                    if (activeCrewCount < type.getMaxPerCabin()) {
                        CrewMember newMember = new CrewMember(type, name);
                        currentCrew.add(newMember);
                        cabinAssignments.put(component, currentCrew);
                        
                        // If it's an alien, add to alien list for bonus tracking
                        if (type.isAlien()) {
                            aliens.add(new AlienPassenger(type.getAlienColor(), name));
                        }
                        
                        return true;
                    }
                }
            }
        }
        
        return false; // No space available
    }
    
    /**
     * Removes a crew member from the ship (death, abandonment, etc.)
     */
    public boolean removeCrewMember(String name) {
        for (var entry : cabinAssignments.entrySet()) {
            List<CrewMember> crew = entry.getValue();
            for (CrewMember member : crew) {
                if (member.getName().equals(name) && member.isActive()) {
                    member.setActive(false);
                    
                    // If it was an alien, deactivate from alien list
                    if (member.isAlien()) {
                        aliens.stream()
                              .filter(alien -> alien.getName().equals(name))
                              .findFirst()
                              .ifPresent(alien -> alien.setActive(false));
                    }
                    
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Gets total active crew count.
     */
    public int getTotalCrewCount() {
        return cabinAssignments.values().stream()
                .flatMap(List::stream)
                .mapToInt(member -> member.isActive() ? 1 : 0)
                .sum();
    }
    
    /**
     * Gets combat bonus from all active purple aliens.
     */
    public int getCombatBonus() {
        return aliens.stream()
                .filter(AlienPassenger::isActive)
                .mapToInt(AlienPassenger::getCombatBonus)
                .sum();
    }
    
    /**
     * Gets engine bonus from all active aliens.
     */
    public int getEngineBonus() {
        return aliens.stream()
                .filter(AlienPassenger::isActive)
                .mapToInt(AlienPassenger::getEngineBonus)
                .sum();
    }
    
    /**
     * Gets life support bonus from all active aliens.
     */
    public int getLifeSupportBonus() {
        return aliens.stream()
                .filter(AlienPassenger::isActive)
                .mapToInt(AlienPassenger::getLifeSupportBonus)
                .sum();
    }
    
    /**
     * Gets end-game bonus credits from all active aliens.
     */
    public int getEndGameBonus() {
        return aliens.stream()
                .filter(AlienPassenger::isActive)
                .mapToInt(AlienPassenger::getEndGameBonus)
                .sum();
    }
    
    /**
     * Gets count of specific alien type.
     */
    public int getAlienCount(AlienColor color) {
        return (int) aliens.stream()
                .filter(AlienPassenger::isActive)
                .filter(alien -> alien.getColor() == color)
                .count();
    }
    
    /**
     * Gets all active aliens.
     */
    public List<AlienPassenger> getActiveAliens() {
        return aliens.stream()
                .filter(AlienPassenger::isActive)
                .toList();
    }
    
    /**
     * Gets all crew members in a specific cabin.
     */
    public List<CrewMember> getCrewInCabin(Component cabin) {
        return cabinAssignments.getOrDefault(cabin, new ArrayList<>());
    }
    
    /**
     * Gets all active crew members.
     */
    public List<CrewMember> getAllActiveCrew() {
        return cabinAssignments.values().stream()
                .flatMap(List::stream)
                .filter(CrewMember::isActive)
                .toList();
    }
    
    /**
     * Loses crew members due to epidemic or other effects.
     */
    public int loseCrewMembers(int count) {
        int lost = 0;
        List<CrewMember> activeCrew = getAllActiveCrew();
        
        for (int i = 0; i < Math.min(count, activeCrew.size()); i++) {
            CrewMember member = activeCrew.get(i);
            if (removeCrewMember(member.getName())) {
                lost++;
            }
        }
        
        return lost;
    }
    
    /**
     * Checks if ship has life support for all crew.
     */
    public boolean hasAdequateLifeSupport() {
        int totalCrew = getTotalCrewCount();
        int lifeSupport = ship.countAllAdjacentCabins() + getLifeSupportBonus();
        return lifeSupport >= totalCrew;
    }
}