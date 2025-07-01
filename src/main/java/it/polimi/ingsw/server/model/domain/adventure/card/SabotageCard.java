package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.adventure.AdventureCardVisitor;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.GamePhase;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

/**
 * Sabotage adventure card implementation.
 * According to Galaxy Trucker rules: "Destroys a random component on the ship with the smallest crew."
 */
public class SabotageCard extends AdventureCard {
    private static final Random random = new Random();

    public SabotageCard(CardLevel level, String id) {
        super(id, level, "Sabotage: Destroys a random component on ships with the smallest crew.", AdventureType.SABOTAGE);
    }

    @Override
    public void accept(AdventureCardVisitor visitor, GameModel gameModel) {
        visitor.visitSabotageCard(this, gameModel);
    }

    /**
     * Applies sabotage effect following exact Galaxy Trucker rules:
     * - Affects ship with smallest crew (leader if tied)
     * - Uses dice-based coordinate targeting (2 dice for column, 2 dice for row)
     * - Up to 3 attempts if no component found
     * 
     * @param ships List of all ships in the current adventure
     */
    public void applySabotage(List<Ship> ships) {
        if (ships.isEmpty()) {
            return;
        }

        // Find ship with smallest crew (leader if tied)
        Ship targetShip = findSmallestCrewShip(ships);
        if (targetShip == null) {
            System.out.println("Sabotage: No valid target ship found");
            return;
        }

        System.out.println("Sabotage: Targeting ship with " + targetShip.getCrew() + " crew members");
        
        // Attempt to destroy component using dice-based targeting (up to 3 attempts)
        boolean componentDestroyed = attemptSabotageDestruction(targetShip);
        
        if (!componentDestroyed) {
            System.out.println("Sabotage: No component found after 3 attempts - sabotage failed");
        }
    }

    /**
     * Finds the ship with smallest crew. If tied, returns the one farthest ahead (leader).
     * This follows Galaxy Trucker rule: "Among tied players, the one farthest ahead is affected"
     */
    private Ship findSmallestCrewShip(List<Ship> ships) {
        if (ships.isEmpty()) return null;
        
        int minCrew = ships.stream()
                .mapToInt(Ship::getCrew)
                .min()
                .orElse(Integer.MAX_VALUE);

        // Find ships with minimum crew
        List<Ship> tiedShips = ships.stream()
                .filter(ship -> ship.getCrew() == minCrew)
                .toList();

        if (tiedShips.size() == 1) {
            return tiedShips.get(0);
        }

        // If multiple ships tied, return the first one (leader in flight order)
        // Note: The ships list should already be in flight order
        System.out.println("Sabotage: " + tiedShips.size() + " ships tied with " + minCrew + " crew, targeting leader");
        return tiedShips.get(0);
    }

    /**
     * Attempts to destroy a component using Galaxy Trucker dice-based targeting.
     * Rolls 2 dice for column, then 2 dice for row. Up to 3 attempts total.
     */
    private boolean attemptSabotageDestruction(Ship targetShip) {
        Component[][] board = targetShip.getBoard();
        int maxAttempts = 3;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // Roll 2 dice for column coordinates (2-12, converted to 0-based indexing)
            int columnRoll1 = random.nextInt(6) + 1;
            int columnRoll2 = random.nextInt(6) + 1;
            int columnTotal = columnRoll1 + columnRoll2;
            int targetColumn = Math.min(columnTotal - 1, board[0].length - 1); // Clamp to board size
            
            // Roll 2 dice for row coordinates (2-12, converted to 0-based indexing)
            int rowRoll1 = random.nextInt(6) + 1;
            int rowRoll2 = random.nextInt(6) + 1;
            int rowTotal = rowRoll1 + rowRoll2;
            int targetRow = Math.min(rowTotal - 1, board.length - 1); // Clamp to board size
            
            System.out.println("Sabotage attempt " + attempt + ": Column dice " + columnRoll1 + "+" + 
                             columnRoll2 + "=" + columnTotal + " (col " + targetColumn + "), " +
                             "Row dice " + rowRoll1 + "+" + rowRoll2 + "=" + rowTotal + " (row " + targetRow + ")");
            
            // Check if there's a component at these coordinates
            Component targetComponent = board[targetRow][targetColumn];
            
            if (targetComponent != null) {
                // Cannot destroy starting cabin
                if (targetComponent.getType() == ComponentType.CABIN_START) {
                    System.out.println("Sabotage: Hit starting cabin at (" + targetRow + "," + targetColumn + 
                                     ") - cannot destroy, attempting again");
                    continue;
                }
                
                // Destroy the component
                Position targetPosition = new Position(targetRow, targetColumn);
                targetShip.removeComponent(targetPosition, GamePhase.FLIGHT);
                targetShip.getLostComponents().add(targetComponent);
                
                System.out.println("Sabotage: Destroyed " + targetComponent.getType() + 
                                 " at position (" + targetRow + "," + targetColumn + ")");
                return true;
            } else {
                System.out.println("Sabotage: No component at (" + targetRow + "," + targetColumn + ")");
            }
        }
        
        return false; // All 3 attempts failed
    }

    @Override
    public String getDescription() {
        return "Sabotage: Destroys a random component on ships with the smallest crew.";
    }

    @Override
    public String toString() {
        return "SabotageCard{" +
                "level=" + getLevel() +
                ", id='" + getId() + '\'' +
                '}';
    }
}