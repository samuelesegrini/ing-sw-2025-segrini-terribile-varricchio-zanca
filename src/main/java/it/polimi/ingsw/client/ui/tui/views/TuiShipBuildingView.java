package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.ClientModel;
import it.polimi.ingsw.client.core.state.ComponentInstance;
import it.polimi.ingsw.client.core.state.LocalGameState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.beans.PropertyChangeEvent;
import java.util.Scanner;
import java.util.Set;

/**
 * Pure ComponentInstance-based TUI view for ship building phase.
 * NO ComponentType conversions, NO legacy code, PURE ComponentInstance system.
 * Displays full component information including connectors for proper ship building.
 */
public class TuiShipBuildingView extends BaseUIView {
    private final TuiConsole console;
    private final Scanner scanner;
    private volatile boolean refreshNeeded = false;

    // ANSI escape codes for colors
    final String RESET = "\u001B[0m";

    // Background colors
    final String BG_BLACK = "\u001B[48;2;0;0;0m";           // Pure black
    final String BG_WHITE = "\u001B[48;2;255;255;255m";     // Pure white
    final String BG_RED = "\u001B[48;2;255;0;0m";           // Pure red
    final String BG_BLUE = "\u001B[48;2;0;0;255m";          // Pure blue
    final String BG_GREEN = "\u001B[48;2;0;255;0m";         // Pure green
    final String BG_YELLOW = "\u001B[48;2;255;255;0m";      // Pure yellow
    final String BG_PURPLE = "\u001B[48;2;128;0;128m";      // Medium purple
    final String BG_BROWN = "\u001B[48;2;139;69;19m";       // Saddle brown
    final String BG_VIOLET = "\u001B[48;2;138;43;226m";     // Blue violet
    final String BG_CYAN = "\u001B[48;2;0;255;255m";        // Pure cyan

    final String BG_BRIGHT_BLACK = "\u001B[48;2;128;128;128m";   // Bright black (gray)
    final String BG_BRIGHT_WHITE = "\u001B[48;2;255;255;255m";   // Bright white
    final String BG_BRIGHT_RED = "\u001B[48;2;255;99;71m";       // Bright red (tomato)
    final String BG_BRIGHT_BLUE = "\u001B[48;2;0;191;255m";      // Deep sky blue
    final String BG_BRIGHT_GREEN = "\u001B[48;2;0;255;127m";     // Spring green
    final String BG_BRIGHT_YELLOW = "\u001B[48;2;255;255;102m";  // Bright yellow
    final String BG_BRIGHT_PURPLE = "\u001B[48;2;255;0;255m";    // Bright purple (magenta)
    final String BG_BRIGHT_BROWN = "\u001B[48;2;205;133;63m";    // Bright brown (peru)
    final String BG_BRIGHT_VIOLET = "\u001B[48;2;147;112;219m";  // Bright violet
    final String BG_BRIGHT_CYAN = "\u001B[48;2;0;255;255m";      // Bright cyan

    final String BG_DARK_BLACK = "\u001B[48;2;0;0;0m";           // Pure black
    final String BG_DARK_WHITE = "\u001B[48;2;169;169;169m";     // Dark gray
    final String BG_DARK_RED = "\u001B[48;2;139;0;0m";           // Dark red
    final String BG_DARK_BLUE = "\u001B[48;2;0;0;139m";          // Dark blue
    final String BG_DARK_GREEN = "\u001B[48;2;0;100;0m";         // Dark green
    final String BG_DARK_YELLOW = "\u001B[48;2;184;134;11m";     // Dark goldenrod
    final String BG_DARK_PURPLE = "\u001B[48;2;75;0;130m";       // Indigo
    final String BG_DARK_BROWN = "\u001B[48;2;101;67;33m";       // Dark brown
    final String BG_DARK_VIOLET = "\u001B[48;2;75;0;130m";       // Dark violet
    final String BG_DARK_CYAN = "\u001B[48;2;0;139;139m";        // Dark cyan

