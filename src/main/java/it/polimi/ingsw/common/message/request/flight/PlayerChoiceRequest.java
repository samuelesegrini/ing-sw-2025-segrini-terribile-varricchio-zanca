package it.polimi.ingsw.common.message.request.flight;

import it.polimi.ingsw.common.message.request.AbstractRequest;
import it.polimi.ingsw.common.message.request.RequestContext;
import it.polimi.ingsw.common.message.response.ErrorResponse;
import it.polimi.ingsw.common.message.response.Response;
import it.polimi.ingsw.common.message.response.PlayerChoiceResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.GameSession;
import it.polimi.ingsw.server.model.domain.adventure.AdventureCardController;
import it.polimi.ingsw.server.model.enums.GamePhase;

/**
 * Unified request for player choices during adventure card resolution.
 * Replaces individual choice request types with a single flexible system.
 */
public class PlayerChoiceRequest extends AbstractRequest {
    private final String choiceType;
    private final String choiceValue;
    private final String correlationId;
    
    /**
     * Creates a player choice request.
     * 
     * @param choiceType The type of choice being made
     * @param choiceValue The player's chosen value
     * @param correlationId ID linking this to the original choice request event
     */
    public PlayerChoiceRequest(String choiceType, String choiceValue, String correlationId) {
        super();
        this.choiceType = choiceType;
        this.choiceValue = choiceValue;
        this.correlationId = correlationId;
    }
    
    @Override
    public ValidationResult validate() {
        if (choiceType == null || choiceType.trim().isEmpty()) {
            return ValidationResult.failure("Choice type cannot be empty");
        }
        if (choiceValue == null) {
            return ValidationResult.failure("Choice value cannot be null");
        }
        return ValidationResult.success();
    }
    
    @Override
    public Response execute(RequestContext context) {
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }
        
        GameSession session = context.getGameSession();
        if (session == null) {
            return createErrorResponse("Not in a game", ErrorResponse.INVALID_STATE);
        }
        
        if (session.getCurrentPhase() != GamePhase.FLIGHT) {
            return createErrorResponse("Can only make choices during flight phase", 
                                     ErrorResponse.INVALID_STATE);
        }
        
        try {
            AdventureCardController cardController = session.getAdventureCardController();
            if (cardController == null) {
                return createErrorResponse("Adventure card controller not available", 
                                         ErrorResponse.INVALID_STATE);
            }
            
            // Record the player's choice in the controller
            boolean success = cardController.recordPlayerChoice(
                context.getPlayerId(), 
                choiceType, 
                choiceValue
            );
            
            if (!success) {
                return createErrorResponse("Failed to record player choice", 
                                         ErrorResponse.INTERNAL_ERROR);
            }
            
            return new PlayerChoiceResponse(getCorrelationId(), choiceType, choiceValue);
            
        } catch (Exception e) {
            return createErrorResponse("Error processing player choice: " + e.getMessage(), 
                                     ErrorResponse.INTERNAL_ERROR);
        }
    }
    
    // Getters
    public String getChoiceType() { return choiceType; }
    public String getChoiceValue() { return choiceValue; }
    public String getRequestCorrelationId() { return correlationId; }
}