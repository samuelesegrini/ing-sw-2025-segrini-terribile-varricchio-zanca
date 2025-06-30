package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.network.NetworkClient;
import it.polimi.ingsw.client.ui.tui.Printer;
import it.polimi.ingsw.common.message.event.*;
import it.polimi.ingsw.common.message.event.flight.*;
import it.polimi.ingsw.common.message.response.*;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;

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
        // Check if player already has a tile in hand
        Component heldComponent = clientState.getLocalPlayer().getHeldComponent();
        if (heldComponent != null) {
            printer.printError("You already have a component in hand: " + heldComponent.getType().name().toLowerCase().replace("_", " "));
            printer.printInfo("Use 'place <row> <col>' to place it or 'return' to return it first.");
            return;
        }

        if (tokens.length == 2) {
            try {
                int numComponent = Integer.parseInt(tokens[1]);
                
                // Validate index bounds
                List<Component> faceUpComponents = clientState.getGameModel().getComponentDeck().getFaceUpComponents();
                if (numComponent < 1 || numComponent > faceUpComponents.size()) {
                    printer.printError("Invalid component number. Available face-up components: 1-" + faceUpComponents.size());
                    return;
                }

                printer.printLoading("Taking face-up tile " + numComponent);

                String tileId = faceUpComponents.get(numComponent - 1).getId();
                controller.requestFaceUpTile(tileId);
            } catch (NumberFormatException e) {
                printer.printError("Invalid number format. Please use an integer for the component's number.");
            } catch (IndexOutOfBoundsException e) {
                printer.printError("Component number out of range. Check available face-up components.");
            }
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
                elaborateGiveUpCommand();
                break;
            case "refresh":
                printer.displayFlight(clientState);
                break;
            case "draw":
                elaborateDrawCardCommand();
                break;
            case "combat":
                elaborateCombatCommand(tokens);
                break;
            case "engine":
                elaborateEngineCommand(tokens);
                break;
            case "planet":
                elaboratePlanetCommand(tokens);
                break;
            case "dock":
                elaborateDockCommand(tokens);
                break;
            case "pass":
                elaboratePassCommand();
                break;
            default:
                printer.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    // ==================== FLIGHT COMMAND HANDLERS ====================

    private void elaborateDrawCardCommand() {
        printer.printLoading("Drawing next adventure card...");
        controller.drawAdventureCard();
    }

    private void elaborateCombatCommand(String[] tokens) {
        if (tokens.length != 2) {
            printer.printError("Usage: combat <batteries_to_use>");
            printer.printInfo("Example: combat 2 (use 2 batteries for double cannons)");
            return;
        }

        try {
            int batteries = Integer.parseInt(tokens[1]);
            if (batteries < 0) {
                printer.printError("Batteries must be non-negative.");
                return;
            }

            printer.printLoading("Declaring combat strength using " + batteries + " batteries...");
            controller.declareCombatStrength(batteries);
        } catch (NumberFormatException e) {
            printer.printError("Invalid number format. Please use integers for batteries.");
        }
    }

    private void elaborateEngineCommand(String[] tokens) {
        if (tokens.length < 1 || tokens.length > 2) {
            printer.printError("Usage: engine [batteries_to_use]");
            printer.printInfo("Example: engine 1 (use 1 battery for extra engine power)");
            return;
        }

        try {
            int batteries = tokens.length == 2 ? Integer.parseInt(tokens[1]) : 0;
            if (batteries < 0) {
                printer.printError("Batteries must be non-negative.");
                return;
            }

            printer.printLoading("Declaring engine strength using " + batteries + " batteries...");
            controller.declareEngineStrength(batteries);
        } catch (NumberFormatException e) {
            printer.printError("Invalid number format. Please use integers for batteries.");
        }
    }

    private void elaboratePlanetCommand(String[] tokens) {
        if (tokens.length != 2) {
            printer.printError("Usage: planet <planet_number>");
            printer.printInfo("Example: planet 1 (choose first planet)");
            printer.printInfo("Use 'pass' to skip planet selection");
            return;
        }

        try {
            int planetIndex = Integer.parseInt(tokens[1]);
            if (planetIndex < 1) {
                printer.printError("Planet number must be positive.");
                return;
            }

            printer.printLoading("Choosing planet " + planetIndex + "...");
            controller.choosePlanet(planetIndex - 1); // Convert to 0-based index
        } catch (NumberFormatException e) {
            printer.printError("Invalid number format. Please use integers for planet number.");
        }
    }

    private void elaborateDockCommand(String[] tokens) {
        boolean dockingDecision = true;
        
        if (tokens.length == 2) {
            String decision = tokens[1].toLowerCase();
            if (decision.equals("no") || decision.equals("false") || decision.equals("decline")) {
                dockingDecision = false;
            }
        }

        if (dockingDecision) {
            printer.printLoading("Attempting to dock at abandoned structure...");
        } else {
            printer.printLoading("Declining docking opportunity...");
        }
        
        controller.dock(dockingDecision);
    }

    private void elaboratePassCommand() {
        printer.printLoading("Passing on current opportunity...");
        // This could be used for planets or other optional choices
        controller.choosePlanet(0); // 0 typically means "pass" in the protocol
    }

    private void elaborateGiveUpCommand() {
        printer.printWarning("Are you sure you want to give up the flight? (y/n)");
        String confirmation = reader.readLine().trim().toLowerCase();
        if (confirmation.startsWith("y")) {
            printer.printWarning("Giving up flight...");
            // TODO: Implement give up functionality in controller
            printer.printInfo("Your ship has been abandoned. You'll receive half-price for goods.");
        } else {
            printer.printInfo("Continuing flight.");
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
        printer.printInfo("Phase changed to: " + e.getNewPhase());
        
        switch (e.getNewPhase()) {
            case BUILDING:
                clientState.setCurrentView(ClientState.ViewState.BUILDING);
                printer.displayBuilding(clientState);
                break;
            case FLIGHT:
                clientState.setCurrentView(ClientState.ViewState.FLIGHT);
                printer.displayFlight(clientState);
                break;
            default:
                printer.printInfo("Entered " + e.getNewPhase() + " phase");
                break;
        }
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
    public void onFlipBuildingTimerResponse(GenericSuccessResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Building timer flipped successfully!");
            // Timer state will be updated by the BuildingTimerFlippedEvent
        } else {
            printer.printError("Failed to flip building timer.");
        }
    }

    @Override
    public void onRequestFaceUpTileResponse(RequestFaceUpTileResponse response) {
        if (response.isSuccess()) {
            printer.printSuccess("Face-up tile taken successfully!");
            // Force refresh the display to show the updated held component
            if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
                printer.displayBuilding(clientState);
            }
        } else {
            printer.printError("Face-up tile request failed.");
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
        String tileType = event.getTileType().toLowerCase().replace("_", " ");
        String offeredToPlayerId = event.getOfferedToPlayerId();
        String reason = event.getReason();
        
        if (offeredToPlayerId.equals(clientState.getPlayerId())) {
            printer.printInfo("⚡ You have been offered a " + tileType + " component!");
            if (reason != null && !reason.isEmpty()) {
                printer.printInfo("Reason: " + reason);
            }
            printer.printInfo("Use 'take " + event.getTileId() + "' to accept the offer");
        } else {
            printer.printInfo("A " + tileType + " component was offered to " + event.getOfferedToPlayerNickname());
        }
        
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
        String playerId = event.getPlayerId();
        String playerNickname = event.getPlayerNickname();
        String componentType = event.getComponent().getType().name().toLowerCase().replace("_", " ");
        
        if (playerId.equals(clientState.getPlayerId())) {
            printer.printSuccess("Component placed successfully!");
        } else {
            printer.printInfo(playerNickname + " placed a " + componentType + " component");
        }
        
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    @Override
    public void onComponentTakenEvent(ComponentTakenEvent event) {
        String playerId = event.getPlayerId().toString();
        String playerNickname = event.getPlayerNickname();
        String componentType = event.getComponent().getType().name().toLowerCase().replace("_", " ");
        
        if (playerId.equals(clientState.getPlayerId())) {
            printer.printSuccess("You took a " + componentType + " component");
            // Force refresh the display immediately to show the held component
            if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
                printer.displayBuilding(clientState);
            }
        } else {
            printer.printInfo(playerNickname + " took a " + componentType + " component");
        }
    }

    @Override
    public void onComponentReservedEvent(ComponentReservedEvent event) {
        String playerId = event.getPlayerId().toString();
        String playerNickname = event.getPlayerNickname();
        
        if (playerId.equals(clientState.getPlayerId())) {
            printer.printSuccess("Component reserved successfully!");
        } else {
            printer.printInfo(playerNickname + " reserved a component");
        }
        
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }

    // ==================== BUILDING TIMER EVENT HANDLERS ====================
    
    @Override
    public void onBuildingTimerFlippedEvent(BuildingTimerFlippedEvent event) {
        // Update client state with timer information
        clientState.setTimerStage(event.getCurrentStage());
        clientState.setTimeRemaining(event.getTimeRemaining());
        clientState.setTotalFlips(event.getTotalFlips());
        
        // Show timer flip notification
        String playerName = event.getPlayerNickname();
        String stageDescription = getStageDescription(event.getCurrentStage());
        
        if (event.getPlayerId().equals(clientState.getPlayerId())) {
            printer.printSuccess("⏰ You flipped the building timer!");
        } else {
            printer.printInfo("⏰ " + playerName + " flipped the building timer");
        }
        
        printer.printInfo("Timer Stage: " + stageDescription);
        
        if (event.getTimeRemaining() > 0) {
            int seconds = (int) (event.getTimeRemaining() / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            printer.printInfo("Time Remaining: " + String.format("%02d:%02d", minutes, seconds));
        } else if (event.getTimeRemaining() == 0) {
            printer.printWarning("⚠️ Timer expired!");
        }
        
        // Refresh building display if in building phase
        if (clientState.getCurrentView() == ClientState.ViewState.BUILDING) {
            printer.displayBuilding(clientState);
        }
    }
    
    private String getStageDescription(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Ready to Begin";
            case FIRST_TIMER -> "First Timer Active";
            case FIRST_EXPIRED -> "First Timer Expired";
            case SECOND_TIMER -> "Second Timer Active";
            case SECOND_EXPIRED -> "Second Timer Expired";
            case BUILDING_ENDED -> "Building Complete";
        };
    }

    // ==================== FLIGHT PHASE EVENT HANDLERS ====================
    
    public void onFlightPhaseStartedEvent(FlightPhaseStartedEvent event) {
        printer.printSuccess("🚀 Flight phase started! Time to explore the galaxy!");
        printer.printInfo("Players: " + event.getPlayerCount() + " | Route length: " + event.getRouteLength());
        clientState.setCurrentView(ClientState.ViewState.FLIGHT);
        printer.displayFlight(clientState);
    }
    
    public void onFlightPositionUpdateEvent(FlightPositionUpdateEvent event) {
        if (clientState.getCurrentView() == ClientState.ViewState.FLIGHT) {
            printer.printInfo("📍 Flight positions updated");
            printer.displayFlight(clientState);
        }
    }
    
    public void onAdventureCardDrawnEvent(AdventureCardDrawnEvent event) {
        var card = event.getCard();
        if (card != null) {
            String cardType = card.getType().toString().toLowerCase().replace("_", " ");
            printer.printInfo("🎴 Adventure card drawn: " + cardType);
            printer.printInfo("Card " + event.getCardNumber() + " of " + event.getTotalCards());
            
            // Display card-specific information
            switch (card.getType().toString()) {
                case "COMBAT" -> printer.printWarning("⚔️ Combat encounter ahead!");
                case "PLANET" -> printer.printInfo("🪐 Planets available for exploration!");
                case "METEOR" -> printer.printWarning("☄️ Meteor swarm incoming!");
                case "OPEN_SPACE" -> printer.printInfo("🌌 Open space - time to fly!");
                case "ABANDONED" -> printer.printInfo("🏗️ Abandoned structure discovered!");
                default -> printer.printInfo("📜 Special event card");
            }
        }
        
        if (clientState.getCurrentView() == ClientState.ViewState.FLIGHT) {
            printer.displayFlight(clientState);
        }
    }
    
    public void onAdventureCardPlayerTurnEvent(AdventureCardPlayerTurnEvent event) {
        long remainingTime = event.getRemainingTime();
        
        printer.printInfo("⏰ Your turn! Make your choice...");
        if (remainingTime > 0) {
            printer.printInfo("Time remaining: " + (remainingTime / 1000) + " seconds");
        }
        printer.printInfo("Available commands depend on the current adventure card. Use 'help' for options.");
        
        var card = event.getCard();
        if (card != null) {
            printer.printInfo("Current card: " + card.getId());
        }
    }
    
    public void onAdventureCardCompletedEvent(AdventureCardCompletedEvent event) {
        printer.printSuccess("✅ Adventure card resolved!");
        
        if (event.getPlayerChoices() != null && !event.getPlayerChoices().isEmpty()) {
            printer.printInfo("Adventure outcome processed for all players.");
        }
        
        if (clientState.getCurrentView() == ClientState.ViewState.FLIGHT) {
            printer.displayFlight(clientState);
        }
    }
    
    public void onAdventureCardTimeoutEvent(AdventureCardTimeoutEvent event) {
        printer.printWarning("⏰ Adventure card timed out! Default choice applied.");
        
        var card = event.getCard();
        if (card != null) {
            printer.printInfo("Card: " + card.getId());
        }
    }
    
    public void onAdventureCardResultEvent(AdventureCardResultEvent event) {
        printer.printInfo("📊 Adventure result:");
        
        var card = event.getCard();
        if (card != null) {
            printer.printInfo("Card: " + card.getName());
        }
        printer.printInfo("Card " + event.getCardNumber() + " resolved");
        printer.printInfo("Effects applied to all players");
    }
    
    public void onCombatStartedEvent(CombatStartedEvent event) {
        printer.printWarning("⚔️ Combat encounter: " + event.getEnemyName());
        printer.printInfo("Enemy strength: " + event.getEnemyStrength());
        if (event.getEnemyDescription() != null) {
            printer.printInfo("Description: " + event.getEnemyDescription());
        }
        
        long decisionTime = event.getDecisionTimeLimit();
        if (decisionTime > 0) {
            printer.printInfo("Decision time: " + (decisionTime / 1000) + " seconds");
        }
        
        printer.printInfo("Commands: 'combat <batteries>' to declare combat strength");
    }
    
    public void onCombatResolvedEvent(CombatResolvedEvent event) {
        String outcome = event.getOutcome().toString();
        
        printer.printInfo("⚔️ Combat resolved: " + event.getEnemyName());
        printer.printInfo("Your strength: " + event.getPlayerStrength() + " vs Enemy: " + event.getEnemyStrength());
        
        switch (outcome) {
            case "VICTORY" -> {
                printer.printSuccess("🏆 Victory! You defeated the enemy!");
                var reward = event.getReward();
                if (reward != null && reward.getCredits() > 0) {
                    printer.printSuccess("💰 Reward: " + reward.getCredits() + " credits");
                }
            }
            case "DEFEAT" -> {
                printer.printWarning("💥 Defeat! You suffered damage from the enemy.");
                var penalty = event.getPenalty();
                if (penalty != null && penalty.getCreditsLost() > 0) {
                    printer.printWarning("💸 Penalty: " + penalty.getCreditsLost() + " credits lost");
                }
            }
            case "DRAW" -> printer.printInfo("🤝 Draw! No effect on either side.");
            default -> printer.printInfo("Combat outcome: " + outcome);
        }
    }
    
    public void onDiceRollEvent(DiceRollEvent event) {
        printer.printInfo("🎲 Dice roll (" + event.getPurpose() + "): " + event.getDiceValues());
    }
    
    public void onResourceUpdateEvent(ResourceUpdateEvent event) {
        String playerId = event.getPlayerId();
        
        if (playerId.equals(clientState.getPlayerId())) {
            printer.printInfo("📊 Resources updated");
            if (clientState.getCurrentView() == ClientState.ViewState.FLIGHT) {
                printer.displayFlight(clientState);
            }
        }
    }
    
    public void onShipDamagedEvent(ShipDamagedEvent event) {
        String playerId = event.getPlayerId();
        String playerNickname = event.getPlayerNickname();
        int row = event.getRow();
        int col = event.getCol();
        String damageSource = event.getDamageSource();
        var componentType = event.getComponentLost();
        
        if (playerId.equals(clientState.getPlayerId())) {
            printer.printWarning("💥 Your ship took damage!");
            printer.printWarning("Lost: " + componentType + " at position (" + 
                (row + 5) + "," + (col + 4) + ")");
            printer.printWarning("Source: " + damageSource);
        } else {
            printer.printInfo("💥 " + playerNickname + "'s ship took damage from " + damageSource);
        }
        
        if (clientState.getCurrentView() == ClientState.ViewState.FLIGHT) {
            printer.displayFlight(clientState);
        }
    }
    
    public void onGameEndedEvent(GameEndedEvent event) {
        printer.printSuccess("🏁 Flight completed!");
        printer.printInfo("End reason: " + event.getReason());
        
        if (event.getFinalScores() != null && !event.getFinalScores().isEmpty()) {
            printer.printInfo("📊 Final scores:");
            event.getFinalScores().forEach((playerId, score) -> {
                String nickname = playerId; // Use playerId as nickname for now
                if (playerId.equals(clientState.getPlayerId())) {
                    printer.printSuccess("  🏆 You: " + score + " credits");
                } else {
                    printer.printInfo("  " + nickname + ": " + score + " credits");
                }
            });
        }
        
        printer.printInfo("Returning to lobby...");
        clientState.setCurrentView(ClientState.ViewState.LOBBY);
        printer.displayLobby(clientState);
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
