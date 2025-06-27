package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.common.message.response.*;

public interface newUI {
    void start();





    void onErrorResponse(ErrorResponse response);

    void onReconnectResponse(ReconnectResponse response);

    // Login

    void onLoginResponse(LoginResponse response);

    // Lobby

    void onCreateGameResponse(CreateGameResponse response);

    void onJoinGameResponse(JoinGameResponse response);

    void onListGamesResponse(ListGamesResponse response);

    // Game Lobby

    void onStartGameResponse(StartGameResponse response);

    void onLeaveGameResponse(LeaveGameResponse response);

    void onSetPlayerReadyResponse(SetPlayerReadyResponse response);

    // Building

    void onTakeTileResponse(TakeTileResponse response);

    void onReserveTileResponse(ReserveTileResponse response);

    void onPlaceTileResponse(PlaceTileResponse response);

    void onReturnTileResponse(ReturnTileResponse response);

    void onFlipBuildingTimerResponse(FlipBuildingTimerResponse response);

    void onRequestFaceUpTileResponse(RequestFaceUpTileResponse response);

    void onValidateShipResponse(ValidateShipResponse response);

    // Flight

    void onCombatStrengthResponse(CombatStrengthResponse response);

    // TODO onCombatStrengthResponse

    void onDeclareStrengthResponse(DeclareStrengthResponse response);

    void onDockResponse(DockResponse response);


}
