package it.polimi.ingsw.common.message.event;

/**
 * Enumeration of all event types in the game.
 */
public enum EventType {

    // Game lifecycle events
    GAME_CREATED,
    GAME_STARTED,         // NEW: From your documentation
    GAME_ENDED,
    GAME_SUSPENDED,       // NEW: For resilience
    GAME_LOBBY_UPDATE,    // NEW: For lobby updates
    GAMES_LIST_UPDATE,    // NEW: For main lobby games list updates

    // Player status events
    PLAYER_JOINED_GAME,
    PLAYER_LEFT_GAME,
    PLAYER_DISCONNECTED,
    PLAYER_RECONNECTED,
    PLAYER_READY_CHANGED,
    PLAYER_REGISTERED,        // NEW: Player authentication events
    PLAYER_UNREGISTERED,      // NEW: Player disconnection events
    PLAYER_COMPONENT_CHANGED, // NEW: Player held component changes

    // Game settings events
    GAME_SETTINGS_CHANGED,

    // Building phase events
    BUILDING_PHASE_STARTED,
    TILE_DRAWN,
    TILE_PLACED,
    TILE_REMOVED,
    TILE_ROTATED,
    TILE_RETURNED,
    COMPONENT_OFFERED,
    COMPONENT_TAKEN,      // NEW: For taking components to hand
    COMPONENT_PLACED,     // NEW: For placing components on ship
    COMPONENT_RESERVED,   // For actual reservation action
    BUILDING_TIMER_FLIPPED,
    SHIP_VALIDATION_COMPLETED,
    BUILDING_PHASE_COMPLETED,
    PHASE_CHANGED,
    SHIP_BUILDING_STATE_SYNC,
    COMPONENT_REMOVED,        // NEW: For component removal from ship
    SHIP_STATS_UPDATED,       // NEW: For ship stats recalculation

    // Flight phase events
    FLIGHT_PHASE_STARTED,
    ADVENTURE_CARD_DRAWN,
    ADVENTURE_CARD_PLAYER_TURN,
    ADVENTURE_CARD_COMPLETED,
    ADVENTURE_CARD_RESOLVED,      // NEW: Adventure card resolution complete
    ADVENTURE_CARD_TIMEOUT,
    PLAYER_CHOICE_REQUEST,        // NEW: Request player input for adventure cards
    PLAYER_POSITION_CHANGED,
    FLIGHT_POSITION_UPDATE,
    SHIP_DAMAGED,
    COMPONENT_LOST,
    RESOURCE_UPDATE,
    CREDITS_CHANGED,
    PLAYER_CREDITS_CHANGED,   // NEW: Player credit changes
    CREW_CHANGED,
    DICE_ROLLED,

    // Turn events
    TURN_STARTED,
    TURN_ENDED,

    // Combat events
    COMBAT_STARTED,
    COMBAT_RESOLVED,

    // State synchronization events
    GAME_STATE_SYNC,
}