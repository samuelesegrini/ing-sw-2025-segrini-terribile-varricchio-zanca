package it.polimi.ingsw.client.ui.tui;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import java.util.logging.Logger;

/**
 * Unified console utility class for the TUI.
 * This class provides a single point of access for all console operations,
 * using JAnsi for text formatting and color support.
 */
public class TuiConsole {
    private static final Logger LOGGER = Logger.getLogger(TuiConsole.class.getName());

    private final boolean supportsAnsi;

    /**
     * Initializes the TUI console.
     * It installs the JAnsi system hook to enable ANSI escape code processing.
     */
    public TuiConsole() {
        try {
            AnsiConsole.systemInstall();
            this.supportsAnsi = !isWindows() || isAnsiSupported();
            LOGGER.info("TuiConsole initialized with ANSI support: " + supportsAnsi);
        } catch (Exception e) {
            LOGGER.severe("Failed to install JAnsi console: " + e.getMessage());
            throw new RuntimeException("Failed to initialize console", e);
        }
    }

    /**
     * Shuts down the console, uninstalling the JAnsi system hook.
     */
    public void shutdown() {
        try {
            AnsiConsole.systemUninstall();
            LOGGER.info("TuiConsole shut down successfully.");
        } catch (Exception e) {
            LOGGER.warning("Error during TuiConsole shutdown: " + e.getMessage());
        }
    }

