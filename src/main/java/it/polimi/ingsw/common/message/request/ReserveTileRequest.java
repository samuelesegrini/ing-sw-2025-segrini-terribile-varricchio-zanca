package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.ComponentReservedEvent;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.ReserveTileResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Request to reserve a component tile that is currently in the player's hand.
 * This moves the component from hand to a reservation area for later use.
 */
public class ReserveTileRequest extends AbstractRequest {
    private final String tileId;

    public ReserveTileRequest(String tileId) {
        super();
        this.tileId = tileId;
    }

    @Override
    public ValidationResult validate() {
        if (tileId == null || tileId.trim().isEmpty()) {
            return ValidationResult.failure("Tile ID is required", "tileId");
        }
        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Get game session
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }

        // Check phase
        if (session.getCurrentPhase() != GamePhase.BUILDING) {
            return createErrorResponse("Not in building phase", ErrorResponse.INVALID_STATE);
        }

        // Get player
        PlayerId playerId = context.getPlayerId();
        Player player = session.getPlayer(playerId);
        if (player == null) {
            return createErrorResponse("Player not found", ErrorResponse.INTERNAL_ERROR);
        }

        // For now, implement basic validation - full server logic can be added later
        // Get component by ID (simplified)
        Component component = session.getComponentById(tileId);
        if (component == null) {
            return createErrorResponse("Component not found", ErrorResponse.NOT_FOUND);
        }

        try {
            // ENHANCED: Check level-specific feature support and provide detailed error messages
            ComponentDeck deck = session.getGameModel().getComponentDeck();
            
            // Check if reservations are supported in this game level
            if (!deck.supportsComponentReservation()) {
                return createErrorResponse(
                    String.format("Component reservations are not available in %s mode. This feature is only available in Level II and higher.", 
                                deck.getGameLevel()), 
                    ErrorResponse.INVALID_STATE);
            }
            
            // Check current reservation count and provide detailed feedback
            long currentReservations = deck.getReservedComponents(playerId.toString()).size();
            int maxReservations = deck.getMaxReservationsPerPlayer();
            
            if (currentReservations >= maxReservations) {
                return createErrorResponse(
                    String.format("Cannot reserve component - you already have %d/%d reservations. Remove a reservation first.", 
                                currentReservations, maxReservations), 
                    ErrorResponse.INVALID_STATE);
            }
            
            // Attempt to reserve the component
            boolean reserved = deck.reserveComponent(playerId.toString(), component);
            
            if (!reserved) {
                return createErrorResponse("Failed to reserve component - please try again", ErrorResponse.INTERNAL_ERROR);
            }
            
            // Remove component from player's hand since it's now reserved
            player.clearHeldComponent();

            // Publish event FIRST - this is the single source of truth for state updates
            ComponentReservedEvent event = new ComponentReservedEvent(
                session.getGameId(),
                component,
                player,
                deck
            );
            context.publishEvent(event);

            // Return lightweight response - event contains the state update
            return new ReserveTileResponse(getCorrelationId());

        } catch (Exception e) {
            return createErrorResponse("Failed to reserve component: " + e.getMessage(), ErrorResponse.INTERNAL_ERROR);
        }
    }

    public String getTileId() {
        return tileId;
    }
}