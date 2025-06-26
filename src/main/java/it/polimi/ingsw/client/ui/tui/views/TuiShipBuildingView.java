//package it.polimi.ingsw.client.ui.tui.views;
//
//import it.polimi.ingsw.client.core.ClientState;
//import it.polimi.ingsw.client.controller.ClientController;
//import it.polimi.ingsw.client.ui.core.BaseUIView;
//import it.polimi.ingsw.client.ui.core.UIContext;
//import it.polimi.ingsw.client.core.UIRefreshable;
//import it.polimi.ingsw.client.ui.tui.TuiConsole;
//import it.polimi.ingsw.client.ui.tui.TuiContext;
//import it.polimi.ingsw.server.core.GameSession;
//import it.polimi.ingsw.server.model.domain.ship.Position;
//import it.polimi.ingsw.server.model.domain.ship.Ship;
//import it.polimi.ingsw.server.model.domain.ship.components.Component;
//import it.polimi.ingsw.server.model.enums.ship.Direction;
//
//import java.beans.PropertyChangeEvent;
//import java.util.Scanner;
//import java.util.Set;
//
//public class TuiShipBuildingView extends BaseUIView implements UIRefreshable {
//    private final TuiConsole console;
//    private final Scanner scanner;
//    private volatile boolean refreshNeeded = false;
//
//    private final String currentPlayerId;
//
//    // ANSI escape codes for colors
//    final String RESET = "\u001B[0m";
//    final String BG_RED = "\u001B[48;2;255;0;0m";               // Pure red
//    final String BG_YELLOW = "\u001B[48;2;255;255;0m";          // Pure yellow
//    final String BG_PURPLE = "\u001B[48;2;128;0;128m";          // Medium purple
//    final String BG_BROWN = "\u001B[48;2;139;69;19m";           // Saddle brown
//    final String BG_BRIGHT_GREEN = "\u001B[48;2;0;255;127m";    // Spring green
//
//    public TuiShipBuildingView(TuiContext context) {
//        this.console = context.getConsole();
//        this.scanner = new Scanner(System.in);
//        initialize(context);
//
//        this.currentPlayerId = context.getController().getPlayerId();
//    }
//
//    @Override
//    public ClientState.ViewState getViewState() {
//        return ClientState.ViewState.GAME;
//    }
//
//    @Override
//    public String getTitle() {
//        return "Ship Building (TUI)";
//    }
//
//    @Override
//    protected void onShow() {
//        displayFullInterface();
//        startInputLoop();
//    }
//
//    @Override
//    protected void onHide() {}
//
//    @Override
//    protected void onRefresh() {
//        refresh();
//    }
//
//    @Override
//    public void refresh() {
//        if (context != null && context.getClientState() != null && context.getClientState().isInGame()) {
//            System.out.println("[TUI] Refreshing ship building display...");
//            displayFullInterface();
//        }
//    }
//
//    private void displayFullInterface() {
//        console.clearScreen();
//        console.printSectionHeader("SHIP BUILDING PHASE");
//
//        ClientState clientState = context.getClientState();
//
//        displayTimer(clientState);
//        console.println("");
//
//        displayShipBoard(clientState);
//        console.println("");
//
//        displayKey();
//        console.println("");
//
//        displayShipStats(clientState);
//        console.println("");
//
//        displayComponentInventory(clientState);
//        console.println("");
//
//        console.printInfo("Commands: place <row> <col> <component> | take | return <component> | validate | flip | help | refresh");
//        console.println("");
//
//        refreshNeeded = false;
//    }
//
//    private void displayTimer(ClientState clientState) {
//        long timeRemaining = clientState.getGameModel().getBuildingTimeRemaining();
//        boolean flipped = clientState.getGameModel().isBuildingTimerFlipped();
//
//        console.println("⏱ Timer: ");
//        if (timeRemaining > 0) {
//            long seconds = timeRemaining / 1000;
//            if (seconds < 30) {
//                console.printError(String.format("%d seconds%s", seconds, flipped ? " (FLIPPED)" : ""));
//            } else {
//                console.printWarning(String.format("%d seconds%s", seconds, flipped ? " (FLIPPED)" : ""));
//            }
//        } else {
//            console.printError("TIME UP!");
//        }
//    }
//
//    private void displayShipBoard(ClientState clientState) {
//        Component[][] shipBoard = clientState.getLocalPlayerShip().getBoard();
//        Set<Position> forbiddenPositions = clientState.getLocalPlayerShip().getForbiddenPositions();
//
//        console.printInfo(String.format("Your Ship (%dx%d grid) - [Symbol/Direction/Connectors]:", shipBoard.length, shipBoard[0].length));
//        console.println("");
//
//        StringBuilder sb = new StringBuilder();
//
//        sb.append("\t  4  \t  5  \t  6  \t  7  \t  8  \t  9  \t  10\n");
//
//        for (int row = 0; row < shipBoard.length; row++) {
//            sb.append("   ");
//            for (int col = 0; col < shipBoard[0].length; col++) {
//                Component component = shipBoard[row][col];
//                if (component == null) {
//                    sb.append("     ");
//                } else {
//                    sb.append("  ");
//                    switch (component.getConnectorAt(Direction.UP)) {
//                        case UNIVERSAL -> sb.append("U");
//                        case DOUBLE -> sb.append("D");
//                        case SINGLE -> sb.append("S");
//                        case PLAIN -> sb.append(" ");
//                    }
//                    sb.append("  ");
//                }
//                sb.append("\t");
//            }
//            sb.append("\n");
//
//            sb.append(" ").append(row + 5).append(" ").append("\t");
//            for (int col = 0; col < shipBoard[0].length; col++) {
//                Component component = shipBoard[row][col];
//                if (component == null) {
//                    if (forbiddenPositions.contains(new Position(row, col)))
//                        sb.append("  🚫️  ");
//                    else
//                        sb.append("  ⬛️  ");
//                } else {
//                    sb.append(" ");
//
//                    switch (component.getConnectorAt(Direction.LEFT)) {
//                        case UNIVERSAL -> sb.append("U");
//                        case DOUBLE -> sb.append("D");
//                        case SINGLE -> sb.append("S");
//                        case PLAIN -> sb.append(" ");
//                    }
//
//                    switch (component.getType()) {
//                        case BATTERY -> sb.append("🔋");
//                        case CABIN -> sb.append("⛺️");
//                        case CABIN_START -> sb.append(BG_YELLOW).append("⛺️").append(RESET);
//                        case CANNON_SINGLE -> sb.append("🔫");
//                        case CANNON_DOUBLE -> sb.append(BG_BRIGHT_GREEN).append("🔫").append(RESET);
//                        case CARGO_HOLD -> sb.append("📦️");
//                        case CARGO_HOLD_SPECIAL -> sb.append(BG_RED).append("📦️").append(RESET);
//                        case ENGINE_SINGLE -> sb.append("🚀");
//                        case ENGINE_DOUBLE -> sb.append(BG_BRIGHT_GREEN).append("🚀").append(RESET);
//                        case LIFE_SUPPORT_BROWN -> sb.append(BG_BROWN).append("🫁️").append(RESET);
//                        case LIFE_SUPPORT_PURPLE -> sb.append(BG_PURPLE).append("🫁️").append(RESET);
//                        case SHIELD -> sb.append("🛡️");
//                        case STRUCTURAL -> sb.append("🔗️");
//                    }
//
//                    switch (component.getConnectorAt(Direction.RIGHT)) {
//                        case UNIVERSAL -> sb.append("U");
//                        case DOUBLE -> sb.append("D");
//                        case SINGLE -> sb.append("S");
//                        case PLAIN -> sb.append(" ");
//                    }
//
//                    sb.append(" ");
//                }
//                sb.append("\t");
//            }
//            sb.append("\n");
//
//            sb.append("   ");
//            for (int col = 0; col < shipBoard[0].length; col++) {
//                Component component = shipBoard[row][col];
//                if (component == null) {
//                    sb.append("     ");
//                } else {
//                    sb.append("  ");
//                    switch (component.getConnectorAt(Direction.DOWN)) {
//                        case UNIVERSAL -> sb.append("U");
//                        case DOUBLE -> sb.append("D");
//                        case SINGLE -> sb.append("S");
//                        case PLAIN -> sb.append(" ");
//                    }
//                    sb.append("  ");
//                }
//                sb.append("\t");
//            }
//            sb.append("\n");
//        }
//        console.println(sb);
//        console.println("");
//    }
//
//    private void displayKey() {
//        console.println("Key:");
//        StringBuilder sb = new StringBuilder();
//        sb.append("Components: ");
//        sb.append("🔋").append(": Battery, ");
//        sb.append("⛺️").append(": Start Crew, ");
//        sb.append(BG_YELLOW).append("⛺️").append(RESET).append(": Crew, ");
//        sb.append("🔫").append(": Single Cannon, ");
//        sb.append(BG_BRIGHT_GREEN).append("🔫").append(RESET).append(": Double Cannon, ");
//        sb.append("📦️").append(": Common Cargo, ");
//        sb.append(BG_RED).append("📦️").append(RESET).append(": Special Cargo, ");
//        sb.append("🚀").append(": Single Engine, ");
//        sb.append(BG_BRIGHT_GREEN).append("🚀").append(RESET).append(": Double Engine, ");
//        sb.append(BG_BROWN).append("🫁️").append(RESET).append(": Brown Life Support, ");
//        sb.append(BG_PURPLE).append("🫁️").append(RESET).append(": Purple Life Support, ");
//        sb.append("🛡️").append(": Shield, ");
//        sb.append("🔗️").append(": Structural Module");
//        console.println(sb);
//
//        console.println("Connectors: U=Universal, S=Single, D=Double, P=Plain (no connector)");
//        console.println("Directions: U=Up, D=Down, L=Left, R=Right");
//    }
//
//    private void displayShipStats(ClientState clientState) {
//        console.printInfo("Ship Statistics:");
//
//        Ship ship = clientState.getLocalPlayerShip();
//        double engines = ship.getEngines();
//        double cannons = ship.getCannons();
//        int crew = ship.getCrew();
//        int batteries = ship.getBatteries();
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("\t");
//        sb.append("Engines: ").append(String.format("%.1f", engines)).append(" | ");
//        sb.append("Cannons: ").append(String.format("%.1f", cannons)).append(" | ");
//        sb.append("Crew: ").append(crew).append(" | ");
//        sb.append("Batteries: ").append(batteries);
//        console.println(sb);
//    }
//
//    private void displayComponentInventory(ClientState clientState) {
//        GameSession.ShipBuildingSyncState syncState = gameSession.getShipBuildingSyncState(playerId);
//        var heldTiles = clientState.getHeldTiles();
//        console.printInfo("Held Components (" + heldTiles.size() + "/2):");
//        if (heldTiles.isEmpty()) {
//            console.println("  (none)");
//        } else {
//            for (int i = 0; i < heldTiles.size(); i++) {
//                console.println("  " + (i + 1) + ". " + formatComponentName(heldTiles.get(i)));
//            }
//        }
//
//        var availableTiles = clientState.getAvailableTiles();
//        console.printInfo("Available Face-up Components: " + availableTiles.size());
//        if (!availableTiles.isEmpty()) {
//            for (int i = 0; i < Math.min(5, availableTiles.size()); i++) {
//                console.println("  " + formatComponentName(availableTiles.get(i)));
//            }
//            if (availableTiles.size() > 5) {
//                console.println("  ... and " + (availableTiles.size() - 5) + " more");
//            }
//        }
//    }
//
//    private void startInputLoop() {
//        Thread inputThread = new Thread(() -> {
//            while (active) {
//                try {
//                    console.println("Enter command: ");
//                    String input = scanner.nextLine();
//                    handleInput(input);
//                } catch (Exception e) {
//                    break;
//                }
//            }
//        });
//        inputThread.setDaemon(true);
//        inputThread.start();
//    }
//
//    private void handleInput(String input) {
//        if (input == null || input.trim().isEmpty()) {
//            return;
//        }
//
//        String[] parts = input.trim().split("\\s+");
//        String command = parts[0].toLowerCase();
//
//        switch (command) {
//            case "h":
//            case "help":
//                showHelp();
//                break;
//            case "p":
//            case "place":
//                handlePlaceCommand(parts);
//                break;
//            case "t":
//            case "take":
//                handleTakeCommand();
//                break;
//            case "r":
//            case "return":
//                handleReturnCommand(parts);
//                break;
//            case "v":
//            case "validate":
//                handleValidateCommand();
//                break;
//            case "f":
//            case "flip":
//                handleFlipTimerCommand();
//                break;
//            case "q":
//            case "quit":
//                if (context != null && context.getController() != null) {
//                    context.getController().disconnect();
//                }
//                break;
//            default:
//                console.printError("Unknown command: " + command + ". Type 'help' for available commands.");
//                break;
//        }
//    }
//
//    private void handlePlaceCommand(String[] parts) {
//        if (parts.length < 4) {
//            console.printError("Usage: place <row> <col> <component_number>");
//            console.printInfo("Example: place 2 3 1 (places held component #1 at row 2, col 3)");
//            return;
//        }
//
//        try {
//            int row = Integer.parseInt(parts[1]);
//            int col = Integer.parseInt(parts[2]);
//            int componentIndex = Integer.parseInt(parts[3]) - 1; // Convert to 0-based index
//
//            ClientState clientState = context.getClientState();
//            var heldTiles = clientState.getHeldTiles();
//
//            if (componentIndex < 0 || componentIndex >= heldTiles.size()) {
//                console.printError("Invalid component number. You have " + heldTiles.size() + " held components.");
//                return;
//            }
//
//            if (row < 0 || row >= 5 || col < 0 || col >= 7) {
//                console.printError("Invalid position. Row must be 0-4, column must be 0-6.");
//                return;
//            }
//
//            var component = heldTiles.get(componentIndex);
//            var position = new it.polimi.ingsw.server.model.domain.ship.Position(row, col);
//
//            if (!clientState.canPlaceComponent(component, position)) {
//                console.printError("Cannot place component at that position.");
//                return;
//            }
//
//            // Send place tile request to server
//            if (context != null && context.getController() != null) {
//                // This would send a PlaceTileRequest to the server
//                console.printInfo("Placing " + formatComponentName(component) + " at (" + row + "," + col + ")...");
//                // context.getController().placeTile(componentId, row, col, 0);
//            }
//
//        } catch (NumberFormatException e) {
//            console.printError("Invalid number format. Please use integers for row, col, and component number.");
//        }
//    }
//
//    private void handleTakeCommand() {
//        ClientState clientState = context.getClientState();
//
//        if (!clientState.canReserveMoreTiles()) {
//            console.printError("You already have the maximum number of held components (2).");
//            return;
//        }
//
//        // Send take tile request to server
//        if (context != null && context.getController() != null) {
//            console.printInfo("Taking a random component from the pile...");
//            // context.getController().takeTile();
//        }
//    }
//
//    private void handleReturnCommand(String[] parts) {
//        if (parts.length < 2) {
//            console.printError("Usage: return <component_number>");
//            console.printInfo("Example: return 1 (returns held component #1)");
//            return;
//        }
//
//        try {
//            int componentIndex = Integer.parseInt(parts[1]) - 1; // Convert to 0-based index
//            ClientState clientState = context.getClientState();
//            var heldTiles = clientState.getHeldTiles();
//
//            if (componentIndex < 0 || componentIndex >= heldTiles.size()) {
//                console.printError("Invalid component number. You have " + heldTiles.size() + " held components.");
//                return;
//            }
//
//            var component = heldTiles.get(componentIndex);
//
//            // Send return tile request to server
//            if (context != null && context.getController() != null) {
//                console.printInfo("Returning " + formatComponentName(component) + " to face-up pile...");
//                // context.getController().returnTile(componentId);
//            }
//
//        } catch (NumberFormatException e) {
//            console.printError("Invalid number format. Please use an integer for component number.");
//        }
//    }
//
//    private void handleValidateCommand() {
//
//        // Send validate ship request to server
//        if (context != null && context.getController() != null) {
//            console.printInfo("Validating your ship...");
//            // context.getController().validateShip();
//        }
//    }
//
//    private void handleFlipTimerCommand() {
//
//        // Send flip timer request to server
//        if (context != null && context.getController() != null) {
//            console.printInfo("Flipping the building timer...");
//            // context.getController().flipBuildingTimer();
//        }
//    }
//
//    private void showHelp() {
//        console.println("Available Commands:");
//        console.printInfo("Building Commands:");
//        console.println("  place <row> <col> <component#> - Place held component on ship grid");
//        console.println("  take                          - Take a random component from pile");
//        console.println("  return <component#>           - Return held component to face-up pile");
//        console.println("  validate                      - Validate your ship construction");
//        console.println("  flip                          - Flip building timer for more time");
//        console.println("");
//        console.printInfo("General Commands:");
//        console.println("  help / h                      - Show this help message");
//        console.println("  refresh / re                  - Refresh the display");
//        console.println("  quit / q                      - Quit the game");
//        console.println("");
//        console.printInfo("Examples:");
//        console.println("  place 2 3 1                  - Place held component #1 at row 2, column 3");
//        console.println("  return 2                      - Return held component #2 to face-up pile");
//        console.println("");
//    }
//}