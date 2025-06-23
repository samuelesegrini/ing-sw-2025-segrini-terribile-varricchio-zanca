package it.polimi.ingsw.common.message.event;

/**
 * Enumeration of all event types in the game.
 */
public enum EventType {
    // Connection events
    PLAYER_CONNECTED,
    PLAYER_DISCONNECTED,
    PLAYER_RECONNECTED,

    // Game lifecycle events
    GAME_CREATED,
    GAME_STARTED,         // NEW: From your documentation
    GAME_ENDED,
    GAME_SUSPENDED,       // NEW: For resilience
    GAME_LOBBY_UPDATE,    // NEW: For lobby updates

    // Player status events
    PLAYER_JOINED_GAME,
    PLAYER_LEFT_GAME,
    PLAYER_READY_CHANGED,

    // Game settings events
    GAME_SETTINGS_CHANGED,

    // Building phase events
    TILE_DRAWN,
    TILE_PLACED,
    TILE_REMOVED,
    TILE_ROTATED,
    TILE_RETURNED,
    COMPONENT_OFFERED,
    COMPONENT_RESERVED,
    BUILDING_TIMER_FLIPPED,
    SHIP_VALIDATION_COMPLETED,
    BUILDING_PHASE_COMPLETED,
    PHASE_CHANGED,
    SHIP_BUILDING_STATE_SYNC,

    // Flight phase events
    ADVENTURE_CARD_DRAWN,
    PLAYER_POSITION_CHANGED,
    FLIGHT_POSITION_UPDATE,
    SHIP_DAMAGED,
    COMPONENT_LOST,
    RESOURCE_UPDATE,
    CREDITS_CHANGED,
    CREW_CHANGED,
    DICE_ROLLED,

    // Turn events
    TURN_STARTED,
    TURN_ENDED,

    // Combat events
    COMBAT_STARTED,
    COMBAT_RESOLVED,
}