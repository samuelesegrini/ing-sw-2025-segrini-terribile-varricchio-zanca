package it.polimi.ingsw.server.commands;

import java.util.concurrent.CompletableFuture; // Use common Position
import java.util.logging.Level; // Assuming RuleEngine is separate
import java.util.logging.Logger;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.player.Player; // Import the model Component
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.domain.ship.Position;
import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.domain.ship.components.Component;
import it.polimi.ingsw.model.enums.GamePhase;
import it.polimi.ingsw.server.game.RuleEngine;

/**
 * Command to place a component onto a player's ship grid.
 */
public class PlaceComponentCommand implements Command {

    private static final Logger logger = Logger.getLogger(PlaceComponentCommand.class.getName());

    private final PlayerId playerId;
    private final Component componentToPlace; // The actual model Component object
    private final Position targetPosition; // Use common Position for input
    // private final RuleEngine ruleEngine; // Keep RuleEngine for more complex checks later

    /**
     * Constructor.
     *
     * @param playerId        The ID of the player placing the component.
     * @param componentToPlace The model Component object to place.
     * @param targetPosition  The grid Position (common) to place the component at.
     * @param ruleEngine      The rule engine (currently unused, for future complex validation).
     */
    public PlaceComponentCommand(PlayerId playerId, Component componentToPlace, Position targetPosition, RuleEngine ruleEngine) {
        this.playerId = playerId;
        this.componentToPlace = componentToPlace;
        this.targetPosition = targetPosition;
        // this.ruleEngine = ruleEngine;
    }

    @Override
    public CompletableFuture<CommandResult> execute(GameModel model) {
        try {
            // 1. Validate Phase
            if (model.getCurrentPhase() != GamePhase.BUILDING) {
                return CompletableFuture.completedFuture(CommandResult.failure("Cannot place component: Not in BUILDING phase."));
            }

            // 2. Get Player and Ship
            Player player = model.getPlayerById(playerId);
            if (player == null) {
                return CompletableFuture.completedFuture(CommandResult.failure("Player not found: " + playerId));
            }
            Ship ship = player.getShip();
            if (ship == null) {
                return CompletableFuture.completedFuture(CommandResult.failure("Player ship not found."));
            }

            // 3. Validate Player's Turn (if applicable)
            // if (!model.getCurrentPlayer().getId().equals(playerId)) { ... }

            // 4. Validate component ownership (Does the player have this component?)
            // TODO: Implement check if player actually possesses componentToPlace (e.g., in hand)
            // boolean playerHasComponent = player.hasComponentInHand(componentToPlace); // Requires Player method
            // if (!playerHasComponent) { return CompletableFuture.completedFuture(CommandResult.failure("Player does not possess this component.")); }

            // 5. Convert Position and Validate placement using Ship methods
            Position modelPosition = new it.polimi.ingsw.model.domain.ship.Position(targetPosition.getRow(), targetPosition.getCol()); // Assuming model Position(row, col)

            // Check forbidden positions
            if (ship.forbiddenPositions.contains(modelPosition)) {
                logger.log(Level.WARNING, "Player {0} attempted placement on forbidden position {1}", new Object[]{playerId, modelPosition});
                return CompletableFuture.completedFuture(CommandResult.failure("Placement forbidden at this position."));
            }

            // Check if position is occupied using the Ship's board
            if (ship.getBoard()[modelPosition.getRow()][modelPosition.getCol()] != null) {
                logger.log(Level.WARNING, "Player {0} attempted placement on occupied position {1}", new Object[]{playerId, modelPosition});
                return CompletableFuture.completedFuture(CommandResult.failure("Position is already occupied."));
            }

            // TODO: Add more complex validation using RuleEngine if needed (e.g., connector matching)
            // boolean connectorsValid = ruleEngine.validateConnectors(ship, componentToPlace, modelPosition);
            // if (!connectorsValid) { ... }

            // 6. Execute placement using Ship.addComponent
            ship.addComponent(componentToPlace, modelPosition); // Use the correct method

            // TODO: Remove component from player's hand/inventory
            // player.removeComponentFromHand(componentToPlace);

            logger.log(Level.INFO, "Player {0} placed component {1} at {2}", new Object[]{playerId, componentToPlace.getClass().getSimpleName(), modelPosition});
            return CompletableFuture.completedFuture(CommandResult.success("Component placed."));

        } catch (IllegalArgumentException e) {
            // Catch exceptions specifically from Ship.addComponent (e.g., double-check for occupied/forbidden)
            logger.log(Level.WARNING, "Placement failed for player {0}: {1}", new Object[]{playerId, e.getMessage()});
            return CompletableFuture.completedFuture(CommandResult.failure("Placement error: " + e.getMessage()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error executing PlaceComponentCommand for player " + playerId, e);
            return CompletableFuture.completedFuture(CommandResult.failure("Internal server error placing component."));
        }
    }
}