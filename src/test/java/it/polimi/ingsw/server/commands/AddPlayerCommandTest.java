package it.polimi.ingsw.server.commands;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.GamePhase;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AddPlayerCommandTest {

    private GameModel gameModel;
    private PlayerId playerId;

    @BeforeEach
    void setUp() {
        // Initialize a GameModel for testing
        GameConfigurationManager configManager = new GameConfigurationManager();
        gameModel = new GameModel(GameLevel.TEST_FLIGHT, configManager, 2);
        playerId = new PlayerId(UUID.randomUUID(), "Player1");
    }

    @Test
    void testAddPlayerSuccess() {
        // Set the correct phase
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Execute the command
        AddPlayerCommand command = new AddPlayerCommand(playerId);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertTrue(result.isSuccess(), "The command should succeed");
        assertNotNull(gameModel.getPlayerById(playerId), "The player should be added to the game model");
    }

    @Test
    void testAddPlayerInvalidPhase() {
        // Set an invalid phase
        gameModel.setCurrentPhase(GamePhase.BUILDING);

        // Execute the command
        AddPlayerCommand command = new AddPlayerCommand(playerId);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertFalse(result.isSuccess(), "The command should fail");
        assertEquals("Cannot add players, game is not in SETUP phase.", result.getMessage(), "The error message should be correct");
    }

    @Test
    void testAddPlayerDuplicateId() {
        // Set the correct phase
        gameModel.setCurrentPhase(GamePhase.SETUP);

        // Add a player with the same ID
        gameModel.addPlayer(playerId, "Player1");

        // Execute the command
        AddPlayerCommand command = new AddPlayerCommand(playerId);
        CommandResult result = command.execute(gameModel).join();

        // Verify the result
        assertFalse(result.isSuccess(), "The command should fail");
        assertEquals("Player with ID " + playerId + " already exists", result.getMessage(), "The error message should be correct");
    }
}