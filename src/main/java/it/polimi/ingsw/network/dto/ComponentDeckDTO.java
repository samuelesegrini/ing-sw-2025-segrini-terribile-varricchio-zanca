package it.polimi.ingsw.network.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Data Transfer Object representing the state of the shared component pool.
 */
public class ComponentDeckDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Example: Map<ComponentTypeName, Count>
    private Map<String, Integer> availableComponents;
    private int remainingCount; // Overall count if needed

    public ComponentDeckDTO() {
        // Default constructor
    }

    // --- Getters and Setters ---

    public Map<String, Integer> getAvailableComponents() {
        return availableComponents;
    }

    public void setAvailableComponents(Map<String, Integer> availableComponents) {
        this.availableComponents = availableComponents;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
    }
}