package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.tui.Printer;
import it.polimi.ingsw.common.message.event.*;
import it.polimi.ingsw.common.message.response.*;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class newTUI implements newUI {
    private static Terminal terminal;
    private static LineReader reader;
    private final Printer printer;

    private final ClientController controller;
    private final ClientState clientState;
    private final NetworkClient networkClient;

    public newTUI(ClientController controller) throws IOException {
        terminal = TerminalBuilder.builder().system(true).build();
        reader = LineReaderBuilder.builder().terminal(terminal).build();
        this.printer = new Printer(terminal, reader);

        this.controller = controller;
        controller.setUI(this);

        this.clientState = controller.getClientState();
        this.networkClient = controller.getNetworkClient();
    }

    @Override
    public void start() {
        printer.printHeader();

        printer.displayConnection();
        if (!elaborateConnection()) {
            printer.printError("Connection failed. Exiting.");
            shutdown();
            return;
        }

        System.out.println("Connected to server.");

        printer.displayLogin();
        elaborateLogin();

        String input;
        while (true) {
            try {
                input = reader.readLine();
                elaborateInput(input);
            } catch (UserInterruptException e) {
                printer.printWarning("Operation cancelled. Exiting client.");
                break;
            } catch (EndOfFileException e) {
                printer.printWarning("Exiting client (Ctrl+D detected).");
                break;
            } catch (Exception e) {
                printer.printError("An unexpected error occurred: " + e.getMessage());
            }
        }
        shutdown();
    }

    private void elaborateInput(String input) {
        if (input.isEmpty()) {
            return;
        }

        String[] tokens = input.split("\\s+");

        switch (clientState.getCurrentView()) {
            case LOBBY -> elaborateLobbyCommand(tokens);
            case GAME_LOBBY -> elaborateGameLobbyCommand(tokens);
            case BUILDING -> elaborateBuildingCommand(tokens);
            case FLIGHT -> elaborateFlightCommand(tokens);
            //case END -> displayEndGame();
            default -> printer.printError("Invalid command. Type 'help' for available commands.");
        }
    }

    private boolean elaborateConnection() {
        // Hostname
        String hostname;
        try {
            hostname = reader.readLine("Enter hostname (default: localhost): ").trim();
            if (hostname.isEmpty()) {
                hostname = "localhost";
            }
        } catch (UserInterruptException | EndOfFileException e) {
            return false;
        }

        // Protocol
        boolean useSocket = true;
        try {
            String protocol = reader.readLine("Select protocol: (1) Socket, (2) RMI (default: 1): ").trim().toLowerCase();
            if (protocol.equals("2") || protocol.equals("rmi")) {
                useSocket = false;
                printer.printInfo("Selected protocol: RMI");
            } else {
                printer.printInfo("Selected protocol: Socket");
            }
        } catch (UserInterruptException | EndOfFileException e) {
            return false;
        }

        // Port
        int port = useSocket ? 12345 : 1099;
        try {
            String portInput = reader.readLine("Enter port (default: " + port + "): ").trim();
            if (!portInput.isEmpty()) {
                try {
                    port = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    printer.printError("Invalid port number. Using default port " + port + ".");
                }
            }
        } catch (NumberFormatException e) {
            printer.printError("Invalid port number. Using default port " + port + ".");
        }

        String protocolName = useSocket ? "Socket" : "RMI";
        printer.printLoading("Connecting to " + hostname + ":" + port + " via " + protocolName);

        try {
            return controller.connect(hostname, port, useSocket).get();
        } catch (ExecutionException | InterruptedException e) {
            printer.printError("Connection error: " + e.getMessage());
            if (e instanceof InterruptedException)
                Thread.currentThread().interrupt();
            return false;
        }
    }

    private void elaborateLogin() {
        String nickname = reader.readLine("Enter nickname: ").trim();

        Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
        while (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            printer.printError("Invalid nickname format. Please try again.");
            nickname = reader.readLine("Enter nickname: ").trim();
        }

        printer.printLoading("Logging in as " + nickname);

        controller.login(nickname);

        printer.printLoading("Done");
    }

    private void elaborateLobbyCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        switch (command) {
            case "c":
            case "create":
                elaborateCreateCommand(tokens);
                break;
            case "j":
            case "join":
                elaborateJoinCommand(tokens);
                break;
            case "refresh":
                controller.refreshGameList();
                break;
            case "h":
            case "help":
                //displayCommands();
                break;
            default:
                printer.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void elaborateCreateCommand(String[] tokens) {
        if (tokens.length != 4) {
            printer.printError("'create' command requires 3 arguments.");
            return;
        }

        String gameName = tokens[1];
        if (gameName.length() < 3 || gameName.length() > 15) {
            printer.printError("gameName length must be between 3 and 15 characters.");
            return;
        }

        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(tokens[2]);

            if (maxPlayers < 2 || maxPlayers > 4) {
                printer.printError("maxPlayers must be between 2 and 4.");
                return;
            }
        } catch (NumberFormatException e) {
            printer.printError("Invalid maxPlayers number.");
            return;
        }

        String gameLevel = tokens[3];
        if (!gameLevel.equalsIgnoreCase("test_flight") && !gameLevel.equalsIgnoreCase("level_II")) {
            printer.printError("gameLevel must be either 'test_flight' or 'level_II'.");
            return;
        }

        controller.createGame(gameName, maxPlayers, gameLevel);
    }

    private void elaborateJoinCommand(String[] tokens) {
        if (tokens.length != 2) {
            printer.printError("'join' command requires 1 argument.");
            return;
        }

        String gameId = tokens[1];

        controller.joinGame(gameId);
    }

    private void elaborateGameLobbyCommand(String[] tokens) {
        String command = tokens[0].trim().toLowerCase();
        switch (command) {
            case "r":
            case "ready":
                elaborateReadyCommand();
                break;
            case "u":
            case "unready":
                elaborateUnreadyCommand();
                break;
            case "start":
                elaborateStartCommand();
                break;
            case "l":
            case "leave":
            case "quit":
                elaborateLeaveCommand();
                break;
            case "refresh":
                // TODO: NON C'È METODO NEL CONTROLLER PER AGGIORNARE DATI GAME LOBBY?
                break;
            case "h":
            case "help":
                printer.printGameLobbyCommands(clientState);
                break;
            default:
                printer.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void elaborateReadyCommand() {
        String playerId = controller.getPlayerId();
        if (!controller.getClientState().isPlayerReady(playerId)) {
            controller.setPlayerReady(true);
            printer.printSuccess("Marked as ready!");
        } else {
            printer.printWarning("You are already ready!");
        }
    }

    private void elaborateUnreadyCommand() {
        String playerId = controller.getPlayerId();
        if (controller.getClientState().isPlayerReady(playerId)) {
            controller.setPlayerReady(false);
            printer.printSuccess("Marked as not ready!");
        } else {
            printer.printWarning("You are already not ready!");
        }
    }

    private void elaborateStartCommand() {
        String playerId = controller.getPlayerId();
        if (!playerId.equals(clientState.getHostPlayerId())) {
            printer.printError("Only the host can start the game!");
            return;
        }

        if (!clientState.areAllPlayersReady()) {
            printer.printError("Not all players are ready!");
            return;
        }

        controller.startGame();
        printer.printSuccess("Starting game...");
    }

    private void elaborateLeaveCommand() {
        printer.printWarning("Are you sure you want to leave the lobby? (y/n)");
        String confirmation = reader.readLine().trim().toLowerCase();
        if (confirmation.startsWith("y")) {
            controller.leaveGame();
            printer.printSuccess("Leaving lobby...");
        } else {
            printer.printInfo("Cancelled leaving lobby.");
        }
    }

    // Building

    private void elaborateBuildingCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();
        switch (command) {
            case "h":
            case "help":
                //showHelp();
                break;
            case "refresh":
                printer.displayBuilding(clientState);
                break;
            case "p":
            case "place":
                elaboratePlaceCommand(tokens);
                break;
            case "t":
            case "take":
                elaborateTakeCommand(tokens);
                break;
            case "r":
            case "return":
                elaborateReturnCommand();
                break;
            case "rot":
            case "rotate":
                elaborateRotateCommand();
                break;
            case "v":
            case "validate":
                elaborateValidateCommand();
                break;
            case "f":
            case "flip":
                elaborateFlipTimerCommand();
                break;
            case "q":
            case "quit":
                elaborateQuitCommand();
                break;
            default:
                printer.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void elaboratePlaceCommand(String[] tokens) {
        if (tokens.length != 3) {
            printer.printError("Usage: place <row> <col>");
            return;
        }

        try {
            int row = Integer.parseInt(tokens[1]) - 5;
            int col = Integer.parseInt(tokens[2]) - 4;

            if (row < 0 || row >= 5 || col < 0 || col >= 7) {
                printer.printError("Invalid position. Row must be 5-9, column must be 4-10.");
                return;
            }

            // TODO: Check place component (SERVER)

            Component heldComponent = clientState.getLocalPlayer().getHeldComponent();

            printer.printLoading("Placing component at (" + (row + 5) + "," + (col + 4) + ")");
            controller.placeTile(heldComponent.getId(), row, col, 0);

        } catch (NumberFormatException e) {
            printer.printError("Invalid number format. Please use integers for row, col, and component number.");
        }
    }

    private void elaborateTakeCommand(String[] tokens) {
        // TODO: Check if player already has a tile in hand
//        if (!clientState.canReserveMoreTiles()) {
//            printer.printError("You already have the maximum number of held components (2).");
//            return;
//        }

        if (tokens.length == 2) {
            // TODO
            //controller.requestFaceUpTile()
        } else {
            printer.printLoading("Taking a random component from the pile");
            controller.takeTile();
        }
    }

    private void elaborateReturnCommand() {
        try {
            Component component = clientState.getLocalPlayer().getHeldComponent();

            printer.printLoading("Returning component to face-up pile");
            controller.returnTile(component.getId());
        } catch (NumberFormatException e) {
            printer.printError("Invalid number format. Please use an integer for component number.");
        }
    }

    private void elaborateRotateCommand() {
        Component component = clientState.getLocalPlayer().getHeldComponent();

        printer.printLoading("Rotating component");
        // TODO: Chiama funzione rotate controller (MANCA)
    }

    private void elaborateValidateCommand() {
        printer.printLoading("Validating your ship");
        controller.validateShip();
    }

    private void elaborateFlipTimerCommand() {
        printer.printLoading("Flipping the building timer");
        controller.flipBuildingTimer();
    }

    private void elaborateQuitCommand() {
        printer.printLoading("Disconnecting");
        controller.disconnect();
        //shutdown();
    }

    // Flight

    private void elaborateFlightCommand(String[] tokens) {
        String command = tokens[0].toLowerCase();

        // TODO: Dipende dalla carta

        switch (command) {
            case "h":
            case "help":
                printer.printFlightCommands();
                break;
            case "q":
            case "quit":
                elaborateQuitCommand();
                break;
            case "giveup":
                //elaborateGiveUpCommand();  // TODO
                break;
            case "refresh":
                printer.displayFlight(clientState);
                break;
            default:
                printer.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    // ON RESPONSE METHODS

    @Override
    public void onErrorResponse(ErrorResponse r) {
        // TODO: Implement error response handling
        // - Display error message with appropriate formatting
        // - Handle different error types (connection, game, validation)
        printer.printError("Error: " + (r.getErrorMessage() != null ? r.getErrorMessage() : "Unknown error"));
    }

    @Override
    public void onReconnectResponse(ReconnectResponse r) {
        // TODO: Implement reconnect response handling
        // - Show reconnection status
        // - Restore game state if successful
        if (r.isSuccess()) {
            printer.printSuccess("Reconnected successfully!");
        } else {
            printer.printError("Reconnection failed.");
        }
    }

    @Override
    public void onPhaseChangedEvent(PhaseChangedEvent e) {
        //TODO
    }

    // Login

    @Override
    public void onLoginResponse(LoginResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Login successful!");
            printer.print("Welcome " + r.getNickname() + "!");

            clientState.setCurrentView(ClientState.ViewState.LOBBY);
            printer.displayLobby(clientState);
        } else {
            printer.printError("Login failed.");
        }
    }

    // Lobby

    @Override
    public void onCreateGameResponse(CreateGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game created successfully!");

            clientState.setCurrentView(ClientState.ViewState.GAME_LOBBY);  // TODO: FAI FARE A UPDATECLIENTSTATE NEI MESSAGGI
            printer.printInfo("You are now in the game lobby. Use 'ready' to mark yourself ready, 'leave' to exit.");
            // TODO: Implement proper game lobby display
            // - Show game name, level, max players
            // - Display current players list with ready status
            // - Show available commands (ready, unready, start, leave, help)
            // - Real-time updates when players join/leave/ready
            printer.displayGameLobby(clientState);
        } else {
            printer.printError("Failed to create game.");
        }
    }

    @Override
    public void onJoinGameResponse(JoinGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game joined successfully!");

            clientState.setCurrentView(ClientState.ViewState.GAME_LOBBY);  // TODO: FAI FARE A UPDATECLIENTSTATE NEI MESSAGGI
            printer.printInfo("You are now in the game lobby. Use 'ready' to mark yourself ready, 'leave' to exit.");

            printer.displayGameLobby(clientState);
        } else {
            printer.printError("Failed to join game.");
        }
    }

    @Override
    public void onListGamesResponse(ListGamesResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Games list fetched successfully!");
            printer.displayLobby(clientState);
        } else {
            printer.printError("Failed to fetch games list.");
        }
    }

    @Override
    public void onGamesListUpdateEvent(GamesListUpdateEvent event) {
        if (clientState.getCurrentView() == ClientState.ViewState.LOBBY) {
            printer.displayLobby(clientState);
        }
    }

    @Override
    public void onGameCreatedEvent(GameCreatedEvent event) {
        // Display appropriate view based on current state
        if (clientState.getCurrentView() == ClientState.ViewState.LOBBY) {
            printer.displayLobby(clientState);
        } else if (clientState.getCurrentView() == ClientState.ViewState.GAME_LOBBY) {
            printer.displayGameLobby(clientState);
        }
    }

    // Game Lobby

    @Override
    public void onStartGameResponse(GenericSuccessResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game started successfully!");

            clientState.setCurrentView(ClientState.ViewState.BUILDING);
            printer.displayBuilding(clientState);
        } else {
            printer.printError("Failed to start game.");
        }
    }

    @Override
    public void onLeaveGameResponse(LeaveGameResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Game left successfully!");

            clientState.setCurrentView(ClientState.ViewState.LOBBY);
            printer.displayLobby(clientState);
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
            printer.displayGameLobby(clientState);
        } else {
            printer.printError("Failed to ready/unready.");
        }
    }

    @Override
    public void onGameLobbyUpdateEvent(GameLobbyUpdateEvent event) {
        if (clientState.getCurrentView() == ClientState.ViewState.GAME_LOBBY) {
            printer.displayGameLobby(clientState);
        } else if (clientState.getCurrentView() == ClientState.ViewState.LOBBY) {
            printer.displayLobby(clientState);
        }
    }

    @Override
    public void onPlayerJoinedGameEvent(PlayerJoinedGameEvent event) {
        printer.displayGameLobby(clientState);
    }

    @Override
    public void onPlayerLeftGameEvent(PlayerLeftGameEvent event) {
        printer.displayGameLobby(clientState);
    }

    @Override
    public void onPlayerReadyChangedEvent(PlayerReadyChangedEvent event) {
        printer.displayGameLobby(clientState);
    }

    @Override
    public void onGameStartedEvent(GameStartedEvent event) {
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    // Building

    @Override
    public void onTakeTileResponse(GenericSuccessResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile taken successfully!");
        } else {
            printer.printError("Failed to take tile.");
        }
    }

    @Override
    public void onReserveTileResponse(GenericSuccessResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile reserved successfully!");
            printer.displayBuilding(clientState); // TODO
        } else {
            printer.printError("Failed to reserve tile.");
        }
    }

    @Override
    public void onPlaceTileResponse(GenericSuccessResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile placed successfully!");
            printer.displayBuilding(clientState); // TODO
        } else {
            printer.printError("Failed to place tile.");
        }
    }

    @Override
    public void onReturnTileResponse(ReturnTileResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Tile returned successfully!");
            printer.displayBuilding(clientState); // TODO
        } else {
            printer.printError("Failed to return tile.");
        }
    }

    @Override
    public void onFlipBuildingTimerResponse(FlipBuildingTimerResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Building timer flipped successfully!");
            printer.displayBuilding(clientState); // TODO
        } else {
            printer.printError("Failed to flip building timer.");
        }
    }

    @Override
    public void onRequestFaceUpTileResponse(RequestFaceUpTileResponse response) {
        // TODO: Implement face-up tile request response
        // - Show offered tile information
        // - Display accept/decline options
        if (response.isSuccess()) {
            printer.printSuccess("Face-up tile offered!");
        } else {
            printer.printError("No face-up tile available.");
        }
    }

    @Override
    public void onValidateShipResponse(ValidateShipResponse response) {
        // TODO: Implement ship validation response
        // - Show validation results (valid/invalid)
        // - Display any validation errors or warnings
        // - Update ship status display
        if (response.isSuccess()) {
            printer.printSuccess("Ship is valid and ready for flight!");
        } else {
            printer.printError("Ship validation failed: " + response.getErrorMessage());
        }
    }

    @Override
    public void onComponentOfferedEvent(ComponentOfferedEvent event) {
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    // Flight

    @Override
    public void onCombatStrengthResponse(CombatStrengthResponse response) {
        // TODO: Implement combat strength response
        // - Show combat results (success/failure)
        // - Display damage taken or goods lost
        // - Update ship status after combat
        if (response.isSuccess()) {
            printer.printSuccess("Combat resolved successfully!");
        } else {
            printer.printError("Combat failed!");
        }
    }

    @Override
    public void onDeclareStrengthResponse(DeclareStrengthResponse response) {
        // TODO: Implement declare strength response
        // - Show strength declaration confirmation
        // - Display current strength values
        if (response.isSuccess()) {
            printer.printSuccess("Strength declared successfully!");
        } else {
            printer.printError("Failed to declare strength.");
        }
    }

    @Override
    public void onDockResponse(DockResponse response) {
        // TODO: Implement dock response
        // - Show docking results
        // - Display goods delivered and credits earned
        // - Update player status
        if (response.isSuccess()) {
            printer.printSuccess("Successfully docked!");
        } else {
            printer.printError("Docking failed.");
        }
    }

    // Component events (TODO: These were called but not implemented)
    
    @Override
    public void onComponentPlacedEvent(ComponentPlacedEvent event) {
        // TODO: Implement component placed event handling
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    @Override
    public void onComponentTakenEvent(ComponentTakenEvent event) {
        // TODO: Implement component taken event handling
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    @Override
    public void onComponentReservedEvent(ComponentReservedEvent event) {
        // TODO: Implement component reserved event handling
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    public void shutdown() {
        printer.shutdown();
        try {
            if (reader != null)
                reader.getTerminal().close();
        } catch (IOException e) {
            System.err.println("Error closing reader terminal: " + e.getMessage());
        }
        System.exit(0);
    }
}
