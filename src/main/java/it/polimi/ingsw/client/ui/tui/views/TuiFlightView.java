package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class TuiFlightView extends BaseUIView {
    private final TuiConsole console;
    private final Scanner scanner;
    private volatile boolean refreshNeeded = false;

    // ANSI escape codes for colors
    final String RESET = "\u001B[0m";
    final String BG_RED = "\u001B[48;2;255;0;0m";               // Pure red
    final String BG_YELLOW = "\u001B[48;2;255;255;0m";          // Pure yellow
    final String BG_PURPLE = "\u001B[48;2;128;0;128m";          // Medium purple
    final String BG_BROWN = "\u001B[48;2;139;69;19m";           // Saddle brown
    final String BG_BRIGHT_GREEN = "\u001B[48;2;0;255;127m";    // Spring green

    public TuiFlightView(TuiContext context) {
        this.console = context.getConsole();
        this.scanner = new Scanner(System.in);
        initialize(context);
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.GAME;
    }

    @Override
    public String getTitle() {
        return "Game";
    }

    @Override
    protected void onShow() {
        displayFullInterface();
        startInputLoop();
    }

    @Override
    protected void onHide() {}

    @Override
    protected void onRefresh() {
        displayFullInterface();
    }

    @Override
    protected void onPropertyChange(PropertyChangeEvent evt) {
        refreshNeeded = true;
        switch (evt.getPropertyName()) {
            case "shipGridUpdated":
                displayFullInterface();
                break;
        }
    }

    private void displayFullInterface() {
        console.clearScreen();
        console.printSectionHeader("FLIGHT PHASE");

        ClientState clientState = context.getClientState();

        displayFlightOrder(clientState);
        console.println("");

        displayShipBoard(clientState);
        console.println("");

        displayKey();
        console.println("");

        displayShipStats(clientState);
        console.println("");
    }

    private void displayFlightOrder(ClientState clientState) {
        Map<Player, PlayerFlightData> playerDataMap = clientState.getGameModel().getFlightBoard().getPlayerDataMap();
        int length = clientState.getGameModel().getFlightBoard().getRoute().getLength();
        List<Player> currentOrder = clientState.getGameModel().getFlightBoard().getCurrentOrder();

        String[] flightOrder = new String[length];

        console.println("Scoreboard:");

        int j = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < flightOrder.length; i++) {
            int pos = playerDataMap.get(currentOrder.get(j)).getPosition();
            if (i == pos) {
                sb.append(" ");
            } else {
                sb.append(currentOrder.get(j).getId().getNickname());
                j++;
            }
            sb.append(" | ");
        }
    }

    private void displayShipBoard(ClientState clientState) {
        Component[][] shipBoard = clientState.getLocalPlayerShip().getBoard();
        Set<Position> forbiddenPositions = clientState.getLocalPlayerShip().getForbiddenPositions();

        console.printInfo(String.format("Your Ship (%dx%d grid) - [Symbol/Direction/Connectors]:", shipBoard.length, shipBoard[0].length));
        console.println("");

        StringBuilder sb = new StringBuilder();

        sb.append("\t  4  \t  5  \t  6  \t  7  \t  8  \t  9  \t  10\n");

        for (int row = 0; row < shipBoard.length; row++) {
            sb.append("   ");
            for (int col = 0; col < shipBoard[0].length; col++) {
                Component component = shipBoard[row][col];
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
            for (int col = 0; col < shipBoard[0].length; col++) {
                Component component = shipBoard[row][col];
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
            for (int col = 0; col < shipBoard[0].length; col++) {
                Component component = shipBoard[row][col];
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
        console.println(sb);
        console.println("");
    }

    private void displayKey() {
        console.println("Key:");
        StringBuilder sb = new StringBuilder();
        sb.append("Components: ");
        sb.append("🔋").append(": Battery, ");
        sb.append("⛺️").append(": Start Crew, ");
        sb.append(BG_YELLOW).append("⛺️").append(RESET).append(": Crew, ");
        sb.append("🔫").append(": Single Cannon, ");
        sb.append(BG_BRIGHT_GREEN).append("🔫").append(RESET).append(": Double Cannon, ");
        sb.append("📦️").append(": Common Cargo, ");
        sb.append(BG_RED).append("📦️").append(RESET).append(": Special Cargo, ");
        sb.append("🚀").append(": Single Engine, ");
        sb.append(BG_BRIGHT_GREEN).append("🚀").append(RESET).append(": Double Engine, ");
        sb.append(BG_BROWN).append("🫁️").append(RESET).append(": Brown Life Support, ");
        sb.append(BG_PURPLE).append("🫁️").append(RESET).append(": Purple Life Support, ");
        sb.append("🛡️").append(": Shield, ");
        sb.append("🔗️").append(": Structural Module");
        console.println(sb);

        console.println("Connectors: U=Universal, S=Single, D=Double, P=Plain (no connector)");
        console.println("Directions: U=Up, D=Down, L=Left, R=Right");
    }

    private void displayShipStats(ClientState clientState) {
        console.printInfo("Ship Statistics:");

        Ship ship = clientState.getLocalPlayerShip();
        double engines = ship.getEngines();
        double cannons = ship.getCannons();
        int crew = ship.getCrew();
        int batteries = ship.getBatteries();

        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        sb.append("Engines: ").append(String.format("%.1f", engines)).append(" | ");
        sb.append("Cannons: ").append(String.format("%.1f", cannons)).append(" | ");
        sb.append("Crew: ").append(crew).append(" | ");
        sb.append("Batteries: ").append(batteries);
        console.println(sb);
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

    private void showHelp() {
        console.println("Available Commands:");
        console.println("  (h) help                              - Show this help message");
        console.println("  (q) quit                              - Quit the game");
        console.println("");
    }
}
