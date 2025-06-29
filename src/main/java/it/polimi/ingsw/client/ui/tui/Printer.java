package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.io.PrintWriter;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

import java.util.List;
import java.util.Set;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * The Printer class provides functionality for printing messages and game displays for the TUI.
 */
public class Printer {
    private static Terminal terminal;
    private static LineReader reader;
    //private static PrintWriter writer;

    public Printer(Terminal _terminal, LineReader _reader) {
        //writer = new PrintWriter(System.out, true);
        terminal = _terminal;
        reader = _reader;
        AnsiConsole.systemInstall();
    }

    public void shutdown() {
        AnsiConsole.systemUninstall();
    }

    public void print(String message) {
        //writer.println(message);
        reader.printAbove(message);
    }

    public void printHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(ansi()
                .fgBrightCyan()
                .a("╔═══════════════════════════════════════════════════════════════╗")
                .reset()).append("\n");
        sb.append(ansi()
                .fgBrightCyan()
                .a("║                     ")
                .fgBrightYellow().bold()
                .a("GALAXY TRUCKER CLIENT")
                .reset().fgBrightCyan()
                .a("                     ║")
                .reset()).append("\n");
        sb.append(ansi()
                .fgBrightCyan()
                .a("╚═══════════════════════════════════════════════════════════════╝")
                .reset()).append("\n");
        print(sb.toString());
    }

    public void printSectionHeader(String title) {
        int width = 50;
        String paddedTitle = center(title, width - 2);

        StringBuilder sb = new StringBuilder();
        sb.append(ansi().fgBrightBlue().a("┌").a("─".repeat(width - 2)).a("┐").reset()).append("\n");
        sb.append(ansi().fgBrightBlue().a("│").bold().a(paddedTitle).reset().fgBrightBlue().a("│").reset()).append("\n");
        sb.append(ansi().fgBrightBlue().a("└").a("─".repeat(width - 2)).a("┘").reset()).append("\n");
        print(sb.toString());
    }

    // MESSAGE PRINTING

    public void printSuccess(String message) {
        print(ansi().fgBrightGreen().a(message).reset().toString());
    }

    public void printError(String message) {
        print(ansi().fgBrightRed().a(message).reset().toString());
    }

    public void printInfo(String message) {
        print(ansi().fgBrightCyan().a(message).reset().toString());
    }

    public void printWarning(String message) {
        print(ansi().fgBrightYellow().a(message).reset().toString());
    }

    public void printLoading(String message) {
        print(ansi().fgCyan().a(message + "...").reset().toString());
    }


    // VIEW PRINTING

    // Connection Phase

    public void printConnectionPhase() {
        printSectionHeader("CONNECTION");
    }

    // Login Phase

    public void printLoginPhase() {
        printSectionHeader("LOGIN");
    }

    // Lobby Phase

    public void printLobbyPhase(ClientState clientState) {
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
    }

    public void printLobbyCommands() {
        printInfo("Available Commands:");
        System.out.println("  create <name> <players> <level> - Create a game (e.g., create my_game 4 LEVEL_II)");
        System.out.println("  join <gameId>                   - Join an existing game");
        System.out.println("  refresh                         - Refresh the list of games");
        System.out.println("  help                            - Show this help message");
        System.out.println();
    }

    // Building Phase

    public void printBuildingPhase(ClientState clientState, Component heldComponent) {
        clearScreen();
        printSectionHeader("SHIP BUILDING");

        Ship ship = clientState.getLocalPlayerShip();
        if (ship == null) {
            printError("Ship data not available.");
            return;
        }

        printShipBoard(ship);
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

    private void printShipBoard(Ship ship) {
        Component[][] shipBoard = ship.getBoard();
        Set<Position> forbiddenPositions = ship.getForbiddenPositions();

        printInfo("YOUR SHIP BOARD:");

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
                        case CABIN_START -> sb.append(ansi().bgBrightYellow().a("⛺️").reset());
                        case CANNON_SINGLE -> sb.append("🔫");
                        case CANNON_DOUBLE -> sb.append(ansi().bgBrightGreen().append("🔫").reset());
                        case CARGO_HOLD -> sb.append("📦️");
                        case CARGO_HOLD_SPECIAL -> sb.append(ansi().bgBrightRed().append("📦️").reset());
                        case ENGINE_SINGLE -> sb.append("🚀");
                        case ENGINE_DOUBLE -> sb.append(ansi().bgBrightGreen().append("🚀").reset());
                        case LIFE_SUPPORT_BROWN -> sb.append(ansi().bgYellow().append("🫁️").reset());
                        case LIFE_SUPPORT_PURPLE -> sb.append(ansi().bgBrightMagenta().append("🫁️").reset());
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
        System.out.println(sb);
        System.out.println();
    }

    // TODO
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


    // UTILITIES

    private void clearScreen() {
        System.out.print(ansi().eraseScreen().cursor(1, 1));
        System.out.flush();
    }

    // Table Printing

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

        for (String[] row : data) {
            printTableRow(row, widths, false);
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
                rowLine.append(ansi().bold().a(formattedCell).reset());
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

    // TODO: Add missing phase display methods

    /**
     * TODO: Implement game lobby phase display
     * Should show:
     * - Game name, level, max players
     * - Current players list with ready status indicators
     * - Available commands (ready, unready, start, leave, help)
     * - Real-time updates when players join/leave/ready
     */
    // public void printGameLobbyPhase(ClientState clientState) { }

    /**
     * TODO: Implement building phase display
     * Should show:
     * - Ship grid for building (visual representation)
     * - Component deck with available tiles
     * - Timer display and current phase info
     * - Player's hand (held components)
     * - Available commands (take, place, reserve, return, flip, validate)
     * - Other players' ship status
     */
    // public void printBuildingPhase(ClientState clientState) { }

    /**
     * TODO: Implement flight phase display
     * Should show:
     * - Current adventure card details
     * - Ship status (engines, cannons, crew, cargo, shields)
     * - Flight progress and route position
     * - Available commands (draw, strength, choice, dock)
     * - Combat results and damage
     */
    // public void printFlightPhase(ClientState clientState) { }

    /**
     * TODO: Implement game end phase display
     * Should show:
     * - Final scores for all players
     * - Winner announcement
     * - Game statistics summary
     * - Return to lobby option
     */
    // public void printGameEndPhase(ClientState clientState) { }
}