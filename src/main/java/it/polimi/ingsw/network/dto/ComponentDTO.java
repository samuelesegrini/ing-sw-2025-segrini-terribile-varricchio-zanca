package it.polimi.ingsw.network.dto;

import java.io.Serializable;

/**
 * Data Transfer Object representing a ship component.
 */
public class ComponentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type; // e.g., "CABIN", "ENGINE", "CANNON"
    private int rotation; // 0, 90, 180, 270 degrees
    private int currentHp;
    private int maxHp;
    // Add other component-specific details as needed
    // private int crewCapacity; // For cabins
    // private int power; // For engines

    public ComponentDTO() {
        // Default constructor
    }

    // Add getters and setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }
}