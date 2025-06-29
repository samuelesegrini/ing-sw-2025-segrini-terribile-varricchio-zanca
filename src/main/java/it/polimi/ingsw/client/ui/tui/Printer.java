package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.io.PrintWriter;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
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

    public void displayConnection() {
        clearScreen();
        printSectionHeader("CONNECTION");
    }

    // Login Phase

    public void displayLogin() {
        clearScreen();
        printSectionHeader("LOGIN");
    }

    // Lobby Phase

    public void displayLobby(ClientState clientState) {
        clearScreen();
        printSectionHeader("LOBBY");

        printInfo("Joinable Games:");
        List<GameInfo> joinableGames = clientState.getJoinableGames();
        if (joinableGames.isEmpty()) {
            print("No games available to join.");
        } else {
            String[] headers = {"GAME ID", "GAME NAME", "LEVEL", "PLAYERS"};
            String[][] data = joinableGames.stream()
                    .map(g -> new String[]{g.getGameId(), g.getGameName(), g.getGameLevel().toString(), g.getCurrentPlayerCount() + "/" + g.getMaxPlayers()})
                    .toArray(String[][]::new);
            printTable(headers, data);
        }
        print("");
        printLobbyCommands();
    }

    public void printLobbyCommands() {
        printInfo("Available Commands:");
        print("  (c) create <gameName> <maxPlayers> <gameLevel>     - Create a game (create my_game 4 LEVEL_II)");
        print("  (j) join <gameId>                                  - Join an existing game");
        print("  (r) refresh                                        - Refresh the list of games");
        print("  (h) help                                           - Show this help message");
        print("");
    }

    // Game Lobby Phase

    public void displayGameLobby(ClientState clientState) {
        clearScreen();
        printSectionHeader("GAME LOBBY");

        // Display game information
        if (clientState.getCurrentGameLobby() != null) {
            var gameModel = clientState.getCurrentGameLobby();
            printInfo("Game: " + gameModel.getGameId());
            printInfo("Level: " + gameModel.getGameLevel());
            printInfo("Players: " + gameModel.getPlayers().size() +
                    "/" + gameModel.getMaxPlayers());
            print("");
        }

        printPlayersInGameLobby(clientState);
        //displayStatus();
        printGameLobbyCommands(clientState);
    }

    private void printPlayersInGameLobby(ClientState clientState) {
        print("Players in Lobby:");

        List<Player> players = clientState.getPlayersInLobby();
        if (players == null || players.isEmpty()) {
            printError("No players in lobby");
            return;
        }

        String currentPlayerId = clientState.getPlayerId();
        String hostId = clientState.getHostPlayerId();

        String[] headers = {"NICKNAME", "STATUS", "HOST"};
        String[][] data = new String[players.size()][3];

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            boolean isHost = player.getId().toString().equals(hostId);
            String status = player.isReady() ? "Ready" : "Not Ready";
            String hostIndicator = isHost ? "★" : "";

            data[i][0] = player.getId().getNickname();
            data[i][1] = status;
            data[i][2] = hostIndicator;
        }

        printTable(headers, data);
        print("");
    }

    private void printGameLobbyCommands(ClientState clientState) {
        print("Available Commands:");

        String currentPlayerId = clientState.getPlayerId();
        boolean isHost = currentPlayerId != null && currentPlayerId.equals(clientState.getHostPlayerId());
        boolean isReady = clientState.isPlayerReady(currentPlayerId);
        boolean allReady = clientState.areAllPlayersReady();

        if (isReady) {
            print("  (u) unready - Mark yourself as not ready");
        } else {
            print("  (r) ready - Mark yourself as ready");
        }

        if (isHost && allReady) {
            print("  (s) start - Start the game");
        }

        print("  (l) leave - Leave the lobby");
        print("  (r) refresh - Refresh the lobby display");
        print("  (h) help - Show this help message");
        print("");
    }

    // Building Phase

    public void displayBuilding(ClientState clientState, Component heldComponent) {
        clearScreen();
        printSectionHeader("SHIP BUILDING");

        Ship ship = clientState.getLocalPlayerShip();
        if (ship == null) {
            printError("Ship data not available.");
            return;
        }

        printShipBoard(ship);
        print("");
        printHeldComponent(heldComponent);
        print("");
        printComponentDeck(clientState.getComponentDeck());
        print("");
        printShipStats(ship);
        print("");
        printShipBuildingCommands();
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
                    sb.append(getConnectorSymbol(component.getConnectorAt(Direction.UP)));
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
                    sb.append(getConnectorSymbol(component.getConnectorAt(Direction.LEFT)));
                    sb.append(getComponentEmoji(component));
                    sb.append(getConnectorSymbol(component.getConnectorAt(Direction.RIGHT)));
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
                    sb.append(getConnectorSymbol(component.getConnectorAt(Direction.DOWN)));
                    sb.append("  ");
                }
                sb.append("\t");
            }
            sb.append("\n");
        }
        print(sb.toString());
        print("");
    }

    private String getComponentEmoji(Component component) {
        if (component == null)
            return null;

        return switch (component.getType()) {
            case BATTERY -> "🔋";
            case CABIN -> "⛺️";
            case CABIN_START -> ansi().bgBrightYellow().a("⛺️").reset().toString();
            case CANNON_SINGLE -> "🔫";
            case CANNON_DOUBLE -> ansi().bgBrightGreen().append("🔫").reset().toString();
            case CARGO_HOLD -> "📦️";
            case CARGO_HOLD_SPECIAL -> ansi().bgBrightRed().append("📦️").reset().toString();
            case ENGINE_SINGLE -> "🚀";
            case ENGINE_DOUBLE -> ansi().bgBrightGreen().append("🚀").reset().toString();
            case LIFE_SUPPORT_BROWN -> ansi().bgYellow().append("🫁️").reset().toString();
            case LIFE_SUPPORT_PURPLE -> ansi().bgBrightMagenta().append("🫁️").reset().toString();
            case SHIELD -> "🛡️";
            case STRUCTURAL -> "🔗️";
        };
    }

    private String getConnectorSymbol(ConnectorType connectorType) {
        return switch (connectorType) {
            case UNIVERSAL -> "U";
            case DOUBLE -> "D";
            case SINGLE -> "S";
            case PLAIN -> " ";
        };
    }

    // TODO: CONTROLLA
    private void printHeldComponent(Component heldComponent) {
        print("HELD COMPONENT:");
        if (heldComponent == null) {
            print("  (none) - Use 'take' to draw a component.");
        } else {
            print("  Type: " + heldComponent.getType() + " " + getComponentEmoji(heldComponent));
            print("  Direction: " + heldComponent.getCurrentDirection());
        }
    }

    // TODO: CONTROLLA
    private void printComponentDeck(ComponentDeck deck) {
        print("COMPONENT DECK:");
        if (deck == null) {
            print("  Deck data not available.");
            return;
        }
        print(String.format("  Draw pile: %d | Face-up: %d | Discarded: %d",
                deck.getRemainingCards(), deck.getFaceUpCount(), deck.getDiscardedCards()));
    }

    // TODO: CONTROLLA
    private void printShipStats(Ship ship) {
        print("Ship Stats:");
        print(String.format("  Engines: %d | Cannons: %d | Crew: %d | Cargo: %d | Batteries: %d | Shields: %d",
                ship.getEngineCount(), ship.getCannonCount(), ship.getCrewCapacity(),
                ship.getCargoCapacity(), ship.getBatteryCount(), ship.getShieldCount()));
    }

    public void printShipBuildingCommands() {
        printInfo("Available Commands:");
        print("  take                   - Draw a component");
        print("  place <row> <col>      - Place held component");
        print("  rotate                 - Rotate held component");
        print("  return                 - Return held component to deck");
        print("  validate               - Validate ship construction");
        print("  flip                   - Flip building timer");
        print("  help                   - Show commands again");
        print("");
    }

    // Flight Phase

    public void displayFlight(ClientState clientState) {
        clearScreen();
        printSectionHeader("FLIGHT");

        Ship ship = clientState.getLocalPlayerShip();
        if (ship == null) {
            printError("Ship data not available.");
            return;
        }

        // Print adventure card if available
        AdventureCard currentCard = null;
        Optional<AdventureCard> optCard = clientState.getGameModel().getAdventureDeck().getCurrentCard();
        if (optCard.isPresent()) {
            currentCard = optCard.get();
            printAdventureCard(currentCard);
            print("");
        }

        printShipBoard(ship);
        print("");
        printShipStats(ship);
        print("");
        printFlightCommands();
    }

    public void printAdventureCard(AdventureCard card) {
        // TODO
    }

    public void printFlightCommands() {
        print("Available Commands:");
        print("  (h) help                              - Show this help message");
        print("  (q) quit                              - Quit the game");
        print("  giveup                                - Give up the game");
        // TODO
//        if (currentCard != null) {
//            print("  [number]                              - Make choice for current adventure card");
//        }
        print("");
    }




    // UTILITIES

    private void clearScreen() {
        if (terminal.puts(InfoCmp.Capability.clear_screen)) {
            terminal.flush();
        } else {
            System.out.print(Ansi.ansi().eraseScreen().cursor(1, 1).toString());
            System.out.flush();
        }
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
        print(rowLine.toString());
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
        print(separatorLine.toString());
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