package it.polimi.ingsw.server.model.crew;

import it.polimi.ingsw.server.model.component.ComponentKind;

/**
 * The two alien species a ship can carry.
 *
 * <p>An alien takes the space of two humans and needs a life support module of its own
 * colour attached to its cabin. In exchange it adds two to one of the ship's
 * attributes — but only if that attribute is already above zero, since the manual is
 * clear that neither species will fight bare-tentacled or get out and push (p.18).
 */
public enum AlienColor {

    /** A warlike species. Adds two to firepower. */
    PURPLE(ComponentKind.PURPLE_LIFE_SUPPORT),

    /** Excellent mechanics. Adds two to engine power. */
    BROWN(ComponentKind.BROWN_LIFE_SUPPORT);

    private final ComponentKind lifeSupport;

    AlienColor(ComponentKind lifeSupport) {
        this.lifeSupport = lifeSupport;
    }

    /**
     * Returns the life support module this species needs.
     *
     * @return the matching life support kind
     */
    public ComponentKind lifeSupport() {
        return lifeSupport;
    }

    /**
     * Returns the species a life support module houses.
     *
     * @param kind a life support component kind
     * @return the species that module supports
     * @throws IllegalArgumentException if the kind is not a life support module
     */
    public static AlienColor supportedBy(ComponentKind kind) {
        for (AlienColor color : values()) {
            if (color.lifeSupport == kind) {
                return color;
            }
        }
        throw new IllegalArgumentException(kind + " is not a life support module");
    }
}
