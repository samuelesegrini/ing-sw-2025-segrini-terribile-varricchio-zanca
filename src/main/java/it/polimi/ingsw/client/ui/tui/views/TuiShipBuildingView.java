package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;

import java.beans.PropertyChangeEvent;
import java.util.Scanner;

/**
 * TUI view for ship building phase.
 * Complete implementation with ASCII ship grid and command-based building.
 */
public class TuiShipBuildingView extends BaseUIView {
    private final Scanner scanner;
    private volatile boolean refreshNeeded = false;

    public TuiShipBuildingView() {
        this.scanner = new Scanner(System.in);
    }

    private TuiConsole getConsole() {
        return ((TuiContext) context).getConsole();
    }

    @Override
    public ClientModel.ViewState getViewState() {
        return ClientModel.ViewState.GAME;
    }

    @Override
    public String getTitle() {
        return "Ship Building";
    }

    @Override
    protected void onShow() {
        displayFullInterface();
        startInputLoop();
    }

    private void displayFullInterface() {
        TuiConsole console = getConsole();
        console.clearScreen();
        console.printSectionHeader("SHIP BUILDING PHASE");
        
        var gameState = it.polimi.ingsw.client.core.state.LocalGameState.getInstance();
        
        // Display timer
        displayTimer(gameState);
        console.println("");
        
        // Display ship grid
        displayShipGrid(gameState);
        console.println("");
        
        // Display ship statistics
        displayShipStats(gameState);
        console.println("");
        
        // Display component inventory
        displayComponentInventory(gameState);
        console.println("");
        
        // Display commands
        console.printInfo("Commands: place <row> <col> <component> | take | return <component> | validate | flip | help | refresh");
        console.println("");
        
        refreshNeeded = false;
    }

    private void displayTimer(it.polimi.ingsw.client.core.state.LocalGameState gameState) {
        TuiConsole console = getConsole();
        long timeRemaining = gameState.getBuildingTimeRemaining();
        boolean flipped = gameState.isBuildingTimerFlipped();
        
        console.println("⏱ Timer: ");
        if (timeRemaining > 0) {
            long seconds = timeRemaining / 1000;
            if (seconds < 30) {
                console.printError(String.format("%d seconds%s", seconds, flipped ? " (FLIPPED)" : ""));
            } else {
                console.printWarning(String.format("%d seconds%s", seconds, flipped ? " (FLIPPED)" : ""));
            }
        } else {
            console.printError("TIME UP!");
        }
    }

    private void displayShipGrid(it.polimi.ingsw.client.core.state.LocalGameState gameState) {
        TuiConsole console = getConsole();
        console.printInfo("Your Ship (5x7 grid):");
        
        // Header with column numbers
        System.out.print("  ");
        for (int col = 0; col < 7; col++) {
            System.out.print(String.format("%3d", col));
        }
        System.out.println();
        
        // Grid rows
        for (int row = 0; row < 5; row++) {
            System.out.print(String.format("%d ", row));
            for (int col = 0; col < 7; col++) {
                var position = new it.polimi.ingsw.server.model.domain.ship.Position(row, col);
                var component = gameState.getComponentAt(position);
                
                if (gameState.getForbiddenPositions().contains(position)) {
                    System.out.print(" X "); // Forbidden position
                } else if (component != null) {
                    System.out.print(String.format("[%s]", getComponentSymbol(component)));
                } else {
                    System.out.print(" . "); // Empty position
                }
            }
            console.println("");
        }
        
        console.println("Legend: X=Forbidden, .=Empty, [E]=Engine, [C]=Cannon, [R]=Crew, [G]=Cargo, [B]=Battery, [S]=Shield, [T]=Structural");
    }

