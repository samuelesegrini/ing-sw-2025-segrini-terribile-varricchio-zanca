package it.polimi.ingsw.network.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * Data Transfer Object representing a player's ship.
 */
public class ShipDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    // Grid mapping position to component
    private Map<PositionDTO, ComponentDTO> grid;
    private boolean validStructure;
    private int crewCount;
    private double totalEnginePower;
    private int batteries;
    // Add other relevant stats: cannons, shields, cargo capacity/contents, etc.

    public ShipDTO() {
        // Default constructor
    }

    // --- Getters and Setters ---

    public Map<PositionDTO, ComponentDTO> getGrid() {
        return grid;
    }

    public void setGrid(Map<PositionDTO, ComponentDTO> grid) {
        this.grid = grid;
    }

    public boolean isValidStructure() {
        return validStructure;
    }

    public void setValidStructure(boolean validStructure) {
        this.validStructure = validStructure;
    }

    public int getCrewCount() {
        return crewCount;
    }

    public void setCrewCount(int crewCount) {
        this.crewCount = crewCount;
    }

    public double getTotalEnginePower() {
        return totalEnginePower;
    }

    public void setTotalEnginePower(double totalEnginePower) {
        this.totalEnginePower = totalEnginePower;
    }

    public int getBatteries() {
        return batteries;
    }

    public void setBatteries(int batteries) {
        this.batteries = batteries;
    }
}