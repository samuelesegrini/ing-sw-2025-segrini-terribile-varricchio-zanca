package it.polimi.ingsw.common.model;

public enum GameSessionState {
    LOBBY,             // Players are joining, getting ready
    SHIP_BUILDING,     // Players are actively building their ships (real-time phase)
    FLIGHT_PREPARATION,// Brief phase after building, before flight (e.g. check ships, fill batteries - as per rules page 9 "Preparing for Launch")
    FLIGHT,            // Adventure cards are being revealed and resolved
    SCORING,           // Final scores are being calculated after the flight
    FINISHED,          // Game is over, results displayed, session might be archived or closed
    ABORTED            // Game ended prematurely (e.g. all players left)
}