package it.polimi.ingsw.common.game;

/**
 * What a component tile does once it is welded to a ship.
 *
 * <p>The thirteen kinds and their counts are printed on page 3 of the manual and are
 * asserted by the game data tests.
 */
public enum ComponentKind {

    /** Holds the ship together and does nothing else. Eight tiles. */
    STRUCTURAL_MODULE,

    /** Houses two humans, or one alien when a matching life support is attached. Seventeen tiles. */
    CABIN,

    /** The cabin a player starts with. Never houses an alien. One per player colour. */
    STARTING_CABIN,

    /** Worth one firepower pointing forward, half otherwise. Twenty-five tiles. */
    SINGLE_CANNON,

    /** Worth two firepower pointing forward, one otherwise, and costs a battery. Eleven tiles. */
    DOUBLE_CANNON,

    /** Worth one engine power. Twenty-one tiles. */
    SINGLE_ENGINE,

    /** Worth two engine power and costs a battery. Nine tiles. */
    DOUBLE_ENGINE,

    /** Stores two or three charges. Seventeen tiles, eleven of two and six of three. */
    BATTERY,

    /** Two or three slots, any colour but red. Fifteen tiles. */
    CARGO_HOLD,

    /** One or two slots, any colour including red. Nine tiles. */
    SPECIAL_CARGO_HOLD,

    /** Protects two adjacent sides from small meteors and light fire, for a battery. Eight tiles. */
    SHIELD,

    /** Lets a purple alien live in an attached cabin. Six tiles. */
    PURPLE_LIFE_SUPPORT,

    /** Lets a brown alien live in an attached cabin. Six tiles. */
    BROWN_LIFE_SUPPORT;

    /**
     * Tells whether tiles of this kind carry a printed number of slots or charges.
     *
     * @return {@code true} for batteries and both kinds of cargo hold
     */
    public boolean hasCapacity() {
        return this == BATTERY || this == CARGO_HOLD || this == SPECIAL_CARGO_HOLD;
    }

    /**
     * Tells whether tiles of this kind can house crew.
     *
     * @return {@code true} for cabins, including the starting cabin
     */
    public boolean isCabin() {
        return this == CABIN || this == STARTING_CABIN;
    }

    /**
     * Tells whether tiles of this kind make a cabin habitable by an alien.
     *
     * @return {@code true} for both life support modules
     */
    public boolean isLifeSupport() {
        return this == PURPLE_LIFE_SUPPORT || this == BROWN_LIFE_SUPPORT;
    }

    /**
     * Tells whether tiles of this kind contribute firepower and can shoot big meteors.
     *
     * @return {@code true} for both cannons
     */
    public boolean isCannon() {
        return this == SINGLE_CANNON || this == DOUBLE_CANNON;
    }

    /**
     * Tells whether tiles of this kind contribute engine power.
     *
     * @return {@code true} for both engines
     */
    public boolean isEngine() {
        return this == SINGLE_ENGINE || this == DOUBLE_ENGINE;
    }

    /**
     * Tells whether using a tile of this kind costs a battery charge.
     *
     * <p>Manual p.6 and p.7: double cannons, double engines and shields all draw on
     * the battery bank. Single cannons and single engines never do, and the player has
     * no say in whether they are counted.
     *
     * @return {@code true} when activation costs one charge
     */
    public boolean consumesCharge() {
        return this == DOUBLE_CANNON || this == DOUBLE_ENGINE || this == SHIELD;
    }
}
