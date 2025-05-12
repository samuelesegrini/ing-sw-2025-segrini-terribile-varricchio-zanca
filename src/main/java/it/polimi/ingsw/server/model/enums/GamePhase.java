package it.polimi.ingsw.server.model.enums;

/**
 * Represents the different phases of the game
 */
public enum GamePhase {
    /**
     * Initial setup phase where players join and game is configured
     */
    SETUP,

    /**
     * Building phase where players construct their ships
     */
    BUILDING,

    /**
     * Flight phase where players navigate through space
     */
    FLIGHT,

    /**
     * End phase where final scores are calculated and winner is determined
     */
    END;

    /**
     * Gets the next phase in the game sequence
     * @return the next game phase, or END if already at END phase
     */
    public GamePhase getNextPhase() {
        return switch (this) {
            case SETUP -> BUILDING;
            case BUILDING -> FLIGHT;
            case FLIGHT, END -> END;
        };
    }
}