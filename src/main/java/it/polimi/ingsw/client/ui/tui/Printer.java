package it.polimi.ingsw.client.ui.tui;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.tui.components.TuiTimerView;
import it.polimi.ingsw.common.model.GameInfo;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.general.BuildingTimer;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Shield;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import it.polimi.ingsw.server.model.enums.player.PlayerColor;

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

//        String currentPlayerId = clientState.getPlayerId();
        String hostId = clientState.getHostPlayerId();

        String[] headers = {"NICKNAME", "STATUS", "HOST"};
        String[][] data = new String[players.size()][3];

        // Calculate the max visual width needed for nicknames
        int maxNicknameWidth = "NICKNAME".length();
        for (Player player : players) {
            String colorCircle = getColoredCircle(player.getColor());
            String nameWithCircle = colorCircle + " " + player.getId().getNickname();
            int visualWidth = stripAnsi(nameWithCircle).length();
            maxNicknameWidth = Math.max(maxNicknameWidth, visualWidth);
        }

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            boolean isHost = player.getId().toString().equals(hostId);
            String status = player.isReady() ? "Ready" : "Not Ready";
            String hostIndicator = isHost ? "★" : "";
            String colorCircle = getColoredCircle(player.getColor());

            String nameWithCircle = colorCircle + " " + player.getId().getNickname();
            int visualWidth = stripAnsi(nameWithCircle).length();
            int padding = maxNicknameWidth - visualWidth;
            String paddedName = nameWithCircle + " ".repeat(Math.max(0, padding));

            data[i][0] = paddedName;
            data[i][1] = status;
            data[i][2] = hostIndicator;
        }

        printTable(headers, data);
        print("");
    }

    public void printGameLobbyCommands(ClientState clientState) {
        print("Available Commands:");

        String currentPlayerId = clientState.getPlayerId();
        boolean isHost = currentPlayerId != null && currentPlayerId.equals(clientState.getHostPlayerId());
        boolean isReady = clientState.isPlayerReady(currentPlayerId);
        boolean allReady = clientState.areAllPlayersReady();

        if (isReady) {
            print("  (u) unready    - Mark yourself as not ready");
        } else {
            print("  (r) ready      - Mark yourself as ready");
        }

        if (isHost && allReady) {
            print("  (s) start      - Start the game");
        }

        print("  (l) leave      - Leave the game lobby");
        print("  (r) refresh    - Refresh the game lobby display");
        print("  (h) help       - Show this help message");
        print("");
    }

    // Building Phase

    public void displayBuilding(ClientState clientState) {
        clearScreen();
        printSectionHeader("SHIP BUILDING");

        // Display building timer if available
        if (clientState.getTimerStage() != BuildingTimer.TimerStage.NOT_STARTED) {
            printBuildingTimerDisplay(clientState);
            print("");
        }

        Component heldComponent = clientState.getLocalPlayer().getHeldComponent();

        Ship ship = clientState.getLocalPlayerShip();
        if (ship == null) {
            printError("Ship data not available.");
            return;
        }

        printComponentDeck(clientState.getComponentDeck());
        print("");
        printShipBoard(ship);
        print("");
        printHeldComponent(heldComponent);
//        print("");
//        printShipStats(ship);
        print("");
        print("Building Commands: take | place <row> <col> | rotate | return | validate | flip | refresh | quit | help");
        //printBuildingCommands();
        print("");
    }

    private void printBuildingTimerDisplay(ClientState clientState) {
        BuildingTimer.TimerStage stage = clientState.getTimerStage();
        long timeRemaining = clientState.getTimeRemaining();
        int totalFlips = clientState.getTotalFlips();
        
        // Header
        print("═══════════════════════════════════════");
        print("            BUILDING TIMER             ");
        print("               Flips: " + String.format("%02d", totalFlips) + "               ");
        print("═══════════════════════════════════════");
        
        // Stage progress visualization
        String stage1 = getStageIndicator(stage.ordinal() >= 1, stage == BuildingTimer.TimerStage.FIRST_TIMER);
        String stage2 = getStageIndicator(stage.ordinal() >= 3, stage == BuildingTimer.TimerStage.SECOND_TIMER);
        String end = getStageIndicator(stage == BuildingTimer.TimerStage.BUILDING_ENDED, stage == BuildingTimer.TimerStage.BUILDING_ENDED);
        
        print("Progress: " + stage1 + " ──→ " + stage2 + " ──→ " + end);
        print("         First   Second    End");
        print("Stage: " + getStageDescription(stage));
        
        // Time display
        print("");
        if (timeRemaining > 0) {
            int seconds = (int) (timeRemaining / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            print("Time Remaining: " + timeStr);
            
            // Progress bar
            displayProgressBar(timeRemaining);
        } else if (timeRemaining == 0) {
            print("Time: EXPIRED ⏰");
            displayProgressBar(0);
        } else {
            print("Timer: Not Active");
        }
        
        // Instructions
        print("");
        String instruction = getInstructionText(stage);
        print("💡 " + instruction);
        
        // Contextual hints
        switch (stage) {
            case FIRST_TIMER -> {
                if (timeRemaining > 30000) {
                    print("   Keep building! Timer will run for " + (timeRemaining / 1000) + " more seconds.");
                } else if (timeRemaining > 0) {
                    print("   ⚠️  Timer running low! Get ready to flip.");
                }
            }
            case SECOND_TIMER -> {
                if (timeRemaining > 30000) {
                    print("   Finish your ship! Only finished players can end building.");
                } else if (timeRemaining > 0) {
                    print("   🚨 Second timer running low! Finish quickly!");
                }
            }
            case SECOND_EXPIRED -> {
                print("   ⏰ Building will end automatically in 10 seconds if no one flips!");
            }
        }
        
        print("═══════════════════════════════════════");
    }
    
    private String getStageIndicator(boolean completed, boolean active) {
        if (active) {
            return "◉"; // Active stage - filled circle with dot
        } else if (completed) {
            return "●"; // Completed stage - filled circle
        } else {
            return "○"; // Not reached - empty circle
        }
    }
    
    private String getStageDescription(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Ready to Begin";
            case FIRST_TIMER -> "First Timer Active";
            case FIRST_EXPIRED -> "First Timer Expired";
            case SECOND_TIMER -> "Second Timer Active";
            case SECOND_EXPIRED -> "Second Timer Expired";
            case BUILDING_ENDED -> "Building Complete";
        };
    }
    
    private String getInstructionText(BuildingTimer.TimerStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Type 'flip' to start the first timer";
            case FIRST_TIMER -> "Wait for timer to expire, then type 'flip'";
            case FIRST_EXPIRED -> "Type 'flip' to start the second timer";
            case SECOND_TIMER -> "Finish your ship, then type 'flip' when timer expires";
            case SECOND_EXPIRED -> "Type 'flip' to end building (ship must be finished)";
            case BUILDING_ENDED -> "Building phase complete! Preparing for flight...";
        };
    }
    
    private void displayProgressBar(long timeRemaining) {
        // Calculate progress (assuming 90s stages)
        double progress;
        if (timeRemaining <= 0) {
            progress = 1.0;
        } else {
            progress = 1.0 - ((double) timeRemaining / 90000);
        }
        
        int barLength = 30;
        int filled = (int) (progress * barLength);
        
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");
        
        String percentage = String.format("%.1f%%", progress * 100);
        print(bar.toString() + " " + percentage);
    }

    private void printComponentDeck(ComponentDeck deck) {
        print("COMPONENT DECK:");
        print("");

        if (deck == null) {
            print("  Deck data not available.");
            return;
        }
        print(String.format("  Draw pile: %d | Face-up: %d | Discarded: %d",
                deck.getRemainingCards(), deck.getFaceUpCount(), deck.getDiscardedCards()));

        printFaceUpComponents(deck.getFaceUpComponents());
    }

    private void printFaceUpComponents(List<Component> faceUpComponents) {
        print("FACE-UP COMPONENTS:");

        if (faceUpComponents.isEmpty()) {
            print("  (none)");
            return;
        }

        // TODO: USA TAB PER TENERE ORDINATO

        StringBuilder sb = new StringBuilder();
        int rowFirst = 0;
        int rowEnd = 10;
        int size = faceUpComponents.size();
        for (int row = 0; row < Math.ceilDiv(size, 10); row++) {
            for (Component component : faceUpComponents.subList(rowFirst, Math.min(rowEnd, size))) {
                sb.append("    ");
                sb.append(getConnectorSymbol(component, Direction.UP));
                sb.append("    ");
            }
            sb.append("\n");

            for (Component component : faceUpComponents.subList(rowFirst, Math.min(rowEnd, size))) {
                sb.append("  ");
                sb.append(getConnectorSymbol(component, Direction.LEFT));
                sb.append(getComponentEmoji(component));
                sb.append(getConnectorSymbol(component, Direction.RIGHT));
                sb.append("  ");
            }
            sb.append("\n");

            for (Component component : faceUpComponents.subList(rowFirst, Math.min(rowEnd, size))) {
                sb.append("    ");
                sb.append(getConnectorSymbol(component, Direction.DOWN));
                sb.append("    ");
            }
            sb.append("\n");

            rowFirst += 10;
            rowEnd += 10;

            sb.append("\n");
        }

        print(sb.toString());
    }

    private void printShipBoard(Ship ship) {
        Component[][] shipBoard = ship.getBoard();
        Set<Position> forbiddenPositions = ship.getForbiddenPositions();

        printInfo("YOUR SHIP BOARD:");
        print("");

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
                    sb.append(getConnectorSymbol(component, Direction.UP));
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
                    //sb.append(" ");
                    sb.append(getConnectorSymbol(component, Direction.LEFT));
                    sb.append(getComponentEmoji(component));
                    sb.append(getConnectorSymbol(component, Direction.RIGHT));
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
                    sb.append(getConnectorSymbol(component, Direction.DOWN));
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
            case CABIN, CABIN_START -> "⛺️";
            //case CABIN_START -> ansi().bgBrightYellow().a("⛺️").reset().toString(); // TODO: Colore del giocatore
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

    private String getConnectorSymbol(Component component, Direction direction) {
        if (component == null)
            return null;

        if (component.getType() == ComponentType.SHIELD && ((Shield) component).getProtectedDirections().contains(direction)) {
//            return switch (direction) {
//                case UP, DOWN -> ansi().fgBrightGreen().a("—").reset().toString();
//                case RIGHT, LEFT -> ansi().fgBrightGreen().a("|").reset().toString();
//            };

            return "🟢️";
        }

        return switch (component.getConnectorAt(direction)) {
            case UNIVERSAL -> "💠️";
            case DOUBLE -> "🔷️";
            case SINGLE -> "🔶️";
            case PLAIN -> "  ";
        };
    }

    private void printHeldComponent(Component heldComponent) {
        print("HELD COMPONENT:");
        if (heldComponent == null) {
            print("  (none) - Use 'take' to draw a component.");
            return;
        }

        print("  Type: " + heldComponent.getType() + " " + getComponentEmoji(heldComponent));
        print("  Direction: " + heldComponent.getCurrentDirection());

        StringBuilder sb = new StringBuilder();

//        sb.append("\t┌────────┐");
//        sb.append("\n");

//        sb.append("\t│    ");
        sb.append("    ");
        sb.append(getConnectorSymbol(heldComponent, Direction.UP));
//        sb.append("    │");
        sb.append("\n");

//        sb.append("\t│  ");
        sb.append("  ");
        sb.append(getConnectorSymbol(heldComponent, Direction.LEFT));
        sb.append(getComponentEmoji(heldComponent));
        sb.append(getConnectorSymbol(heldComponent, Direction.RIGHT));
//        sb.append("  │");
        sb.append("\n");

//        sb.append("\t│    ");
        sb.append("    ");
        sb.append(getConnectorSymbol(heldComponent, Direction.DOWN));
//        sb.append("    │");
        sb.append("\n");

//        sb.append("\t└────────┘");
//        sb.append("\n");

        print(sb.toString());
    }

    // TODO: CONTROLLA
    private void printShipStats(Ship ship) {
        print("Ship Stats:");
        print(String.format("  Engines: %d | Cannons: %d | Crew: %d | Cargo: %d | Batteries: %d | Shields: %d",
                ship.getEngineCount(), ship.getCannonCount(), ship.getCrewCapacity(),
                ship.getCargoCapacity(), ship.getBatteryCount(), ship.getShieldCount()));
    }

    public void printBuildingCommands() {
        printInfo("Available Commands:");
        print("  take / take <num>      - Draw a component");
        print("  place <row> <col>      - Place held component");
        print("  rotate                 - Rotate held component");
        print("  return                 - Return held component to deck");
        print("  validate               - Validate ship construction");
        print("  flip                   - Flip building timer");
        print("  refresh                - Refresh the list of games");
        print("  quit                   - Quit the game");
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
        print("  (h) help   - Show this help message");
        print("  (q) quit   - Quit the game");
        print("  giveup     - Give up the game");
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
    
    /**
     * Gets a colored circle representing the player's color.
     * @param color The player's color
     * @return A colored circle string with ANSI escape codes
     */
    private String getColoredCircle(PlayerColor color) {
        if (color == null) {
            return "○"; // Empty circle for null color
        }
        
        return switch (color) {
            case RED -> Ansi.ansi().fgBrightRed().a("●").reset().toString();
            case GREEN -> Ansi.ansi().fgBrightGreen().a("●").reset().toString();
            case BLUE -> Ansi.ansi().fgBrightBlue().a("●").reset().toString();
            case YELLOW -> Ansi.ansi().fgBrightYellow().a("●").reset().toString();
        };
    }
    
    /**
     * Strips ANSI escape codes from a string for width calculation.
     */
    private String stripAnsi(String str) {
        if (str == null) return "";
        return str.replaceAll("\u001B\\[[0-9;]*m", "");
    }
    
    // Enhanced Building Phase Display Methods
    
    public void printBuildingTimer(long timeRemaining, String stage) {
        if (timeRemaining > 0) {
            long seconds = timeRemaining / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            
            String timeStr = String.format("%02d:%02d", minutes, seconds);
            printWarning("Building Timer (" + stage + "): " + timeStr);
        } else {
            printInfo("Building Timer: " + stage);
        }
    }
    
    public void printComponentOffer(String componentType, String reason, long expiresInSeconds) {
        printSectionHeader("COMPONENT OFFER");
        printInfo("Component: " + componentType);
        if (reason != null && !reason.isEmpty()) {
            printInfo("Reason: " + reason);
        }
        if (expiresInSeconds > 0) {
            printWarning("Expires in: " + expiresInSeconds + " seconds");
        }
        printInfo("Use 'take [componentId]' to accept or ignore to decline");
        print("");
    }
    
    public void printValidationResults(boolean isValid, String errorDetails) {
        printSectionHeader("SHIP VALIDATION");
        if (isValid) {
            printSuccess("Ship is structurally sound and ready for flight!");
            printInfo("All components are properly connected");
        } else {
            printError("Ship validation failed!");
            if (errorDetails != null && !errorDetails.isEmpty()) {
                printError("Issues: " + errorDetails);
            }
            printWarning("Fix these issues before attempting flight");
        }
        print("");
    }
}