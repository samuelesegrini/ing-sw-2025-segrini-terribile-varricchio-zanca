package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.common.message.event.*;
import it.polimi.ingsw.common.message.event.flight.*;
import it.polimi.ingsw.common.message.response.*;

public interface newUI {
    void start();





    void onErrorResponse(ErrorResponse response);

    void onReconnectResponse(ReconnectResponse response);

    void onPhaseChangedEvent(PhaseChangedEvent event);

    // LOGIN

    public void onLoginResponse(LoginResponse response);

    // LOBBY

    void onCreateGameResponse(CreateGameResponse response);

    void onJoinGameResponse(JoinGameResponse response);

    void onListGamesResponse(ListGamesResponse response);

    void onGamesListUpdateEvent(GamesListUpdateEvent event);

    void onGameCreatedEvent(GameCreatedEvent event);

    // GAME LOBBY

    void onStartGameResponse(GenericSuccessResponse response);

    void onLeaveGameResponse(LeaveGameResponse response);

    void onSetPlayerReadyResponse(SetPlayerReadyResponse response);

    void onGameLobbyUpdateEvent(GameLobbyUpdateEvent event);

    void onPlayerJoinedGameEvent(PlayerJoinedGameEvent event);

    void onPlayerLeftGameEvent(PlayerLeftGameEvent event);

    void onPlayerReadyChangedEvent(PlayerReadyChangedEvent event);

    void onGameStartedEvent(GameStartedEvent event);

    // BUILDING

    void onTakeTileResponse(GenericSuccessResponse response);

    void onReserveTileResponse(GenericSuccessResponse response);

    void onPlaceTileResponse(GenericSuccessResponse response);

    void onReturnTileResponse(ReturnTileResponse response);

    void onFlipBuildingTimerResponse(GenericSuccessResponse response);

    void onRequestFaceUpTileResponse(RequestFaceUpTileResponse response);

    void onValidateShipResponse(ValidateShipResponse response);

    void onViewForecastPileResponse(ViewForecastPileResponse response);

    void onFinishShipResponse(FinishShipResponse response);

    void onComponentPlacedEvent(ComponentPlacedEvent event);

    void onComponentTakenEvent(ComponentTakenEvent event);

    void onComponentReservedEvent(ComponentReservedEvent event);

    void onComponentOfferedEvent(ComponentOfferedEvent event);
    
    void onBuildingTimerFlippedEvent(BuildingTimerFlippedEvent event);

    // FLIGHT RESPONSES

    void onCombatStrengthResponse(CombatStrengthResponse response);

    void onDeclareStrengthResponse(DeclareStrengthResponse response);

    void onDockResponse(DockResponse response);
    
    // FLIGHT EVENTS
    
    void onFlightPhaseStartedEvent(FlightPhaseStartedEvent event);
    
    void onFlightPositionUpdateEvent(FlightPositionUpdateEvent event);
    
    void onAdventureCardDrawnEvent(AdventureCardDrawnEvent event);
    
    void onAdventureCardPlayerTurnEvent(AdventureCardPlayerTurnEvent event);
    
    void onAdventureCardCompletedEvent(AdventureCardCompletedEvent event);
    
    void onAdventureCardTimeoutEvent(AdventureCardTimeoutEvent event);
    
    void onAdventureCardResultEvent(AdventureCardResultEvent event);
    
    void onCombatStartedEvent(CombatStartedEvent event);
    
    void onCombatResolvedEvent(CombatResolvedEvent event);
    
    void onDiceRollEvent(DiceRollEvent event);
    
    void onResourceUpdateEvent(ResourceUpdateEvent event);
    
    void onShipDamagedEvent(ShipDamagedEvent event);
    
    void onGameEndedEvent(GameEndedEvent event);
}
