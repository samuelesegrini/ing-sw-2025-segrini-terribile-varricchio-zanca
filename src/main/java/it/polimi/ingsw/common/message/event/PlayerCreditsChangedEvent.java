package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.logging.Logger;

/**
 * Event broadcast when a player's credit balance changes.
 * Updates the UI to show current credit amounts.
 */
public class PlayerCreditsChangedEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(PlayerCreditsChangedEvent.class.getName());
    
    private final PlayerId playerId;
    private final String playerNickname;
    private final int oldCredits;
    private final int newCredits;
    private final int changeAmount;

    public PlayerCreditsChangedEvent(String gameId, PlayerId playerId, String playerNickname, 
                                   int oldCredits, int newCredits) {
        super(EventType.PLAYER_CREDITS_CHANGED, gameId, playerId);
        this.playerId = playerId;
        this.playerNickname = playerNickname;
        this.oldCredits = oldCredits;
        this.newCredits = newCredits;
        this.changeAmount = newCredits - oldCredits;
        LOGGER.fine("PlayerCreditsChangedEvent created for player: " + playerNickname + 
                   ", credits: " + oldCredits + " -> " + newCredits);
    }

    public PlayerId getPlayerId() {
        return playerId;
    }

    public String getPlayerNickname() {
        return playerNickname;
    }

    public int getOldCredits() {
        return oldCredits;
    }

    public int getNewCredits() {
        return newCredits;
    }

    public int getChangeAmount() {
        return changeAmount;
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Send to all players in the game to show credit changes
        return super.shouldSendTo(clientId, context);
    }

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Update player's credit amount
        clientState.setPlayerCredits(playerId.toString(), newCredits);
        clientState.incrementStateVersion();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        context.runOnUIThread(() -> {
            LOGGER.fine("Handling PlayerCreditsChangedEvent for player: " + playerNickname + 
                       ", new credits: " + newCredits);
            
            // UI updates will be handled by the refreshCurrentViewOnly() call in updateClientState
        });
    }
}