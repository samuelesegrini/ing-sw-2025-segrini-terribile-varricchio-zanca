package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.*;

/**
 * Manages goods shortage mechanics for adventure cards.
 * Implements Galaxy Trucker rules for first-come-first-served goods distribution,
 * capacity validation, and shortage warnings.
 */
public class GoodsShortageManager {
    
    /**
     * Validates if a player can land on a specific planet given their cargo capacity.
     * Checks both normal and special cargo holds.
     * 
     * @param player The player attempting to land
     * @param planet The planet to land on
     * @return A validation result with success status and detailed messages
     */
    public static GoodsValidationResult validatePlanetLanding(Player player, Planet planet) {
        if (planet.isVisited()) {
            return new GoodsValidationResult(false, 
                "Planet " + planet.getNumber() + " has already been claimed by another player");
        }
        
        Map<GoodType, Integer> planetGoods = planet.getGoodQuantities();
        
        // Check if player has sufficient cargo capacity
        boolean canStore = player.getShip().addResources(planetGoods);
        if (!canStore) {
            StringBuilder reason = new StringBuilder("Insufficient cargo capacity for planet ")
                .append(planet.getNumber()).append(". Needs: ");
            
            for (Map.Entry<GoodType, Integer> entry : planetGoods.entrySet()) {
                if (entry.getValue() > 0) {
                    reason.append(entry.getValue()).append(" ").append(entry.getKey()).append(" ");
                }
            }
            
            if (planet.requiresSpecialCargo()) {
                reason.append("(requires special cargo hold for RED goods)");
            }
            
            return new GoodsValidationResult(false, reason.toString());
        }
        
        return new GoodsValidationResult(true, "Can land on planet " + planet.getNumber());
    }
    
    /**
     * Analyzes planet competition and provides strategic information for players.
     * 
     * @param planets List of available planets
     * @param players List of players in turn order
     * @return Competition analysis for each planet
     */
    public static Map<Integer, PlanetCompetitionInfo> analyzePlanetCompetition(
            List<Planet> planets, List<Player> players) {
        
        Map<Integer, PlanetCompetitionInfo> analysis = new HashMap<>();
        
        for (Planet planet : planets) {
            if (!planet.isVisited()) {
                PlanetCompetitionInfo info = new PlanetCompetitionInfo();
                info.planetNumber = planet.getNumber();
                info.totalValue = planet.calculateTotalValue();
                info.requiresSpecialCargo = planet.requiresSpecialCargo();
                info.totalGoods = planet.getTotalGoodsQuantity();
                
                // Count how many players can potentially land on this planet
                int eligiblePlayers = 0;
                for (Player player : players) {
                    GoodsValidationResult validation = validatePlanetLanding(player, planet);
                    if (validation.isSuccess()) {
                        eligiblePlayers++;
                        info.eligiblePlayers.add(player.getPlayerId());
                    }
                }
                
                info.competitionLevel = calculateCompetitionLevel(eligiblePlayers, info.totalValue);
                analysis.put(planet.getNumber(), info);
            }
        }
        
        return analysis;
    }
    
    /**
     * Calculates competition level based on eligible players and planet value.
     */
    private static CompetitionLevel calculateCompetitionLevel(int eligiblePlayers, int totalValue) {
        if (eligiblePlayers <= 1) {
            return CompetitionLevel.NONE;
        } else if (eligiblePlayers == 2) {
            return totalValue >= 8 ? CompetitionLevel.MODERATE : CompetitionLevel.LOW;
        } else if (eligiblePlayers == 3) {
            return totalValue >= 6 ? CompetitionLevel.HIGH : CompetitionLevel.MODERATE;
        } else { // 4+ players
            return CompetitionLevel.EXTREME;
        }
    }
    
    /**
     * Generates shortage warnings for players about limited goods availability.
     */
    public static List<String> generateShortageWarnings(Map<Integer, PlanetCompetitionInfo> competition) {
        List<String> warnings = new ArrayList<>();
        
        for (PlanetCompetitionInfo info : competition.values()) {
            switch (info.competitionLevel) {
                case HIGH:
                    warnings.add("WARNING: Planet " + info.planetNumber + 
                        " (value: " + info.totalValue + " credits) has high competition!");
                    break;
                case EXTREME:
                    warnings.add("CRITICAL: Planet " + info.planetNumber + 
                        " (value: " + info.totalValue + " credits) has extreme competition! " +
                        "Only one player will succeed!");
                    break;
            }
            
            if (info.requiresSpecialCargo && info.eligiblePlayers.size() > 1) {
                warnings.add("NOTICE: Planet " + info.planetNumber + 
                    " requires special cargo holds for RED goods");
            }
        }
        
        return warnings;
    }
    
    /**
     * Result of goods validation for planet landing.
     */
    public static class GoodsValidationResult {
        private final boolean success;
        private final String message;
        
        public GoodsValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    /**
     * Information about competition for a specific planet.
     */
    public static class PlanetCompetitionInfo {
        public int planetNumber;
        public int totalValue;
        public int totalGoods;
        public boolean requiresSpecialCargo;
        public CompetitionLevel competitionLevel;
        public List<PlayerId> eligiblePlayers = new ArrayList<>();
    }
    
    /**
     * Competition levels for planet goods.
     */
    public enum CompetitionLevel {
        NONE,     // 0-1 eligible players
        LOW,      // 2 players, low value
        MODERATE, // 2-3 players, mixed value
        HIGH,     // 3+ players, high value
        EXTREME   // 4+ players
    }
}