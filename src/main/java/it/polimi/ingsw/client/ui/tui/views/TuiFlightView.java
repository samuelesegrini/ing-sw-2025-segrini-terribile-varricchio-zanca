package it.polimi.ingsw.client.ui.tui.views;

import it.polimi.ingsw.client.core.ClientState;
import it.polimi.ingsw.client.ui.core.BaseUIView;
import it.polimi.ingsw.client.ui.tui.TuiConsole;
import it.polimi.ingsw.client.ui.tui.TuiContext;
import it.polimi.ingsw.client.controller.ClientController;
import it.polimi.ingsw.server.model.domain.adventure.card.AdventureCard;
import it.polimi.ingsw.server.model.domain.adventure.card.PlanetsCard;
import it.polimi.ingsw.server.model.domain.adventure.card.MeteorSwarmCard;
import it.polimi.ingsw.server.model.domain.adventure.card.AbandonedShipCard;
import it.polimi.ingsw.server.model.domain.adventure.card.AbandonedStationCard;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.domain.flight.PlayerFlightData;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;

import java.util.ArrayList;

import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import it.polimi.ingsw.common.message.request.flight.LandPlanetRequest;
import it.polimi.ingsw.common.message.request.flight.PlanetChoiceRequest;
import it.polimi.ingsw.common.message.request.flight.CombatStrengthRequest;
import it.polimi.ingsw.common.message.request.flight.EngineStrengthRequest;
import it.polimi.ingsw.common.message.request.flight.DockRequest;
import it.polimi.ingsw.common.message.request.flight.DeclareStrengthRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class TuiFlightView extends BaseUIView {
    private final TuiConsole console;
    private final Scanner scanner;
    private final ClientController controller;
    private volatile boolean refreshNeeded = false;
    private AdventureCard currentCard;
    private Map<String, Integer> playerPositions = new HashMap<>();
    private Map<String, Integer> previousPlayerPositions = new HashMap<>();
    private boolean isMyTurn = false;
    private long turnTimeRemaining = 0;
    private String lastActionPerformed = "";
    private boolean shipDamageUpdated = false;
    private boolean showDamageAnimation = false;

    // ANSI escape codes for colors
    final String RESET = "\u001B[0m";
    final String BG_RED = "\u001B[48;2;255;0;0m";               // Pure red
    final String BG_YELLOW = "\u001B[48;2;255;255;0m";          // Pure yellow
    final String BG_PURPLE = "\u001B[48;2;128;0;128m";          // Medium purple
    final String BG_BROWN = "\u001B[48;2;139;69;19m";           // Saddle brown
    final String BG_BRIGHT_GREEN = "\u001B[48;2;0;255;127m";    // Spring green

    public TuiFlightView(ClientController controller, TuiContext context) {
        this.controller = controller;
        this.console = context.getConsole();
        this.scanner = context.getScanner();
        initialize(context);
    }

    @Override
    public ClientState.ViewState getViewState() {
        return ClientState.ViewState.FLIGHT;
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
        // Check for position changes to show movement animation
        checkForMovementUpdates();
        
        // Check for ship damage updates
        checkForShipDamageUpdates();
        
        displayFullInterface();
        
        // Show any animations after display
        if (showDamageAnimation) {
            showShipDamageAnimation();
            showDamageAnimation = false;
        }
    }


    private void displayFullInterface() {
        console.clearScreen();
        console.printSectionHeader("🚀 FLIGHT PHASE");
        
        // Show turn indicator with animation
        displayTurnIndicator();

        ClientState clientState = context.getClientState();

        displayFlightOrder(clientState);
        console.println("");
        
        // Show last action feedback
        if (!lastActionPerformed.isEmpty()) {
            console.printHighlight("Last Action: " + lastActionPerformed);
            console.println("");
        }

        displayShipBoard(clientState);
        console.println("");

        displayKey();
        console.println("");

        displayShipStats(clientState);
        console.println("");
    }

    private void displayFlightOrder(ClientState clientState) {
        if (clientState.getGameModel() == null || clientState.getGameModel().getFlightBoard() == null) {
            console.println("Flight board not available");
            return;
        }
        
        Map<Player, PlayerFlightData> playerDataMap = clientState.getGameModel().getFlightBoard().getPlayerDataMap();
        int length = clientState.getGameModel().getFlightBoard().getRoute().getLength();
        List<Player> currentOrder = clientState.getGameModel().getFlightBoard().getCurrentOrder();

        console.println("=== FLIGHT BOARD ===");
        console.println("Route Progress:");
        
        // Create a visual representation of the flight route
        StringBuilder routeDisplay = new StringBuilder();
        StringBuilder playerDisplay = new StringBuilder();
        
        for (int position = 0; position < length; position++) {
            routeDisplay.append(String.format("%3d", position));
            
            // Find players at this position
            List<String> playersAtPosition = new ArrayList<>();
            for (Map.Entry<Player, PlayerFlightData> entry : playerDataMap.entrySet()) {
                if (entry.getValue().getPosition() == position) {
                    playersAtPosition.add(entry.getKey().getId().getNickname().substring(0, 
                        Math.min(2, entry.getKey().getId().getNickname().length())));
                }
            }
            
            if (playersAtPosition.isEmpty()) {
                playerDisplay.append("   ");
            } else {
                playerDisplay.append(String.format("%3s", String.join("", playersAtPosition)));
            }
            
            if (position < length - 1) {
                routeDisplay.append("---");
                playerDisplay.append("   ");
            }
        }
        
        console.println(routeDisplay.toString());
        console.println(playerDisplay.toString());
        
        // Show detailed player positions
        console.println("\nPlayer Positions:");
        for (Map.Entry<Player, PlayerFlightData> entry : playerDataMap.entrySet()) {
            String playerName = entry.getKey().getId().getNickname();
            int position = entry.getValue().getPosition();
            console.println(String.format("  %s: Position %d/%d", playerName, position, length));
        }
    }

    private void displayShipBoard(ClientState clientState) {
        if (clientState == null || clientState.getLocalPlayerShip() == null) {
            console.printError("Ship information not available");
            return;
        }
        
        Component[][] shipBoard = clientState.getLocalPlayerShip().getBoard();
        Set<Position> forbiddenPositions = clientState.getLocalPlayerShip().getForbiddenPositions();
        Set<Component> lostComponents = clientState.getLocalPlayerShip().getLostComponents();

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

                    // Check if component is damaged
                    boolean isDamaged = lostComponents.contains(component);
                    
                    if (isDamaged) {
                        sb.append("💥"); // Explosion emoji for damaged components
                    } else {
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
        sb.append("🔗️").append(": Structural Module, ");
        sb.append("💥").append(": DAMAGED Component");
        console.println(sb);

        console.println("Connectors: U=Universal, S=Single, D=Double, P=Plain (no connector)");
        console.println("Directions: U=Up, D=Down, L=Left, R=Right");
    }

    private void displayShipStats(ClientState clientState) {
        if (clientState == null || clientState.getLocalPlayerShip() == null) {
            console.printError("Ship statistics not available");
            return;
        }
        
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

        // If we have an active adventure card, handle adventure card input
        if (currentCard != null) {
            handleAdventureCardInput(input.trim());
            return;
        }

        switch (command) {
            case "h":
            case "help":
                showHelp();
                break;
            case "q":
            case "quit":
                if (controller != null) {
                    controller.disconnect();
                }
                break;
            case "giveup":
            case "surrender":
                handleGiveUpChoice();
                break;
            case "aliens":
                displayAlienPassengers();
                break;
            case "integrity":
            case "structure":
                checkStructuralIntegrity();
                break;
            case "batteries":
            case "power":
                displayDetailedBatteryInfo();
                break;
            case "management":
            case "systems":
                displayPowerManagement();
                break;
            default:
                console.printError("Unknown command: " + command + ". Type 'help' for available commands.");
                break;
        }
    }

    private void handleAdventureCardInput(String input) {
        if (!isMyTurn) {
            console.printError("It's not your turn yet. Please wait.");
            return;
        }
        
        try {
            AdventureType type = currentCard.getType();
            switch (type) {
                case PLANETS -> handlePlanetChoice(Integer.parseInt(input));
                case PIRATES, SLAVERS, SMUGGLERS -> handleCombatChoice(input);
                case ABANDONED_SHIP -> handleAbandonedShipChoice(input);
                case ABANDONED_STATION -> handleAbandonedStationChoice(input);
                case OPEN_SPACE -> handleEngineChoice(Integer.parseInt(input));
                case METEOR_SWARM -> handleMeteorSwarmChoice(input);
                case WAR_ZONE -> handleWarZoneChoice(input);
                case STARDUST, EPIDEMIC -> handleSpecialEventChoice(input);
                default -> console.printError("This card type doesn't require input.");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter a number.");
        }
    }
    
    private void handlePlanetChoice(int choice) {
        if (choice < 0 || (currentCard instanceof PlanetsCard planetsCard && choice > planetsCard.getPlanets().size())) {
            console.printError("Invalid planet choice. Please try again.");
            return;
        }
        
        // Send new planet choice request
        PlanetChoiceRequest request = new PlanetChoiceRequest(choice);
        sendPlanetChoiceRequest(request);
        isMyTurn = false; // No longer our turn
        console.println("Planet choice sent: " + (choice == 0 ? "Skip" : "Planet " + choice));
    }
    
    private void handleCombatChoice(String input) {
        try {
            int batteriesToUse = Integer.parseInt(input);
            if (batteriesToUse < 0) {
                console.printError("Cannot use negative batteries.");
                return;
            }
            
            // Calculate and display combat summary
            double cannonStrength = getCurrentCannonStrength();
            int alienBonus = getPurpleAlienCombatBonus();
            double totalStrength = cannonStrength + alienBonus + batteriesToUse;
            
            console.println("\n⚔️ COMBAT DECLARATION ⚔️");
            console.println("Base cannon strength: " + cannonStrength);
            if (alienBonus > 0) {
                console.println("Purple alien bonus: +" + alienBonus);
            }
            if (batteriesToUse > 0) {
                console.println("Battery boost: +" + batteriesToUse);
            }
            console.println("Total declared strength: " + totalStrength);
            console.println("🎲 Combat dice will be rolled by server...");
            console.println("Final combat strength = " + totalStrength + " + [dice roll]");
            
            // Validate battery usage
            if (!validateBatteryUsage(batteriesToUse, "Combat against enemies")) {
                console.println("⚠️ Insufficient batteries! Using maximum available: " + getCurrentBatteries());
                batteriesToUse = getCurrentBatteries();
            }
            
            // Send combat strength request
            CombatStrengthRequest request = new CombatStrengthRequest(batteriesToUse);
            sendCombatStrengthRequest(request);
            isMyTurn = false; // No longer our turn
            
            console.println("Combat declaration sent. Awaiting battle resolution...");
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter number of batteries to use.");
        }
    }
    
    private void handleAbandonedShipChoice(String input) {
        try {
            int choice = Integer.parseInt(input);
            AbandonedShipCard shipCard = (AbandonedShipCard) currentCard;
            
            switch (choice) {
                case 1 -> {
                    // Check if player has enough crew
                    int currentCrew = getCurrentCrew();
                    if (currentCrew < shipCard.getCrewLost()) {
                        console.printError("Insufficient crew! You need at least " + shipCard.getCrewLost() + 
                                          " crew members but only have " + currentCrew);
                        return;
                    }
                    
                    console.println("Docking and exploring abandoned ship...");
                    console.println("Crew lost: " + shipCard.getCrewLost());
                    console.println("Credits gained: " + shipCard.getCreditsGained());
                    console.println("Flight days lost: " + shipCard.getLostDays());
                    
                    // Send dock request
                    DockRequest dockRequest = new DockRequest(true);
                    sendDockRequest(dockRequest);
                    isMyTurn = false;
                    currentCard = null;
                }
                case 2 -> {
                    console.println("Continuing without exploring the abandoned ship...");
                    DockRequest dockRequest = new DockRequest(false);
                    sendDockRequest(dockRequest);
                    isMyTurn = false;
                    currentCard = null;
                }
                default -> console.printError("Invalid choice. Enter 1 or 2.");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter 1 or 2.");
        }
    }
    
    private void handleAbandonedStationChoice(String input) {
        try {
            int choice = Integer.parseInt(input);
            AbandonedStationCard stationCard = (AbandonedStationCard) currentCard;
            
            switch (choice) {
                case 1 -> {
                    // Check if player has enough crew
                    int currentCrew = getCurrentCrew();
                    if (currentCrew < stationCard.getMinCrewRequired()) {
                        console.printError("Insufficient crew! You need at least " + stationCard.getMinCrewRequired() + 
                                          " crew members but only have " + currentCrew);
                        return;
                    }
                    
                    console.println("Docking at abandoned station...");
                    console.println("Minimum crew required: " + stationCard.getMinCrewRequired());
                    console.println("Available goods:");
                    stationCard.getGoodQuantities().forEach((goodType, quantity) -> 
                        console.println("  " + goodType + ": " + quantity));
                    console.println("Flight days lost: " + stationCard.getLostDays());
                    
                    // Send dock request
                    DockRequest dockRequest = new DockRequest(true);
                    sendDockRequest(dockRequest);
                    isMyTurn = false;
                    currentCard = null;
                }
                case 2 -> {
                    console.println("Continuing without docking at the station...");
                    DockRequest dockRequest = new DockRequest(false);
                    sendDockRequest(dockRequest);
                    isMyTurn = false;
                    currentCard = null;
                }
                default -> console.printError("Invalid choice. Enter 1 or 2.");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter 1 or 2.");
        }
    }

    private void sendRequest(LandPlanetRequest request) {
        // Send request through controller
        if (controller != null) {
            controller.sendRequest(request);
        } else {
            console.printError("Cannot send request - controller not available.");
        }
    }

    private void showHelp() {
        console.println("Available Commands:");
        console.println("  (h) help                              - Show this help message");
        console.println("  (q) quit                              - Quit the game");
        console.println("  giveup/surrender                      - Give up the current flight");
        console.println("  aliens                                - Show alien passenger bonuses");
        console.println("  integrity/structure                   - Check ship structural integrity");
        console.println("  batteries/power                       - Show detailed battery status");
        console.println("  management/systems                    - Display power management info");
        if (currentCard != null) {
            console.println("  [number]                              - Make choice for current adventure card");
        }
        console.println("");
    }

    /**
     * Displays an adventure card and its options.
     */
    public void displayAdventureCard(AdventureCard card) {
        this.currentCard = card;
        
        console.println("\n=== ADVENTURE CARD ===");
        console.println("Type: " + card.getClass().getSimpleName());
        console.println("Description: " + card.getDescription());
        
        if (isMyTurn) {
            console.println("*** IT'S YOUR TURN! ***");
            if (turnTimeRemaining > 0) {
                console.println("Time remaining: " + (turnTimeRemaining / 1000) + " seconds");
            }
            showCardOptions(card);
        } else {
            console.println("Waiting for other players to make their choices...");
        }
        console.println("");
    }
    
    /**
     * Called when it becomes this player's turn to choose.
     */
    public void setPlayerTurn(boolean isMyTurn, long timeRemaining) {
        this.isMyTurn = isMyTurn;
        this.turnTimeRemaining = timeRemaining;
        
        if (currentCard != null) {
            displayAdventureCard(currentCard); // Refresh display
        }
    }
    
    private void showCardOptions(AdventureCard card) {
        AdventureType type = card.getType();
        switch (type) {
            case PLANETS -> showPlanetOptions((PlanetsCard) card);
            case PIRATES, SLAVERS, SMUGGLERS -> showCombatOptions();
            case ABANDONED_SHIP -> showAbandonedShipOptions((AbandonedShipCard) card);
            case ABANDONED_STATION -> showAbandonedStationOptions((AbandonedStationCard) card);
            case OPEN_SPACE -> showEngineOptions();
            case METEOR_SWARM -> showMeteorSwarmOptions((MeteorSwarmCard) card);
            case WAR_ZONE -> showWarZoneOptions();
            case STARDUST, EPIDEMIC -> showSpecialEventOptions(card);
            default -> console.println("Card will be processed automatically.");
        }
    }
    
    private void showPlanetOptions(PlanetsCard planetsCard) {
        console.println("\nAvailable planets:");
        console.println("0. Skip (don't land on any planet)");
        
        for (int i = 0; i < planetsCard.getPlanets().size(); i++) {
            Planet planet = planetsCard.getPlanets().get(i);
            if (!planet.isVisited()) {
                console.println((i + 1) + ". Planet " + planet.getNumber() + 
                    " - Resources: " + planet.getGoodQuantities() + 
                    " (Cost: " + planetsCard.getLostDays() + " days)");
            }
        }
        
        console.println("\nChoose planet (0-" + planetsCard.getPlanets().size() + "): ");
    }
    
    private void showCombatOptions() {
        console.println("\nCombat encounter!");
        console.println("Declare how many batteries you want to use for cannon strength.");
        console.println("Each battery increases your combat strength by 1.");
        
        double baseCannons = getCurrentCannonStrength();
        int alienBonus = getPurpleAlienCombatBonus();
        double totalCombatStrength = baseCannons + alienBonus;
        
        console.println("\nYour combat capabilities:");
        console.println("  Base cannon strength: " + baseCannons);
        if (alienBonus > 0) {
            console.println("  Purple alien bonus: +" + alienBonus);
            console.println("  Total combat strength: " + totalCombatStrength);
        }
        console.println("  Available batteries: " + getCurrentBatteries());
        
        console.println("\nEnter the number of batteries to use (0 to use no batteries):");
    }
    
    private void showEngineOptions() {
        console.println("\nOpen Space - Engine Power Required!");
        console.println("Declare your engine strength. You can boost it with batteries.");
        
        double baseEngines = getCurrentEngineStrength();
        int alienBonus = getBrownAlienEngineBonus();
        double totalEngineStrength = baseEngines + alienBonus;
        
        console.println("\nYour engine capabilities:");
        console.println("  Base engine strength: " + baseEngines);
        if (alienBonus > 0) {
            console.println("  Brown alien bonus: +" + alienBonus);
            console.println("  Total engine strength: " + totalEngineStrength);
        }
        console.println("  Available batteries: " + getCurrentBatteries());
        
        console.println("\nEnter your base engine strength (your ship's engine power):");
    }
    
    private void showAbandonedShipOptions(AbandonedShipCard shipCard) {
        console.println("\n=== ABANDONED SHIP DETECTED ===");
        console.println("A derelict ship drifts nearby. You could explore it for salvage...");
        
        if (shipCard.isVisited()) {
            console.println("This ship has already been explored by another player.");
            return;
        }
        
        console.println("\nRisk Assessment:");
        console.println("  Crew members at risk: " + shipCard.getCrewLost());
        console.println("  Potential credits: " + shipCard.getCreditsGained());
        console.println("  Time cost: " + shipCard.getLostDays() + " flight days");
        
        console.println("\nYour current crew: " + getCurrentCrew());
        
        if (getCurrentCrew() < shipCard.getCrewLost()) {
            console.println("⚠️  WARNING: Insufficient crew for safe exploration!");
        }
        
        console.println("\nOptions:");
        console.println("1. Explore the abandoned ship (risk crew for credits)");
        console.println("2. Continue without exploring");
        console.println("\nChoose action (1-2): ");
    }
    
    private void showAbandonedStationOptions(AbandonedStationCard stationCard) {
        console.println("\n=== ABANDONED STATION FOUND ===");
        console.println("A derelict space station with valuable cargo awaits...");
        
        if (stationCard.isVisited()) {
            console.println("This station has already been looted by another player.");
            return;
        }
        
        console.println("\nStation Requirements:");
        console.println("  Minimum crew needed: " + stationCard.getMinCrewRequired());
        console.println("  Time cost: " + stationCard.getLostDays() + " flight days");
        
        console.println("\nAvailable cargo:");
        stationCard.getGoodQuantities().forEach((goodType, quantity) -> {
            console.println("  " + goodType + " goods: " + quantity);
        });
        
        console.println("\nYour current crew: " + getCurrentCrew());
        console.println("Your cargo capacity:");
        console.println("  Normal goods: " + getCurrentNormalCargoCapacity());
        console.println("  Special goods: " + getCurrentSpecialCargoCapacity());
        
        if (getCurrentCrew() < stationCard.getMinCrewRequired()) {
            console.println("⚠️  WARNING: Insufficient crew to dock safely!");
        }
        
        console.println("\nOptions:");
        console.println("1. Dock at the station (collect goods, lose time)");
        console.println("2. Continue without docking");
        console.println("\nChoose action (1-2): ");
    }

    /**
     * Updates player positions display in real-time.
     */
    public void updatePositions(Map<String, Integer> positions) {
        this.playerPositions = positions;
        // Trigger a full refresh to show updated positions
        if (active) {
            refreshNeeded = true;
            displayFullInterface();
        }
    }
    
    /**
     * Shows a detailed flight board with position tracking
     */
    public void showFlightMovement(String playerName, int oldPosition, int newPosition, String reason) {
        console.println("\n=== MOVEMENT UPDATE ===");
        console.println(String.format("%s moved from position %d to %d", playerName, oldPosition, newPosition));
        if (reason != null && !reason.isEmpty()) {
            console.println("Reason: " + reason);
        }
        console.println("");
        
        // Update positions and refresh display
        if (playerPositions.containsKey(playerName)) {
            playerPositions.put(playerName, newPosition);
        }
        displayFullInterface();
    }
    
    /**
     * Shows flight day penalties in real-time
     */
    public void showFlightDayPenalty(String playerName, int daysLost, String reason) {
        console.println("\n=== FLIGHT DAY PENALTY ===");
        console.println(String.format("%s loses %d flight day%s", playerName, daysLost, daysLost == 1 ? "" : "s"));
        console.println("Reason: " + reason);
        console.println(playerName + " moves backward on the flight board...");
        console.println("");
    }
    
    /**
     * Displays dice roll results with proper Galaxy Trucker theming
     */
    public void showDiceRoll(String purpose, List<Integer> diceValues) {
        console.println("\n=== DICE ROLL ===");
        console.println("Purpose: " + purpose);
        
        // Create visual dice representation
        StringBuilder diceDisplay = new StringBuilder("🎲 Dice rolled: ");
        for (int i = 0; i < diceValues.size(); i++) {
            int value = diceValues.get(i);
            diceDisplay.append("[").append(value).append("]");
            if (i < diceValues.size() - 1) {
                diceDisplay.append(" + ");
            }
        }
        
        int total = diceValues.stream().mapToInt(Integer::intValue).sum();
        if (diceValues.size() > 1) {
            diceDisplay.append(" = ").append(total);
        }
        
        console.println(diceDisplay.toString());
        
        // Add contextual information based on purpose
        if (purpose.toLowerCase().contains("combat")) {
            console.println("💥 Combat dice rolled for battle resolution");
        } else if (purpose.toLowerCase().contains("meteor")) {
            console.println("☄️ Meteor targeting dice - determining impact location");
        } else if (purpose.toLowerCase().contains("damage")) {
            console.println("💀 Damage dice - determining component destruction");
        } else if (purpose.toLowerCase().contains("exploration")) {
            console.println("🔍 Exploration dice - determining discovery results");
        }
        
        console.println("");
    }
    
    /**
     * Displays combat resolution with dice results
     */
    public void showCombatResolution(String playerName, int playerStrength, int diceRoll, 
                                   int enemyStrength, int enemyDice, boolean victory) {
        console.println("\n=== COMBAT RESOLUTION ===");
        console.println("Player: " + playerName);
        console.println(String.format("Your combat: %d + %d = %d", playerStrength, diceRoll, playerStrength + diceRoll));
        console.println(String.format("Enemy combat: %d + %d = %d", enemyStrength, enemyDice, enemyStrength + enemyDice));
        
        if (victory) {
            console.println("🏆 VICTORY! You defeated the enemy!");
            console.println("💰 Collecting rewards...");
        } else {
            console.println("💀 DEFEAT! The enemy overcame your defenses!");
            console.println("💥 Taking damage...");
        }
        console.println("");
    }
    
    /**
     * Displays meteor strike resolution with dice targeting
     */
    public void showMeteorStrike(List<Integer> targetingDice, String impactLocation, 
                               int meteorStrength, boolean deflected) {
        console.println("\n=== METEOR STRIKE ===");
        
        // Show targeting dice
        StringBuilder targeting = new StringBuilder("🎯 Targeting: ");
        for (int i = 0; i < targetingDice.size(); i++) {
            targeting.append("[").append(targetingDice.get(i)).append("]");
            if (i < targetingDice.size() - 1) {
                targeting.append(" ");
            }
        }
        console.println(targeting.toString());
        
        console.println("📍 Impact location: " + impactLocation);
        console.println("💥 Meteor strength: " + meteorStrength);
        
        if (deflected) {
            console.println("🛡️ Meteor deflected by shields/cannons!");
        } else {
            console.println("💀 Meteor impact causes damage!");
        }
        console.println("");
    }
    
    private void handleEngineChoice(int engineStrength) {
        if (engineStrength < 0) {
            console.printError("Engine strength cannot be negative.");
            return;
        }
        
        // For Open Space cards, ask if player wants to use batteries for boost
        console.println("Do you want to use batteries to boost engine power? (Enter 0 for no boost, or number of batteries):");
        String batteryInput = scanner.nextLine().trim();
        
        try {
            int batteriesToUse = Integer.parseInt(batteryInput);
            if (batteriesToUse < 0) {
                console.printError("Cannot use negative batteries. Using 0 batteries.");
                batteriesToUse = 0;
            }
            
            // Send engine strength request with optional battery boost
            EngineStrengthRequest request = new EngineStrengthRequest(engineStrength, batteriesToUse);
            sendEngineStrengthRequest(request);
            isMyTurn = false; // No longer our turn
            
            if (batteriesToUse > 0) {
                console.println("Engine strength declared: " + engineStrength + " + " + batteriesToUse + " battery boost = " + (engineStrength + batteriesToUse));
            } else {
                console.println("Engine strength declared: " + engineStrength + " (no battery boost)");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid battery input. Using engine strength without boost.");
            EngineStrengthRequest request = new EngineStrengthRequest(engineStrength);
            sendEngineStrengthRequest(request);
            isMyTurn = false;
            console.println("Engine strength declared: " + engineStrength + " (no battery boost)");
        }
    }
    
    private void sendCombatStrengthRequest(CombatStrengthRequest request) {
        // Send request through controller
        if (controller != null) {
            controller.sendRequest(request);
        } else {
            console.printError("Cannot send request - controller not available.");
        }
    }
    
    private void sendEngineStrengthRequest(EngineStrengthRequest request) {
        // Send request through controller
        if (controller != null) {
            controller.sendRequest(request);
        } else {
            console.printError("Cannot send request - controller not available.");
        }
    }
    
    private void sendPlanetChoiceRequest(PlanetChoiceRequest request) {
        // Send request through controller
        if (controller != null) {
            controller.sendRequest(request);
        } else {
            console.printError("Cannot send request - controller not available.");
        }
    }
    
    private void sendDockRequest(DockRequest request) {
        // Send request through controller
        if (controller != null) {
            controller.sendRequest(request);
        } else {
            console.printError("Cannot send request - controller not available.");
        }
    }
    
    private void sendDeclareStrengthRequest(DeclareStrengthRequest request) {
        // Send request through controller
        if (controller != null) {
            controller.sendRequest(request);
        } else {
            console.printError("Cannot send request - controller not available.");
        }
    }
    
    private int getCurrentBatteries() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().getBatteries();
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0;
    }
    
    private double getCurrentEngineStrength() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().getEngines();
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0.0;
    }
    
    /**
     * Handles meteor swarm encounters
     */
    private void handleMeteorSwarmChoice(String input) {
        try {
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1 -> handleCannonDefense();
                case 2 -> handleShieldDefense();
                case 3 -> handleDirectDamage();
                default -> console.printError("Invalid choice. Enter 1, 2, or 3.");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter 1, 2, or 3.");
        }
    }
    
    private void handleCannonDefense() {
        double cannonStrength = getCurrentCannonStrength();
        int alienBonus = getPurpleAlienCombatBonus();
        double totalCombat = cannonStrength + alienBonus;
        
        console.println("💥 CANNON DEFENSE ACTIVATED 💥");
        console.println("Preparing to engage meteors with cannon fire...");
        console.println("Your base cannon strength: " + cannonStrength);
        if (alienBonus > 0) {
            console.println("Purple alien combat bonus: +" + alienBonus);
        }
        console.println("Total combat strength: " + totalCombat);
        
        if (totalCombat <= 0) {
            console.printError("No cannon strength available! Taking direct damage instead.");
            handleDirectDamage();
            return;
        }
        
        console.println("\n🔋 Battery Boost Options:");
        console.println("Available batteries: " + getCurrentBatteries());
        console.println("Enter number of batteries to use for cannon boost (0 for no boost):");
        
        try {
            String batteryInput = scanner.nextLine().trim();
            int batteriesToUse = Integer.parseInt(batteryInput);
            
            if (batteriesToUse < 0) {
                console.printError("Cannot use negative batteries. Using 0 batteries.");
                batteriesToUse = 0;
            }
            
            // Validate battery usage
            if (!validateBatteryUsage(batteriesToUse, "Cannon defense against meteors")) {
                console.println("Using maximum available batteries: " + getCurrentBatteries());
                batteriesToUse = getCurrentBatteries();
            }
            
            double finalStrength = totalCombat + batteriesToUse;
            
            console.println("\n⚔️ COMBAT RESOLUTION ⚔️");
            console.println("Declared cannon strength: " + finalStrength);
            if (batteriesToUse > 0) {
                console.println("Batteries committed: " + batteriesToUse);
            }
            console.println("🎲 Waiting for server to roll combat dice...");
            console.println("Your final strength will be: " + finalStrength + " + [dice roll]");
            console.println("Meteors will be destroyed if your total ≥ meteor strength");
            
            // Send cannon strength declaration
            DeclareStrengthRequest cannonRequest = new DeclareStrengthRequest(
                DeclareStrengthRequest.DecisionType.DECLARE_CANNON_STRENGTH, batteriesToUse);
            sendDeclareStrengthRequest(cannonRequest);
            
            currentCard = null;
            isMyTurn = false;
            
        } catch (NumberFormatException e) {
            console.printError("Invalid battery input. Using cannons without battery boost.");
            console.println("🎲 Rolling dice for basic cannon defense...");
            
            DeclareStrengthRequest cannonRequest = new DeclareStrengthRequest(
                DeclareStrengthRequest.DecisionType.DECLARE_CANNON_STRENGTH, 0);
            sendDeclareStrengthRequest(cannonRequest);
            currentCard = null;
            isMyTurn = false;
        }
    }
    
    private void handleShieldDefense() {
        int shieldStrength = getCurrentShieldStrength();
        
        console.println("Using shields to absorb meteor impact...");
        console.println("Your shield strength: " + shieldStrength);
        
        if (shieldStrength <= 0) {
            console.printError("No shields available! Taking direct damage instead.");
            handleDirectDamage();
            return;
        }
        
        console.println("Shields will absorb meteor damage using battery power.");
        console.println("Each shield use costs 1 battery per meteor blocked.");
        console.println("Available batteries: " + getCurrentBatteries());
        
        // For shield defense, we don't need batteries upfront - they're consumed per meteor blocked
        // Just indicate shield defense choice
        console.println("Shield defense activated. Batteries will be consumed as meteors are blocked.");
        
        // TODO: Create specific shield defense request or use existing system
        console.println("Shield defense choice recorded.");
        currentCard = null;
        isMyTurn = false;
    }
    
    private void handleDirectDamage() {
        console.println("Taking meteor damage directly...");
        console.println("⚠️  WARNING: Components will be destroyed by meteors!");
        console.println("No defense mounted - all meteors will hit your ship.");
        console.println("Prepare for impact...");
        
        // No request needed - server will apply direct damage
        currentCard = null;
        isMyTurn = false;
    }
    
    private void showMeteorSwarmOptions(MeteorSwarmCard meteorCard) {
        console.println("\n=== METEOR SWARM APPROACHING ===");
        console.println("Meteors detected:");
        
        for (int i = 0; i < meteorCard.getMeteorPattern().size(); i++) {
            Meteor meteor = meteorCard.getMeteorPattern().get(i);
            String intensity = meteor.getShotIntensity() == ShotIntensity.HEAVY ? "HEAVY" : "LIGHT";
            String direction = meteor.getApproach().toString();
            console.println("  " + (i + 1) + ". " + intensity + " meteor from " + direction);
        }
        
        console.println("\nDefense Options:");
        console.println("1. Use cannons to destroy meteors (requires cannon strength)");
        console.println("2. Use shields to absorb impact (consumes batteries)");
        console.println("3. Take damage directly (components will be destroyed)");
        console.println("\nYour ship stats:");
        console.println("  Cannons: " + getCurrentCannonStrength());
        console.println("  Batteries: " + getCurrentBatteries());
        console.println("  Shields: " + getCurrentShieldStrength());
        console.println("\nChoose defense strategy (1-3):");
    }
    
    /**
     * Handles war zone encounters
     */
    private void handleWarZoneChoice(String input) {
        try {
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1 -> {
                    console.println("Attempting to fight through the war zone...");
                    currentCard = null;
                    isMyTurn = false;
                }
                case 2 -> {
                    console.println("Trying to sneak around the combat area...");
                    currentCard = null;
                    isMyTurn = false;
                }
                case 3 -> {
                    console.println("Backing away from the war zone...");
                    currentCard = null;
                    isMyTurn = false;
                }
                default -> console.printError("Invalid choice. Enter 1, 2, or 3.");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter 1, 2, or 3.");
        }
    }
    
    private void showWarZoneOptions() {
        console.println("\n=== WAR ZONE ENCOUNTER ===");
        console.println("You've entered a dangerous combat zone!");
        console.println("Multiple threats detected:");
        console.println("\nResponse Options:");
        console.println("1. Fight through (requires strong cannons and crew)");
        console.println("2. Sneak around (requires engines and stealth)");
        console.println("3. Retreat (lose flight days but avoid damage)");
        console.println("\nYour ship capabilities:");
        console.println("  Combat Strength: " + getCurrentCannonStrength());
        console.println("  Engine Power: " + getCurrentEngineStrength());
        console.println("  Crew Members: " + getCurrentCrew());
        console.println("\nChoose action (1-3):");
    }
    
    /**
     * Handles special events like Stardust and Epidemic
     */
    private void handleSpecialEventChoice(String input) {
        try {
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1 -> {
                    console.println("Taking protective measures...");
                    currentCard = null;
                    isMyTurn = false;
                }
                case 2 -> {
                    console.println("Accepting the consequences...");
                    currentCard = null;
                    isMyTurn = false;
                }
                default -> console.printError("Invalid choice. Enter 1 or 2.");
            }
        } catch (NumberFormatException e) {
            console.printError("Invalid input. Please enter 1 or 2.");
        }
    }
    
    private void showSpecialEventOptions(AdventureCard card) {
        console.println("\n=== SPECIAL EVENT ===");
        console.println("Event: " + card.getType().toString());
        console.println(card.getDescription());
        
        switch (card.getType()) {
            case STARDUST -> {
                console.println("\nStardust interferes with ship systems!");
                console.println("You will lose 1 flight day per exposed connector.");
                console.println("Current exposed connectors: " + getCurrentExposedConnectors());
                console.println("This penalty is automatic and cannot be avoided.");
            }
            case EPIDEMIC -> {
                console.println("\nDisease outbreak on ship!");
                console.println("Crew members in connected cabins are at risk.");
                console.println("Current crew: " + getCurrentCrew());
                console.println("Connected cabins will lose crew members.");
            }
            default -> {
                // Handle other special events (like sabotage) generically
                console.println("\nSpecial event affects your ship!");
                console.println("Check the event description for specific effects.");
            }
        }
        
        console.println("\nOptions:");
        console.println("1. Use resources to minimize damage");
        console.println("2. Accept full consequences");
        console.println("\nChoose action (1-2):");
    }
    
    // Helper methods for getting ship statistics
    private double getCurrentCannonStrength() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().getCannons();
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0.0;
    }
    
    private int getCurrentShieldStrength() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                Ship ship = clientState.getLocalPlayerShip();
                Component[][] board = ship.getBoard();
                int shieldCount = 0;
                
                // Count all shield components on the ship
                for (Component[] row : board) {
                    for (Component component : row) {
                        if (component != null && component.getType() == ComponentType.SHIELD) {
                            shieldCount++;
                        }
                    }
                }
                
                return shieldCount;
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0;
    }
    
    private int getCurrentCrew() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().getCrew();
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0;
    }
    
    private int getCurrentExposedConnectors() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                Ship ship = clientState.getLocalPlayerShip();
                Component[][] board = ship.getBoard();
                int exposedCount = 0;
                
                // Check each component for exposed connectors
                for (int row = 0; row < board.length; row++) {
                    for (int col = 0; col < board[row].length; col++) {
                        Component component = board[row][col];
                        if (component != null) {
                            // Check each direction for exposed connectors
                            exposedCount += countExposedConnectorsForComponent(component, board, row, col);
                        }
                    }
                }
                
                return exposedCount;
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0;
    }
    
    private int countExposedConnectorsForComponent(Component component, Component[][] board, int row, int col) {
        int exposedCount = 0;
        
        // Check each direction
        Direction[] directions = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = {0, 0, -1, 1};
        
        for (int i = 0; i < directions.length; i++) {
            Direction dir = directions[i];
            ConnectorType connectorType = component.getConnectorAt(dir);
            
            // If this side has a connector (not PLAIN)
            if (connectorType != ConnectorType.PLAIN) {
                int newRow = row + dRow[i];
                int newCol = col + dCol[i];
                
                // Check if this connector is exposed (no adjacent component or off the board)
                boolean isExposed = false;
                
                if (newRow < 0 || newRow >= board.length || newCol < 0 || newCol >= board[0].length) {
                    // Off the board - definitely exposed
                    isExposed = true;
                } else if (board[newRow][newCol] == null) {
                    // No adjacent component - exposed
                    isExposed = true;
                } else {
                    // There is an adjacent component - check if their connectors are compatible
                    Component adjacent = board[newRow][newCol];
                    Direction oppositeDir = getOppositeDirection(dir);
                    ConnectorType adjacentConnector = adjacent.getConnectorAt(oppositeDir);
                    
                    // If adjacent component has no connector on this side, our connector is exposed
                    if (adjacentConnector == ConnectorType.PLAIN) {
                        isExposed = true;
                    }
                }
                
                if (isExposed) {
                    exposedCount++;
                }
            }
        }
        
        return exposedCount;
    }
    
    private Direction getOppositeDirection(Direction dir) {
        return switch (dir) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }
    
    private int getCurrentNormalCargoCapacity() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().calculateNormalGoodsCapacity();
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0;
    }
    
    private int getCurrentSpecialCargoCapacity() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().calculateSpecialGoodsCapacity();
            }
        } catch (Exception e) {
            // Fallback - return 0 if we can't get the actual value
        }
        return 0;
    }
    
    /**
     * Calculates combat bonus from purple aliens on the ship.
     * According to Galaxy Trucker rules, purple aliens provide +2 combat strength.
     */
    private int getPurpleAlienCombatBonus() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().getPurpleAlienCombatBonus();
            }
        } catch (Exception e) {
            // Fallback
        }
        return 0;
    }
    
    /**
     * Calculates engine bonus from brown aliens on the ship.
     * According to Galaxy Trucker rules, brown aliens provide +2 engine strength.
     */
    private int getBrownAlienEngineBonus() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayerShip() != null) {
                return clientState.getLocalPlayerShip().getBrownAlienEngineBonus();
            }
        } catch (Exception e) {
            // Fallback
        }
        return 0;
    }
    
    /**
     * Shows alien passenger information
     */
    public void displayAlienPassengers() {
        console.println("\n=== ALIEN PASSENGERS ===");
        int purpleAliens = getPurpleAlienCombatBonus() / 2; // Each gives +2, so divide by 2 to get count
        int brownAliens = getBrownAlienEngineBonus() / 2;
        
        if (purpleAliens > 0 || brownAliens > 0) {
            console.println("Active alien bonuses:");
            if (purpleAliens > 0) {
                console.println("  Purple aliens: " + purpleAliens + " (+" + (purpleAliens * 2) + " combat strength)");
            }
            if (brownAliens > 0) {
                console.println("  Brown aliens: " + brownAliens + " (+" + (brownAliens * 2) + " engine strength)");
            }
        } else {
            console.println("No alien passengers currently aboard.");
        }
        
        console.println("\nRemember: Aliens require life support systems!");
        console.println("If life support is destroyed, aliens are lost.");
        console.println("");
    }
    
    /**
     * Handles voluntary giving up decision
     */
    private void handleGiveUpChoice() {
        console.println("\n=== GIVE UP FLIGHT ===");
        console.println("Are you sure you want to give up this flight?");
        console.println("Consequences:");
        console.println("  - No further rewards or position bonuses");
        console.println("  - Goods sold at half price");
        console.println("  - Still pay penalties for lost components");
        console.println("  - Finish last in the race");
        
        console.println("\nType 'yes' to confirm giving up, or anything else to continue:");
        
        try {
            String confirmation = scanner.nextLine().trim().toLowerCase();
            if ("yes".equals(confirmation)) {
                console.println("Giving up flight... returning to base.");
                // TODO: Send give up request to server
                // For now, just notify user
                console.println("Flight abandoned. You'll receive final scoring at journey's end.");
            } else {
                console.println("Continuing flight...");
            }
        } catch (Exception e) {
            console.printError("Error reading input. Continuing flight...");
        }
    }
    
    /**
     * Checks for forced giving up conditions
     */
    public void checkForcedGiveUp() {
        ClientState clientState = controller.getClientState();
        if (clientState == null || clientState.getLocalPlayerShip() == null) {
            return;
        }
        
        Ship ship = clientState.getLocalPlayerShip();
        boolean shouldGiveUp = false;
        String reason = "";
        
        // Check for forced giving up conditions according to Galaxy Trucker rules
        if (ship.getCrew() <= 0) {
            shouldGiveUp = true;
            reason = "No crew members remaining to operate the ship";
        } else if (ship.getEngines() <= 0) {
            shouldGiveUp = true;
            reason = "No engine power remaining for Open Space navigation";
        }
        // TODO: Add "getting lapped by leader" check when position tracking is fully implemented
        
        if (shouldGiveUp) {
            console.println("\n⚠️  FORCED TO GIVE UP ⚠️");
            console.println("Reason: " + reason);
            console.println("Your flight has ended. You must give up and return to base.");
            console.println("Final scoring will be calculated at journey's end.");
            // TODO: Automatically send give up request to server
        }
    }
    
    /**
     * Handles sabotage events (can be called from server when sabotage occurs)
     */
    public void handleSabotageEvent(String targetPlayer) {
        console.println("\n💥 === SABOTAGE EVENT === 💥");
        console.println("Internal sabotage detected!");
        console.println("Target: " + targetPlayer);
        
        if (targetPlayer.equals(getCurrentPlayerName())) {
            console.println("⚠️  YOUR SHIP HAS BEEN SABOTAGED!");
            console.println("A random component will be destroyed by saboteurs.");
            console.println("Security protocols failed to prevent the attack.");
            
            // Show current ship state for context
            console.println("\nYour ship before sabotage:");
            console.println("  Crew: " + getCurrentCrew());
            console.println("  Components at risk: All connected components");
            console.println("\nSabotage targeting ship with smallest crew...");
            
            // Check if this triggers forced giving up
            checkForcedGiveUp();
        } else {
            console.println(targetPlayer + "'s ship has been sabotaged.");
            console.println("Their ship will lose a random component.");
        }
        
        console.println("");
    }
    
    /**
     * Gets the current player's name
     */
    private String getCurrentPlayerName() {
        try {
            ClientState clientState = controller.getClientState();
            if (clientState != null && clientState.getLocalPlayer() != null) {
                return clientState.getLocalPlayer().getId().getNickname();
            }
        } catch (Exception e) {
            // Fallback
        }
        return "Unknown Player";
    }
    
    /**
     * Validates battery usage and provides detailed feedback
     */
    private boolean validateBatteryUsage(int requestedBatteries, String purpose) {
        int availableBatteries = getCurrentBatteries();
        
        console.println("\n=== BATTERY USAGE VALIDATION ===");
        console.println("Purpose: " + purpose);
        console.println("Requested batteries: " + requestedBatteries);
        console.println("Available batteries: " + availableBatteries);
        
        if (requestedBatteries < 0) {
            console.printError("❌ Cannot use negative batteries!");
            return false;
        }
        
        if (requestedBatteries == 0) {
            console.println("✅ No batteries will be used.");
            return true;
        }
        
        if (requestedBatteries > availableBatteries) {
            console.printError("❌ Insufficient batteries!");
            console.println("You need " + requestedBatteries + " but only have " + availableBatteries);
            console.println("Maximum usable: " + availableBatteries);
            return false;
        }
        
        console.println("✅ Battery usage validated.");
        console.println("Batteries after use: " + (availableBatteries - requestedBatteries));
        return true;
    }
    
    /**
     * Shows detailed battery breakdown for the ship
     */
    public void displayDetailedBatteryInfo() {
        console.println("\n=== DETAILED BATTERY STATUS ===");
        
        try {
            ClientState clientState = controller.getClientState();
            if (clientState == null || clientState.getLocalPlayerShip() == null) {
                console.printError("Ship information not available");
                return;
            }
            
            Ship ship = clientState.getLocalPlayerShip();
            Component[][] board = ship.getBoard();
            int totalBatteries = 0;
            int batteryComponents = 0;
            
            console.println("Battery Components on Ship:");
            
            for (int row = 0; row < board.length; row++) {
                for (int col = 0; col < board[row].length; col++) {
                    Component component = board[row][col];
                    if (component != null && component.getType() == ComponentType.BATTERY) {
                        batteryComponents++;
                        // Assume each battery component provides 2-3 batteries based on Galaxy Trucker rules
                        int componentBatteries = 2; // Default assumption - could be read from component if available
                        totalBatteries += componentBatteries;
                        
                        console.println("  Battery at (" + (row + 5) + "," + (col + 4) + "): " + componentBatteries + " batteries");
                    }
                }
            }
            
            console.println("\nBattery Summary:");
            console.println("  Battery components: " + batteryComponents);
            console.println("  Total batteries: " + totalBatteries);
            console.println("  Currently available: " + ship.getBatteries());
            
            if (ship.getBatteries() < totalBatteries) {
                console.println("⚠️  Some batteries may be committed to other systems");
                console.println("  Committed batteries: " + (totalBatteries - ship.getBatteries()));
            }
            
        } catch (Exception e) {
            console.printError("Error retrieving battery information: " + e.getMessage());
        }
        
        console.println("");
    }
    
    /**
     * Shows power management options and current commitments
     */
    public void displayPowerManagement() {
        console.println("\n=== POWER MANAGEMENT ===");
        
        double enginePower = getCurrentEngineStrength();
        double cannonPower = getCurrentCannonStrength();
        int batteries = getCurrentBatteries();
        int shields = getCurrentShieldStrength();
        
        console.println("Current Power Status:");
        console.println("  Engine strength: " + enginePower);
        console.println("  Cannon strength: " + cannonPower);
        console.println("  Available batteries: " + batteries);
        console.println("  Shield generators: " + shields);
        
        // Show alien bonuses if any
        int purpleBonus = getPurpleAlienCombatBonus();
        int brownBonus = getBrownAlienEngineBonus();
        
        if (purpleBonus > 0 || brownBonus > 0) {
            console.println("\nAlien Bonuses:");
            if (purpleBonus > 0) {
                console.println("  Purple alien combat bonus: +" + purpleBonus);
            }
            if (brownBonus > 0) {
                console.println("  Brown alien engine bonus: +" + brownBonus);
            }
        }
        
        console.println("\nPower Usage Guidelines:");
        console.println("  - Each battery adds +1 to engine or cannon strength");
        console.println("  - Shield use requires 1 battery per meteor blocked");
        console.println("  - Double engines/cannons require battery power to function");
        console.println("  - Once committed, batteries cannot be recovered during flight");
        
        console.println("");
    }
    
    /**
     * Shows component disconnection and structural integrity information
     */
    public void displayComponentDisconnection(Set<Position> disconnectedPositions) {
        if (disconnectedPositions.isEmpty()) {
            return;
        }
        
        console.println("\n⚠️  === STRUCTURAL FAILURE === ⚠️");
        console.println("Components have been disconnected from your ship!");
        console.println("Disconnected components fall off into space and are lost.");
        
        console.println("\nComponents lost at positions:");
        for (Position pos : disconnectedPositions) {
            console.println("  Row " + (pos.getRow() + 5) + ", Column " + (pos.getCol() + 4));
        }
        
        console.println("\nStructural integrity compromised!");
        console.println("Lost components cost 1 credit each at journey's end.");
        console.println("Check your ship grid to see the damage...");
        
        // Refresh the ship display to show damage
        displayFullInterface();
        
        // Check if this triggers forced giving up
        checkForcedGiveUp();
    }
    
    /**
     * Analyzes ship structural integrity and warns about potential disconnections
     */
    public void checkStructuralIntegrity() {
        ClientState clientState = controller.getClientState();
        if (clientState == null || clientState.getLocalPlayerShip() == null) {
            return;
        }
        
        Ship ship = clientState.getLocalPlayerShip();
        Component[][] board = ship.getBoard();
        Set<Component> lostComponents = ship.getLostComponents();
        
        console.println("\n=== STRUCTURAL INTEGRITY CHECK ===");
        
        int totalComponents = 0;
        int damagedComponents = lostComponents.size();
        int vulnerableComponents = 0;
        
        // Count total and vulnerable components
        for (Component[] row : board) {
            for (Component component : row) {
                if (component != null) {
                    totalComponents++;
                    // TODO: Add logic to check if component is vulnerable to disconnection
                    // This would require pathfinding to ensure all components remain connected
                    // to the cabin after potential damage
                }
            }
        }
        
        console.println("Ship Structure Analysis:");
        console.println("  Total components: " + totalComponents);
        console.println("  Damaged components: " + damagedComponents);
        console.println("  Vulnerable to disconnection: " + vulnerableComponents + " (TODO: implement full check)");
        
        if (damagedComponents > 0) {
            console.println("⚠️  WARNING: Damaged components may cause cascading failures!");
        }
        
        console.println("Remember: If components become disconnected from your cabin,");
        console.println("they fall off and are permanently lost!");
        console.println("");
    }
    
    /**
     * Displays turn indicator with animated effects.
     */
    private void displayTurnIndicator() {
        if (isMyTurn) {
            console.println("🌟 YOUR TURN 🌟");
            if (turnTimeRemaining > 0) {
                long seconds = turnTimeRemaining / 1000;
                console.println("⏰ Time remaining: " + seconds + " seconds");
            }
        } else {
            console.println("⏳ Waiting for other players...");
        }
        console.println("");
    }
    
    /**
     * Checks for movement updates and shows animation.
     */
    private void checkForMovementUpdates() {
        ClientState clientState = context.getClientState();
        if (clientState == null || clientState.getGameModel() == null) {
            return;
        }
        
        // Compare current positions with previous positions
        for (Player player : clientState.getGameModel().getPlayers()) {
            String playerId = player.getId().toString();
            int currentPos = player.getFlightData().getPosition();
            Integer previousPos = previousPlayerPositions.get(playerId);
            
            if (previousPos != null && currentPos != previousPos) {
                // Position changed - show movement animation
                showMovementAnimation(player.getNickname(), previousPos, currentPos);
            }
            
            previousPlayerPositions.put(playerId, currentPos);
        }
    }
    
    /**
     * Shows movement animation for player position changes.
     */
    private void showMovementAnimation(String playerName, int fromPos, int toPos) {
        console.println("");
        if (toPos > fromPos) {
            console.println("🚀 " + playerName + " advances from position " + fromPos + " to " + toPos);
            lastActionPerformed = playerName + " moved forward " + (toPos - fromPos) + " spaces";
        } else {
            console.println("📉 " + playerName + " moves back from position " + fromPos + " to " + toPos);
            lastActionPerformed = playerName + " moved backward " + (fromPos - toPos) + " spaces";
        }
        console.println("");
    }
    
    /**
     * Checks for ship damage updates.
     */
    private void checkForShipDamageUpdates() {
        ClientState clientState = context.getClientState();
        if (clientState == null || clientState.getLocalPlayerShip() == null) {
            return;
        }
        
        Ship ship = clientState.getLocalPlayerShip();
        Set<Component> lostComponents = ship.getLostComponents();
        
        // Simple check - if there are lost components, show damage animation
        if (!lostComponents.isEmpty()) {
            showDamageAnimation = true;
        }
    }
    
    /**
     * Shows ship damage animation effects.
     */
    private void showShipDamageAnimation() {
        console.println("");
        console.println("💥💥💥 IMPACT DETECTED 💥💥💥");
        console.println("⚠️  Ship systems analyzing damage...");
        
        // Simulate damage assessment delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        console.println("🔧 Damage assessment complete.");
        console.println("Check ship display for component status.");
        console.println("");
        
        lastActionPerformed = "Ship took damage - components may be lost";
    }
    
    /**
     * Updates turn status with enhanced feedback.
     */
    public void updateTurnStatus(boolean isPlayerTurn, long timeRemaining) {
        this.isMyTurn = isPlayerTurn;
        this.turnTimeRemaining = timeRemaining;
        
        if (isPlayerTurn) {
            lastActionPerformed = "Your turn started";
        }
    }
    
    /**
     * Enhanced real-time refresh that preserves user context.
     */
    public void enhancedRefresh() {
        // Store current display state
        boolean wasMyTurn = isMyTurn;
        String lastAction = lastActionPerformed;
        
        // Perform standard refresh
        onRefresh();
        
        // Show status changes
        if (wasMyTurn != isMyTurn) {
            if (isMyTurn) {
                console.println("🎯 It's now your turn to make decisions!");
            } else {
                console.println("⏳ Turn passed to next player.");
            }
        }
    }
}
