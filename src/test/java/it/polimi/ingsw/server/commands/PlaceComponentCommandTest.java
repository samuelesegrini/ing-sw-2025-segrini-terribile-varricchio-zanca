package it.polimi.ingsw.server.commands;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.domain.ship.Position;
import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.domain.ship.components.CargoHold;
import it.polimi.ingsw.server.model.domain.ship.components.Component;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlaceComponentCommandTest {

    private GameModel gameModel;
    private PlayerId playerId;
    private Player player;
    private Ship playerShip;

    @BeforeEach
    void setUp() {
        // Initialize GameModel
        GameConfigurationManager configManager = new GameConfigurationManager();
        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, 2);
        playerId = new PlayerId(UUID.randomUUID(), "Player1");

        // Add player
        gameModel.addPlayer(playerId, "Player1");
        player = gameModel.getPlayerById(playerId);
        assertNotNull(player, "Player should be added to the game model");

        // Set the game phase to BUILDING
        gameModel.setCurrentPhase(GamePhase.BUILDING);
    }

    @Test
    void testPlaceComponentSuccess() {
        // Create a CargoHold component
        Map<Direction, ConnectorType> connectors = new HashMap<>();
        connectors.put(Direction.UP, ConnectorType.PLAIN);
        connectors.put(Direction.DOWN, ConnectorType.PLAIN);
        connectors.put(Direction.LEFT, ConnectorType.PLAIN);
        connectors.put(Direction.RIGHT, ConnectorType.PLAIN);

        CargoHold cargoHold = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);
        playerShip = new Ship (player, GameLevel.LEVEL_II);
        player.setShip(playerShip);
        assertNotNull(playerShip, "Player's ship should be initialized");
        playerShip.addComponent(cargoHold, new Position(1, 2));


        // Define the target position
        Position targetPosition = new Position(1, 3);

        // Execute the PlaceComponentCommand
        PlaceComponentCommand command = new PlaceComponentCommand(playerId, cargoHold, targetPosition, null);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertTrue(result.isSuccess(), "The command should succeed");
        assertEquals("Component placed.", result.getMessage(), "The success message should be correct");

        // Verify the component is placed on the ship
        Component[][] board = playerShip.getBoard();
        assertNotNull(board[targetPosition.getRow()][targetPosition.getCol()], "The component should be placed on the ship");
        assertEquals(cargoHold, board[targetPosition.getRow()][targetPosition.getCol()], "The placed component should match the expected component");
    }

    @Test
    void testPlaceComponentInvalidPhase() {
        // Set an invalid phase
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Create a CargoHold component
        Map<Direction, ConnectorType> connectors = new HashMap<>();
        connectors.put(Direction.UP, ConnectorType.PLAIN);
        connectors.put(Direction.DOWN, ConnectorType.PLAIN);
        connectors.put(Direction.LEFT, ConnectorType.PLAIN);
        connectors.put(Direction.RIGHT, ConnectorType.PLAIN);

        CargoHold cargoHold = new CargoHold(ComponentType.CARGO_HOLD, connectors, 5);

        // Define the target position
        Position targetPosition = new Position(1, 2);

        // Execute the PlaceComponentCommand
        PlaceComponentCommand command = new PlaceComponentCommand(playerId, cargoHold, targetPosition, null);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertFalse(result.isSuccess(), "The command should fail");
        assertEquals("Cannot place component: Not in BUILDING phase.", result.getMessage(), "The error message should be correct");
    }
}