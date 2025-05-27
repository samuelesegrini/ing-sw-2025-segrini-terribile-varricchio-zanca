package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.GameClientController;
import it.polimi.ingsw.client.model.ClientViewModel;
import it.polimi.ingsw.client.ui.View;
import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
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

import java.util.List;


public class TUI implements Runnable, View {
    private static final Logger logger = LoggerFactory.getLogger(TUI.class);
    private final Terminal terminal;
    private final LineReader reader;
    private boolean isRunning;

    private GameClientController gameClientController;
    private ClientViewModel clientViewModel;


    public TUI(ClientViewModel clientViewModel) throws Exception {
        this.terminal = TerminalBuilder.terminal();
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        this.clientViewModel = clientViewModel;
    }


    public void configureViewModelListeners() {
        if (clientViewModel != null) {
            clientViewModel.appStatusProperty().addListener((obs, oldState, newState) -> {
                logger.info("ViewModel status changed: " + oldState + " -> " + newState);
                clear();
                showPromptForCurrentState();
            });

            clientViewModel.statusMessageProperty().addListener((obs, oldMsg, newMsg) -> {
                show("[STATUS]" + newMsg);
            });
        }
    }


    @Override
    public void run() {
        isRunning = true;
        clear();
        show("Welcome to Galaxy Trucker - TUI");

        while (isRunning) {
            showPromptForCurrentState();

            String input = null;
            try {
                input = reader.readLine(">> ");
                processInput(input);
            } catch (UserInterruptException e) {
                stop();
                close();
            } catch (Exception e) {
                logger.error("Failed to process line {}", input, e);
            }
        }
    }

    public void showPromptForCurrentState() {
        ClientViewModel.AppStatus state = clientViewModel.appStatusProperty().get();
        // TODO: Togliere quelli non necessari
        switch (state) {
            case NOT_CONNECTED:
                break;
            case LOGIN_SCREEN:
                show("Enter connection details in format: <host> <port> <nickname> <technology>");
                show("Example: localhost 1234 Player1 Socket");
                break;
            case CONNECTING:
                break;
            case LOGGING_IN:
                show("Connecting to server... please wait.");
                break;
            case LOGGED_IN_BROWSING_LOBBIES:
                show("Game Browser Options:");
                showHelp();
                break;
            case GAME_LOBBY:
                show("Lobby Options:");
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

    // Command Processing

    public void processInput(String input) {
        if (clientViewModel == null || input == null || input.trim().isEmpty()) {
            return;
        }

        if (input.equalsIgnoreCase("quit")) {
            // TODO: Dovrà disconnettersi come si deve
            stop();
            close();
        } else if (input.equalsIgnoreCase("help")) {
            showHelp();
        } else {
            ClientViewModel.AppStatus state = clientViewModel.appStatusProperty().get();
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
        if (tokens.length != 4) {
            show("Invalid login command. Expected 4 arguments.");
            return;
        }

        String host = tokens[0];

        int port;
        try {
            port = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            show("Invalid port number: " + tokens[1]);
            return;
        }
        if (port < 1024 || port > 65535) {
            show("Invalid port number: " + port);
            return;
        }

        String nickname = tokens[2];
        if (nickname.length() < 3 || nickname.length() > 15) {
            show("Invalid nickname length: " + nickname.length());
        }

        String technology = tokens[3];
        if (!technology.equalsIgnoreCase("RMI") && !technology.equalsIgnoreCase("Socket")) {
            show("Invalid technology: " + technology);
            return;
        }

        if (gameClientController != null) {
            gameClientController.setupNetworkAndConnect(host, port, nickname, technology);
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
                break;
            case "create":
                processCreateGameCommand(input);
                break;
            case "join":
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
                break;
            case "leave":
                break;
            default:
                show("Unknown command: " + command);
                break;
        }
    }

    // Lobby Browser Commands

    public void processCreateGameCommand(String input) {
        String[] tokens = input.split("\\s+");
        if (tokens.length != 3) {
            show("Invalid create game command. Expected 3 arguments.");
            return;
        }

        String name = tokens[0];
        if (name.length() < 3 || name.length() > 15) {
            show("Invalid game name length: " + name.length());
            return;
        }

        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            show("Invalid max players number: " + tokens[1]);
            return;
        }

        GameLevel level;
        if (tokens[2].equalsIgnoreCase("test_flight"))
            level = GameLevel.TEST_FLIGHT;
        else if (tokens[2].equalsIgnoreCase("level_II"))
            level = GameLevel.LEVEL_II;
        else {
            show("Invalid game level: " + tokens[2]);
            return;
        }

        GameSettingsDTO gameSettings = new GameSettingsDTO(name, maxPlayers, level);
        CreateGameRequestCommand message = new CreateGameRequestCommand(gameSettings);

        // TODO: Manda messaggio
    }

    // Basic I/O

    public void show(String output) {
        terminal.writer().println(output);
        terminal.flush();
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
        switch (clientViewModel.appStatusProperty().get()) {
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
                show("  create <n> <maxPlayers> - Create a new game");
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
                logger.warn("Unhandled state: {}. Ignoring help message.", clientViewModel.appStatusProperty().get());
        }
    }

    //



    // System Messages

    @Override
    public void onErrorMessage(ErrorMessage message) {}

    @Override
    public void onServerLoginResponse(ServerLoginResponse message) {
        if (message.getPlayerId().equals(clientViewModel.loggedInPlayerIdProperty().get())) {
            if (message.isSuccess()) {
                show("Login successful!");
            } else {
                show("Login failed.");
            }
        }
    }

    // Setup Messages

    // TODO: O troppi messaggi o questo dovrebbe avere un playerId?
    @Override
    public void onCreateGameResponseEvent(CreateGameResponseEvent message) {
        if (message.isSuccess()) {
            for (PlayerInfoDTO playerInfo : message.getPlayersInLobby()) {
                if (playerInfo.getPlayerId().equals(clientViewModel.loggedInPlayerIdProperty().get()) && playerInfo.isHost()) {
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

    // Getters & Setters

    public GameClientController getGameClientController() {
        return gameClientController;
    }

    public void setGameClientController(GameClientController gameClientController) {
        this.gameClientController = gameClientController;
    }
}