    // Text colors
    final String FG_BLACK = "\u001B[38;2;0;0;0m";           // Pure black
    final String FG_WHITE = "\u001B[38;2;255;255;255m";     // Pure white
    final String FG_RED = "\u001B[38;2;255;0;0m";           // Pure red
    final String FG_BLUE = "\u001B[38;2;0;0;255m";          // Pure blue
    final String FG_GREEN = "\u001B[38;2;0;255;0m";         // Pure green
    final String FG_YELLOW = "\u001B[38;2;255;255;0m";      // Pure yellow
    final String FG_PURPLE = "\u001B[38;2;128;0;128m";      // Medium purple
    final String FG_BROWN = "\u001B[38;2;139;69;19m";       // Saddle brown
    final String FG_VIOLET = "\u001B[38;2;138;43;226m";     // Blue violet
    final String FG_CYAN = "\u001B[38;2;0;255;255m";        // Pure cyan

    public TuiShipBuildingView(TuiContext context) {
        this.console = context.getConsole();
        this.scanner = new Scanner(System.in);
        initialize(context);
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

    @Override
    protected void onHide() {
        // TUI views don't need special hiding logic
    }

    @Override
    protected void onRefresh() {
        displayFullInterface();
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        refreshNeeded = true;
        if ("shipGridUpdated".equals(evt.getPropertyName()) ||
                "heldTilesUpdated".equals(evt.getPropertyName()) ||
                "buildingTimerUpdated".equals(evt.getPropertyName())) {
            displayFullInterface();
        }
    }

    private void displayFullInterface() {
        console.clearScreen();
        console.printSectionHeader("SHIP BUILDING PHASE - COMPONENT SYSTEM");

        LocalGameState gameState = LocalGameState.getInstance();

        displayTimer(gameState);
        console.println("");

        displayShipGrid(gameState);
        console.println("");

        displayShipStats(gameState);
        console.println("");

        displayComponentInventory(gameState);
        console.println("");

        displayAvailableComponents(gameState);
        console.println("");

        console.printInfo("Commands: place <row> <col> <component#> | rotate <component#> | take | return <component#> | validate | flip | help | refresh");
        console.println("");

        refreshNeeded = false;
    }

    private void displayTimer(LocalGameState gameState) {
        long timeRemaining = gameState.getBuildingTimeRemaining();
        boolean flipped = gameState.isBuildingTimerFlipped();

        console.println("⌛️ Timer: ");
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

//    private void displayShipGridWithConnectors(LocalGameState gameState) {
//        int gridRows = gameState.getGridRows();
//        int gridCols = gameState.getGridCols();
//        console.printInfo(String.format("Your Ship (%dx%d grid) - [Symbol/Direction/Connectors]:", gridRows, gridCols));
//        console.println("");
//
//        // Header with column numbers
//        System.out.print("    ");
//        for (int col = 0; col < gridCols; col++) {
//            System.out.print(String.format("%6d", col));
//        }
//        System.out.println();
//
//        // Grid rows with component details
//        ComponentInstance[][] shipGrid = gameState.getShipGrid();
//        for (int row = 0; row < gridRows; row++) {
//            System.out.print(String.format("%2d ", row));
//            for (int col = 0; col < gridCols; col++) {
//                ComponentInstance component = shipGrid[row][col];
//                Position position = new Position(row, col);
//
//                if (gameState.getForbiddenPositions().contains(position)) {
//                    System.out.print("  [X] "); // Forbidden position
//                } else if (component != null) {
//                    // Display: [Symbol/Direction]
//                    String symbol = component.getDisplaySymbol();
//                    String direction = component.getCurrentDirection().name().substring(0, 1); // U/D/L/R
//                    System.out.print(String.format("[%s/%s] ", symbol, direction));
//                } else {
//                    System.out.print("  .   "); // Empty position
//                }
//            }
//            console.println("");
//        }
//
//        console.println("");
//        console.println("Legend: X=Forbidden, .=Empty");
//        console.println("Components: [Symbol/Direction] - E=Engine, C=Cannon, R=Crew, G=Cargo, B=Battery, S=Shield, L=LifeSupport, T=Structural");
//        console.println("Directions: U=Up, D=Down, L=Left, R=Right");
//    }

    private void displayShipGrid(LocalGameState gameState) {
        ComponentInstance[][] shipGrid = gameState.getShipGrid();
        Set<Position> forbiddenPositions = gameState.getForbiddenPositions();

        console.printInfo(String.format("Your Ship (%dx%d grid) - [Symbol/Direction/Connectors]:", shipGrid.length, shipGrid[0].length));
        console.println("");

        StringBuilder sb = new StringBuilder();

        sb.append("\t  4  \t  5  \t  6  \t  7  \t  8  \t  9  \t  10\n");

        for (int row = 0; row < shipGrid.length; row++) {
            sb.append("   ");
            for (int col = 0; col < shipGrid[0].length; col++) {
                ComponentInstance component = shipGrid[row][col];
                if (component == null) {
                    sb.append("     ");
                } else {
                    sb.append("  ");
                    switch (component.getConnectorAt(Direction.UP)) {
                        case UNIVERSAL -> sb.append("U");
                        case DOUBLE -> sb.append("D");
                        case SINGLE -> sb.append("S");
                        case PLAIN -> sb.append(" ");
                    }
                    sb.append("  ");
                }
                sb.append("\t");
            }
            sb.append("\n");

            sb.append(" ").append(row + 5).append(" ").append("\t");
            for (int col = 0; col < shipGrid[0].length; col++) {
                ComponentInstance component = shipGrid[row][col];
                if (component == null) {
                    if (forbiddenPositions.contains(new Position(row, col)))
                        sb.append("  🚫️  ");
                    else
                        sb.append("  ⬛️  ");
                } else {
                    sb.append(" ");

                    switch (component.getConnectorAt(Direction.LEFT)) {
                        case UNIVERSAL -> sb.append("U");
                        case DOUBLE -> sb.append("D");
                        case SINGLE -> sb.append("S");
                        case PLAIN -> sb.append(" ");
                    }

                    switch (component.getType()) {
                        case BATTERY -> sb.append("🔋");
                        case CABIN -> sb.append("⛺️");
                        case CABIN_START -> sb.append(BG_YELLOW).append("⛺️").append(RESET);
                        case CANNON_SINGLE -> sb.append("🔫");
                        case CANNON_DOUBLE -> sb.append(BG_BRIGHT_GREEN).append("🔫").append(RESET);
                        case CARGO_HOLD -> sb.append("📦️");
                        case CARGO_HOLD_SPECIAL -> sb.append(BG_RED).append("📦️").append(RESET);
                        case ENGINE_SINGLE -> sb.append("🚀");
                        case ENGINE_DOUBLE -> sb.append(BG_BRIGHT_GREEN).append("🚀").append(RESET);
                        case LIFE_SUPPORT_BROWN -> sb.append(BG_BROWN).append("🫁️").append(RESET);
                        case LIFE_SUPPORT_PURPLE -> sb.append(BG_PURPLE).append("🫁️").append(RESET);
                        case SHIELD -> sb.append("🛡️");
                        case STRUCTURAL -> sb.append("🔗️");
                    }

                    switch (component.getConnectorAt(Direction.RIGHT)) {
                        case UNIVERSAL -> sb.append("U");
                        case DOUBLE -> sb.append("D");
                        case SINGLE -> sb.append("S");
                        case PLAIN -> sb.append(" ");
                    }

                    sb.append(" ");
                }
                sb.append("\t");
            }
            sb.append("\n");

            sb.append("   ");
            for (int col = 0; col < shipGrid[0].length; col++) {
                ComponentInstance component = shipGrid[row][col];
                if (component == null) {
                    sb.append("     ");
                } else {
                    sb.append("  ");
                    switch (component.getConnectorAt(Direction.DOWN)) {
                        case UNIVERSAL -> sb.append("U");
                        case DOUBLE -> sb.append("D");
                        case SINGLE -> sb.append("S");
                        case PLAIN -> sb.append(" ");
                    }
                    sb.append("  ");
                }
                sb.append("\t");
            }
            sb.append("\n");
        }
        System.out.println(sb);

        console.println("");
        console.println("Legend: X=Forbidden, .=Empty");
        console.println("Components: [Symbol/Direction] - E=Engine, C=Cannon, R=Crew, G=Cargo, B=Battery, S=Shield, L=LifeSupport, T=Structural");
        console.println("Directions: U=Up, D=Down, L=Left, R=Right");
    }

    private void displayShipStats(LocalGameState gameState) {
        console.printInfo("Ship Statistics:");
        console.println(String.format("  Engines: %d | Cannons: %d | Crew: %d | Cargo: %d | Batteries: %d | Shields: %d",
                gameState.getShipStats(LocalGameState.ComponentStatType.ENGINES),
                gameState.getShipStats(LocalGameState.ComponentStatType.CANNONS),
                gameState.getShipStats(LocalGameState.ComponentStatType.CREW),
                gameState.getShipStats(LocalGameState.ComponentStatType.CARGO),
                gameState.getShipStats(LocalGameState.ComponentStatType.BATTERIES),
                gameState.getShipStats(LocalGameState.ComponentStatType.SHIELDS)
        ));

        if (gameState.isShipValidated()) {
            var errors = gameState.getValidationErrors();
            if (errors.isEmpty()) {
                console.printSuccess("Ship is valid and ready for flight!");
            } else {
                console.printError("Ship has validation errors:");
                for (String error : errors) {
                    console.println("  - " + error);
                }
            }
        }
    }

    private void displayComponentInventory(LocalGameState gameState) {
        var heldComponents = gameState.getHeldTiles();
        console.printInfo("Held Components (" + heldComponents.size() + "/2):");
        if (heldComponents.isEmpty()) {
            console.println("  (none)");
        } else {
            for (int i = 0; i < heldComponents.size(); i++) {
                ComponentInstance component = heldComponents.get(i);
                console.println(String.format("  %d. %s", (i + 1), formatComponentDetails(component)));
                displayComponentConnectors(component, "     ");
            }
        }
    }

    private void displayAvailableComponents(LocalGameState gameState) {
        var availableComponents = gameState.getAvailableTiles();
        var faceUpComponents = gameState.getFaceUpJunkyardTiles();

        console.printInfo("Face-up Components Available: " + (availableComponents.size() + faceUpComponents.size()));

        // Display available components
        if (!availableComponents.isEmpty()) {
            console.println("  From Warehouse:");
            for (int i = 0; i < Math.min(5, availableComponents.size()); i++) {
                ComponentInstance component = availableComponents.get(i);
                console.println("    - " + formatComponentDetails(component));
            }
            if (availableComponents.size() > 5) {
                console.println("    ... and " + (availableComponents.size() - 5) + " more");
            }
        }

        // Display face-up junkyard components
        if (!faceUpComponents.isEmpty()) {
            console.println("  From Returned Components:");
            for (int i = 0; i < Math.min(3, faceUpComponents.size()); i++) {
                ComponentInstance component = faceUpComponents.get(i);
                console.println("    - " + formatComponentDetails(component));
            }
            if (faceUpComponents.size() > 3) {
                console.println("    ... and " + (faceUpComponents.size() - 3) + " more");
            }
        }
    }

    private String formatComponentDetails(ComponentInstance component) {
        return String.format("%s [%s] (ID: %s, Dir: %s)",
                component.getDisplayName(),
                component.getDisplaySymbol(),
                component.getId().substring(Math.max(0, component.getId().length() - 8)), // Last 8 chars of ID
                component.getCurrentDirection().name()
        );
    }

    private void displayComponentConnectors(ComponentInstance component, String indent) {
        console.println(indent + "Connectors:");
        for (Direction dir : Direction.values()) {
            ConnectorType connector = component.getConnectorAt(dir);
            if (connector != ConnectorType.PLAIN) {
                console.println(String.format("%s  %s: %s", indent, dir.name(), connector.name()));
            }
        }
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

        String[] tokens = input.trim().split("\\s+");
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "h":
            case "help":
                showHelp();
                break;
            case "p":
            case "place":
                handlePlaceCommand(tokens);
                break;
            case "rot":
            case "rotate":
                handleRotateCommand(tokens);
                break;
            case "t":
            case "take":
                handleTakeCommand();
                break;
            case "r":
            case "return":
                handleReturnCommand(tokens);
                break;
            case "v":
            case "validate":
                handleValidateCommand();
                break;
            case "f":
            case "flip":
                handleFlipTimerCommand();
                break;
            case "re":
            case "refresh":
                displayFullInterface();
                break;
            case "i":
            case "info":
                handleInfoCommand(tokens);
                break;
            case "q":
            case "quit":
                if (context != null && context.getController() != null) {
                    context.getController().disconnect();
                }
                break;
            default:
                console.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void handlePlaceCommand(String[] tokens) {
        if (tokens.length != 4) {
            console.printError("Usage: place <row> <col> <component_number>");
            console.printInfo("Example: place 5 6 1 (places held component #1 at row 5, col 6)");
            return;
        }

        try {
            int row = Integer.parseInt(tokens[1]) - 5;
            int col = Integer.parseInt(tokens[2]) - 4;
            int componentIndex = Integer.parseInt(tokens[3]) - 1;

            LocalGameState gameState = LocalGameState.getInstance();
            var heldComponents = gameState.getHeldTiles();

            if (componentIndex < 0 || componentIndex >= heldComponents.size()) {
                console.printError("Invalid component number. You have " + heldComponents.size() + " held components.");
                return;
            }

            if (row < 0 || row >= gameState.getGridRows() || col < 0 || col >= gameState.getGridCols()) {
                console.printError("Invalid position. Row must be 0-" + (gameState.getGridRows()-1) +
                        ", column must be 0-" + (gameState.getGridCols()-1) + ".");
                return;
            }

            ComponentInstance component = heldComponents.get(componentIndex);
            Position position = new Position(row, col);

            if (!gameState.canPlaceComponent(component, position)) {
                console.printError("Cannot place component at that position. Check connectors and placement rules.");
                displayComponentConnectors(component, "Component connectors: ");
                return;
            }

            // Send place tile request to server
            if (context != null && context.getController() != null) {
                console.printInfo("Placing " + formatComponentDetails(component) + " at (" + row + "," + col + ")...");
                context.getController().placeTile(component.getId(), row, col, component.getCurrentDirection().ordinal());
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid number format. Please use integers for row, col, and component number.");
        }
    }

    private void handleRotateCommand(String[] tokens) {
        if (tokens.length != 2) {
            console.printError("Usage: rotate <component_number>");
            console.printInfo("Example: rotate 1 (rotates held component #1 clockwise)");
            return;
        }

        try {
            int componentIndex = Integer.parseInt(tokens[1]) - 1;
            LocalGameState gameState = LocalGameState.getInstance();
            var heldComponents = gameState.getHeldTiles();

            if (componentIndex < 0 || componentIndex >= heldComponents.size()) {
                console.printError("Invalid component number. You have " + heldComponents.size() + " held components.");
                return;
            }

            ComponentInstance component = heldComponents.get(componentIndex);
            Direction oldDirection = component.getCurrentDirection();
            component.rotate();
            Direction newDirection = component.getCurrentDirection();

            console.printSuccess("Rotated " + component.getDisplayName() + " from " + oldDirection + " to " + newDirection);
            displayComponentConnectors(component, "New connectors: ");

        } catch (NumberFormatException e) {
            console.printError("Invalid number format. Please use an integer for component number.");
        }
    }

    private void handleTakeCommand() {
        LocalGameState gameState = LocalGameState.getInstance();

        if (!gameState.canReserveMoreTiles()) {
            console.printError("You already have the maximum number of held components (2).");
            return;
        }

        // Send take tile request to server
        if (context != null && context.getController() != null) {
            console.printInfo("Taking a random component from the pile...");
            context.getController().takeTile();
        }
    }

    private void handleReturnCommand(String[] tokens) {
        if (tokens.length != 2) {
            console.printError("Usage: return <component_number>");
            console.printInfo("Example: return 1 (returns held component #1)");
            return;
        }

        try {
            int componentIndex = Integer.parseInt(tokens[1]) - 1;
            LocalGameState gameState = LocalGameState.getInstance();
            var heldComponents = gameState.getHeldTiles();

            if (componentIndex < 0 || componentIndex >= heldComponents.size()) {
                console.printError("Invalid component number. You have " + heldComponents.size() + " held components.");
                return;
            }

            ComponentInstance component = heldComponents.get(componentIndex);

            // Send return tile request to server
            if (context != null && context.getController() != null) {
                console.printInfo("Returning " + formatComponentDetails(component) + " to face-up pile...");
                context.getController().returnTile(component.getId());
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid number format. Please use an integer for component number.");
        }
    }

    private void handleValidateCommand() {
        if (context != null && context.getController() != null) {
            console.printInfo("Validating your ship...");
            context.getController().validateShip();
        }
    }

    private void handleFlipTimerCommand() {
        if (context != null && context.getController() != null) {
            console.printInfo("Flipping the building timer...");
            context.getController().flipBuildingTimer();
        }
    }

    private void handleInfoCommand(String[] tokens) {
        if (tokens.length != 2) {
            console.printError("Usage: info <component_number>");
            console.printInfo("Example: info 1 (shows detailed info for held component #1)");
            return;
        }

        try {
            int componentIndex = Integer.parseInt(tokens[1]) - 1;
            LocalGameState gameState = LocalGameState.getInstance();
            var heldComponents = gameState.getHeldTiles();

            if (componentIndex < 0 || componentIndex >= heldComponents.size()) {
                console.printError("Invalid component number. You have " + heldComponents.size() + " held components.");
                return;
            }

            ComponentInstance component = heldComponents.get(componentIndex);

            console.printInfo("=== Component Details ===");
            console.println("Name: " + component.getDisplayName());
            console.println("Type: " + component.getType());
            console.println("ID: " + component.getId());
            console.println("Current Direction: " + component.getCurrentDirection());
            console.println("Symbol: " + component.getDisplaySymbol());
            displayComponentConnectors(component, "");
            console.println("========================");
        } catch (NumberFormatException e) {
            console.printError("Invalid number format. Please use an integer for component number.");
        }
    }

    private void showHelp() {
        console.println("Available Commands:");
        console.printInfo("Building Commands:");
        console.println("  (p) place <row> <col> <#component>    - Place held component on ship grid");
        console.println("  (rot) rotate <component#>             - Rotate held component clockwise");
        console.println("  (t) take                              - Take a random component from pile");
        console.println("  (r) return <component#>               - Return held component to face-up pile");
        console.println("  (v) validate                          - Validate your ship construction");
        console.println("  (f) flip                              - Flip building timer for more time");
        console.println("");
        console.printInfo("Information Commands:");
        console.println("  (i) info <component#>                 - Show detailed component information");
        console.println("  (h) help                              - Show this help message");
        console.println("  (re) refresh                          - Refresh the display");
        console.println("  (q) quit                              - Quit the game");
        console.println("");
        console.printInfo("Examples:");
        console.println("  place 5 6 1                           - Place held component #1 at row 5, column 6");
        console.println("  rotate 1                              - Rotate held component #1 clockwise");
        console.println("  info 2                                - Show details for held component #2");
        console.println("  return 2                              - Return held component #2 to face-up pile");
        console.println("");
        console.printInfo("Component Display:");
        console.println("  Ship grid shows: [Symbol/Direction]");
        console.println("  Connectors: UNIVERSAL (connects to any), SINGLE, DOUBLE, PLAIN (no connection)");
        console.println("");
    }
}