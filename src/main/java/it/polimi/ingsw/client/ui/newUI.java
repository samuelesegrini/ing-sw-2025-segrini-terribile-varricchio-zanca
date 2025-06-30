package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.common.message.event.*;
import it.polimi.ingsw.common.message.response.*;

public interface newUI {
    void start();





    void onErrorResponse(ErrorResponse response);

    void onReconnectResponse(ReconnectResponse response);

    void onPhaseChangedEvent(PhaseChangedEvent event);

    // Login

    public void onLoginResponse(LoginResponse response);

    // Lobby

    void onCreateGameResponse(CreateGameResponse response);

    void onJoinGameResponse(JoinGameResponse response);

    void onListGamesResponse(ListGamesResponse response);

    void onGamesListUpdateEvent(GamesListUpdateEvent event);

    void onGameCreatedEvent(GameCreatedEvent event);

    // Game Lobby

    void onStartGameResponse(GenericSuccessResponse response);

    void onLeaveGameResponse(LeaveGameResponse response);

    void onSetPlayerReadyResponse(SetPlayerReadyResponse response);

    void onGameLobbyUpdateEvent(GameLobbyUpdateEvent event);

    void onPlayerJoinedGameEvent(PlayerJoinedGameEvent event);

    void onPlayerLeftGameEvent(PlayerLeftGameEvent event);

    void onPlayerReadyChangedEvent(PlayerReadyChangedEvent event);

    void onGameStartedEvent(GameStartedEvent event);

    // Building

    void onTakeTileResponse(GenericSuccessResponse response);

    void onReserveTileResponse(GenericSuccessResponse response);

    void onPlaceTileResponse(GenericSuccessResponse response);

    void onReturnTileResponse(ReturnTileResponse response);

    void onFlipBuildingTimerResponse(FlipBuildingTimerResponse response);

    void onRequestFaceUpTileResponse(RequestFaceUpTileResponse response);

    void onValidateShipResponse(ValidateShipResponse response);

    // Flight

    void onCombatStrengthResponse(CombatStrengthResponse response);

    void onDeclareStrengthResponse(DeclareStrengthResponse response);

    void onDockResponse(DockResponse response);

    // Building events (TODO: These were already being called but not declared)

    void onComponentPlacedEvent(ComponentPlacedEvent event);

    void onComponentTakenEvent(ComponentTakenEvent event);

    void onComponentReservedEvent(ComponentReservedEvent event);

}
