package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.server.model.domain.general.GameModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Event broadcast to all clients when the available games list changes.
 * This includes when games are created, players join/leave, or games are deleted.
 */
public class GamesListUpdateEvent extends AbstractEvent {
    private final List<GameModel> availableGames;

    public GamesListUpdateEvent(List<GameModel> availableGames) {
        super(EventType.GAMES_LIST_UPDATE, null, null); // Global event, no specific game or player
        this.availableGames = new ArrayList<>(availableGames);
    }

    public List<GameModel> getAvailableGames() {
        return new ArrayList<>(availableGames);
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Update the available games list in the client model
        context.getController().getModel().setAvailableGames(availableGames);
    }

    @Override
    public boolean shouldSendTo(String clientId, EventFilterContext context) {
        // Global event - send to all clients regardless of which game they're in
        return true;
    }

    @Override
    public String toString() {
        return String.format("GamesListUpdateEvent{availableGames=%d games}", 
                           availableGames != null ? availableGames.size() : 0);
    }
}