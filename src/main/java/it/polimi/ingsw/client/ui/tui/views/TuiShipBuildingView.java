package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.core.UIContext;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.client.ui.tui.TuiManager;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.ShipValidationService;
import it.polimi.ingsw.server.model.domain.general.ComponentDeck;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

/**
 * Full-featured TUI Ship Building view with ASCII art and complete functionality.
 * Mirrors GUI capabilities in text-based interface.
 */
public class TuiShipBuildingView extends BaseUIView {
    private final ClientController controller;
    private final TuiContext context;
    private final TuiManager tuiManager;
    private final Scanner scanner;
    
    // TUI state management
    private Component heldComponent = null;
    private boolean showHints = true;
    private int selectedRow = 2; // Start at cabin position
    private int selectedCol = 3;

    public TuiShipBuildingView(ClientController controller, TuiContext context) {
        this.controller = controller;
        this.context = context;
        this.tuiManager = context.getTuiManager();
        this.scanner = context.getScanner();
        initialize(context);
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.BUILDING;
    }

    @Override
    public String getTitle() {
        return "Ship Building (TUI)";
    }

    @Override
    protected void onShow() {
        displayWelcome();
        startInteractiveLoop();
    }

    @Override
    protected void onHide() {
        // TUI cleanup
    }

    @Override
    protected void onRefresh() {
        refresh();
    }

    @Override
    public void refresh() {
        if (context != null && context.getClientState() != null && context.getClientState().isInGame()) {
            // Refresh is handled by the interactive display
        }
    }
    
    private void displayWelcome() {
        tuiManager.clear();
        tuiManager.println("╔══════════════════════════════════════════════════════════════╗");
        tuiManager.println("║                    GALAXY TRUCKER - SHIP BUILDING           ║");
        tuiManager.println("║                      Text-Based Interface                    ║");
        tuiManager.println("╚══════════════════════════════════════════════════════════════╝");
        tuiManager.println("");
        displayCommands();
    }
    
    private void displayCommands() {
        tuiManager.println("🎮 COMMANDS:");
        tuiManager.println("  📦 'take' - Draw a component from deck");
        tuiManager.println("  🎯 'place <row> <col>' - Place held component (e.g., 'place 1 2')");
        tuiManager.println("  🔄 'rotate' - Rotate held component 90° clockwise");
        tuiManager.println("  📤 'return' - Return held component to deck");
        tuiManager.println("  🎲 'reserve' - Reserve held component (Level II+)");
        tuiManager.println("  ✅ 'validate' - Validate ship construction");
        tuiManager.println("  🚀 'finish' - Finish ship building and prepare for flight");
        tuiManager.println("  ⏰ 'flip' - Flip building timer");
        tuiManager.println("  📚 'forecast <pile>' - View forecast pile (0=left, 1=center, 2=right)");
        tuiManager.println("  🛑 'stop-view' - Stop viewing current forecast pile");
        tuiManager.println("  🆘 'help' - Show commands again");
        tuiManager.println("  🚪 'quit' - Exit to lobby");
        tuiManager.println("");
    }
    
