package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.domain.adventure.entity.Planet;
import it.polimi.ingsw.server.model.domain.flight.FlightBoard;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.adventure.CombatAttributeType;
import it.polimi.ingsw.server.model.enums.adventure.PenaltyType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import it.polimi.ingsw.server.model.enums.resource.GoodType;

import java.util.*;
import java.util.Objects;

/**
 * Visitor interface for processing different types of adventure cards.
 * <p>
 * This interface implements the Visitor design pattern to allow operations 
 * on adventure cards without modifying their classes. Each method handles 
 * a specific card type and can return a generic result.
 * </p>
 *
 */
public class AdventureCardVisitor {

    /**
     * Visits an AbandonedShipCard following exact Galaxy Trucker rules:
     * - First-come-first-served opportunity
     * - Only one player can use the opportunity
     * - Requires sufficient crew to attempt salvage
     * - Costs crew members, gains credits, loses flight days
     *
     * @param card The abandoned ship card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitAbandonedShipCard(AbandonedShipCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());
        System.out.println("  Opportunity: Lose " + card.getCrewLost() + " crew, gain " + 
                         card.getCreditsGained() + " credits, lose " + card.getLostDays() + " flight days");

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        // First-come-first-served: check players in flight order
        for(Player player : playersOrdered) {
            System.out.println("\nPlayer " + player.getId().getNickname() + " - Crew: " + player.getShip().getCrew());
            
            // Check if player has enough crew to attempt salvage
            if (player.getShip().getCrew() > card.getCrewLost()) {
                System.out.println("  ELIGIBLE - Attempting salvage...");
                
                // Player automatically takes the opportunity (in Galaxy Trucker, this is usually automatic)
                // Remove crew members
                int originalCrew = player.getShip().getCrew();
                player.getShip().setCrew(originalCrew - card.getCrewLost());
                
                // Add credits
                player.addCredits(card.getCreditsGained());
                
                // Lose flight days
                int originalPosition = player.getFlightData().getPosition();
                flightBoard.movePlayer(player, card.getLostDays(), false);
                int newPosition = player.getFlightData().getPosition();
                
                System.out.println("  SUCCESS! Crew: " + originalCrew + " -> " + player.getShip().getCrew() + 
                                 ", Credits: +" + card.getCreditsGained() + 
                                 ", Position: " + originalPosition + " -> " + newPosition);
                
                // Opportunity taken - remaining players miss out
                for (int i = playersOrdered.indexOf(player) + 1; i < playersOrdered.size(); i++) {
                    Player remainingPlayer = playersOrdered.get(i);
                    System.out.println("Player " + remainingPlayer.getId().getNickname() + " - Too late, opportunity taken");
                }
                
                return true; // Opportunity used, card resolved
                
            } else {
                System.out.println("  INELIGIBLE - Insufficient crew (needs > " + card.getCrewLost() + ")");
            }
        }
        
        // No eligible players found
        System.out.println("\nNo players eligible to salvage abandoned ship - opportunity lost");
        return true;
    }

    /**
     * Visits a MeteorSwarmCard following exact Galaxy Trucker rules:
     * - Handle meteors sequentially (top to bottom on card)
     * - Leader rolls 2 dice for each meteor impact location
     * - All players affected simultaneously by same dice roll
     * - Small meteors bounce off smooth sides, can be blocked by shields
     * - Large meteors must be shot by cannons pointing at them
     *
     * @param card The meteor swarm card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitMeteorSwarmCard(MeteorSwarmCard card, GameModel state){
        System.out.println("Resolving "+card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        if(flightBoard.getPlayerCount()==1){
            System.out.println("Skipping " +card.getType()+" card - single player game");
            return true;
        }

        Player leader = playersOrdered.get(0);
        System.out.println("Meteor Swarm: Leader " + leader.getId().getNickname() + " will roll dice for all meteors");

        int meteorIndex = 0;
        for(Meteor meteor: card.getMeteorPattern()){
            meteorIndex++;
            
            // Leader rolls 2 dice for meteor impact position (Galaxy Trucker rule)
            Random dice = new Random();
            int die1 = dice.nextInt(6) + 1; // 1-6
            int die2 = dice.nextInt(6) + 1; // 1-6
            int diceTotal = die1 + die2;
            int index = Math.min(diceTotal - 2, 10); // Convert 2-12 to 0-10 for board indexing
            
            System.out.println("Meteor " + meteorIndex + " (" + meteor.getShotIntensity() + ") from " + 
                             meteor.getApproach() + " - Leader rolls: " + die1 + "+" + die2 + "=" + 
                             diceTotal + " (index " + index + ")");

            // Apply this meteor to ALL players simultaneously using the same dice roll
            for(Player player : playersOrdered){
                Ship ship = player.getShip();
                Position impactPosition = ship.findFirstComponent(meteor.getApproach(), index);
                
                if(impactPosition == null){
                    System.out.println("  " + player.getId().getNickname() + ": Meteor missed (no component at impact zone)");
                    continue;
                }

                Component impactComponent = ship.getBoard()[impactPosition.getRow()][impactPosition.getCol()];
                System.out.println("  " + player.getId().getNickname() + ": Meteor targeting " + 
                                 impactComponent.getType() + " at (" + impactPosition.getRow() + 
                                 "," + impactPosition.getCol() + ")");

                // Apply Galaxy Trucker defense rules
                boolean defended = false;
                
                if(meteor.getShotIntensity() == ShotIntensity.LIGHT){
                    // Small meteors: bounce off smooth sides OR can be blocked by shields
                    
                    // Check if meteor hits a smooth side (bounces off harmlessly)
                    ConnectorType hitConnector = impactComponent.getConnectorAt(meteor.getApproach());
                    if(hitConnector == ConnectorType.PLAIN){
                        System.out.println("    Deflected by smooth side - no damage");
                        defended = true;
                    }
                    // Check if exposed connector (vulnerable) can be protected by shield
                    else if(ship.protectedByShield(meteor.getApproach()) && ship.getBatteries() > 0){
                        ship.setBatteries(ship.getBatteries() - 1);
                        System.out.println("    Blocked by shield (1 battery spent)");
                        defended = true;
                    }
                    else if(ship.protectedByShield(meteor.getApproach())){
                        System.out.println("    Shield available but no battery power");
                    }
                } else {
                    // Large meteors: must be shot down by cannons
                    Component protectingCannon = findProtectingCannon(ship, meteor.getApproach(), index);
                    if(protectingCannon != null){
                        if(protectingCannon.getType() == ComponentType.CANNON_DOUBLE){
                            if(ship.getBatteries() > 0){
                                ship.setBatteries(ship.getBatteries() - 1);
                                System.out.println("    Shot down by double cannon (1 battery spent)");
                                defended = true;
                            } else {
                                System.out.println("    Double cannon available but no battery power");
                            }
                        } else if(protectingCannon.getType() == ComponentType.CANNON_SINGLE){
                            System.out.println("    Shot down by single cannon");
                            defended = true;
                        }
                    }
                }

                if(!defended){
                    // Meteor hits - destroy component
                    System.out.println("    IMPACT! " + impactComponent.getType() + " destroyed");
                    ship.removeComponent(impactPosition, GamePhase.FLIGHT);
                    ship.getLostComponents().add(impactComponent);
                    ship.updateStats();
                    
                    // Check for ship viability
                    if(ship.getEngines() <= 0){
                        System.out.println("    WARNING: " + player.getId().getNickname() + " lost all engines!");
                    }
                } else {
                    System.out.println("    Successfully defended - no damage");
                }
            }
            
            System.out.println(); // Blank line between meteors for readability
        }
        return true;
    }
    
    /**
     * Helper method to find the specific cannon protecting against a meteor.
     *
     * @param ship The ship to inspect
     * @param direction The direction the meteor is coming from
     * @param index The row or column the meteor is pointing
     * @return The cannon protecting against the meteor
     *
     */
    private Component findProtectingCannon(Ship ship, Direction direction, int index) {
        Component[][] board = ship.getBoard();
        
        if (direction == Direction.UP || direction == Direction.DOWN) {
            // Check the specific column
            if (index < 0 || index >= board[0].length) return null;
            
            for (int row = 0; row < board.length; row++) {
                Component component = board[row][index];
                if (component != null && 
                    (component.getType() == ComponentType.CANNON_SINGLE || 
                     component.getType() == ComponentType.CANNON_DOUBLE) &&
                    component.getDirection() == direction) {
                    return component;
                }
            }
        } else {
            // Check the specific row and adjacent rows for LEFT/RIGHT
            int[] rowsToCheck = {index, index - 1, index + 1};
            for (int row : rowsToCheck) {
                if (row >= 0 && row < board.length) {
                    for (int col = 0; col < board[0].length; col++) {
                        Component component = board[row][col];
                        if (component != null && 
                            (component.getType() == ComponentType.CANNON_SINGLE || 
                             component.getType() == ComponentType.CANNON_DOUBLE) &&
                            component.getDirection() == direction) {
                            return component;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Visits a PiratesCard following exact Galaxy Trucker rules:
     * - Attack players in flight order until defeated or all attacked
     * - Win: Player gets reward and can choose to take flight day penalty
     * - Lose: Player receives cannon fire
     * - Tie: No effect, enemy continues to next player
     *
     * @param card The pirates card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitPiratesCard(PiratesCard card, GameModel state){
        System.out.println("Resolving: " + card.getType() + " (Strength: " + card.getPowerLevel() + ")");
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        boolean enemyDefeated = false;
        List<Player> defeatedPlayers = new ArrayList<>();

        // Attack players sequentially until enemy is defeated
        for(Player player : playersOrdered) {
            if (enemyDefeated) {
                System.out.println("Player " + player.getId().getNickname() + " avoids combat (Pirates already defeated)");
                break;
            }

            int playerCannonStrength = (int) player.getShip().getCannons();
            System.out.println("Player " + player.getId().getNickname() + " - Cannon Strength: " + 
                             playerCannonStrength + " vs Pirates: " + card.getPowerLevel());

            if (playerCannonStrength > card.getPowerLevel()) {
                // Player defeats Pirates
                enemyDefeated = true;
                System.out.println("  VICTORY! " + player.getId().getNickname() + " defeats the Pirates!");
                
                // Player can choose to take reward (credits) and lose flight days
                // For now, automatically take the reward (could be made into a choice later)
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                
                System.out.println("  Reward: " + card.getCreditReward() + " credits, " + 
                                 card.getMovementPenalty() + " flight days lost");
                
            } else if (playerCannonStrength == card.getPowerLevel()) {
                // Tie - no effect, Pirates continue to next player
                System.out.println("  TIE - No effect, Pirates continue attacking");
                
            } else {
                // Player loses - receives cannon fire
                System.out.println("  DEFEAT - " + player.getId().getNickname() + " receives cannon fire");
                defeatedPlayers.add(player);
            }
        }

        // Apply cannon fire to all defeated players
        if (!defeatedPlayers.isEmpty() && !enemyDefeated) {
            System.out.println("\nApplying Pirate cannon fire to defeated players:");
            
            for (Player player : defeatedPlayers) {
                System.out.println("Attacking " + player.getId().getNickname() + ":");
                
                for (CannonFire cannonFire : card.getAttackPattern()) {
                    // Roll 2 dice for impact location
                    Random dice = new Random();
                    int die1 = dice.nextInt(6) + 1;
                    int die2 = dice.nextInt(6) + 1;
                    int diceTotal = die1 + die2;
                    int index = Math.min(diceTotal - 2, 10); // Convert 2-12 to 0-10 indexing

                    System.out.println("  " + cannonFire.getIntensity() + " cannon fire from " + 
                                     cannonFire.getApproach() + " - Dice: " + die1 + "+" + die2 + 
                                     "=" + diceTotal + " (index " + index + ")");

                    Position impactPosition = player.getShip().findFirstComponent(cannonFire.getApproach(), index);
                    if (impactPosition == null) {
                        System.out.println("    Miss - no component at impact location");
                        continue;
                    }

                    Component impactComponent = player.getShip().getBoard()[impactPosition.getRow()][impactPosition.getCol()];
                    System.out.println("    Target: " + impactComponent.getType() + " at (" + 
                                     impactPosition.getRow() + "," + impactPosition.getCol() + ")");

                    // Check shield defense (only for light cannon fire)
                    boolean defended = false;
                    if (cannonFire.isBlockable() && player.getShip().protectedByShield(cannonFire.getApproach())) {
                        if (player.getShip().getBatteries() > 0) {
                            player.getShip().setBatteries(player.getShip().getBatteries() - 1);
                            System.out.println("    Blocked by shield (1 battery spent)");
                            defended = true;
                        } else {
                            System.out.println("    Shield available but no battery power");
                        }
                    }

                    if (!defended) {
                        // Component destroyed
                        System.out.println("    IMPACT! " + impactComponent.getType() + " destroyed");
                        player.getShip().removeComponent(impactPosition, GamePhase.FLIGHT);
                        player.getShip().getLostComponents().add(impactComponent);
                        player.getShip().updateStats();
                    }
                }
            }
        }

        return true;
    }

    
    /**
     * Visits a PlanetsCard following exact Galaxy Trucker rules:
     * - Players choose planets in flight order (leader first)
     * - Only one rocket allowed per planet
     * - Players can choose to skip
     * - Landing costs flight days
     *
     * @param card The planets card to process
     * @param state Current game state
     * @return Returns true when planet resolution is complete
     */
    public boolean visitPlanetsCard(PlanetsCard card, GameModel state){
        System.out.println("Resolving: " + card.getType() + " (" + card.getPlanets().size() + " planets available)");

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        // Track which planets have been taken (Galaxy Trucker rule: only one rocket per planet)
        Set<Integer> takenPlanets = new HashSet<>();
        
        // Display available planets
        for (int i = 0; i < card.getPlanets().size(); i++) {
            Planet planet = card.getPlanets().get(i);
            System.out.println("  Planet " + (i + 1) + ": " + planet.getGoodQuantities() + 
                             " (cost: " + card.getLostDays() + " flight days)");
        }

        // Players choose in flight order
        for(Player player : playersOrdered) {
            System.out.println("\nPlayer " + player.getId().getNickname() + "'s turn to choose:");
            
            // Find first available planet that player can use
            boolean playerLanded = false;
            
            for (int i = 0; i < card.getPlanets().size(); i++) {
                Planet planet = card.getPlanets().get(i);
                int planetNumber = i + 1;
                
                // Check if planet is available (not taken by previous player)
                if (takenPlanets.contains(planetNumber)) {
                    System.out.println("  Planet " + planetNumber + " already taken");
                    continue;
                }
                
                // Check if player has cargo space for planet's goods
                if (player.getShip().addResources(planet.getGoodQuantities())) {
                    // Player successfully lands
                    takenPlanets.add(planetNumber);
                    flightBoard.movePlayer(player, card.getLostDays(), false);
                    
                    System.out.println("  LANDED on Planet " + planetNumber + 
                                     " - Loaded: " + planet.getGoodQuantities() + 
                                     ", Lost: " + card.getLostDays() + " flight days");
                    
                    playerLanded = true;
                    break; // Player can only land on one planet
                    
                } else {
                    System.out.println("  Planet " + planetNumber + " - insufficient cargo space");
                }
            }
            
            if (!playerLanded) {
                System.out.println("  SKIPPED - No available planets with sufficient cargo space");
            }
        }
        
        // Summary
        if (takenPlanets.isEmpty()) {
            System.out.println("\nNo players landed on any planets");
        } else {
            System.out.println("\nPlanets taken: " + takenPlanets + 
                             ", Planets remaining: " + (card.getPlanets().size() - takenPlanets.size()));
        }
        
        return true;
    }
    
    /**
     * Visits an OpenSpaceCard.
     * Now uses the AdventureCardController for proper turn-based engine strength declaration.
     *
     * @param card The open space card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitOpenSpaceCard (OpenSpaceCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        // Check for players with no engines who must abandon flight
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        List<Player> defeated = new ArrayList<>();

        for(Player player : playersOrdered){
            if(player.getShip().getEngines() <= 0){
                defeated.add(player);
                System.out.println("Player " + player.getId().getNickname() + 
                                 " has no engines and must abandon flight in Open Space");
            }
        }
        
        if(!defeated.isEmpty()){
            for(Player player : defeated){
                flightBoard.abandonPlayer(player);
            }
        }
        
        // Return false to indicate this card needs controller processing for choices
        // The AdventureCardController will handle the turn-based engine strength declarations
        return false;
    }
    
    /**
     * Visits a StardustCard with automated exposed connector penalty calculation.
     *
     * @param card The stardust card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitStardustCard(StardustCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        Collections.reverse(playersOrdered); // Process in reverse order for stardust

        for(Player player : playersOrdered){
            Ship ship = player.getShip();
            
            // Calculate exposed connectors with detailed analysis
            int exposedConnectors = calculateExposedConnectors(ship);
            
            // Apply stardust penalty (move backward)
            int initialPosition = player.getFlightData().getPosition();
            flightBoard.movePlayer(player, exposedConnectors, false);
            int finalPosition = player.getFlightData().getPosition();
            
            System.out.println("Player " + player.getId().getNickname() + 
                             " - Stardust Analysis: " + exposedConnectors + " exposed connectors found. " +
                             "Position: " + initialPosition + " -> " + finalPosition + " (moved back " + 
                             (initialPosition - finalPosition) + " spaces)");
        }
        return true;
    }
    
    /**
     * Calculates the number of exposed connectors on a ship with detailed analysis.
     * An exposed connector is one that is not plain and not connected to another component.
     *
     * @param ship The ship to inspect
     * @return The number of exposed connectors
     *
     */
    private int calculateExposedConnectors(Ship ship) {
        Component[][] board = ship.getBoard();
        int exposedCount = 0;
        
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                Component component = board[row][col];
                if (component == null) continue;
                
                // Check each direction for exposed connectors
                for (Direction direction : Direction.values()) {
                    ConnectorType connector = component.getConnectorAt(direction);
                    
                    // Skip plain connectors (they don't count as exposed)
                    if (connector == ConnectorType.PLAIN) continue;
                    
                    // Check if this connector is exposed (not connected to another component)
                    Position neighborPos = new Position(row, col).offsetBy(direction);
                    
                    if (isConnectorExposed(board, row, col, direction, neighborPos)) {
                        exposedCount++;
                        System.out.println("  Exposed " + connector + " connector at (" + row + "," + col + 
                                         ") facing " + direction);
                    }
                }
            }
        }
        
        return exposedCount;
    }
    
    /**
     * Determines if a connector is exposed (not properly connected to an adjacent component).
     * @param board The board to inspect
     * @param row The row index of the connector to inspect
     * @param col The column index of the connector to inspect
     * @param direction The side of the component to inspect
     * @param neighborPos The position of the neighbor
     * @return true if the connector is exposed, false otherwise
     *
     */
    private boolean isConnectorExposed(Component[][] board, int row, int col, Direction direction, Position neighborPos) {
        int nRow = neighborPos.getRow();
        int nCol = neighborPos.getCol();
        
        // If neighbor position is out of bounds, connector is exposed to space
        if (nRow < 0 || nRow >= board.length || nCol < 0 || nCol >= board[0].length) {
            return true;
        }
        
        // If no component at neighbor position, connector is exposed
        Component neighborComponent = board[nRow][nCol];
        if (neighborComponent == null) {
            return true;
        }
        
        // Check if the neighbor component has a compatible connector
        ConnectorType neighborConnector = neighborComponent.getConnectorAt(direction.getOpposite());
        ConnectorType currentConnector = board[row][col].getConnectorAt(direction);
        
        // If either connector is plain, they can't connect properly
        if (currentConnector == ConnectorType.PLAIN || neighborConnector == ConnectorType.PLAIN) {
            return true; // Exposed due to incompatible connection
        }
        
        // If both are non-plain connectors, they connect properly
        return false; // Not exposed, properly connected
    }
    
    /**
     * Visits a SlaversCard following exact Galaxy Trucker rules:
     * - Attack players in flight order until defeated or all attacked
     * - Win: Player gets reward and can choose to take flight day penalty
     * - Lose: Player loses crew members
     * - Tie: No effect, enemy continues to next player
     *
     * @param card The slavers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSlaversCard(SlaversCard card, GameModel state){
        System.out.println("Resolving: " + card.getType() + " (Strength: " + card.getPowerLevel() + ")");
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        boolean enemyDefeated = false;

        // Attack players sequentially until enemy is defeated
        for(Player player: playersOrdered){
            if (enemyDefeated) {
                System.out.println("Player " + player.getId().getNickname() + " avoids combat (Slavers already defeated)");
                break;
            }

            int playerCannonStrength = (int) player.getShip().getCannons();
            System.out.println("Player " + player.getId().getNickname() + " - Cannon Strength: " + 
                             playerCannonStrength + " vs Slavers: " + card.getPowerLevel());

            if (playerCannonStrength > card.getPowerLevel()) {
                // Player defeats Slavers
                enemyDefeated = true;
                System.out.println("  VICTORY! " + player.getId().getNickname() + " defeats the Slavers!");
                
                // Player can choose to take reward (credits) and lose flight days
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                
                System.out.println("  Reward: " + card.getCreditReward() + " credits, " + 
                                 card.getMovementPenalty() + " flight days lost");
                
            } else if (playerCannonStrength == card.getPowerLevel()) {
                // Tie - no effect, Slavers continue to next player
                System.out.println("  TIE - No effect, Slavers continue attacking");
                
            } else {
                // Player loses - loses crew members
                int crewLoss = Math.min(card.getCrewLossAmount(), player.getShip().getCrew());
                int originalCrew = player.getShip().getCrew();
                player.getShip().setCrew(originalCrew - crewLoss);
                
                System.out.println("  DEFEAT - " + player.getId().getNickname() + " loses " + crewLoss + 
                                 " crew members (" + originalCrew + " -> " + player.getShip().getCrew() + ")");
                
                // Check if player should be abandoned due to no crew
                if (player.getShip().getCrew() <= 0) {
                    System.out.println("  " + player.getId().getNickname() + " lost all crew - ship abandoned!");
                    flightBoard.abandonPlayer(player);
                }
            }
        }

        return true;
    }
    
    /**
     * Visits a SmugglersCard following exact Galaxy Trucker rules:
     * - Attack players in flight order until defeated or all attacked
     * - Win: Player gets goods reward and can choose to lose flight days
     * - Lose: Player loses most valuable goods
     * - Tie: No effect, enemy continues to next player
     *
     * @param card The smugglers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSmugglersCard(SmugglersCard card, GameModel state){
        System.out.println("Resolving: " + card.getType() + " (Strength: " + card.getPowerLevel() + ")");
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        boolean enemyDefeated = false;

        // Attack players sequentially until enemy is defeated
        for(Player player: playersOrdered){
            if (enemyDefeated) {
                System.out.println("Player " + player.getId().getNickname() + " avoids combat (Smugglers already defeated)");
                break;
            }

            double playerCannonStrength = player.getShip().getCannons();
            System.out.println("Player " + player.getId().getNickname() + " - Cannon Strength: " + 
                             playerCannonStrength + " vs Smugglers: " + card.getPowerLevel());

            if (playerCannonStrength > card.getPowerLevel()) {
                // Player defeats Smugglers
                enemyDefeated = true;
                System.out.println("  VICTORY! " + player.getId().getNickname() + " defeats the Smugglers!");
                
                // Player can choose to take reward (goods) and lose flight days
                boolean goodsAdded = player.getShip().addResources(card.getAvailableGoods());
                if (goodsAdded) {
                    flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                    System.out.println("  Reward: " + card.getAvailableGoods() + " goods, " + 
                                     card.getMovementPenalty() + " flight days lost");
                } else {
                    System.out.println("  No cargo space for goods reward - no flight days lost");
                }
                
            } else if (playerCannonStrength == card.getPowerLevel()) {
                // Tie - no effect, Smugglers continue to next player
                System.out.println("  TIE - No effect, Smugglers continue attacking");
                
            } else {
                // Player loses - loses most valuable goods
                System.out.println("  DEFEAT - " + player.getId().getNickname() + " loses valuable goods");
                
                boolean goodsRemoved = player.getShip().removeValuableResources(card.getGoodsLostIfDefeated());
                if (goodsRemoved) {
                    System.out.println("    Lost " + card.getGoodsLostIfDefeated() + " most valuable goods");
                } else {
                    System.out.println("    Not enough goods to lose - player escapes with minimal loss");
                }
            }
        }

        return true;
    }
    
    /**
     * Visits a CombatZoneCard following exact Galaxy Trucker rules:
     * - Evaluates 3 sequential criteria (crew, engines, cannons)
     * - For each criterion, weakest player(s) face penalty
     * - If tied, the player farthest ahead (leader) faces penalty
     * - Players declare strengths in order for engine/cannon comparisons
     *
     * @param card The combat zone card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitCombatZoneCard(CombatZoneCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        if(flightBoard.getPlayerCount() == 1){
            System.out.println("Skipping " + card.getType() + " card - single player game");
            return true;
        }

        int checkNumber = 0;
        for(CombatCheck check : card.getCombatChecks()){
            checkNumber++;
            CombatAttributeType attributeType = check.getAttribute();
            
            // Detailed analysis of combat comparison
            System.out.println("Combat Check " + checkNumber + " - Comparing " + attributeType + ":");
            for(Player player : playersOrdered) {
                double attributeValue = getPlayerAttributeValue(player, attributeType);
                System.out.println("  " + player.getId().getNickname() + ": " + attributeValue);
            }
            
            Player combatLoser = check.getCombatLoser(playersOrdered);
            if (combatLoser == null) {
                System.out.println("  No combat loser determined - skipping check");
                continue;
            }
            
            double loserValue = getPlayerAttributeValue(combatLoser, attributeType);
            System.out.println("  Combat loser: " + combatLoser.getId().getNickname() + 
                             " (lowest " + attributeType + ": " + loserValue + ")");

            // Apply penalty with detailed tracking
            PenaltyType penalty = check.getPenaltyType();
            switch (penalty){
                case CREW_LOSS:
                    int initialCrew = combatLoser.getShip().getCrew();
                    int crewLoss = Math.min(check.getPenaltyValue(), initialCrew);
                    combatLoser.getShip().setCrew(initialCrew - crewLoss);
                    System.out.println("  " + combatLoser.getId().getNickname() + " lost " + crewLoss + 
                                     " crew members (" + initialCrew + " -> " + combatLoser.getShip().getCrew() + ")");
                    
                    if(combatLoser.getShip().getCrew() <= 0){
                        System.out.println("  " + combatLoser.getId().getNickname() + " lost all crew - ship abandoned!");
                        flightBoard.abandonPlayer(combatLoser);
                    }
                    break;
                    
                case FLIGHT_DAYS_LOSS:
                    int initialPosition = combatLoser.getFlightData().getPosition();
                    flightBoard.movePlayer(combatLoser, check.getPenaltyValue(), false);
                    int finalPosition = combatLoser.getFlightData().getPosition();
                    System.out.println("  " + combatLoser.getId().getNickname() + " lost " + check.getPenaltyValue() + 
                                     " flight days (position: " + initialPosition + " -> " + finalPosition + ")");
                    break;
                    
                case GOODS_LOSS:
                    int goodsLost = countTotalResources(combatLoser.getShip());
                    boolean success = combatLoser.getShip().removeValuableResources(check.getPenaltyValue());
                    int goodsRemaining = countTotalResources(combatLoser.getShip());
                    int actualLoss = goodsLost - goodsRemaining;
                    
                    if (success) {
                        System.out.println("  " + combatLoser.getId().getNickname() + " lost " + actualLoss + 
                                         " valuable goods (" + goodsLost + " -> " + goodsRemaining + ")");
                    } else {
                        System.out.println("  " + combatLoser.getId().getNickname() + " had insufficient goods, lost " + 
                                         actualLoss + " goods instead of " + check.getPenaltyValue());
                    }
                    break;
                    
                case CANNON_FIRE:
                    System.out.println("  Applying " + check.getCannonFires().size() + " cannon fire attacks:");
                    processCombatCannonFire(combatLoser, check.getCannonFires());
                    break;
            }
            
            // Update ship stats after combat effects
            combatLoser.getShip().updateStats();
        }
        return true;
    }
    
    /**
     * Gets the attribute value for a player based on the combat attribute type.
     *
     * @param player The player
     * @param attributeType The type of attribute to find value of
     * @return the value of the attribute for the player
     *
     */
    private double getPlayerAttributeValue(Player player, CombatAttributeType attributeType) {
        switch (attributeType) {
            case CREW_COUNT:
                return player.getShip().getCrew();
            case CANNON_STRENGTH:
                return player.getShip().getCannons();
            case ENGINE_POWER:
                return player.getShip().getEngines();
            default:
                return 0;
        }
    }
    
    /**
     *
     * Processes cannon fire attacks for combat zones.
     *
     * @param target The player target of the attack
     * @param cannonFires The list of cannons attacking the player
     *
     */
    private void processCombatCannonFire(Player target, List<CannonFire> cannonFires) {
        Ship ship = target.getShip();
        
        for(int i = 0; i < cannonFires.size(); i++) {
            CannonFire cannonFire = cannonFires.get(i);
            
            // Roll 2d6 for cannon fire position
            Random dice1 = new Random();
            Random dice2 = new Random();
            int roll1 = dice1.nextInt(6) + 1;
            int roll2 = dice2.nextInt(6) + 1;
            int totalRoll = roll1 + roll2;
            int index = totalRoll - 2; // Convert 2-12 roll to 0-10 array index
            
            System.out.println("    Cannon Fire " + (i + 1) + " - Direction: " + cannonFire.getApproach() + 
                             ", Dice: " + roll1 + "+" + roll2 + "=" + totalRoll + ", Index: " + index);

            Position impactPosition = ship.findFirstComponent(cannonFire.getApproach(), index);
            if(impactPosition == null){
                System.out.println("    Miss - no component at impact position");
                continue;
            }
            
            Component impactComponent = ship.getBoard()[impactPosition.getRow()][impactPosition.getCol()];
            System.out.println("    Target: " + impactComponent.getType() + " at (" + 
                             impactPosition.getRow() + "," + impactPosition.getCol() + ")");

            // Check shield protection
            if(cannonFire.isBlockable() && ship.protectedByShield(cannonFire.getApproach())) {
                if(ship.getBatteries() > 0) {
                    ship.setBatteries(ship.getBatteries() - 1);
                    System.out.println("    Blocked by shield (battery used)");
                    continue;
                } else {
                    System.out.println("    Shield present but no battery power");
                }
            }
            
            // Component destroyed
            System.out.println("    Component destroyed!");
            ship.removeComponent(impactPosition, GamePhase.FLIGHT);
            ship.getLostComponents().add(impactComponent);
        }
    }


    /**
     * Visits an EpidemicCard with improved cabin connectivity analysis.
     *
     * @param card The epidemic card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitEpidemicCard(EpidemicCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Player player : playersOrdered ) {
            Ship ship = player.getShip();
            int initialCrew = ship.getCrew();
            
            // Analyze cabin connectivity to determine crew loss
            int connectedCabinGroups = analyzeConnectedCabins(ship);
            int crewLoss = Math.min(connectedCabinGroups, initialCrew); // Can't lose more crew than you have
            
            int finalCrew = initialCrew - crewLoss;
            ship.setCrew(Math.max(0, finalCrew)); // Ensure crew doesn't go negative
            
            System.out.println("Player " + player.getId().getNickname() + 
                             " - Epidemic Analysis: " + connectedCabinGroups + " connected cabin groups found. " +
                             "Crew: " + initialCrew + " -> " + ship.getCrew() + " (lost " + crewLoss + ")");
            
            // Check if player should be abandoned due to no crew
            if(ship.getCrew() <= 0){
                System.out.println("Player " + player.getId().getNickname() + " lost all crew to epidemic!");
                flightBoard.abandonPlayer(player);
            }
        }
        return true;
    }
    
    /**
     * Analyzes connected cabin groups to determine epidemic crew loss.
     * Uses flood-fill algorithm to find connected cabin components.
     *
     * @param ship The ship to analyze
     * @return the number of cabin groups in which the cabins are connected to each other
     */
    private int analyzeConnectedCabins(Ship ship) {
        Component[][] board = ship.getBoard();
        boolean[][] visited = new boolean[board.length][board[0].length];
        int cabinGroups = 0;
        
        // Find all cabin components and group them by connectivity
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[0].length; col++) {
                if (!visited[row][col] && board[row][col] != null && 
                    board[row][col].getType() == ComponentType.CABIN) {
                    
                    // Found an unvisited cabin - start a new connected group
                    Set<Position> cabinGroup = new HashSet<>();
                    floodFillCabins(ship, new Position(row, col), visited, cabinGroup);
                    
                    if (!cabinGroup.isEmpty()) {
                        cabinGroups++;
                        System.out.println("  Cabin group " + cabinGroups + " contains " + 
                                         cabinGroup.size() + " connected cabins");
                    }
                }
            }
        }
        
        return cabinGroups;
    }
    
    /**
     * Flood-fill algorithm specifically for cabin connectivity analysis.
     *
     * @param ship The ship to inspect
     * @param position The position where to start to search for connected cabins
     * @param visited To save whether the cabin was already added to a cabin group or not
     * @param cabinGroup The cabin group to which cabins are being added
     *
     */
    private void floodFillCabins(Ship ship, Position position, boolean[][] visited, Set<Position> cabinGroup) {
        int row = position.getRow();
        int col = position.getCol();
        Component[][] board = ship.getBoard();
        
        // Check bounds and visit status
        if (row < 0 || row >= board.length || col < 0 || col >= board[0].length ||
            visited[row][col] || board[row][col] == null) {
            return;
        }
        
        // Only process cabin components
        if (board[row][col].getType() != ComponentType.CABIN) {
            return;
        }
        
        // Mark as visited and add to group
        visited[row][col] = true;
        cabinGroup.add(position);
        
        // Check all four directions for connected cabins
        for (Direction direction : Direction.values()) {
            Position neighborPos = position.offsetBy(direction);
            int nRow = neighborPos.getRow();
            int nCol = neighborPos.getCol();
            
            // Check bounds
            if (nRow < 0 || nRow >= board.length || nCol < 0 || nCol >= board[0].length ||
                visited[nRow][nCol] || board[nRow][nCol] == null) {
                continue;
            }
            
            // Check if neighbor is a cabin
            if (board[nRow][nCol].getType() != ComponentType.CABIN) {
                continue;
            }
            
            Component currentCabin = board[row][col];
            Component neighborCabin = board[nRow][nCol];
            
            // Check if cabins are properly connected (not blocked by plain connectors)
            boolean areConnected = currentCabin.getConnectorAt(direction) != ConnectorType.PLAIN ||
                                 neighborCabin.getConnectorAt(direction.getOpposite()) != ConnectorType.PLAIN;
            
            if (areConnected) {
                floodFillCabins(ship, neighborPos, visited, cabinGroup);
            }
        }
    }
        
    
    /**
     * Visits an AbandonedStationCard following exact Galaxy Trucker rules:
     * - First-come-first-served opportunity
     * - Only one player can use the opportunity
     * - Requires minimum crew to dock
     * - Gains goods, loses flight days, no crew loss
     *
     * @param card The abandoned station card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitAbandonedStationCard(AbandonedStationCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());
        System.out.println("  Opportunity: Gain " + card.getGoodQuantities() + 
                         " goods, lose " + card.getLostDays() + 
                         " flight days (req: " + card.getMinCrewRequired() + " crew)");

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        // First-come-first-served: check players in flight order
        for (Player player : playersOrdered) {
            System.out.println("\nPlayer " + player.getId().getNickname() + 
                             " - Crew: " + player.getShip().getCrew() + 
                             " (needs " + card.getMinCrewRequired() + ")");
            
            // Check crew requirement
            if (player.getShip().getCrew() >= card.getMinCrewRequired()) {
                System.out.println("  CREW OK - Checking cargo space...");
                
                // Check if player has cargo space for the goods
                if (player.getShip().addResources(card.getGoodQuantities())) {
                    System.out.println("  CARGO OK - Docking...");
                    
                    // Lose flight days
                    int originalPosition = player.getFlightData().getPosition();
                    flightBoard.movePlayer(player, card.getLostDays(), false);
                    int newPosition = player.getFlightData().getPosition();
                    
                    System.out.println("  SUCCESS! Goods: +" + card.getGoodQuantities() + 
                                     ", Position: " + originalPosition + " -> " + newPosition);
                    
                    // Opportunity taken - remaining players miss out
                    for (int i = playersOrdered.indexOf(player) + 1; i < playersOrdered.size(); i++) {
                        Player remainingPlayer = playersOrdered.get(i);
                        System.out.println("Player " + remainingPlayer.getId().getNickname() + 
                                         " - Too late, station already looted");
                    }
                    
                    return true; // Opportunity used, card resolved
                    
                } else {
                    System.out.println("  INSUFFICIENT CARGO SPACE - Cannot dock");
                }
            } else {
                System.out.println("  INSUFFICIENT CREW - Cannot dock");
            }
        }
        
        // No eligible players found
        System.out.println("\nNo players eligible to dock at abandoned station - opportunity lost");
        return true;
    }
    
    /**
     * Helper method to count total resources in a ship.
     *
     * @param ship The ship that contains the resources
     * @return The total number resources in a ship
     *
     */
    private int countTotalResources(Ship ship) {
        Map<GoodType, Integer> resources = ship.getResources();
        return resources.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Visits a SabotageCard following exact Galaxy Trucker rules.
     *
     * @param card The sabotage card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSabotageCard(SabotageCard card, GameModel state) {
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        if (flightBoard.getPlayerCount() == 1) {
            System.out.println("Skipping " + card.getType() + " card - single player game");
            return true;
        }

        // Extract ships in flight order for sabotage targeting
        List<Ship> ships = playersOrdered.stream()
                .map(Player::getShip)
                .filter(Objects::nonNull)
                .toList();

        // Apply sabotage using the card's exact Galaxy Trucker implementation
        card.applySabotage(ships);

        return true;
    }

    /**
     * Helper method to check if a ship can add resources (simplified version).
     *
     * @param ship The ship to add resources to
     * @param resourcesToAdd The resources to add to the ship
     *
     * @return true if the ship can add resources, false if there is no more cargo space available
     *
     */
    private boolean canShipAddResources(Ship ship, Map<GoodType, Integer> resourcesToAdd) {
        // Simple implementation - try to add and see if it succeeds
        // This is a simplified check, in reality you'd want to verify cargo space
        try {
            return ship.addResources(resourcesToAdd);
        } catch (Exception e) {
            return false;
        }
    }
}