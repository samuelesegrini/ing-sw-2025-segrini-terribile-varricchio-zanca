package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Shield component that provides directional protection from incoming damage.
 * Shields can absorb shots and protect adjacent components according to Galaxy Trucker rules.
 */
public class Shield extends Component {
    private static final long serialVersionUID = 1L;
    
    private Set<Direction> protectedDirections;
    private boolean charged;
    private int shieldStrength;
    private int maxShieldStrength;
    private int powerConsumption;
    private boolean isActive;
    private int damageAbsorbed;

    public Shield(ComponentType type, Map<Direction, ConnectorType> connectors, String id) {
        super(type, connectors, id);
        this.charged = false;
        this.protectedDirections = new HashSet<>();
        
        // Shield protects based on facing direction: facing direction + clockwise direction
        updateProtectedDirections();
        
        // Initialize shield properties with hardcoded values (no JSON properties exist)
        this.shieldStrength = 1;
        this.maxShieldStrength = 1;
        this.powerConsumption = 1;
        
        this.isActive = true;
        this.damageAbsorbed = 0;
    }

    @Override
    public void rotate() {
        // Call parent rotation to handle connectors
        super.rotate();
        
        // Recalculate protected directions based on new facing direction
        updateProtectedDirections();
    }

    /**
     * Updates protected directions based on the shield's facing direction.
     * A shield protects its facing direction and the clockwise adjacent direction.
     */
    private void updateProtectedDirections() {
        this.protectedDirections.clear();
        this.protectedDirections.add(getDirection()); // Facing direction
        this.protectedDirections.add(getDirection().rotateClockwise()); // Clockwise adjacent
    }

    @Override
    public void use(UseComponentVisitor v) {
        v.useShield(this.getShip(), this);
    }

    @Override
    public void count(Ship ship) {
        // Shields contribute to ship's defensive capabilities
        // Note: Shield counting handled separately in ship validation
    }

    @Override
    public boolean check(Ship ship) {
        // Shields need power connection to function
        return hasValidPowerConnection(ship);
    }

