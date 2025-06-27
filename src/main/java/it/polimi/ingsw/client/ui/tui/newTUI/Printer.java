package it.polimi.ingsw.client.ui.tui.newTUI;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.GamePhase;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.util.List;

/**
 * The Printer class provides functionality for printing all messages and game displays for the TUI.
 */
public class Printer {

    public Printer() {
        AnsiConsole.systemInstall();
    }

    public void shutdown() {
        AnsiConsole.systemUninstall();
    }

    public void printHeader() {
        System.out.println(Ansi.ansi()
                .fgBrightCyan()
                .a("╔═══════════════════════════════════════════════════════════════╗")
                .reset());
        System.out.println(Ansi.ansi()
                .fgBrightCyan()
                .a("║                     ")
                .fgBrightYellow().bold()
                .a("GALAXY TRUCKER CLIENT")
                .reset().fgBrightCyan()
                .a("                     ║")
                .reset());
        System.out.println(Ansi.ansi()
                .fgBrightCyan()
                .a("╚═══════════════════════════════════════════════════════════════╝")
                .reset());
        System.out.println();
    }

    public void printSectionHeader(String title) {
        int width = 60;
        String paddedTitle = center(title, width - 2);
        StringBuilder sb = new StringBuilder();
        sb.append(Ansi.ansi().fgBrightBlue().append("┌").append("─".repeat(width - 2)).append("┐").reset());
        sb.append(Ansi.ansi().fgBrightBlue().a("│").bold().a(paddedTitle).reset().fgBrightBlue().a("│").reset());
        sb.append(Ansi.ansi().fgBrightBlue().append("└").append("─".repeat(width - 2)).append("┘").reset());
        System.out.println();
    }

    // --- Message Printing ---
    public void printError(String message) {
        System.out.println(Ansi.ansi().fgBrightRed().a("[ERROR] ").reset().a(message));
    }

    public void printSuccess(String message) {
        System.out.println(Ansi.ansi().fgBrightGreen().a("[SUCCESS] ").reset().a(message));
    }

    public void printInfo(String message) {
        System.out.println(Ansi.ansi().fgBrightCyan().a("[INFO] ").reset().a(message));
    }

    public void printWarning(String message) {
        System.out.println(Ansi.ansi().fgBrightYellow().a("[WARNING] ").reset().a(message));
    }

    public void printLoading(String message) {
        System.out.println(Ansi.ansi().fgCyan().a(message + "...").reset());
    }


    // --- View Specific Prompts & Displays ---

    public void printConnectionPrompt() {
        printSectionHeader("CONNECTION");
        printInfo("Please enter the server address to connect.");
        System.out.print("Server IP:Port > ");
    }

    public void printLoginPrompt() {
        printSectionHeader("LOGIN");
        printSuccess("Connected to server successfully!");
        printInfo("Please choose a nickname (3-20 characters, letters, numbers, _, -).");
        System.out.print("Enter nickname: ");
    }

    public void printLobby(ClientState clientState) {
        clearScreen();
        printSectionHeader("LOBBY - Welcome " + clientState.getCurrentNickname());

        printInfo("Joinable Games:");
        List<GameInfo> joinableGames = clientState.getJoinableGames();
        if (joinableGames.isEmpty()) {
            System.out.println("No games available to join.");
        } else {
            String[] headers = {"GAME ID", "GAME NAME", "LEVEL", "PLAYERS"};
            String[][] data = joinableGames.stream()
                    .map(g -> new String[]{g.getGameId(), g.getGameName(), g.getGameLevel().toString(), g.getCurrentPlayerCount() + "/" + g.getMaxPlayers()})
                    .toArray(String[][]::new);
            printTable(headers, data);
        }
        System.out.println();
        printLobbyCommands();
        System.out.print("Lobby > ");
    }

    public void printLobbyCommands() {
        printInfo("Available Commands:");
        System.out.println("  create <name> <players> <level> - Create a game (e.g., create my_game 4 LEVEL_II)");
        System.out.println("  join <gameId>                   - Join an existing game");
        System.out.println("  refresh                         - Refresh the list of games");
        System.out.println("  help                            - Show this help message");
        System.out.println();
    }

    public void printShipBuildingInterface(ClientState clientState, Component heldComponent) {
        clearScreen();
        printSectionHeader("SHIP BUILDING");

        Ship ship = clientState.getLocalPlayerShip();
        if (ship == null) {
            printError("Ship data not available.");
            return;
        }

        printShipGrid(ship);
        System.out.println();
        printHeldComponent(heldComponent);
        System.out.println();
        printComponentDeck(clientState.getComponentDeck());
        System.out.println();
        printShipStats(ship);
        System.out.println();
        printShipBuildingCommands();
        System.out.print("Shipyard > ");
    }

