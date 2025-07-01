package it.polimi.ingsw.common.message.response;

import it.polimi.ingsw.common.info.AdventureCardInfo;

import java.util.List;

public class ViewForecastPileResponse extends AbstractResponse {
    private final List<AdventureCardInfo> cards;

    public ViewForecastPileResponse(java.util.UUID correlationId, List<AdventureCardInfo> cards) {
        super(correlationId);
        this.cards = cards;
    }

    public List<AdventureCardInfo> getCards() {
        return cards;
    }

    @Override
    public void handleOnClient(ClientContext context) {
        // Route to the UI interface for proper handling
        var controller = context.getController();
        if (controller != null && controller instanceof it.polimi.ingsw.client.controller.ClientController) {
            var clientController = (it.polimi.ingsw.client.controller.ClientController) controller;
            // Get the UI instance and call the appropriate handler
            var ui = clientController.getUI();
            if (ui != null) {
                ui.onViewForecastPileResponse(this);
            } else {
                // Fallback to basic output if UI not available
                handleFallbackOutput();
            }
        } else {
            handleFallbackOutput();
        }
    }
    
    private void handleFallbackOutput() {
        if (isSuccess()) {
            System.out.println("Forecast pile contents (" + cards.size() + " cards):");
            for (int i = 0; i < cards.size(); i++) {
                var card = cards.get(i);
                System.out.println((i + 1) + ". " + card.getId() + " (Level " + 
                                 card.getLevel() + "): " + card.getDescription());
            }
        } else {
            System.err.println("Failed to view forecast pile: " + getErrorMessage());
        }
    }
}
