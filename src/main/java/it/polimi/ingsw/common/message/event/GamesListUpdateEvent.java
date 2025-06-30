package it.polimi.ingsw.common.message.event;

import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.general.GameModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Event broadcast to all clients when the available games list changes.
 * This includes when games are created, players join/leave, or games are deleted.
 */
public class GamesListUpdateEvent extends AbstractEvent {
    private static final Logger LOGGER = Logger.getLogger(GamesListUpdateEvent.class.getName());
    private final List<GameInfo> availableGames;

    public GamesListUpdateEvent(List<GameModel> gameModels) {
        super(EventType.GAMES_LIST_UPDATE, null, null); // Global event, no specific game or player
        this.availableGames = gameModels.stream()
                .map(GameInfo::fromGameModel)
                .collect(Collectors.toList());
        LOGGER.fine("GamesListUpdateEvent instantiated with " + availableGames.size() + " games.");
    }

    public List<GameInfo> getAvailableGames() {
        return new ArrayList<>(availableGames);
    }

    @Override
    public void updateClientState(it.polimi.ingsw.client.core.ClientState clientState) {
        // Event updates client state with complete games list
        clientState.updateGamesList(availableGames);
        clientState.incrementStateVersion();
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // First update client state
        updateClientState(context.getClientState());
        
        // Then handle UI updates
        LOGGER.fine("CLIENT UPDATE - Receiving GamesListUpdateEvent with " + availableGames.size() + " games:");
        for (GameInfo game : availableGames) {
            LOGGER.finer("  - Game ID: " + game.getGameId() + ", Name: " + game.getGameName());
        }
        
        // Direct UI notification via newUI
        context.getNewUI().onGamesListUpdateEvent(this);
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