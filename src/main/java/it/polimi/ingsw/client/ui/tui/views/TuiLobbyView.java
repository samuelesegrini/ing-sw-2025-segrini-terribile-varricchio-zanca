package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.common.GameInfo;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * TUI view for the lobby (where players can create or join games).
 */
public class TuiLobbyView extends BaseUIView {
    private final TuiConsole console;
    private final Scanner scanner;

    public TuiLobbyView(TuiContext context) {
        this.console = context.getConsole();
        this.scanner = new Scanner(System.in);
        initialize(context);
    }
    
    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.LOBBY;
    }
    
    @Override
    public String getTitle() {
        return "Lobby";
    }

    @Override
    protected void onShow() {
        context.getController().refreshGameList()
                .thenAccept(success -> {
                    if (success) {
                        LOGGER.info("Refreshed game list.");
                    } else {
                        LOGGER.warning("Failed to refresh game list.");
                    }
                });

        displayLobby();
        startInputLoop();
    }
    
    @Override
    protected void onHide() {}

    @Override
    protected void onRefresh() {
        displayLobby();
    }

    @Override
    protected void onPropertyChange(java.beans.PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "availableGames":
            case "inProgressGames":
                displayLobby();
                break;
        }
    }

    private void displayLobby() {
        if (context == null || context.getModel() == null) {
            return;
        }

        console.clearScreen();
        console.printSectionHeader("LOBBY");

        displayJoinableGames();
        displayInProgressGames();
        displayAvailableGames();
        displayCommands();
    }

    private void displayJoinableGames() {
        console.println("Available Games (Waiting for Players):");

        List<GameInfo> games = context.getModel().getJoinableGames();
        if (games == null || games.isEmpty()) {
            console.println("No games available to join.");
            console.println("");
            return;
        }

        String[] headers = {"GAME ID", "GAME NAME", "GAME LEVEL", "PLAYERS"};
        String[][] data = new String[games.size()][4];

        for (int i = 0; i < games.size(); i++) {
            GameInfo game = games.get(i);

            data[i][0] = game.getGameId();
            data[i][1] = game.getGameName();
            data[i][2] = game.getGameLevel().toString();
            data[i][3] = game.getCurrentPlayers() + "/" + game.getMaxPlayers();
        }

        console.printTable(headers, data);
        console.println("");
    }

    private void displayInProgressGames() {
        console.println("Games in Progress:");

        List<GameInfo> games = context.getModel().getGamesInProgress();
        if (games == null || games.isEmpty()) {
            console.println("No games currently in progress.");
            console.println("");
            return;
        }

        String[] headers = {"GAME ID", "GAME NAME", "PHASE", "PLAYERS"};
        String[][] data = new String[games.size()][4];

        for (int i = 0; i < games.size(); i++) {
            GameInfo game = games.get(i);

            data[i][0] = game.getGameId();
            data[i][1] = game.getGameName();
            data[i][2] = getPhaseDisplayText(game.getCurrentPhase());
            data[i][3] = game.getCurrentPlayers() + "/" + game.getMaxPlayers();
        }

        console.printTable(headers, data);
        console.println("");
    }

    private void displayAvailableGames() {
        console.println("All Games:");

        List<GameInfo> games = context.getModel().getAvailableGames();
        if (games == null || games.isEmpty()) {
            console.println("No games available.");
            return;
        }

        String[] headers = {"GAME ID", "GAME NAME", "GAME LEVEL", "PHASE", "PLAYERS"};
        String[][] data = new String[games.size()][5];

        for (int i = 0; i < games.size(); i++) {
            GameInfo game = games.get(i);

            data[i][0] = game.getGameId();
            data[i][1] = game.getGameName();
            data[i][2] = game.getGameLevel().toString();
            data[i][3] = getPhaseDisplayText(game.getCurrentPhase());
            data[i][4] = game.getCurrentPlayers() + "/" + game.getMaxPlayers();
        }

        console.printTable(headers, data);
        console.println("");
    }

    private String getPhaseDisplayText(GamePhase phase) {
        return switch (phase) {
            case SETUP -> "Waiting";
            case BUILDING -> "Building";
            case FLIGHT -> "Flight";
            case END -> "Ended";
        };
    }

    public void displayCommands() {
        console.println("Available Commands:");

        console.println("• (c) create <gameName> <maxPlayers> <gameLevel> - Create a new game");
        console.println("• (j) join <gameId - Join an existing game");
        console.println("");
    }

    private void startInputLoop() {
        Thread inputThread = new Thread(() -> {
            while (active) {
                try {
                    console.println("Enter command: ");
                    String input = scanner.nextLine();
                    handleInput(input);
                } catch (Exception e) {
                    break;
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }

    private void handleInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        String[] tokens = input.split("\\s+");
        String command = tokens[0].toLowerCase();
        switch (command) {
            case "c":
            case "create":
                handleCreateCommand(tokens);
                break;
            case "j":
            case "join":
                handleJoinCommand(tokens);
                break;
            case "h":
            case "help":
                displayCommands();
                break;
            default:
                console.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void handleCreateCommand(String[] tokens) {
        if (tokens.length != 4) {
            console.printError("Error: 'create' command requires 3 arguments.");
            return;
        }

        String gameName = tokens[1];
        if (gameName.length() < 3 || gameName.length() > 15) {
            console.println("Error: gameName length must be between 3 and 15 characters.");
            return;
        }

        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(tokens[2]);

            if (maxPlayers < 2 || maxPlayers > 4) {
                console.println("Error: maxPlayers must be between 2 and 4.");
                return;
            }
        } catch (NumberFormatException e) {
            console.println("Error: invalid maxPlayers number.");
            return;
        }

        String gameLevel = tokens[3];
        if (!gameLevel.equalsIgnoreCase("test_flight") && !gameLevel.equalsIgnoreCase("level_II")) {
            console.println("Error: gameLevel must be either 'test_flight' or 'level_II'.");
            return;
        }

        context.getController().createGame(gameName, maxPlayers, gameLevel);
    }

    private void handleJoinCommand(String[] tokens) {
        if (tokens.length != 2) {
            console.printError("Error: 'join' command requires 1 argument.");
            return;
        }

        String gameId = tokens[1];

        context.getController().joinGame(gameId);
    }
}