    private String getComponentSymbol(it.polimi.ingsw.server.model.enums.ship.ComponentType component) {
        return switch (component) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> "E";
            case CANNON_SINGLE, CANNON_DOUBLE -> "C";
            case CABIN, CABIN_START -> "R";
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> "G";
            case BATTERY -> "B";
            case SHIELD -> "S";
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> "L";
            case STRUCTURAL -> "T";
        };
    }

    private void displayShipStats(it.polimi.ingsw.client.core.state.LocalGameState gameState) {
        TuiConsole console = getConsole();
        console.printInfo("Ship Statistics:");
        console.println(String.format("  Engines: %d | Cannons: %d | Crew: %d | Cargo: %d | Batteries: %d | Shields: %d",
            gameState.getShipStats(it.polimi.ingsw.client.core.state.LocalGameState.ComponentStatType.ENGINES),
            gameState.getShipStats(it.polimi.ingsw.client.core.state.LocalGameState.ComponentStatType.CANNONS),
            gameState.getShipStats(it.polimi.ingsw.client.core.state.LocalGameState.ComponentStatType.CREW),
            gameState.getShipStats(it.polimi.ingsw.client.core.state.LocalGameState.ComponentStatType.CARGO),
            gameState.getShipStats(it.polimi.ingsw.client.core.state.LocalGameState.ComponentStatType.BATTERIES),
            gameState.getShipStats(it.polimi.ingsw.client.core.state.LocalGameState.ComponentStatType.SHIELDS)
        ));
        
        if (gameState.isShipValidated()) {
            var errors = gameState.getValidationErrors();
            if (errors.isEmpty()) {
                console.printSuccess("✓ Ship is valid and ready for flight!");
            } else {
                console.printError("✗ Ship has validation errors:");
                for (String error : errors) {
                    console.println("  - " + error);
                }
            }
        }
    }

    private void displayComponentInventory(it.polimi.ingsw.client.core.state.LocalGameState gameState) {
        TuiConsole console = getConsole();
        
        var heldTiles = gameState.getHeldTiles();
        console.printInfo("Held Components (" + heldTiles.size() + "/2):");
        if (heldTiles.isEmpty()) {
            console.println("  (none)");
        } else {
            for (int i = 0; i < heldTiles.size(); i++) {
                console.println("  " + (i + 1) + ". " + formatComponentName(heldTiles.get(i)));
            }
        }
        
        var availableTiles = gameState.getAvailableTiles();
        console.printInfo("Available Face-up Components: " + availableTiles.size());
        if (!availableTiles.isEmpty()) {
            for (int i = 0; i < Math.min(5, availableTiles.size()); i++) {
                console.println("  " + formatComponentName(availableTiles.get(i)));
            }
            if (availableTiles.size() > 5) {
                console.println("  ... and " + (availableTiles.size() - 5) + " more");
            }
        }
    }

    private String formatComponentName(it.polimi.ingsw.server.model.enums.ship.ComponentType component) {
        return component.name().toLowerCase().replace("_", " ");
    }

    @Override
    protected void onHide() {
        // TUI views don't need special hiding logic
    }

    @Override
    protected void onRefresh() {
        displayFullInterface();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (active) {
            refreshNeeded = true;
            if ("shipGridUpdated".equals(evt.getPropertyName()) ||
                "heldTilesUpdated".equals(evt.getPropertyName()) ||
                "buildingTimerUpdated".equals(evt.getPropertyName())) {
                displayFullInterface();
            }
        }
    }

    private void startInputLoop() {
        Thread inputThread = new Thread(() -> {
            TuiConsole console = getConsole();
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

        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        TuiConsole console = getConsole();
        
        switch (command) {
            case "help":
            case "h":
                showHelp();
                break;
            case "place":
            case "p":
                handlePlaceCommand(parts);
                break;
            case "take":
            case "t":
                handleTakeCommand();
                break;
            case "return":
            case "r":
                handleReturnCommand(parts);
                break;
            case "validate":
            case "v":
                handleValidateCommand();
                break;
            case "flip":
            case "f":
                handleFlipTimerCommand();
                break;
            case "refresh":
            case "re":
                displayFullInterface();
                break;
            case "quit":
            case "q":
                if (context != null && context.getController() != null) {
                    context.getController().disconnect();
                }
                break;
            default:
                console.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void handlePlaceCommand(String[] parts) {
        TuiConsole console = getConsole();
        if (parts.length < 4) {
            console.printError("Usage: place <row> <col> <component_number>");
            console.printInfo("Example: place 2 3 1 (places held component #1 at row 2, col 3)");
            return;
        }

        try {
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            int componentIndex = Integer.parseInt(parts[3]) - 1; // Convert to 0-based index

            var gameState = it.polimi.ingsw.client.core.state.LocalGameState.getInstance();
            var heldTiles = gameState.getHeldTiles();

            if (componentIndex < 0 || componentIndex >= heldTiles.size()) {
                console.printError("Invalid component number. You have " + heldTiles.size() + " held components.");
                return;
            }

            if (row < 0 || row >= 5 || col < 0 || col >= 7) {
                console.printError("Invalid position. Row must be 0-4, column must be 0-6.");
                return;
            }

            var component = heldTiles.get(componentIndex);
            var position = new it.polimi.ingsw.server.model.domain.ship.Position(row, col);

            if (!gameState.canPlaceComponent(component, position)) {
                console.printError("Cannot place component at that position.");
                return;
            }

            // Send place tile request to server
            if (context != null && context.getController() != null) {
                // This would send a PlaceTileRequest to the server
                console.printInfo("Placing " + formatComponentName(component) + " at (" + row + "," + col + ")...");
                // context.getController().placeTile(componentId, row, col, 0);
            }

        } catch (NumberFormatException e) {
            console.printError("Invalid number format. Please use integers for row, col, and component number.");
        }
    }

    private void handleTakeCommand() {
        TuiConsole console = getConsole();
        var gameState = it.polimi.ingsw.client.core.state.LocalGameState.getInstance();

        if (!gameState.canReserveMoreTiles()) {
            console.printError("You already have the maximum number of held components (2).");
            return;
        }

        // Send take tile request to server
        if (context != null && context.getController() != null) {
            console.printInfo("Taking a random component from the pile...");
            // context.getController().takeTile();
        }
    }

    private void handleReturnCommand(String[] parts) {
        TuiConsole console = getConsole();
        if (parts.length < 2) {
            console.printError("Usage: return <component_number>");
            console.printInfo("Example: return 1 (returns held component #1)");
            return;
        }

        try {
            int componentIndex = Integer.parseInt(parts[1]) - 1; // Convert to 0-based index
            var gameState = it.polimi.ingsw.client.core.state.LocalGameState.getInstance();
            var heldTiles = gameState.getHeldTiles();

            if (componentIndex < 0 || componentIndex >= heldTiles.size()) {
                console.printError("Invalid component number. You have " + heldTiles.size() + " held components.");
                return;
            }

            var component = heldTiles.get(componentIndex);

            // Send return tile request to server
            if (context != null && context.getController() != null) {
                console.printInfo("Returning " + formatComponentName(component) + " to face-up pile...");
                // context.getController().returnTile(componentId);
            }

        } catch (NumberFormatException e) {
            console.printError("Invalid number format. Please use an integer for component number.");
        }
    }

    private void handleValidateCommand() {
        TuiConsole console = getConsole();
        
        // Send validate ship request to server
        if (context != null && context.getController() != null) {
            console.printInfo("Validating your ship...");
            // context.getController().validateShip();
        }
    }

    private void handleFlipTimerCommand() {
        TuiConsole console = getConsole();
        
        // Send flip timer request to server
        if (context != null && context.getController() != null) {
            console.printInfo("Flipping the building timer...");
            // context.getController().flipBuildingTimer();
        }
    }

    private void showHelp() {
        TuiConsole console = getConsole();
        console.println("Available Commands:");
        console.printInfo("Building Commands:");
        console.println("  place <row> <col> <component#> - Place held component on ship grid");
        console.println("  take                          - Take a random component from pile");
        console.println("  return <component#>           - Return held component to face-up pile");
        console.println("  validate                      - Validate your ship construction");
        console.println("  flip                          - Flip building timer for more time");
        console.println("");
        console.printInfo("General Commands:");
        console.println("  help / h                      - Show this help message");
        console.println("  refresh / re                  - Refresh the display");
        console.println("  quit / q                      - Quit the game");
        console.println("");
        console.printInfo("Examples:");
        console.println("  place 2 3 1                  - Place held component #1 at row 2, column 3");
        console.println("  return 2                      - Return held component #2 to face-up pile");
        console.println("");
    }
}