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
     * Visits an AbandonedShipCard.
     * Now supports player choice mechanics - players can choose whether to dock.
     *
     * @param card The abandoned ship card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitAbandonedShipCard(AbandonedShipCard card, GameModel state){
        System.out.println("Resolving "+card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        // Check if card is already visited
        if (card.isVisited()) {
            System.out.println("Abandoned ship has already been salvaged");
            return true;
        }

        // Process each player in flight order
        for(Player player : playersOrdered ) {
            // Check if player has enough crew to attempt salvage
            if (player.getShip().getCrew() > card.getCrewLost()) {
                // Player is eligible - they can choose to dock via DockRequest
                // The actual docking logic is now handled by DockRequest
                System.out.println("Player " + player.getId().getNickname() + 
                    " can choose to salvage abandoned ship (cost: " + card.getCrewLost() + 
                    " crew, gain: " + card.getCreditsGained() + " credits, " + 
                    card.getLostDays() + " flight days)");
                
                // In the new system, the first eligible player gets the choice
                // The card remains active until a choice is made via DockRequest
                return false; // Card not yet resolved, waiting for player choice
            } else {
                System.out.println("Player " + player.getId().getNickname() + 
                    " has insufficient crew to salvage ship (needs > " + card.getCrewLost() + ")");
            }
        }
        
        // No eligible players - card is skipped
        System.out.println("No players eligible to salvage abandoned ship");
        return true;
    }

    /**
     * Visits a MeteorSwarmCard with improved defense calculations.
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

        int meteorIndex = 0;
        for(Meteor meteor: card.getMeteorPattern()){
            meteorIndex++;
            
            // Roll dice for meteor impact position (1-6, convert to 0-5 for array indexing)
            Random dice = new Random();
            int diceRoll = dice.nextInt(6) + 1; // 1-6
            int index = diceRoll - 1; // Convert to 0-5 for array indexing
            
            System.out.println("Meteor " + meteorIndex + " - Direction: " + meteor.getApproach() + 
                             ", Dice Roll: " + diceRoll + ", Intensity: " + meteor.getShotIntensity());

            for(Player player : playersOrdered){
                Ship ship = player.getShip();
                Position impactPosition = ship.findFirstComponent(meteor.getApproach(), index);
                
                if(impactPosition == null){
                    System.out.println("Meteor " + meteorIndex + " missed " + player.getId().getNickname() + "'s ship");
                    continue; // Continue to next player, don't break meteor entirely
                }

                Component impactComponent = ship.getBoard()[impactPosition.getRow()][impactPosition.getCol()];
                System.out.println("Meteor " + meteorIndex + " targeting " + player.getId().getNickname() + 
                                 " at position (" + impactPosition.getRow() + "," + impactPosition.getCol() + ")");

                // Check defenses based on meteor intensity
                if(meteor.getShotIntensity() == ShotIntensity.LIGHT){
                    // Light meteors: can be deflected by plain connectors OR blocked by shields
                    if(impactComponent.getConnectorAt(meteor.getApproach()) == ConnectorType.PLAIN){
                        System.out.println(player.getId().getNickname() + " deflected meteor " + meteorIndex + 
                                         " with plain connector");
                        continue; // Deflected, continue to next player
                    }
                    
                    if(ship.protectedByShield(meteor.getApproach())){
                        // Check if shield has battery power
                        if(ship.getBatteries() > 0){
                            ship.setBatteries(ship.getBatteries() - 1);
                            System.out.println(player.getId().getNickname() + " blocked meteor " + meteorIndex + 
                                             " with shield (battery used)");
                            continue; // Blocked, continue to next player
                        } else {
                            System.out.println(player.getId().getNickname() + " has shield but no battery power");
                        }
                    }
                } else {
                    // Heavy meteors: can only be shot down by cannons
                    if(ship.protectedByCannon(meteor.getApproach(), index)){
                        // Check if cannon has battery power for double cannons
                        Component cannon = findProtectingCannon(ship, meteor.getApproach(), index);
                        if(cannon != null && cannon.getType() == ComponentType.CANNON_DOUBLE){
                            if(ship.getBatteries() > 0){
                                ship.setBatteries(ship.getBatteries() - 1);
                                System.out.println(player.getId().getNickname() + " shot down meteor " + meteorIndex + 
                                                 " with double cannon (battery used)");
                                continue; // Shot down, continue to next player
                            } else {
                                System.out.println(player.getId().getNickname() + " has double cannon but no battery power");
                            }
                        } else if(cannon != null && cannon.getType() == ComponentType.CANNON_SINGLE){
                            System.out.println(player.getId().getNickname() + " shot down meteor " + meteorIndex + 
                                             " with single cannon");
                            continue; // Shot down, continue to next player
                        }
                    }
                }

                // No defense worked - meteor hits
                System.out.println(player.getId().getNickname() + " has no effective defense against meteor " + 
                                 meteorIndex + " - component destroyed!");
                ship.removeComponent(impactPosition, GamePhase.FLIGHT);
                ship.getLostComponents().add(impactComponent);
                
                // Update ship stats after component loss
                ship.updateStats();
                
                // Check if ship is still viable (has at least one engine and crew quarters)
                if(ship.getEngines() <= 0){
                    System.out.println(player.getId().getNickname() + "'s ship lost all engines!");
                    // Could add logic to abandon player here
                }
            }
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
     * Visits a PiratesCard.
     *
     * @param card The pirates card to process
     * @param state Current game state
     * @return Result of processing the card
     *
     */
    public boolean visitPiratesCard(PiratesCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        List<Player> defeated = new ArrayList<>();

        for(Player player : playersOrdered ) {
            if(player.getShip().getCannons()>card.getPowerLevel()){
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                System.out.println("Player " + player.getId().getNickname() +
                        " has defeated the pirates and received a reward of " + card.getCreditReward());
            }
            else if(player.getShip().getCannons()==card.getPowerLevel()){
                continue;
            }
            else if(player.getShip().getCannons()<card.getPowerLevel()){
                defeated.add(player);
            }
        }
        if(!defeated.isEmpty()){
            for(Player player : defeated) {
                for(CannonFire cannonFire : card.getAttackPattern()){

                    Random dice1 = new Random();
                    Random dice2 = new Random();
                    int index1 = dice1.nextInt(6) + 1;
                    int index2 = dice2.nextInt(6) + 1;
                    int index = index1 + index2;

                    Component[][] board = player.getShip().getBoard();
                    if ( cannonFire.getApproach() == Direction.UP || cannonFire.getApproach() == Direction.DOWN) {
                        // For UP and DOWN directions, fixedIndex represents a column
                        if (index < 0 || index >= board[0].length) {
                            System.out.println("Column index out of bounds: " + index);
                            continue;
                        }
                    } else {
                        // For LEFT and RIGHT directions, fixedIndex represents a row
                        if (index < 0 || index >= board.length) {
                            System.out.println("Row index out of bounds: " + index);
                            continue;
                        }
                    }

                    System.out.println("Direction: " + cannonFire.getApproach()+ " index: " + index);
                    Position impactPosition = player.getShip().findFirstComponent(cannonFire.getApproach(), index);
                    if(impactPosition==null){
                        System.out.println("Player " + player.getId().getNickname() + " has no component in the impact position");
                        continue;
                    }
                    Component impactComponent = player.getShip().getBoard()[impactPosition.getRow()][impactPosition.getCol()];

                    if(cannonFire.isBlockable() && player.getShip().protectedByShield(cannonFire.getApproach())){
                        System.out.println(player.getId().getNickname()+ " has activated a shield against cannon fire number "
                        +card.getAttackPattern().indexOf(cannonFire));
                    } else {
                        System.out.println(player.getId().getNickname()+ " has no protection against cannon fire number "
                                +card.getAttackPattern().indexOf(cannonFire));
                        player.getShip().removeComponent(impactPosition, GamePhase.FLIGHT);
                        player.getShip().getLostComponents().add(impactComponent);
                        System.out.println("Player " + player.getId().getNickname() + " has lost the component in position ("
                                + impactPosition.getRow()+ "," + impactPosition.getCol() + ")");
                    }
                }
            }
        }
        return true;
    }

    
    /**
     * Visits a PlanetsCard.
     *
     * @param card The planets card to process
     * @param state Current game state
     * @return Returns {code @true} if at least one player has landed on a planet, {code @false} otherwise.
     */
    public boolean visitPlanetsCard(PlanetsCard card, GameModel state){
        System.out.println("Resolving planet: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Player player : playersOrdered ){
            for(Planet planet : card.getPlanets()){
                //the planet must be unvisited and the player must have enough space to gather resources
                if((!planet.isVisited()) && (player.getShip().addResources(planet.getGoodQuantities()))){
                    flightBoard.movePlayer(player, card.getLostDays(), false);
                    planet.setVisited();
                    System.out.println(player.getId().getNickname() + " è atterrato su " + planet.getNumber());
                    break;  // passa al giocatore successivo
                }
            }
        }
        return true;
    }
    
    /**
     * Visits an OpenSpaceCard.
     *
     * @param card The open space card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitOpenSpaceCard (OpenSpaceCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();
        List<Player> defeated = new ArrayList<>();

        for(Player player : playersOrdered){
            flightBoard.movePlayer(player, (int)player.getShip().getEngines(), true);
            if(player.getShip().getEngines() <= 0){
                defeated.add(player);
            }
        }
        if(!defeated.isEmpty()){
            for(Player player : defeated){
                flightBoard.abandonPlayer(player);
            }
        }
        return true;
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
     * Visits a SlaversCard.
     *
     * @param card The slavers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSlaversCard(SlaversCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        for(Player player: playersOrdered){
            if(player.getShip().getCannons() == card.getPowerLevel()){
                continue;
            }
            else if(player.getShip().getCannons() < card.getPowerLevel()){
                player.getShip().setCrew(player.getShip().getCrew()- card.getCrewLossAmount());
            }
            else if(player.getShip().getCannons() > card.getPowerLevel()){
                card.setDefeated();
                //The player CAN claim the reward losing flying days
                player.addCredits(card.getCreditReward());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                break;
            }
        }
        return true;
    }
    
    /**
     * Visits a SmugglersCard.
     *
     * @param card The smugglers card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitSmugglersCard(SmugglersCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());

        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        double powerLevel = card.getPowerLevel();
        for(Player player: playersOrdered){
            double cannonStrength = player.getShip().getCannons();
            if(powerLevel == cannonStrength){
                continue;
            }
            else if (powerLevel < cannonStrength){
                card.setDefeated();
                //The player CAN claim the reward losing flying days
                player.getShip().addResources(card.getAvailableGoods());
                flightBoard.movePlayer(player, card.getMovementPenalty(), false);
                break;
            }
            else if(powerLevel > cannonStrength){
                System.out.println("Power level: " + powerLevel+ " cannon strength: " + cannonStrength);
                if(!(player.getShip().removeValuableResources(card.getGoodsLostIfDefeated()))){
                    throw new IllegalArgumentException("Not enough resources available!");
                }
            }
        }
        return true;
    }
    
    /**
     * Visits a CombatZoneCard with enhanced multi-attribute comparison analysis.
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
     * Visits an AbandonedStationCard.
     * Now supports player choice mechanics - players can choose whether to dock.
     *
     * @param card The abandoned station card to process
     * @param state Current game state
     * @return Result of processing the card
     */
    public boolean visitAbandonedStationCard(AbandonedStationCard card, GameModel state){
        System.out.println("Resolving: " + card.getType());
        FlightBoard flightBoard = state.getFlightBoard();
        List<Player> playersOrdered = flightBoard.getCurrentOrder();

        // Check if card is already visited
        if (card.isVisited()) {
            System.out.println("Abandoned station has already been looted");
            return true;
        }

        // Process each player in flight order
        for (Player player : playersOrdered) {
            // Check if player meets crew requirement and has cargo space
            if (player.getShip().getCrew() >= card.getMinCrewRequired()) {
                // Check if player has enough cargo space (temporarily test add resources)
                Map<GoodType, Integer> stationGoods = card.getGoodQuantities();
                if (canShipAddResources(player.getShip(), stationGoods)) {
                    // Player is eligible - they can choose to dock via DockRequest
                    System.out.println("Player " + player.getId().getNickname() + 
                        " can choose to dock at abandoned station (cost: " + card.getLostDays() + 
                        " flight days, crew req: " + card.getMinCrewRequired() + 
                        ", goods available: " + stationGoods + ")");
                    
                    // In the new system, the first eligible player gets the choice
                    // The card remains active until a choice is made via DockRequest
                    return false; // Card not yet resolved, waiting for player choice
                } else {
                    System.out.println("Player " + player.getId().getNickname() + 
                        " has insufficient cargo space for station goods");
                }
            } else {
                System.out.println("Player " + player.getId().getNickname() + 
                    " has insufficient crew to dock at station (needs " + card.getMinCrewRequired() + ")");
            }
        }
        
        // No eligible players - card is skipped
        System.out.println("No players eligible to dock at abandoned station");
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