    /**
     * Clears the console screen.
     */
    public void clearScreen() {
        if (supportsAnsi) {
            System.out.print(Ansi.ansi().eraseScreen().cursor(1, 1));
            System.out.flush();
        } else {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Prints the application header.
     */
    public void printHeader() {
        if (supportsAnsi) {
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
        } else {
            System.out.println("================================================================");
            System.out.println("                    GALAXY TRUCKER CLIENT                      ");
            System.out.println("================================================================");
        }
        System.out.println();
    }
    
    /**
     * Prints a section header.
     */
//    public void printSectionHeader(String title) {
////        int padding = Math.max(0, (50 - title.length()) / 2);
////        String paddedTitle = " ".repeat(padding) + title + " ".repeat(padding);
//        int totalWidth = 52;  // Total width of the box
//        int contentWidth = totalWidth - 4;  // Subtract 4 for the '│ ' and ' │' on sides
//        String paddedTitle = center(title, contentWidth);
//
//        if (supportsAnsi) {
//            System.out.println(Ansi.ansi()
//                    .fgBrightBlue()
//                    .a("┌" + "─".repeat(totalWidth - 2) + "┐")
//                    .reset());
//            System.out.println(Ansi.ansi()
//                    .fgBrightBlue()
//                    .a("│ ")
//                    .fgBrightDefault().bold()
//                    .a(paddedTitle)
//                    .reset().fgBrightBlue()
//                    .a(" │")
//                    .reset());
//            System.out.println(Ansi.ansi()
//                    .fgBrightBlue()
//                    .a("└" + "─".repeat(totalWidth - 2) + "┘")
//                    .reset());
//        } else {
//            System.out.println("┌" + "─".repeat(totalWidth - 2) + "┐");
//            System.out.println("│ " + paddedTitle + " │");
//            System.out.println("└" + "─".repeat(totalWidth - 2) + "┘");
//        }
//        System.out.println();
//    }

    public void printSectionHeader(String title) {
        int width = 50;  // Total width between the side borders
        String paddedTitle = center(title, width);

        if (supportsAnsi) {
            System.out.println(Ansi.ansi()
                    .fgBrightBlue()
                    .a("┌" + "─".repeat(width) + "┐")
                    .reset());
            System.out.println(Ansi.ansi()
                    .fgBrightBlue()
                    .a("│")
                    .fgBrightDefault().bold()
                    .a(paddedTitle)
                    .reset().fgBrightBlue()
                    .a("│")
                    .reset());
            System.out.println(Ansi.ansi()
                    .fgBrightBlue()
                    .a("└" + "─".repeat(width) + "┘")
                    .reset());
        } else {
            System.out.println("┌" + "─".repeat(width) + "┐");
            System.out.println("│" + paddedTitle + "│");
            System.out.println("└" + "─".repeat(width) + "┘");
        }
        System.out.println();
    }


    public void printError(String message) {
        println(Ansi.ansi().fgBrightRed().a("[ERROR] ").reset().a(message));
    }

    public void printSuccess(String message) {
        println(Ansi.ansi().fgBrightGreen().a("[SUCCESS] ").reset().a(message));
    }

    public void printInfo(String message) {
        println(Ansi.ansi().fgBrightCyan().a("[INFO] ").reset().a(message));
    }

    public void printWarning(String message) {
        println(Ansi.ansi().fgBrightYellow().a("[WARNING] ").reset().a(message));
    }

    public void printLoading(String message) {
        println(Ansi.ansi().fgCyan().a(message + "...").reset());
    }
    
    /**
     * Prints a simple menu with numbered options.
     */
    public void printMenu(String title, String[] options) {
        System.out.println();
        println(Ansi.ansi().fgBrightCyan().bold().a(title).reset());
        System.out.println();

        for (int i = 0; i < options.length; i++) {
            System.out.println(Ansi.ansi()
                    .fgBrightYellow().bold()
                    .a(String.format("%2d. ", i + 1))
                    .reset()
                    .a(options[i]));
        }
        System.out.println();
    }

    /**
     * Prints a table with headers and data.
     */
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
        
        // Print header
        printTableHeader(headers, widths);
        printTableSeparator(widths);

        // Print data rows
        if (data.length == 0) {
            // Print a message for empty table
             String emptyMessage = "No data available";
             StringBuilder line = new StringBuilder();
             line.append("│");
             int totalWidth = 0;
             for(int w : widths) totalWidth += w;
             totalWidth += widths.length*3-1;
             line.append(center(emptyMessage, totalWidth-2));
             line.append("│");
             System.out.println(line);
        } else {
             for (String[] row : data) {
                printTableRow(row, widths);
            }
        }

        printTableSeparator(widths);
    }
    
//    private String center(String text, int len){
//        if (len <= 0) return "";
//        if(text.length() > len) return text.substring(0, len-3) + "...";
//        String out = String.format("%" + len + "s", text);
//        int l = out.length();
//        int p = (l - text.length()) / 2;
//        out = out.substring(0, p) + text + out.substring(p + text.length());
//        return out;
//    }

    private String center(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int padding = width - text.length();
        int leftPad = padding / 2;
        int rightPad = padding - leftPad;
        return " ".repeat(leftPad) + text + " ".repeat(rightPad);
    }


    private void printTableHeader(String[] headers, int[] widths) {
        StringBuilder headerLine = new StringBuilder();
        headerLine.append("┌");
        for (int i = 0; i < headers.length; i++) {
            headerLine.append("─".repeat(widths[i] + 2));
            if (i < headers.length - 1) {
                headerLine.append("┬");
            }
        }
        headerLine.append("┐");
        System.out.println(Ansi.ansi().fg(Ansi.Color.WHITE).bold().a(headerLine.toString()).reset());

        StringBuilder textLine = new StringBuilder();
        textLine.append("│");
        for (int i = 0; i < headers.length; i++) {
//            textLine.append(" ").append(center(headers[i], widths[i])).append(" ");
//            textLine.append("│");
            String paddedHeader = String.format(" %-" + widths[i] + "s ", headers[i]);
            textLine.append(paddedHeader);
            textLine.append("│");
        }
        System.out.println(Ansi.ansi().fg(Ansi.Color.WHITE).bold().a(textLine.toString()).reset());
    }

    private void printTableSeparator(int[] widths) {
        StringBuilder separatorLine = new StringBuilder();
        separatorLine.append("├");
        for (int i = 0; i < widths.length; i++) {
            separatorLine.append("─".repeat(widths[i] + 2));
            if (i < widths.length - 1) {
                separatorLine.append("┼");
            }
        }
        separatorLine.append("┤");
        System.out.println(Ansi.ansi().fg(Ansi.Color.WHITE).bold().a(separatorLine.toString()).reset());
    }

    private void printTableRow(String[] row, int[] widths) {
        StringBuilder rowLine = new StringBuilder();
        rowLine.append("│");
        for (int i = 0; i < row.length; i++) {
             String cell = (row[i] == null) ? "" : row[i];
            rowLine.append(" ").append(cell.concat(" ".repeat(widths[i] - cell.length()))).append(" ");
            rowLine.append("│");
        }
        System.out.println(rowLine.toString());
    }
    
    public void println(Object message) {
        if (supportsAnsi) {
            System.out.println(message);
        } else {
            System.out.println(Ansi.ansi().render(String.valueOf(message)).toString());
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private boolean isAnsiSupported() {
        String term = System.getenv("TERM");
        return term != null && (term.contains("xterm") || term.contains("ansi"));
    }
} 