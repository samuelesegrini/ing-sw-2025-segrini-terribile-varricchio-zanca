package it.polimi.ingsw.common.message.event;


/**
 * Event broadcast when a component tile is offered to a specific player.
 * This can happen in advanced building rules or special game scenarios.
 */
public class ComponentOfferedEvent extends AbstractEvent {
    private final String tileId;
    private final String tileType;
    private final String offeredToPlayerId;
    private final String offeredToPlayerNickname;
    private final String reason;
    private final long offerExpiresAt;

    public ComponentOfferedEvent(String gameId, String tileId, String tileType, 
                                String offeredToPlayerId, String offeredToPlayerNickname,
                                String reason, long offerExpiresAt) {
        super(EventType.COMPONENT_OFFERED, gameId, offeredToPlayerId);
        this.tileId = tileId;
        this.tileType = tileType;
        this.offeredToPlayerId = offeredToPlayerId;
        this.offeredToPlayerNickname = offeredToPlayerNickname;
        this.reason = reason;
        this.offerExpiresAt = offerExpiresAt;
    }

    public String getTileId() {
        return tileId;
    }

    public String getTileType() {
        return tileType;
    }

    public String getOfferedToPlayerId() {
        return offeredToPlayerId;
    }

    public String getOfferedToPlayerNickname() {
        return offeredToPlayerNickname;
    }

    public String getReason() {
        return reason;
    }

    public long getOfferExpiresAt() {
        return offerExpiresAt;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        context.runOnUIThread(() -> {
            // Update game state with component offer
            if (context.getGameState() != null) {
                // context.getGameState().addComponentOffer(tileId, offeredToPlayerId, offerExpiresAt);
            }

            // Show notification
            if (context.getNotificationService() != null) {
                String message;
                
                if (context.isLocalPlayer(offeredToPlayerId)) {
                    // Notification for the player receiving the offer
                    message = String.format("You have been offered a %s tile", tileType);
                    if (reason != null && !reason.isEmpty()) {
                        message += " (" + reason + ")";
                    }
                } else {
                    // Notification for other players
                    message = String.format("A %s tile was offered to %s", tileType, offeredToPlayerNickname);
                }
                
                context.getNotificationService().showInfo(
                        "Component Offered",
                        message
                );
            }

            // Show offer UI for the target player
            // if (context.isLocalPlayer(offeredToPlayerId) && context.getGameUI() != null) {
            //     // Note: This would need to be implemented based on the actual UI interface
            //     // context.getGameUI().showComponentOffer(tileId, tileType, reason, offerExpiresAt);
            // }
        });
    }
}