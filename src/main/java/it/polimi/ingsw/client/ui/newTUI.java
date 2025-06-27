package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.tui.Printer;
import it.polimi.ingsw.common.message.response.*;

import java.util.Scanner;

public class newTUI implements newUI {
    private final Scanner scanner;
    private final Printer printer;

    // Temporanei
    private final ClientController controller;
    private final ClientState clientState;

    public newTUI(ClientController controller) {
        this.printer = new Printer();
        this.scanner = new Scanner(System.in);

        this.controller = controller;
        this.clientState = controller.getClientState();
    }

    @Override
    public void start() {
        printer.printHeader();

        inputListener();
    }

    public void inputListener(){
        String input;
        while (true) {
            input = scanner.nextLine();
            elaborateInput(input);
        }
    }

    private void elaborateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }


    }




    @Override
    public void onErrorResponse(ErrorResponse response) {};

    @Override
    public void onReconnectResponse(ReconnectResponse response) {};

    // Login

    @Override
    public void onLoginResponse(LoginResponse response) {
        System.out.println("Login successful!");
        System.out.println("Welcome " + response.getNickname() + "!");
    }

    // Lobby

    @Override
    public void onCreateGameResponse(CreateGameResponse response) {
        System.out.println("Game created successfully!");
    }

    @Override
    public void onJoinGameResponse(JoinGameResponse response) {}

    @Override
    public void onListGamesResponse(ListGamesResponse response) {}

    // Game Lobby

    @Override
    public void onStartGameResponse(StartGameResponse response) {}

    @Override
    public void onLeaveGameResponse(LeaveGameResponse response) {}

    @Override
    public void onSetPlayerReadyResponse(SetPlayerReadyResponse response) {}

    // Building

    @Override
    public void onTakeTileResponse(TakeTileResponse response) {}

    @Override
    public void onReserveTileResponse(ReserveTileResponse response) {}

    @Override
    public void onPlaceTileResponse(PlaceTileResponse response) {}

    @Override
    public void onReturnTileResponse(ReturnTileResponse response) {}

    @Override
    public void onFlipBuildingTimerResponse(FlipBuildingTimerResponse response) {}

    @Override
    public void onRequestFaceUpTileResponse(RequestFaceUpTileResponse response) {}

    @Override
    public void onValidateShipResponse(ValidateShipResponse response) {}

    // Flight

    @Override
    public void onCombatStrengthResponse(CombatStrengthResponse response) {}

    // TODO onCombatStrengthResponse

    @Override
    public void onDeclareStrengthResponse(DeclareStrengthResponse response) {}

    @Override
    public void onDockResponse(DockResponse response) {}
}
