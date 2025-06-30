package it.polimi.ingsw.server.model.domain.ship;

import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;

import java.util.*;
import java.util.logging.Logger;

/**
 * Centralized ship validation service that consolidates all validation logic
 * for Galaxy Trucker. This replaces the fragmented validation across multiple
 * layers and provides a single source of truth for ship validation.
 * 
 * Handles all building completion scenarios:
 * 1. Timer expiration validation
 * 2. Individual player ship finishing
 * 3. All-players-ready scenarios
 * 4. Manual validation requests
 */
public class UnifiedShipValidationService {
    private static final Logger LOGGER = Logger.getLogger(UnifiedShipValidationService.class.getName());
    
    public enum ValidationContext {
        /** Validation during building phase - real-time feedback */
        BUILDING_REALTIME,
        /** Validation when player finishes ship manually */
        PLAYER_FINISH,
        /** Validation when timer expires - mandatory correction */
        TIMER_EXPIRATION,
        /** Validation for all players when building phase ends */
        BUILDING_COMPLETION,
        /** Validation during flight phase operations */
        FLIGHT_OPERATION
    }
    
    public enum ValidationTrigger {
        MANUAL_REQUEST,          // ValidateShipRequest
        SHIP_FINISH,            // FinishShipRequest  
        TIMER_EXPIRED,          // BuildingTimer timeout
        ALL_PLAYERS_READY,      // All players marked ready
        PHASE_TRANSITION        // Building -> Flight transition
    }
    
    public static class UnifiedValidationResult {
        private final boolean isValid;
        private final boolean requiresCorrection;
        private final List<String> errors;
        private final List<String> warnings;
        private final List<String> tips;
        private final List<Component> illegalComponents;
        private final ValidationContext context;
        private final ValidationTrigger trigger;
        
        public UnifiedValidationResult(boolean isValid, boolean requiresCorrection,
                                     List<String> errors, List<String> warnings, List<String> tips,
                                     List<Component> illegalComponents,
                                     ValidationContext context, ValidationTrigger trigger) {
            this.isValid = isValid;
            this.requiresCorrection = requiresCorrection;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
            this.tips = new ArrayList<>(tips);
            this.illegalComponents = new ArrayList<>(illegalComponents);
            this.context = context;
            this.trigger = trigger;
        }
        
        // Getters
        public boolean isValid() { return isValid; }
        public boolean requiresCorrection() { return requiresCorrection; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public List<String> getTips() { return new ArrayList<>(tips); }
        public List<Component> getIllegalComponents() { return new ArrayList<>(illegalComponents); }
        public ValidationContext getContext() { return context; }
        public ValidationTrigger getTrigger() { return trigger; }
        
        public boolean hasIssues() { return !errors.isEmpty() || !warnings.isEmpty(); }
        public List<String> getAllFeedback() {
            List<String> feedback = new ArrayList<>();
            feedback.addAll(errors);
            if (!warnings.isEmpty()) {
                if (!errors.isEmpty()) feedback.add("--- Warnings ---");
                feedback.addAll(warnings);
            }
            if (!tips.isEmpty()) {
                if (!errors.isEmpty() || !warnings.isEmpty()) feedback.add("--- Tips ---");
                feedback.addAll(tips);
            }
            return feedback;
        }
    }
    
    /**
     * CENTRAL VALIDATION METHOD - Single entry point for all ship validation
     * 
     * @param ship The ship to validate
     * @param context Validation context (building, timer, etc.)
     * @param trigger What triggered this validation
     * @param currentPhase Current game phase
     * @return Comprehensive validation result
     */
    public static UnifiedValidationResult validateShip(Ship ship, ValidationContext context, 
                                                      ValidationTrigger trigger, GamePhase currentPhase) {
        LOGGER.info("Ship validation - Context: " + context + ", Trigger: " + trigger + ", Phase: " + currentPhase);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> tips = new ArrayList<>();
        List<Component> illegalComponents = new ArrayList<>();
        boolean requiresCorrection = false;
        
        // Always update ship stats first
        ship.updateStats();
        
        // Core validation based on context
        switch (context) {
            case BUILDING_REALTIME -> {
                // Real-time validation during building - lenient, informative
                validateBuildingRealtime(ship, errors, warnings, tips);
            }
            case PLAYER_FINISH -> {
                // Player manually finishing - strict validation, allow player choice
                validatePlayerFinish(ship, errors, warnings, tips, illegalComponents);
                requiresCorrection = !illegalComponents.isEmpty();
            }
            case TIMER_EXPIRATION -> {
                // Timer expired - mandatory correction, no player choice
                validateTimerExpiration(ship, errors, warnings, tips, illegalComponents);
                requiresCorrection = true; // Always correct on timer expiration
            }
            case BUILDING_COMPLETION -> {
                // Building phase ending - comprehensive check for all players
                validateBuildingCompletion(ship, errors, warnings, tips, illegalComponents);
                requiresCorrection = !illegalComponents.isEmpty();
            }
            case FLIGHT_OPERATION -> {
                // During flight - structural integrity focus
                validateFlightOperation(ship, currentPhase, errors, warnings, tips);
            }
        }
        
        boolean isValid = errors.isEmpty() && !requiresCorrection;
        
        LOGGER.info("Validation result - Valid: " + isValid + ", Errors: " + errors.size() + 
                   ", Warnings: " + warnings.size() + ", Illegal components: " + illegalComponents.size());
        
        return new UnifiedValidationResult(isValid, requiresCorrection, errors, warnings, tips, 
                                         illegalComponents, context, trigger);
    }
    
