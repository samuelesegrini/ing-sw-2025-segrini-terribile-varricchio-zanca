package it.polimi.ingsw.server.model.domain.crew;

import it.polimi.ingsw.server.model.enums.crew.AlienColor;

import java.io.Serializable;

/**
 * Represents an alien passenger with special abilities in Galaxy Trucker.
 * According to the rules, aliens provide specific bonuses based on their color.
 */
public class AlienPassenger implements Serializable {
    private static final long serialVersionUID = 1L;
    private final AlienColor color;
    private final String name;
    private boolean active;
    
    public AlienPassenger(AlienColor color, String name) {
        this.color = color;
        this.name = name;
        this.active = true;
    }
    
    public AlienColor getColor() {
        return color;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Returns the combat bonus this alien provides.
     * According to Galaxy Trucker rules, purple aliens provide +2 combat strength.
     */
    public int getCombatBonus() {
        if (!active) return 0;
        
        return switch (color) {
            case ALIEN_PURPLE -> 2;  // Purple aliens provide combat bonus
            case ALIEN_BROWN -> 0;   // Brown aliens don't provide combat bonus
        };
    }
    
    /**
     * Returns the engine bonus this alien provides.
     * According to Galaxy Trucker rules, brown aliens provide +2 engine strength.
     */
    public int getEngineBonus() {
        if (!active) return 0;
        
        return switch (color) {
            case ALIEN_PURPLE -> 0;  // Purple aliens don't provide engine bonus
            case ALIEN_BROWN -> 2;   // Brown aliens provide +2 engine bonus per rules
        };
    }
    
    /**
     * Returns the life support efficiency bonus.
     * Some aliens reduce crew requirements or improve life support.
     */
    public int getLifeSupportBonus() {
        if (!active) return 0;
        
        return switch (color) {
            case ALIEN_PURPLE -> 0;  // Purple aliens don't affect life support
            case ALIEN_BROWN -> 1;   // Brown aliens improve life support efficiency
        };
    }
    
    /**
     * Returns bonus credits this alien provides at journey's end.
     */
    public int getEndGameBonus() {
        if (!active) return 0;
        
        return switch (color) {
            case ALIEN_PURPLE -> 3;  // Purple aliens provide end-game credits
            case ALIEN_BROWN -> 2;   // Brown aliens provide smaller end-game bonus
        };
    }
    
    @Override
    public String toString() {
        return String.format("AlienPassenger{color=%s, name='%s', active=%s}", 
                           color, name, active);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AlienPassenger that = (AlienPassenger) obj;
        return color == that.color && name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(color, name);
    }
}