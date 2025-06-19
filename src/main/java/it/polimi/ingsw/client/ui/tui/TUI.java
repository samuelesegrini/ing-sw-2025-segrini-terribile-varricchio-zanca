package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.client.ui.AbstractUserInterface;
import it.polimi.ingsw.client.ui.View;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.building.*;
import it.polimi.ingsw.common.message.setup.*;
import it.polimi.ingsw.common.message.system.ErrorMessage;
import it.polimi.ingsw.common.message.system.ServerLoginResponse;

import it.polimi.ingsw.server.model.enums.GameLevel;
import org.jline.reader.*;
import org.jline.terminal.*;
import org.jline.utils.InfoCmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class TUI extends AbstractUserInterface implements Runnable, View {
    private static final Logger logger = LoggerFactory.getLogger(TUI.class);
    private final Terminal terminal;
    private final LineReader reader;
    private boolean isRunning;

    //private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();


    public TUI() throws Exception {
        this.terminal = TerminalBuilder.terminal();
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
    }

    // Per uniformità con le interfacce
    @Override
    public void initialize() {}
    @Override
    public void start() {
        run();
    }
    @Override
    public void showConnectionPrompt() {
        if (viewModel != null) {
            viewModel.setAppStatus(ClientViewModel.AppStatus.LOGIN_SCREEN);
        }
    }
    @Override
    public void showError(String title, String message) {
        System.out.println("\n[ERROR] " + title + ": " + message);
    }
    @Override
    public void showInfo(String title, String message) {
        System.out.println("\n[INFO] " + title + ": " + message);
    }


    public void configureViewModelListeners() {
        if (viewModel != null) {
            viewModel.appStatusProperty().addListener((obs, oldState, newState) -> {
                logger.info("ViewModel status changed: " + oldState + " -> " + newState);
                clear();
                //showPromptForCurrentState();
                handleAppStatusChange(newState);
            });

            viewModel.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
                show("[STATUS] " + newMsg);
            });
        }
    }

    @Override
    public void handleAppStatusChange(ClientViewModel.AppStatus newStatus) {
        logger.info("Terminal UI handling state change to: " + newStatus);
        // Clear the screen for a better user experience
        clear();

        // Show the status message based on the new state
        show("\n=== Status: " + newStatus + " ===");
        if (viewModel != null && viewModel.statusMessageProperty().get() != null) {
            show(viewModel.statusMessageProperty().get());
        }

        // Show the appropriate prompt for the new state
        ClientViewModel.AppStatus state = viewModel.appStatusProperty().get();
        // TODO: Togliere quelli non necessari
        switch (state) {
            case NOT_CONNECTED:
                break;
            case LOGIN_SCREEN:
                show("Enter connection details in format: <host> <port> <nickname> <technology>");
                show("Example: login localhost 12345 Player1 Socket");
                break;
            case CONNECTING:
                break;
            case LOGGING_IN:
                show("Connecting to server... please wait.");
                break;
            case LOGGED_IN_BROWSING_LOBBIES:
                showHelp();
                break;
            case GAME_LOBBY:
                showHelp();
                break;
            case GAME_BUILDING:
                break;
            case GAME_FLIGHT:
                break;
            case GAME_FINISHED:
                break;
            case DISCONNECTED:
                break;
        }
    }


    @Override
    public void run() {
        isRunning = true;
        clear();
        show("Welcome to Galaxy Trucker - TUI");
        showHelp();

        while (isRunning) {
//            String pendingMessage;
//            while ((pendingMessage = messageQueue.poll()) != null) {
//                terminal.writer().println(pendingMessage);
//                terminal.flush();
//            }

            String input = null;
            try {
                input = getInput();
                processInput(input);
            } catch (UserInterruptException e) {
                stop();
                close();
            } catch (Exception e) {
                logger.error("Failed to process line {}", input, e);
            }
        }
    }

    // Command Processing

    public void processInput(String input) {
        if (viewModel == null || input == null || input.trim().isEmpty()) {
            return;
        }

        if (input.equalsIgnoreCase("quit")) {
            // TODO: Dovrà disconnettersi come si deve
            stop();
            close();
        } else if (input.equalsIgnoreCase("help")) {
            showHelp();
        } else {
            ClientViewModel.AppStatus state = viewModel.appStatusProperty().get();
            // TODO: Togliere quelli non necessari
            switch (state) {
                case NOT_CONNECTED:
                    break;
                case LOGIN_SCREEN:
                    processLoginCommand(input);
                    break;
                case CONNECTING:
                    break;
                case LOGGING_IN:
                    break;
                case LOGGED_IN_BROWSING_LOBBIES:
                    processLobbyBrowserCommand(input);
                    break;
                case GAME_LOBBY:
                    processGameLobbyCommand(input);
                    break;
                case GAME_BUILDING:
                    break;
                case GAME_FLIGHT:
                    break;
                case GAME_FINISHED:
                    break;
                case DISCONNECTED:
                    break;
                default:
                    logger.warn("Unhandled state: {}. Ignoring input: {}", state, input);
                    break;
            }
        }
    }

    public void processLoginCommand(String input) {
        String[] tokens = input.split("\\s+");
        if (tokens.length != 5) {
            show("Invalid login command. Expected 5 arguments.");
            return;
        }

        if (!tokens[0].equalsIgnoreCase("login")) {
            show("Invalid login command. Expected 'login' as first argument.");
            return;
        }

        String host = tokens[1];

        int port;
        try {
            port = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            show("Invalid port number: " + tokens[2]);
            return;
        }
        if (port < 1024 || port > 65535) {
            show("Invalid port number: " + port);
            return;
        }

        String nickname = tokens[3];
        if (nickname.length() < 3 || nickname.length() > 15) {
            show("Invalid nickname length: " + nickname.length());
        }

        String technology = tokens[4];
        if (!technology.equalsIgnoreCase("RMI") && !technology.equalsIgnoreCase("Socket")) {
            show("Invalid technology: " + technology);
            return;
        }

        if (clientController != null) {
            clientController.setupNetworkAndConnect(host, port, nickname, technology);
        } else {
            logger.warn("GameClientController is null. Cannot connect to server.");
        }
    }

    public void processLobbyBrowserCommand(String input) {
        String[] tokens = input.split("\\s+");

        if (tokens.length == 0) {
            show("Invalid lobby browser command. Expected at least 1 argument.");
            return;
        }

        String command = tokens[0].toLowerCase();
        switch (command) {
            case "list":
                processGameListCommand(tokens);
                break;
            case "create":
                processCreateGameCommand(tokens);
                break;
            case "join":
                processJoinGameCommand(tokens);
                break;
            case "refresh":
                break;
            default:
                show("Unknown command: " + command);
                break;
        }
    }

    public void processGameLobbyCommand(String input) {
        String[] tokens = input.split("\\s+");
        if (tokens.length == 0) {
            show("Invalid game lobby command. Expected at least 1 argument.");
            return;
        }

        String command = tokens[0].toLowerCase();
        switch (command) {
            case "players":
                break;
            case "start":
                processStartGameCommand(tokens);
                break;
            case "leave":
                processLeaveGameCommand(tokens);
                break;
            default:
                show("Unknown command: " + command);
                break;
        }
    }

    // Lobby Browser Commands

    public void processGameListCommand(String[] tokens) {
        if (tokens.length > 1) {
            show("Invalid game list command. Expected 0 arguments.");
            return;
        }

        viewModel.requestGameList();
    }

    public void processCreateGameCommand(String[] tokens) {
        if (tokens.length != 4) {
            show("Invalid create game command. Expected 4 arguments.");
            return;
        }

        String name = tokens[1];
        if (name.length() < 3 || name.length() > 15) {
            show("Invalid game name length: " + name.length());
            return;
        }

        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            show("Invalid max players number: " + tokens[2]);
            return;
        }

        GameLevel level;
        if (tokens[3].equalsIgnoreCase("test_flight"))
            level = GameLevel.TEST_FLIGHT;
        else if (tokens[3].equalsIgnoreCase("level_II"))
            level = GameLevel.LEVEL_II;
        else {
            show("Invalid game level: " + tokens[3]);
            return;
        }

        GameSettingsDTO gameSettings = new GameSettingsDTO(name, maxPlayers, level);
        //CreateGameRequestCommand message = new CreateGameRequestCommand(gameSettings);

        viewModel.createGame(gameSettings);
    }

    public void processJoinGameCommand(String[] tokens) {
        if (tokens.length != 2) {
            show("Invalid join game command. Expected 2 arguments.");
            return;
        }

        viewModel.joinGame(tokens[1]);
    }

    // Game Lobby Commands

    public void processStartGameCommand(String[] tokens) {
        if (tokens.length != 1) {
            show("Invalid start game command. Expected 0 arguments.");
            return;
        }

        viewModel.startGame();
    }

    public void processLeaveGameCommand(String[] tokens) {
        if (tokens.length != 1) {
            show("Invalid leave game command. Expected 0 arguments.");
            return;
        }

        viewModel.leaveGame();
    }

    // Basic I/O

    public void show(String output) {
        terminal.writer().println(output);
        //terminal.writer().print(">> ");
        terminal.flush();

//        reader.callWidget(LineReader.REDRAW_LINE);
//        reader.callWidget(LineReader.REDISPLAY);

        //messageQueue.offer(output);
    }

    public void clear() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    public String getInput() {
        return reader.readLine(">> ");
    }

    public void close() {
        try {
            terminal.close();
        } catch (Exception e) {
            System.err.println("Error closing terminal: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
    }

    //

    private void showHelp() {
        show("Available commands:");
        show("  quit - Exit the application");
        show("  help - Show this help message");

        // TODO: Togliere quelli non necessari
        switch (viewModel.appStatusProperty().get()) {
            case NOT_CONNECTED:
                break;
            case LOGIN_SCREEN:
                show("  login <host> <port> <nickname> <technology> - Connect to a server");
                break;
            case CONNECTING:
                break;
            case LOGGING_IN:
                break;
            case LOGGED_IN_BROWSING_LOBBIES:
                show("  list - List available games");
                show("  create <game name> <max players> <game level> - Create a new game");
                show("  join <gameId> - Join an existing game");
                show("  refresh - Refresh game list");
                break;
            case GAME_LOBBY:
                show("  players - List players in lobby");
                show("  start - Start the game (host only)");
                show("  leave - Leave the lobby");
                break;
            case GAME_BUILDING:
                break;
            case GAME_FLIGHT:
                break;
            case GAME_FINISHED:
                break;
            case DISCONNECTED:
                break;
            default:
                logger.warn("Unhandled state: {}. Ignoring help message.", viewModel.appStatusProperty().get());
        }
    }

    //



    // TODO: INUTILI?

    // System Messages

    @Override
    public void onErrorMessage(ErrorMessage message) {}

    @Override
    public void onServerLoginResponse(ServerLoginResponse message) {
        if (message.getPlayerId().equals(viewModel.loggedInPlayerIdProperty().get())) {
            if (message.isSuccess()) {
                show("Login successful!");
            } else {
                show("Login failed.");
            }
        }
    }

    // Setup Messages

    @Override
    public void onCreateGameResponseEvent(CreateGameResponseEvent message) {
        if (message.isSuccess()) {
            for (PlayerInfoDTO playerInfo : message.getPlayersInLobby()) {
                if (playerInfo.getPlayerId().equals(viewModel.loggedInPlayerIdProperty().get()) && playerInfo.isHost()) {
                    show("Game created successfully.");
                    show("You are the host of the game. Use the 'start' command to start the game.");
                }
            }
        }
    }

    @Override
    public void onGameListResponseEvent(GameListResponseEvent message) {}

    @Override
    public void onGameSessionStateChangedEvent(GameSessionStateChangedEvent message) {}

    @Override
    public void onJoinGameResponseEvent(JoinGameResponseEvent message) {}

    @Override
    public void onPlayerJoinedGameSessionNotification(PlayerJoinedGameSessionNotification message) {}

    @Override
    public void onPlayerLeftGameSessionNotification(PlayerLeftGameSessionNotification message) {}

    @Override
    public void onLobbyStateUpdateEvent(LobbyStateUpdateEvent message) {}

    @Override
    public void onServerGameListUpdateNotification(ServerGameListUpdateNotification message) {}

    // Building Messages

    @Override
    public void onAvailableComponentsUpdateEvent(AvailableComponentsUpdateEvent event) {}

    @Override
    public void onBuildingTimerUpdatedEvent(BuildingTimerUpdatedEvent event) {}

    @Override
    public void onComponentOfferedToPlayerEvent(ComponentOfferedToPlayerEvent event) {}

    @Override
    public void onComponentPlacedEvent(ComponentPlacedEvent message) {}

    @Override
    public void onComponentReservedEvent(ComponentReservedEvent message) {}

    @Override
    public void onComponentReturnedToPileEvent(ComponentReturnedToPileEvent message) {}

    @Override
    public void onPlayerFinishedBuildingEvent(PlayerFinishedBuildingEvent message) {}

    @Override
    public void onPlayerShipUpdateEvent(PlayerShipUpdateEvent message) {}

    @Override
    public void onShipBuildingPhaseEndedEvent(ShipBuildingPhaseEndedEvent message) {}

    @Override
    public void onShipCorrectedEvent(ShipCorrectedEvent message) {}

    @Override
    public void onShipValidationResultEvent(ShipValidationResultEvent message) {}
}
