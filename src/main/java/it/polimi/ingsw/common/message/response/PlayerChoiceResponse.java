package it.polimi.ingsw.common.message.response;

/**
 * Response to a player choice request during adventure card resolution.
 */
public class PlayerChoiceResponse extends AbstractResponse {
    private final String choiceType;
    private final String choiceValue;
    
    /**
     * Creates a player choice response.
     * 
     * @param correlationId The correlation ID from the original request
     * @param choiceType The type of choice that was made
     * @param choiceValue The value that was chosen
     */
    public PlayerChoiceResponse(java.util.UUID correlationId, String choiceType, String choiceValue) {
        super(correlationId);
        this.choiceType = choiceType;
        this.choiceValue = choiceValue;
    }
    
    // Getters
    public String getChoiceType() { return choiceType; }
    public String getChoiceValue() { return choiceValue; }

    @Override
    public void handleOnClient(ClientContext context) {
        // Handle player choice response on client
        // TODO: Implement specific client handling logic
    }
}