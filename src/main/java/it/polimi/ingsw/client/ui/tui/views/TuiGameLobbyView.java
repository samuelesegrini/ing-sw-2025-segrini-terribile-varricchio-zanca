package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.server.model.domain.player.Player;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Scanner;

/**
 * TUI view for the game lobby (where players wait for the game to start).
 * Shows player list, ready status, and provides controls for ready/start/leave.
 */
public class TuiGameLobbyView extends BaseUIView { //CONTROLLA
    private final TuiConsole console;
    private final Scanner scanner;

    public TuiGameLobbyView(it.polimi.ingsw.client.ui.core.UIContext context) {
        this.scanner = new Scanner(System.in);
        // Get console through TuiContext casting
        if (context instanceof TuiContext tuiContext) {
            this.console = tuiContext.getConsole();
        } else {
            throw new IllegalStateException("Expected TuiContext but got " + context.getClass());
        }
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.GAME_LOBBY;
    }

    @Override
    public String getTitle() {
        return "Game Lobby";
    }

    @Override
    protected void onShow() {
        displayGameLobby();
        startInputLoop();
    }

    @Override
    protected void onHide() {}

    @Override
    protected void onRefresh() {
        displayGameLobby();
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case "playersInLobby":
            case "currentGameInfo":
            case "playerReady":
                displayGameLobby();
                break;
            case "currentView":
                // Handle view transitions
                if (evt.getNewValue() == ClientState.ViewState.GAME) {
                    console.printSuccess("Game started! Transitioning to ship building...");
                }
                break;
        }
    }

    private void displayGameLobby() {
        if (context == null || context.getClientState() == null) {
            return;
        }

        console.clearScreen();
        console.printSectionHeader("GAME LOBBY");

        ClientState clientState = context.getClientState();
        
        // Display game information
        if (clientState.getCurrentGameLobby() != null) {
            var gameModel = clientState.getCurrentGameLobby();
            console.printInfo("Game: " + gameModel.getGameId());
            console.printInfo("Level: " + gameModel.getGameLevel());
            console.printInfo("Players: " + gameModel.getPlayers().size() + 
                             "/" + gameModel.getMaxPlayers());
            console.println("");
        }

        displayPlayersTable();
        displayStatus();
        displayCommands();
    }

    private void displayPlayersTable() {
        console.println("Players in Lobby:");
        
        List<Player> players = context.getClientState().getPlayersInLobby();
        if (players == null || players.isEmpty()) {
            console.printWarning("No players in lobby");
            return;
        }

        String currentPlayerId = context.getController().getPlayerId().toString();
        String hostId = getHostPlayerId();

        String[] headers = {"NICKNAME", "STATUS", "HOST"};
        String[][] data = new String[players.size()][3];
        
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            boolean isHost = player.getId().toString().equals(hostId);
            String status = "Ready"; // TODO: Add isReady method to Player model
            String hostIndicator = isHost ? "★" : "";
            
            data[i][0] = player.getId().getNickname();
            data[i][1] = status;
            data[i][2] = hostIndicator;
        }
        
        console.printTable(headers, data);
        console.println("");
    }

    private void displayStatus() {
        String currentPlayerId = context.getController().getPlayerId().toString();
        boolean isHost = currentPlayerId != null && currentPlayerId.equals(getHostPlayerId());
        boolean isReady = true; // TODO: Add isPlayerReady method to ClientState
        boolean allReady = areAllPlayersReady();

        if (allReady) {
            if (isHost) {
                console.printSuccess("All players are ready! You can start the game.");
            } else {
                console.printInfo("All players are ready! Waiting for host to start the game...");
            }
        } else {
            console.printWarning("Waiting for all players to be ready...");
        }

        console.printInfo("Your status: " + (isReady ? "Ready" : "Not Ready"));
        console.println("");
    }

    private void displayCommands() {
        console.println("Available Commands:");
        
        String currentPlayerId = context.getController().getPlayerId().toString();
        boolean isHost = currentPlayerId != null && currentPlayerId.equals(getHostPlayerId());
        boolean isReady = true; // TODO: Add isPlayerReady method to ClientState
        boolean allReady = areAllPlayersReady();

        if (isReady) {
            console.println("• (u) unready - Mark yourself as not ready");
        } else {
            console.println("• (r) ready - Mark yourself as ready");
        }

        if (isHost && allReady) {
            console.println("• (s) start - Start the game");
        }

        console.println("• (l) leave - Leave the lobby");
        console.println("• refresh - Refresh the lobby display");
        console.println("• (h) help - Show this help message");
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

        String command = input.trim().toLowerCase();
        
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
            case "refresh":
                displayGameLobby();
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

    private void handleReadyCommand() {
        String playerId = context.getController().getPlayerId();
        if (!context.getModel().isPlayerReady(playerId)) {
            context.getController().setPlayerReady(true);
            console.printSuccess("Marked as ready!");
        } else {
            console.printWarning("You are already ready!");
        }
    }

    private void handleUnreadyCommand() {
        String playerId = context.getController().getPlayerId();
        if (context.getModel().isPlayerReady(playerId)) {
            context.getController().setPlayerReady(false);
            console.printSuccess("Marked as not ready!");
        } else {
            console.printWarning("You are already not ready!");
        }
    }

    private void handleStartCommand() {
        String currentPlayerId = context.getController().getPlayerId();
        if (!currentPlayerId.equals(getHostPlayerId())) {
            console.printError("Only the host can start the game!");
            return;
        }

        if (!areAllPlayersReady()) {
            console.printError("Not all players are ready!");
            return;
        }

        context.getController().startGame();
        console.printSuccess("Starting game...");
    }

    private void handleLeaveCommand() {
        console.printWarning("Are you sure you want to leave the lobby? (y/n)");
        String confirmation = scanner.nextLine();
        if (confirmation != null && confirmation.toLowerCase().startsWith("y")) {
            context.getController().leaveGame();
            console.printSuccess("Leaving lobby...");
        } else {
            console.printInfo("Cancelled leaving lobby.");
        }
    }

    private String getHostPlayerId() {
        // Get host from GameModel which tracks the actual creator/host
        if (context.getModel().getCurrentGame() != null) {
            return context.getModel().getCurrentGame().getCreatorId();
        }
        // Fallback to the first player if GameModel is not available
        List<Player> players = context.getModel().getPlayersInLobby();
        return players != null && !players.isEmpty() ? players.getFirst().getPlayerId() : null;
    }

    private boolean areAllPlayersReady() {
        List<Player> players = context.getModel().getPlayersInLobby();
        return players != null && !players.isEmpty() && 
               players.stream().allMatch(Player::isReady);
    }
}