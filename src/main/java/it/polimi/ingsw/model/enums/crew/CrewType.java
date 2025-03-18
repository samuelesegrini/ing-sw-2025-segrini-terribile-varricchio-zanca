package it.polimi.ingsw.model.enums.crew;

public enum CrewType {
    /**
     * Crew type is Purple Alien.
     */
    ALIEN_PURPLE(true, AlienColor.ALIEN_PURPLE, 1),

    /**
     * Crew type is Brown Alien.
     */
    ALIEN_BROWN(true, AlienColor.ALIEN_BROWN, 1),

    /**
     * Crew type is Human.
     */
    HUMAN(false, null, 2);

    private final boolean isAlien;
    private final AlienColor alienColor;
    private final int maxPerCabin;

    private CrewType(boolean isAlien, AlienColor alienColor, int maxPerCabin) {
        this.isAlien = isAlien;
        this.alienColor = alienColor;
        this.maxPerCabin = maxPerCabin;
    }

    /**
     * Returns true if the crew type is alien, false if human.
     * @return {@code true} if the crew type is alien, {@code false} otherwise
     */
    public boolean isAlien() {
        return this.isAlien;
    }

    /**
     * Gets the alien color (null if crew type is human).
     * @return The alien color (null if CrewType=HUMAN)
     */
    public AlienColor getAlienColor() {
        return this.alienColor;
    }

    /**
     * Gets the maximum number of human/alien crew per cabin.
     * @return The maximum number of human/alien crew
     */
    public int getMaxPerCabin() {
        return this.maxPerCabin;
    }
}