    /**
     * Checks if shield has valid connection to ship's power systems.
     */
    private boolean hasValidPowerConnection(Ship ship) {
        Component[][] board = ship.getBoard();
        
        for (Direction d : Direction.values()) {
            if (this.getPosition() != null) {
                Position neighbor = this.getPosition().offsetBy(d);
                int row = neighbor.getRow();
                int col = neighbor.getCol();

                if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                    Component neighborComponent = board[row][col];
                    if (neighborComponent != null) {
                        ConnectorType thisConnector = this.getConnectorAt(d);
                        ConnectorType neighborConnector = neighborComponent.getConnectorAt(d.getOpposite());
                        
                        if (thisConnector != ConnectorType.PLAIN && neighborConnector != ConnectorType.PLAIN
                            && thisConnector == neighborConnector) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Attempts to absorb incoming damage from a specific direction.
     * @param direction The direction the damage is coming from
     * @param intensity The intensity of the incoming shot
     * @return true if the damage was completely absorbed, false if it penetrated
     */
    public boolean absorbDamage(Direction direction, ShotIntensity intensity) {
        if (!isActive || !charged || !protectedDirections.contains(direction)) {
            return false; // Shield cannot protect from this direction
        }

        int damageValue = getShotDamageValue(intensity);
        
        if (shieldStrength >= damageValue) {
            // Shield completely absorbs the damage
            shieldStrength -= damageValue;
            damageAbsorbed += damageValue;
            
            if (shieldStrength <= 0) {
                // Shield depleted
                isActive = false;
                charged = false;
            }
            
            return true; // Damage completely absorbed
        } else {
            // Shield partially absorbs damage, then fails
            damageAbsorbed += shieldStrength;
            shieldStrength = 0;
            isActive = false;
            charged = false;
            
            return false; // Damage partially absorbed, some penetrates
        }
    }

    /**
     * Converts shot intensity to damage value.
     */
    private int getShotDamageValue(ShotIntensity intensity) {
        return switch (intensity) {
            case LIGHT -> 1;
            case HEAVY -> 2;
        };
    }

    /**
     * Charges the shield using battery power.
     * @param powerAvailable The amount of power available for charging
     * @return The amount of power actually consumed
     */
    public int chargeShield(int powerAvailable) {
        if (charged || !isActive) {
            return 0; // Already charged or inactive
        }
        
        if (powerAvailable >= powerConsumption) {
            charged = true;
            return powerConsumption;
        }
        
        return 0; // Not enough power to charge
    }

    /**
     * Repairs the shield (restores strength and activation).
     * @param repairAmount The amount of repair to apply
     * @return The actual amount repaired
     */
    public int repairShield(int repairAmount) {
        if (shieldStrength >= maxShieldStrength) {
            return 0; // Already at full strength
        }
        
        int actualRepair = Math.min(repairAmount, maxShieldStrength - shieldStrength);
        shieldStrength += actualRepair;
        
        if (shieldStrength > 0) {
            isActive = true;
        }
        
        return actualRepair;
    }

    /**
     * Gets all components protected by this shield.
     */
    public List<Component> getProtectedComponents(Ship ship) {
        List<Component> protectedComponents = new ArrayList<>();
        Component[][] board = ship.getBoard();
        
        for (Direction protectedDir : protectedDirections) {
            if (this.getPosition() != null) {
                Position protectedPos = this.getPosition().offsetBy(protectedDir);
                int row = protectedPos.getRow();
                int col = protectedPos.getCol();
                
                if (row >= 0 && row < board.length && col >= 0 && col < board[0].length) {
                    Component protectedComponent = board[row][col];
                    if (protectedComponent != null) {
                        protectedComponents.add(protectedComponent);
                    }
                }
            }
        }
        
        return protectedComponents;
    }

    /**
     * Checks if this shield can protect from the given direction.
     */
    public boolean canProtectFrom(Direction direction) {
        return isActive && charged && protectedDirections.contains(direction);
    }

    /**
     * Gets shield effectiveness as a percentage.
     */
    public double getShieldEffectiveness() {
        if (maxShieldStrength == 0) return 0.0;
        return (double) shieldStrength / maxShieldStrength;
    }

    /**
     * Discharges the shield (removes power).
     */
    public void discharge() {
        charged = false;
    }

    /**
     * Resets shield for new encounter.
     */
    public void resetForNewEncounter() {
        // Shields maintain their charge and strength between encounters
        // Only reset damage tracking
        damageAbsorbed = 0;
    }

    // Getters and Setters
    public Set<Direction> getProtectedDirections() {
        return new HashSet<>(protectedDirections);
    }

    public void setProtectedDirections(Set<Direction> protectedDirections) {
        this.protectedDirections = new HashSet<>(protectedDirections);
    }

    public boolean isCharged() {
        return charged;
    }

    public void setCharged(boolean charged) {
        this.charged = charged;
    }

    public int getShieldStrength() {
        return shieldStrength;
    }

    public void setShieldStrength(int shieldStrength) {
        this.shieldStrength = Math.max(0, Math.min(shieldStrength, maxShieldStrength));
        if (this.shieldStrength <= 0) {
            isActive = false;
            charged = false;
        }
    }

    public int getMaxShieldStrength() {
        return maxShieldStrength;
    }

    public int getPowerConsumption() {
        return powerConsumption;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        if (!active) {
            charged = false;
        }
    }

    public int getDamageAbsorbed() {
        return damageAbsorbed;
    }

    public boolean isDestroyed() {
        return !isActive || shieldStrength <= 0;
    }

    @Override
    public String toString() {
        return String.format("Shield{strength=%d/%d, charged=%s, active=%s, directions=%s, effectiveness=%.1f%%}", 
                           shieldStrength, maxShieldStrength, charged, isActive, protectedDirections, 
                           getShieldEffectiveness() * 100);
    }
}
