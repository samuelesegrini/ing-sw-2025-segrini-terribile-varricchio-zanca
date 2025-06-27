package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.tui.Printer;
import it.polimi.ingsw.common.message.response.*;
import it.polimi.ingsw.common.model.GameInfo;

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
    public void onErrorResponse(ErrorResponse r) {};

    @Override
    public void onReconnectResponse(ReconnectResponse r) {};

    // Login

    @Override
    public void onLoginResponse(LoginResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Login successful!");
            System.out.println("Welcome " + r.getNickname() + "!");
        } else {
            printer.printError("Login failed.");
        }
    }


    // Lobby

    @Override
    public void onCreateGameResponse(CreateGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game created successfully!");
        } else {
            printer.printError("Failed to create game.");
        }

    }

    @Override
    public void onJoinGameResponse(JoinGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game joined successfully!");
        } else {
            printer.printError("Failed to join game.");
        }
    }

    @Override
    public void onListGamesResponse(ListGamesResponse r) {
        // TODO: Serve?
    }

    // Game Lobby

    @Override
    public void onStartGameResponse(StartGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game started successfully!");
        } else {
            printer.printError("Failed to start game.");
        }
    }

    @Override
    public void onLeaveGameResponse(LeaveGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game left successfully!");
        } else {
            printer.printError("Failed to leave game.");
        }
    }

    @Override
    public void onSetPlayerReadyResponse(SetPlayerReadyResponse r) {
        if (r.isSuccess()) {
            if (r.isReady()) {
                printer.printSuccess("You are ready!");
            } else {
                printer.printSuccess("You are not ready.");
            }
        } else {
            printer.printError("Failed to ready/unready.");
        }
    }

    // Building

    @Override
    public void onTakeTileResponse(TakeTileResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile taken successfully!");
        } else {
            printer.printError("Failed to take tile.");
        }
    }

    @Override
    public void onReserveTileResponse(ReserveTileResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile reserved successfully!");
        } else {
            printer.printError("Failed to reserve tile.");
        }
    }

    @Override
    public void onPlaceTileResponse(PlaceTileResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile placed successfully!");
        } else {
            printer.printError("Failed to place tile.");
        }
    }

    @Override
    public void onReturnTileResponse(ReturnTileResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile returned successfully!");
        } else {
            printer.printError("Failed to return tile.");
        }
    }

    @Override
    public void onFlipBuildingTimerResponse(FlipBuildingTimerResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Building timer flipped successfully!");
        } else {
            printer.printError("Failed to flip building timer.");
        }
    }

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