    /**
     * Validates multiple ships for building completion scenarios
     * (timer expiration, all players ready, phase transition)
     */
    public static Map<PlayerId, UnifiedValidationResult> validateAllShips(GameModel gameModel, 
                                                                          ValidationContext context,
                                                                          ValidationTrigger trigger) {
        LOGGER.info("Validating all ships - Context: " + context + ", Trigger: " + trigger);
        
        Map<PlayerId, UnifiedValidationResult> results = new HashMap<>();
        GamePhase currentPhase = gameModel.getCurrentPhase();
        
        for (Player player : gameModel.getPlayers()) {
            if (player != null && player.getShip() != null) {
                PlayerId playerId = player.getId();
                Ship ship = player.getShip();
                
                UnifiedValidationResult result = validateShip(ship, context, trigger, currentPhase);
                results.put(playerId, result);
                
                LOGGER.info("Player " + playerId + " validation: " + 
                           (result.isValid() ? "VALID" : "INVALID") + 
                           " (Errors: " + result.getErrors().size() + 
                           ", Corrections needed: " + result.requiresCorrection() + ")");
            }
        }
        
        return results;
    }
    
    /**
     * Real-time validation during building phase
     * Provides helpful feedback without blocking gameplay
     */
    private static void validateBuildingRealtime(Ship ship, List<String> errors, 
                                                List<String> warnings, List<String> tips) {
        // Use existing comprehensive validation but with different severity levels
        ShipValidationService.ValidationResult galaxyResult = 
            ShipValidationService.validateGalaxyTruckerRules(ship, GamePhase.BUILDING);
        
        // Convert errors to warnings for real-time (non-blocking)
        warnings.addAll(galaxyResult.getErrors());
        warnings.addAll(galaxyResult.getWarnings());
        
        // Add helpful tips
        tips.add("💡 Use 'Validate Ship' to check for rule violations");
        tips.add("💡 Use 'Finish Ship' when ready to lock in your design");
    }
    
    /**
     * Validation when player manually finishes ship
     * Strict validation but gives player final say
     */
    private static void validatePlayerFinish(Ship ship, List<String> errors, 
                                           List<String> warnings, List<String> tips,
                                           List<Component> illegalComponents) {
        // Check for rule violations
        List<Component> violations = ship.checkPlacingErrors();
        illegalComponents.addAll(violations);
        
        if (!violations.isEmpty()) {
            errors.add("⚠️ Ship has " + violations.size() + " rule violations");
            errors.add("🔧 Choose: Fix manually or finish anyway (auto-correction during transition)");
            
            for (Component violation : violations) {
                warnings.add("Rule violation: " + violation.getType() + " at invalid position");
            }
        }
        
        // Comprehensive Galaxy Trucker validation
        ShipValidationService.ValidationResult galaxyResult = 
            ShipValidationService.validateGalaxyTruckerRules(ship, GamePhase.BUILDING);
        
        // Severe issues become errors
        List<String> galaxyErrors = galaxyResult.getErrors();
        if (!galaxyErrors.isEmpty()) {
            errors.add("🚨 Critical ship issues detected:");
            errors.addAll(galaxyErrors);
        }
        
        warnings.addAll(galaxyResult.getWarnings());
        tips.add("💡 Ship will be automatically corrected during building→flight transition");
    }
    
