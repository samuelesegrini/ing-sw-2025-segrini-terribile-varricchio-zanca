package it.polimi.ingsw.model.enums.crew;

public enum CrewType {
    ALIEN_PURPLE(true, AlienColor.PURPLE, 1),
    ALIEN_BROWN(true, AlienColor.BROWN, 1),
    HUMAN(false, null, 2);

    private boolean isAlien;
    private AlienColor alienColor;
    private int maxPerCabin;

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
        return isAlien;
    }

    /**
     * Gets the alien color (null if crew type is human).
     * @return The alien color (null if CrewType=HUMAN)
     */
    public AlienColor getAlienColor() {
        return alienColor;
    }

    /**
     * Gets the maximum number of human/alien crew per cabin.
     * @return The maximum number of human/alien crew
     */
    public int getMaxPerCabin() {
        return maxPerCabin;
    }
}