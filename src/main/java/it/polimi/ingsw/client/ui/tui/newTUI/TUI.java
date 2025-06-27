package it.polimi.ingsw.client.ui.tui.newTUI;

import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.UI;
import it.polimi.ingsw.client.ui.tui.Printer;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.ShipValidationService;
import it.polimi.ingsw.server.model.enums.GameLevel;

import java.util.Scanner;
import java.util.regex.Pattern;

public class TUI implements UI {
    private final Scanner scanner;
    private final Printer printer = new Printer();
    private final ClientController controller;
    private final ClientState clientState;

    // Game-specific state, previously in TuiShipBuildingView
    private Component heldComponent = null;

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");

    public TUI(ClientController controller) {
        this.controller = controller;
        this.clientState = controller.getClientState();
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void start() {
        printer.printHeader();
        // Start the main input listener loop in a new thread
        //CompletableFuture.runAsync(this::inputListener);
        inputListener();
    }

    /**
    * Main input loop for the TUI. Continuously listens for user commands.
    */
    public void inputListener(){
        String input;
        while (true) {
            input = scanner.nextLine();
            elaborateInput(input);
        }
    }

    /**
    * Displays a prompt to the user based on the current view state.
    */
    private void askForInput() {
        // This method would be called by a client state listener in a fully reactive system.
        // For this simplified model, we call it to show the user what to do next.
        switch (clientState.getCurrentView()) {
            case CONNECTION:
                printer.printConnectionPrompt();
                break;
            case LOGIN:
                printer.printLoginPrompt();
                break;
            case LOBBY:
                printer.printLobby(clientState);
                break;
            case GAME:
                printer.printShipBuildingInterface(clientState, heldComponent);
                break;
            default:
                System.out.print("> ");
        }
    }

    /**
     * Processes the user's input based on the current view state.
     * @param input The command entered by the user.
     */
    private void elaborateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        switch (clientState.getCurrentView()) {
            case CONNECTION -> elaborateConnectionCommand(input);
            case LOGIN -> elaborateLoginCommand(input);
            case LOBBY -> elaborateLobbyCommand(input);
            // case GAME_LOBBY -> elaborateGameLobbyCommand(input);
            case GAME -> elaborateBuildingCommand(input);
            default -> printer.printError("Unknown client state. No commands available.");
        }
    }


    private void elaborateConnectionCommand(String input) {
        // Logic for handling connection commands, e.g., "connect <ip>"
        printer.printInfo("Connection logic not implemented in this version.");
    }

    private void elaborateLoginCommand(String input) {
        String nickname = input.trim();
        if (!NICKNAME_PATTERN.matcher(nickname).matches()) {
            printer.printError("Invalid nickname format. Must be 3-20 characters (letters, numbers, _, -).");
            return;
        }

        printer.printLoading("Logging in as " + nickname + "...");
        controller.login(nickname).thenAccept(success -> {
            if (!success) {
                printer.printError("Login failed. The nickname might be taken or invalid.");
            } else {
                printer.printSuccess("Login successful!");
                // The view state will change automatically, and the next prompt will be for the lobby.
            }
        });
    }

    private void elaborateLobbyCommand(String input) {
        String[] tokens = input.trim().split("\\s+");
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "c", "create" -> handleCreateCommand(tokens);
            case "j", "join" -> handleJoinCommand(tokens);
            case "r", "refresh" -> controller.refreshGameList();
            case "h", "help" -> printer.printLobbyCommands();
            default -> printer.printError("Unknown command. Type 'help' for available commands.");
        }
    }

    private void handleCreateCommand(String[] tokens) {
        if (tokens.length != 4) {
            printer.printError("Usage: create <gameName> <maxPlayers> <gameLevel>");
            printer.printInfo("Example: create MyGame 4 level_II");
            return;
        }

        String gameName = tokens[1];
        int maxPlayers;
        try {
            maxPlayers = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            printer.printError("Invalid number for max players.");
            return;
        }

        String levelStr = tokens[3].toUpperCase();
        try {
            GameLevel.valueOf(levelStr); // Validate enum
        } catch (IllegalArgumentException e) {
            printer.printError("Invalid game level. Use 'TEST_FLIGHT' or 'LEVEL_II'.");
            return;
        }

        printer.printInfo("Creating game...");
        controller.createGame(gameName, maxPlayers, levelStr);
    }

    private void handleJoinCommand(String[] tokens) {
        if (tokens.length != 2) {
            printer.printError("Usage: join <gameId>");
            return;
        }
        String gameId = tokens[1];
        printer.printInfo("Joining game " + gameId + "...");
        controller.joinGame(gameId);
    }

    private void elaborateBuildingCommand(String input) {
        String[] parts = input.trim().toLowerCase().split("\\s+");
        String command = parts[0];

        switch (command) {
            case "take" -> controller.takeTile();
            case "place" -> handlePlaceCommand(parts);
            case "rotate" -> {
                if (heldComponent != null) {
                    heldComponent.rotate();
                    printer.printInfo("Component rotated.");
                } else {
                    printer.printError("You are not holding a component.");
                }
            }
            case "return" -> {
                if (heldComponent != null) {
                    controller.returnTile(heldComponent.getId());
                    heldComponent = null; // Clear held component
                } else {
                    printer.printError("You are not holding a component.");
                }
            }
            case "validate" -> controller.validateShip();
            case "flip" -> controller.flipBuildingTimer();
            case "help" -> printer.printShipBuildingCommands();
            case "quit" -> printer.printInfo("Quit command not implemented."); // Or controller.quitGame()
            default -> printer.printError("Unknown command. Type 'help' for a list of commands.");
        }
    }

    private void handlePlaceCommand(String[] parts) {
        if (heldComponent == null) {
            printer.printError("You must 'take' a component before placing it.");
            return;
        }
        if (parts.length != 3) {
            printer.printError("Usage: place <row> <col>");
            return;
        }
        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            Position pos = new Position(row, col);
            Ship ship = clientState.getLocalPlayerShip();

            if (ship == null) {
                printer.printError("Ship data is not available.");
                return;
            }

            // Perform validation before sending to server
            ShipValidationService.ValidationResult result = ShipValidationService.validateComponentPlacement(ship, heldComponent, pos);
            if (result.isValid()) {
                controller.placeTile(heldComponent.getId(), row, col, heldComponent.getCurrentDirection().ordinal());
                printer.printSuccess("Component placed!");
                heldComponent = null; // Clear after placing
            } else {
                printer.printError("Invalid placement: " + String.join(", ", result.getErrors()));
            }

        } catch (NumberFormatException e) {
            printer.printError("Invalid coordinates. Row and column must be numbers.");
        }
    }

    // --- UI Interface Methods ---

    @Override
    public void shutdown() {
        printer.shutdown();
        scanner.close();
    }

    @Override
    public boolean isRunning() {
        // In this simple model, the TUI runs as long as the application is running.
        return true;
    }

    @Override
    public void showError(String title, String message) {
        printer.printError(title + ": " + message);
    }

    @Override
    public void showInfo(String title, String message) {
        printer.printInfo(title + ": " + message);
    }

    // Method to be called by the controller/client state when the held component is updated
    public void setHeldComponent(Component component) {
        this.heldComponent = component;
        // Refresh the display to show the new component
        askForInput();
    }
}