    private void startInteractiveLoop() {
        while (true) {
            try {
                displayFullGameState();
                tuiManager.print("Galaxy Trucker> ");
                
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.isEmpty()) continue;
                
                if (!processCommand(input)) {
                    break; // Exit loop
                }
                
            } catch (Exception e) {
                tuiManager.println("⚠️ Error: " + e.getMessage());
            }
        }
    }
    
    private void displayFullGameState() {
        tuiManager.clear();
        displayShipGrid();
        tuiManager.println("");
        displayHeldComponent();
        tuiManager.println("");
        displayComponentDeck();
        tuiManager.println("");
        displayShipStats();
        if (showHints) {
            displayHints();
        }
        tuiManager.println("─".repeat(70));
    }
    
    private void displayShipGrid() {
        Ship ship = context.getClientState().getLocalPlayerShip();
        if (ship == null) {
            tuiManager.println("⚠️ No ship data available");
            return;
        }
        
        tuiManager.println("🚀 YOUR SHIP GRID:");
        tuiManager.println("");
        
        // Column headers
        tuiManager.print("    ");
        for (int col = 0; col < ship.getCols(); col++) {
            tuiManager.print(String.format(" %d  ", col));
        }
        tuiManager.println("");
        
        // Top border
        tuiManager.print("  ┌─");
        for (int col = 0; col < ship.getCols(); col++) {
            tuiManager.print("───");
            if (col < ship.getCols() - 1) tuiManager.print("┬");
        }
        tuiManager.println("─┐");
        
        // Rows with content
        for (int row = 0; row < ship.getRows(); row++) {
            tuiManager.print(String.format("%d │ ", row));
            
            for (int col = 0; col < ship.getCols(); col++) {
                Position pos = new Position(row, col);
                Component component = ship.getComponent(pos);
                
                if (component != null) {
                    tuiManager.print(getComponentAscii(component));
                } else if (row == selectedRow && col == selectedCol) {
                    tuiManager.print("[*]"); // Cursor
                } else if (ship.isForbiddenPosition(pos)) {
                    tuiManager.print("███"); // Forbidden
                } else {
                    tuiManager.print("   "); // Empty
                }
                
                if (col < ship.getCols() - 1) tuiManager.print("│");
            }
            tuiManager.println(" │");
            
            // Row separator (except last row)
            if (row < ship.getRows() - 1) {
                tuiManager.print("  ├─");
                for (int col = 0; col < ship.getCols(); col++) {
                    tuiManager.print("───");
                    if (col < ship.getCols() - 1) tuiManager.print("┼");
                }
                tuiManager.println("─┤");
            }
        }
        
        // Bottom border
        tuiManager.print("  └─");
        for (int col = 0; col < ship.getCols(); col++) {
            tuiManager.print("───");
            if (col < ship.getCols() - 1) tuiManager.print("┴");
        }
        tuiManager.println("─┘");
    }
    
    private String getComponentAscii(Component component) {
        if (component == null) return "   ";
        
        return switch (component.getType()) {
            case CABIN, CABIN_START -> "🏠"; // Cabin 
            case ENGINE_SINGLE, ENGINE_DOUBLE -> "🔥"; // Engine
            case CANNON_SINGLE, CANNON_DOUBLE -> "💥"; // Cannon
            case SHIELD -> "🛡️"; // Shield
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> "📦"; // Cargo
            case BATTERY -> "🔋"; // Battery
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> "🫁"; // Life support
            case STRUCTURAL -> "⚙️"; // Structural
            default -> "⚪"; // Unknown
        } + " ";
    }
    
    private void displayHeldComponent() {
        tuiManager.println("✋ HELD COMPONENT:");
        if (heldComponent == null) {
            tuiManager.println("  (none) - Use 'take' to draw a component");
        } else {
            tuiManager.println("  📋 Type: " + heldComponent.getType());
            tuiManager.println("  🔄 Direction: " + heldComponent.getCurrentDirection());
            tuiManager.println("  🎨 ASCII: " + getComponentAscii(heldComponent));
            
            // Show component details
            displayComponentDetails(heldComponent);
        }
    }
    
    private void displayComponentDetails(Component component) {
        tuiManager.println("  📊 Stats:");
        // Add component-specific stats based on type
        switch (component.getType()) {
            case ENGINE_SINGLE, ENGINE_DOUBLE -> tuiManager.println("    ⚡ Engine Power: Provides ship movement");
            case CANNON_SINGLE, CANNON_DOUBLE -> tuiManager.println("    💥 Combat: Provides ship firepower");
            case SHIELD -> tuiManager.println("    🛡️ Defense: Protects against damage");
            case CARGO_HOLD, CARGO_HOLD_SPECIAL -> tuiManager.println("    📦 Storage: Holds goods for trading");
            case BATTERY -> tuiManager.println("    🔋 Power: Provides energy to systems");
            case LIFE_SUPPORT_BROWN, LIFE_SUPPORT_PURPLE -> tuiManager.println("    🫁 Life Support: Keeps crew alive");
            case CABIN, CABIN_START -> tuiManager.println("    🏠 Crew Quarters: Houses crew members");
            case STRUCTURAL -> tuiManager.println("    ⚙️ Structure: Provides ship framework");
        }
    }
    
    private void displayComponentDeck() {
        ComponentDeck deck = context.getClientState().getComponentDeck();
        if (deck == null) {
            tuiManager.println("⚠️ No deck data available");
            return;
        }
        
        tuiManager.println("📚 COMPONENT DECK:");
        tuiManager.println(String.format("  📥 Draw pile: %d components", deck.getRemainingCards()));
        tuiManager.println(String.format("  📤 Face-up: %d components", deck.getFaceUpCount()));
        tuiManager.println(String.format("  🗑️ Discarded: %d components", deck.getDiscardedCards()));
        
        // Show face-up components
        List<Component> faceUp = deck.getFaceUpComponents();
        if (!faceUp.isEmpty()) {
            tuiManager.println("  👁️ Face-up components:");
            for (int i = 0; i < Math.min(faceUp.size(), 5); i++) {
                Component comp = faceUp.get(i);
                tuiManager.println(String.format("    %d. %s %s", i+1, getComponentAscii(comp), comp.getType()));
            }
        }
    }
    
    private void displayShipStats() {
        Ship ship = context.getClientState().getLocalPlayerShip();
        if (ship == null) return;
        
        tuiManager.println("📊 SHIP STATISTICS:");
        tuiManager.println(String.format("  🔥 Engines: %d", ship.getEngineCount()));
        tuiManager.println(String.format("  💥 Cannons: %d", ship.getCannonCount()));
        tuiManager.println(String.format("  👥 Crew Capacity: %d", ship.getCrewCapacity()));
        tuiManager.println(String.format("  📦 Cargo Capacity: %d", ship.getCargoCapacity()));
        tuiManager.println(String.format("  🔋 Battery Power: %d", ship.getBatteryCount()));
        tuiManager.println(String.format("  🛡️ Shield Count: %d", ship.getShieldCount()));
    }
    
    private void displayHints() {
        tuiManager.println("💡 HINTS:");
        
        if (heldComponent != null) {
            Ship ship = context.getClientState().getLocalPlayerShip();
            if (ship != null) {
                // Try to find valid placement suggestions
                List<String> suggestions = findPlacementSuggestions(ship, heldComponent);
                for (String suggestion : suggestions) {
                    tuiManager.println("  • " + suggestion);
                }
            }
        } else {
            tuiManager.println("  • Draw a component with 'take' to start building");
            tuiManager.println("  • Components must be connected to the starting cabin");
            tuiManager.println("  • Check connectors match when placing adjacent components");
        }
    }
    
    private List<String> findPlacementSuggestions(Ship ship, Component component) {
        List<String> suggestions = new ArrayList<>();
        
        // Find valid positions
        for (int row = 0; row < ship.getRows(); row++) {
            for (int col = 0; col < ship.getCols(); col++) {
                Position pos = new Position(row, col);
                ShipValidationService.ValidationResult result = 
                    ShipValidationService.validateComponentPlacement(ship, component, pos);
                
                if (result.isValid()) {
                    suggestions.add(String.format("Position (%d,%d) is valid", row, col));
                    if (suggestions.size() >= 3) break; // Limit suggestions
                }
            }
            if (suggestions.size() >= 3) break;
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("Try rotating the component with 'rotate'");
            suggestions.add("No valid positions found for current orientation");
        }
        
        return suggestions;
    }
    
    private boolean processCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0];
        
        switch (command) {
            case "take" -> {
                controller.takeTile();
                tuiManager.println("📦 Drawing component from deck...");
                return true;
            }
            case "place" -> {
                if (parts.length != 3) {
                    tuiManager.println("❌ Usage: place <row> <col> (e.g., 'place 1 2')");
                    return true;
                }
                try {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    return processPlaceCommand(row, col);
                } catch (NumberFormatException e) {
                    tuiManager.println("❌ Invalid coordinates. Use numbers (e.g., 'place 1 2')");
                    return true;
                }
            }
            case "rotate" -> {
                if (heldComponent != null) {
                    heldComponent.rotate();
                    tuiManager.println("🔄 Component rotated 90° clockwise");
                } else {
                    tuiManager.println("❌ No component to rotate. Use 'take' first.");
                }
                return true;
            }
            case "return" -> {
                if (heldComponent != null) {
                    controller.returnTile(heldComponent.getId());
                    heldComponent = null;
                    tuiManager.println("📤 Component returned to deck");
                } else {
                    tuiManager.println("❌ No component to return.");
                }
                return true;
            }
            case "reserve" -> {
                if (heldComponent != null) {
                    controller.reserveComponent(heldComponent.getId());
                    tuiManager.println("🎲 Attempting to reserve component...");
                } else {
                    tuiManager.println("❌ No component to reserve. Use 'take' first.");
                }
                return true;
            }
            case "validate" -> {
                controller.validateShip();
                tuiManager.println("✅ Validating ship construction...");
                return true;
            }
            case "finish" -> {
                return processFinishCommand();
            }
            case "flip" -> {
                controller.flipBuildingTimer();
                tuiManager.println("⏰ Flipping building timer...");
                return true;
            }
            case "forecast" -> {
                if (parts.length != 2) {
                    tuiManager.println("❌ Usage: forecast <pile> (e.g., 'forecast 0' for left pile)");
                    tuiManager.println("   📍 0 = Left pile, 1 = Center pile, 2 = Right pile");
                    return true;
                }
                try {
                    int pileIndex = Integer.parseInt(parts[1]);
                    if (pileIndex < 0 || pileIndex > 2) {
                        tuiManager.println("❌ Pile must be 0 (left), 1 (center), or 2 (right)");
                        return true;
                    }
                    controller.viewForecastPile(pileIndex);
                    tuiManager.println("📚 Requesting forecast pile " + pileIndex + "...");
                } catch (NumberFormatException e) {
                    tuiManager.println("❌ Invalid pile number. Use 0, 1, or 2");
                }
                return true;
            }
            case "stop-view" -> {
                controller.stopViewingForecastPile();
                tuiManager.println("🛑 Stopping forecast pile view...");
                return true;
            }
            case "help" -> {
                displayCommands();
                tuiManager.println("Press Enter to continue...");
                scanner.nextLine();
                return true;
            }
            case "quit", "exit" -> {
                tuiManager.println("👋 Exiting ship building...");
                return false;
            }
            default -> {
                tuiManager.println("❌ Unknown command: " + command);
                tuiManager.println("💡 Type 'help' for available commands");
                return true;
            }
        }
    }
    
    private boolean processPlaceCommand(int row, int col) {
        if (heldComponent == null) {
            tuiManager.println("❌ No component to place. Use 'take' first.");
            return true;
        }
        
        Ship ship = context.getClientState().getLocalPlayerShip();
        if (ship == null) {
            tuiManager.println("❌ Ship data not available");
            return true;
        }
        
        // Validate placement
        Position position = new Position(row, col);
        ShipValidationService.ValidationResult result = 
            ShipValidationService.validateComponentPlacement(ship, heldComponent, position);
        
        if (!result.isValid()) {
            tuiManager.println("❌ Invalid placement:");
            for (String error : result.getErrors()) {
                tuiManager.println("  • " + error);
            }
            return true;
        }
        
        // Show warnings if any
        if (!result.getWarnings().isEmpty()) {
            tuiManager.println("⚠️ Placement warnings:");
            for (String warning : result.getWarnings()) {
                tuiManager.println("  • " + warning);
            }
        }
        
        // Place component
        controller.placeTile(heldComponent.getId(), row, col, heldComponent.getCurrentDirection().ordinal());
        tuiManager.println("✅ Component placed successfully!");
        heldComponent = null;
        selectedRow = row;
        selectedCol = col;
        
        return true;
    }
    
    private boolean processFinishCommand() {
        tuiManager.println("🚀 Finishing ship building...");
        controller.finishShip();
        return true;
    }

}