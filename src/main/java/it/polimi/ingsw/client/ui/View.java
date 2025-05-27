package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.common.message.building.*;
import it.polimi.ingsw.common.message.setup.*;
import it.polimi.ingsw.common.message.system.*;

public interface View {
    // System Messages


    void onErrorMessage(ErrorMessage message);


    void onServerLoginResponse(ServerLoginResponse message);

    // Setup Messages


    void onCreateGameResponseEvent(CreateGameResponseEvent message);


    void onGameListResponseEvent(GameListResponseEvent message);


    void onGameSessionStateChangedEvent(GameSessionStateChangedEvent message);


    void onJoinGameResponseEvent(JoinGameResponseEvent message);


    void onPlayerJoinedGameSessionNotification(PlayerJoinedGameSessionNotification message);


    void onPlayerLeftGameSessionNotification(PlayerLeftGameSessionNotification message);


    void onLobbyStateUpdateEvent(LobbyStateUpdateEvent message);


    void onServerGameListUpdateNotification(ServerGameListUpdateNotification message);


    // Building Messages


    void onAvailableComponentsUpdateEvent(AvailableComponentsUpdateEvent event);


    void onBuildingTimerUpdatedEvent(BuildingTimerUpdatedEvent event);


    void onComponentOfferedToPlayerEvent(ComponentOfferedToPlayerEvent event);


    void onComponentPlacedEvent(ComponentPlacedEvent message);


    void onComponentReservedEvent(ComponentReservedEvent message);


    void onComponentReturnedToPileEvent(ComponentReturnedToPileEvent message);


    void onPlayerFinishedBuildingEvent(PlayerFinishedBuildingEvent message);


    void onPlayerShipUpdateEvent(PlayerShipUpdateEvent message);


    void onShipBuildingPhaseEndedEvent(ShipBuildingPhaseEndedEvent message);


    void onShipCorrectedEvent(ShipCorrectedEvent message);


    void onShipValidationResultEvent(ShipValidationResultEvent message);
}
