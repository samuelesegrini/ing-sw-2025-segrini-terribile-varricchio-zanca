package it.polimi.ingsw.client.ui;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.tui.Printer;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.common.message.response.*;
import it.polimi.ingsw.common.model.GameInfo;

import it.polimi.ingsw.server.model.domain.player.Player;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
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

    // Temporanei
    private final ClientController controller;
    private final ClientState clientState;

    public newTUI(ClientController controller) throws IOException {
        terminal = TerminalBuilder.builder().system(true).build();
        reader = LineReaderBuilder.builder().terminal(terminal).build();
        this.printer = new Printer(terminal, reader);

        // Temporanei
        this.controller = controller;
        this.clientState = controller.getClientState();
    }

    @Override
    public void start() {
        printer.printHeader();

        printer.printConnectionPhase();
        elaborateConnectionPhase();

        printer.printLoginPhase();
        elaborateLogin();

        String input;
        while (true) {
            input = reader.readLine();
            elaborateInput(input);
        }
    }

    private void elaborateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] tokens = input.trim().split("\\s+");

        switch (clientState.getCurrentView()) {
            //case CONNECTION -> elaborateConnectionCommand(tokens);
            //case LOGIN -> elaborateLoginCommand(tokens);
            case LOBBY -> elaborateLobbyCommand(tokens);
            case GAME_LOBBY -> elaborateGameLobbyCommand(tokens);
        }
    }

    private void elaborateConnectionPhase() {
        // Get hostname
        printer.print("Enter hostname (default: localhost): ");
        String hostname = reader.readLine().trim();
        if (hostname.isEmpty()) {
            hostname = "localhost";
        }

        // Get protocol selection
        printer.print("Select protocol - (1) Socket, (2) RMI (default: 1): ");
        String protocol = reader.readLine().trim().toLowerCase();
        boolean useSocket = true;
        int defaultPort = 12345;

        if (protocol.equals("2") || protocol.equals("rmi")) {
            useSocket = false;
            defaultPort = 1099;
            printer.printInfo("Selected protocol: RMI");
        } else {
            printer.printInfo("Selected protocol: Socket");
        }

        // Get port
        printer.print("Enter port (default: " + defaultPort + "): ");
        String portInput = reader.readLine().trim();
        int port = defaultPort;
        if (!portInput.isEmpty()) {
            try {
                port = Integer.parseInt(portInput);
            } catch (NumberFormatException e) {
                printer.printError("Invalid port number. Using default port " + defaultPort + ".");
                port = defaultPort;
            }
        }

        // Attempt connection by directly calling the controller
        String protocolName = useSocket ? "Socket" : "RMI";
        printer.printLoading("Connecting to " + hostname + ":" + port + " via " + protocolName);

        // We use .get() here to block the TUI input loop until the connection attempt is complete.
        try {
            boolean success = controller.connect(hostname, port, useSocket).get();  // TODO: Sistema
        } catch (ExecutionException | InterruptedException e) {
            printer.printError("Connection error: " + e.getMessage());
        }
    }

    private void elaborateLogin() {
        printer.print("Enter nickname: ");
        String nickname = reader.readLine().trim();

        Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
        while (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            printer.printError("Invalid nickname format. Please try again.");
            nickname = reader.readLine().trim();
        }

        printer.printLoading("Logging in as " + nickname);

        try {
            boolean success = controller.login(nickname).get();

            if (!success) {
                printer.printError("Login failed. The nickname might already be taken or is invalid.");
                elaborateLogin();
            }
            // On success, the controller changes the view state, which will make isActive() false
            // for the next iteration, breaking the loop.

        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            printer.printError("Login error: " + e.getMessage());
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
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
                handleReadyCommand();
                break;
            case "u":
            case "unready":
                handleUnreadyCommand();
                break;
            case "start":
                handleStartCommand();
                break;
            case "l":
            case "leave":
            case "quit":
                handleLeaveCommand();
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

    private void handleReadyCommand() {
        String playerId = controller.getPlayerId() != null ?
                controller.getPlayerId() : null;
        if (!controller.getClientState().isPlayerReady(playerId)) {
            controller.setPlayerReady(true);
            printer.printSuccess("Marked as ready!");
        } else {
            printer.printWarning("You are already ready!");
        }
    }

    private void handleUnreadyCommand() {
        String playerId = controller.getPlayerId() != null ?
                controller.getPlayerId() : null;
        if (controller.getClientState().isPlayerReady(playerId)) {
            controller.setPlayerReady(false);
            printer.printSuccess("Marked as not ready!");
        } else {
            printer.printWarning("You are already not ready!");
        }
    }

    private void handleStartCommand() {
        String currentPlayerId = controller.getPlayerId() != null ?
                controller.getPlayerId() : null;
        if (!currentPlayerId.equals(getHostPlayerId())) {
            printer.printError("Only the host can start the game!");
            return;
        }

        if (!areAllPlayersReady()) {
            printer.printError("Not all players are ready!");
            return;
        }

        controller.startGame();
        printer.printSuccess("Starting game...");
    }

    private void handleLeaveCommand() {
        printer.printWarning("Are you sure you want to leave the lobby? (y/n)");
        String confirmation = reader.readLine().trim().toLowerCase();
        if (confirmation.startsWith("y")) {
            controller.leaveGame();
            printer.printSuccess("Leaving lobby...");
        } else {
            printer.printInfo("Cancelled leaving lobby.");
        }
    }

    // TODO: CONTROLLA
    private String getHostPlayerId() {
        // Get host from GameModel which tracks the actual creator/host
        if (controller.getClientState().getCurrentGameLobby() != null) {
            return controller.getClientState().getCurrentGameLobby().getCreatorId();
        }
        // Fallback to the first player if GameModel is not available
        List<Player> players = controller.getClientState().getPlayersInLobby();
        return players != null && !players.isEmpty() ? players.getFirst().getId().toString() : null;
    }

    // TODO: CONTROLLA
    private boolean areAllPlayersReady() {
        List<Player> players = controller.getClientState().getPlayersInLobby();
        return players != null && !players.isEmpty() &&
                players.stream().allMatch(Player::isReady);
    }

    // ON RESPONSE METHODS

    @Override
    public void onErrorResponse(ErrorResponse r) {};

    @Override
    public void onReconnectResponse(ReconnectResponse r) {};

    // Login

    @Override
    public void onLoginResponse(LoginResponse r) {
        if (r.isSuccess()) {
            printer.printSuccess("Login successful!");
            printer.print("Welcome " + r.getNickname() + "!");
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
    public void onStartGameResponse(GenericSuccessResponse r) {
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
        } else {
            printer.printError("Failed to reserve tile.");
        }
    }

    @Override
    public void onPlaceTileResponse(GenericSuccessResponse response) {
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