    /**
     * Validation when timer expires
     * Mandatory correction, no player choice
     */
    private static void validateTimerExpiration(Ship ship, List<String> errors, 
                                               List<String> warnings, List<String> tips,
                                               List<Component> illegalComponents) {
        LOGGER.info("Timer expiration validation - mandatory correction mode");
        
        // Find all illegal components for removal
        List<Component> violations = ship.checkPlacingErrors();
        illegalComponents.addAll(violations);
        
        // Check connection errors
        List<Component> connectionErrors = ship.checkConnectingErrors();
        illegalComponents.addAll(connectionErrors);
        
        // Timer expiration = mandatory correction
        if (!illegalComponents.isEmpty()) {
            warnings.add("⏰ Timer expired - " + illegalComponents.size() + " components will be auto-removed");
            warnings.add("💰 Each removed component costs 1 credit penalty");
        }
        
        // Comprehensive validation for warnings
        ShipValidationService.ValidationResult galaxyResult = 
            ShipValidationService.validateGalaxyTruckerRules(ship, GamePhase.BUILDING);
        warnings.addAll(galaxyResult.getWarnings());
        
        tips.add("⏰ Galaxy Trucker rule: Timer expiration triggers mandatory ship correction");
    }
    
    /**
     * Validation for building completion (all ships)
     * Comprehensive check for transition to flight
     */
    private static void validateBuildingCompletion(Ship ship, List<String> errors, 
                                                  List<String> warnings, List<String> tips,
                                                  List<Component> illegalComponents) {
        LOGGER.info("Building completion validation");
        
        // Find illegal components for mandatory correction
        List<Component> violations = ship.checkPlacingErrors();
        illegalComponents.addAll(violations);
        
        // Comprehensive Galaxy Trucker validation
        ShipValidationService.ValidationResult galaxyResult = 
            ShipValidationService.validateGalaxyTruckerRules(ship, GamePhase.BUILDING);
        
        // Critical structural issues
        List<String> galaxyErrors = galaxyResult.getErrors();
        if (!galaxyErrors.isEmpty()) {
            errors.addAll(galaxyErrors);
        }
        
        warnings.addAll(galaxyResult.getWarnings());
        
        if (!illegalComponents.isEmpty()) {
            warnings.add("🔧 " + illegalComponents.size() + " components will be auto-corrected for flight");
        }
        
        tips.add("🚀 Preparing for building→flight transition");
        tips.add("⚡ All ships will receive crew, batteries, and final corrections");
    }
    
    /**
     * Validation during flight operations
     * Focus on structural integrity and operational capacity
     */
    private static void validateFlightOperation(Ship ship, GamePhase currentPhase,
                                              List<String> errors, List<String> warnings, List<String> tips) {
        // During flight, focus on operational capacity
        ship.updateStats();
        
        // Check minimum operational requirements
        if (ship.getEngines() <= 0) {
            errors.add("🚨 Ship has no engines - cannot move!");
        }
        
        if (ship.getCrew() <= 0) {
            warnings.add("⚠️ Ship has no crew - limited operational capacity");
        }
        
        // Check structural integrity  
        List<Component> connectionErrors = ship.checkConnectingErrors();
        if (!connectionErrors.isEmpty()) {
            warnings.add("⚠️ " + connectionErrors.size() + " components have connection issues");
        }
        
        tips.add("🛠️ Flight operations require engines, crew, and structural integrity");
    }
    
    /**
     * Legacy compatibility method for existing ShipValidationService calls
     */
    public static ShipValidationService.ValidationResult validateLegacy(Ship ship, GamePhase currentPhase) {
        UnifiedValidationResult result = validateShip(ship, ValidationContext.BUILDING_REALTIME, 
                                                     ValidationTrigger.MANUAL_REQUEST, currentPhase);
        
        return new ShipValidationService.ValidationResult(
            result.isValid(),
            result.getErrors(),
            result.getWarnings()
        );
    }
    
    /**
     * Applies mandatory ship correction based on validation results
     * Used during timer expiration and building completion
     */
    public static int applyMandatoryCorrection(Ship ship, UnifiedValidationResult validationResult) {
        if (!validationResult.requiresCorrection()) {
            return 0;
        }
        
        List<Component> toRemove = validationResult.getIllegalComponents();
        int removedCount = 0;
        
        LOGGER.info("Applying mandatory correction - removing " + toRemove.size() + " illegal components");
        
        for (Component component : toRemove) {
            Position position = findComponentPosition(ship, component);
            if (position != null) {
                try {
                    ship.removeComponent(position, GamePhase.BUILDING);
                    removedCount++;
                    LOGGER.info("Removed illegal component: " + component.getType() + 
                               " at (" + position.getRow() + "," + position.getCol() + ")");
                } catch (Exception e) {
                    LOGGER.warning("Failed to remove component at " + position + ": " + e.getMessage());
                }
            }
        }
        
        // Update ship stats after corrections
        ship.updateStats();
        
        LOGGER.info("Mandatory correction completed - " + removedCount + " components removed");
        return removedCount;
    }
    
    /**
     * Helper method to find component position on ship board
     */
    private static Position findComponentPosition(Ship ship, Component component) {
        Component[][] board = ship.getBoard();
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                if (board[row][col] == component) {
                    return new Position(row, col);
                }
            }
        }
        return null;
    }
}