    private void printShipGrid(Ship ship) {
        System.out.println("🚀 YOUR SHIP GRID:");
        // Column headers
        System.out.print("    ");
        for (int col = 0; col < ship.getCols(); col++) {
            System.out.print(String.format(" %d  ", col));
        }
        System.out.println();

        for (int row = 0; row < ship.getRows(); row++) {
            System.out.print(String.format("%d │ ", row));
            for (int col = 0; col < ship.getCols(); col++) {
                Component component = ship.getComponent(new Position(row, col));
                if (component != null) {
                    System.out.print(getComponentAscii(component));
                } else if (ship.isForbiddenPosition(new Position(row, col))) {
                    System.out.print("███"); // Forbidden
                } else {
                    System.out.print(" . "); // Empty
                }
                System.out.print("│");
            }
            System.out.println();
        }
    }

    private String getComponentAscii(Component component) {
        if (component == null) return "   ";
        return switch (component.getType()) {
            case CABIN, CABIN_START -> "🏠 ";
            case ENGINE_SINGLE, ENGINE_DOUBLE -> "🔥 ";
            case CANNON_SINGLE, CANNON_DOUBLE -> "💥 ";
            case SHIELD -> "🛡 ";
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> "📦 ";
            case BATTERY -> "🔋 ";
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> "🫁 ";
            case STRUCTURAL -> "⚙️ ";
            default -> "⚪️ ";
        } + " ";
    }

    private void printHeldComponent(Component heldComponent) {
        System.out.println("✋ HELD COMPONENT:");
        if (heldComponent == null) {
            System.out.println("  (none) - Use 'take' to draw a component.");
        } else {
            System.out.println("  Type: " + heldComponent.getType() + " " + getComponentAscii(heldComponent));
            System.out.println("  Direction: " + heldComponent.getCurrentDirection());
        }
    }

    private void printComponentDeck(ComponentDeck deck) {
        System.out.println("📚 COMPONENT DECK:");
        if (deck == null) {
            System.out.println("  Deck data not available.");
            return;
        }
        System.out.println(String.format("  Draw pile: %d | Face-up: %d | Discarded: %d",
                deck.getRemainingCards(), deck.getFaceUpCount(), deck.getDiscardedCards()));
    }

    private void printShipStats(Ship ship) {
        System.out.println("📊 SHIP STATISTICS:");
        System.out.println(String.format("  Engines: %d | Cannons: %d | Crew: %d | Cargo: %d | Batteries: %d | Shields: %d",
                ship.getEngineCount(), ship.getCannonCount(), ship.getCrewCapacity(),
                ship.getCargoCapacity(), ship.getBatteryCount(), ship.getShieldCount()));
    }

    public void printShipBuildingCommands() {
        printInfo("Available Commands:");
        System.out.println("  take                   - Draw a component");
        System.out.println("  place <row> <col>      - Place held component");
        System.out.println("  rotate                 - Rotate held component");
        System.out.println("  return                 - Return held component to deck");
        System.out.println("  validate               - Validate ship construction");
        System.out.println("  flip                   - Flip building timer");
        System.out.println("  help                   - Show commands again");
        System.out.println();
    }


    // --- UTILITY METHODS ---

    private void clearScreen() {
        System.out.print(Ansi.ansi().eraseScreen().cursor(1, 1));
        System.out.flush();
    }

    private String center(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = width - text.length();
        int leftPad = padding / 2;
        int rightPad = padding - leftPad;
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }

    public void printTable(String[] headers, String[][] data) {
        if (headers.length == 0) return;

        // Calculate column widths
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
            for (String[] row : data) {
                if (i < row.length && row[i] != null) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }
        }

        printTableSeparator(widths, '┌', '┬', '┐');
        printTableRow(headers, widths, true);
        printTableSeparator(widths, '├', '┼', '┤');

        if (data.length == 0) {
            // Print a message for empty table
        } else {
            for (String[] row : data) {
                printTableRow(row, widths, false);
            }
        }
        printTableSeparator(widths, '└', '┴', '┘');
    }

    private void printTableRow(String[] row, int[] widths, boolean isHeader) {
        StringBuilder rowLine = new StringBuilder();
        rowLine.append("│");
        for (int i = 0; i < row.length; i++) {
            String cell = (row[i] == null) ? "" : row[i];
            String centered = center(cell, widths[i]);
            String formattedCell = String.format(" %s ", centered);
            if (isHeader) {
                rowLine.append(Ansi.ansi().bold().a(formattedCell).reset());
            } else {
                rowLine.append(formattedCell);
            }
            rowLine.append("│");
        }
        System.out.println(rowLine.toString());
    }

    private void printTableSeparator(int[] widths, char left, char mid, char right) {
        StringBuilder separatorLine = new StringBuilder();
        separatorLine.append(left);
        for (int i = 0; i < widths.length; i++) {
            separatorLine.append("─".repeat(widths[i] + 2));
            if (i < widths.length - 1) {
                separatorLine.append(mid);
            }
        }
        separatorLine.append(right);
        System.out.println(separatorLine.toString());
    }
}