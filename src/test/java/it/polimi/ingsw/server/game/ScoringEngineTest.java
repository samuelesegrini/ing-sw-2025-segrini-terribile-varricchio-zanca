package it.polimi.ingsw.server.game;

import it.polimi.ingsw.model.domain.general.GameModel;
import it.polimi.ingsw.model.domain.general.config.GameConfigurationManager;
import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ScoringEngine.
 * Currently contains placeholder tests that will be expanded as scoring rules are implemented.
 */
class ScoringEngineTest {

    private ScoringEngine scoringEngine;
    private GameModel gameModel;
    private Player player1;
    private Player player2;
    private PlayerId playerId1;
    private PlayerId playerId2;

    @BeforeEach
    void setUp() {
        scoringEngine = new ScoringEngine();
        
        // Setup game model with proper configuration manager
        GameConfigurationManager configManager = new GameConfigurationManager();
        gameModel = new GameModel(GameLevel.LEVEL_II, configManager, 2);
        
        // Setup players
        playerId1 = new PlayerId(UUID.randomUUID(), "Player1");
        playerId2 = new PlayerId(UUID.randomUUID(), "Player2");
        player1 = new Player(playerId1);
        player2 = new Player(playerId2);
        
        // Add players to game model
        gameModel.addPlayer(playerId1, "Player1");
        gameModel.addPlayer(playerId2, "Player2");
    }

    @Test
    void calculateFinalScores_ShouldReturnEmptyMap() {
        Map<String, Integer> scores = scoringEngine.calculateFinalScores(gameModel);
        assertNotNull(scores, "Scores map should not be null");
        assertTrue(scores.isEmpty(), "Scores map should be empty in current implementation");
    }

    @Test
    void determineWinner_ShouldReturnNull() {
        Map<String, Integer> scores = Map.of(
            playerId1.toString(), 10,
            playerId2.toString(), 20
        );
        String winner = scoringEngine.determineWinner(scores);
        assertNull(winner, "Winner should be null in current implementation");
    }

    @Test
    void calculateFinalScores_WithNullGameModel_ShouldReturnEmptyMap() {
        Map<String, Integer> scores = scoringEngine.calculateFinalScores(null);
        assertNotNull(scores, "Scores map should not be null even with null game model");
        assertTrue(scores.isEmpty(), "Scores map should be empty with null game model");
    }

    @Test
    void determineWinner_WithEmptyScores_ShouldReturnNull() {
        Map<String, Integer> scores = Map.of();
        String winner = scoringEngine.determineWinner(scores);
        assertNull(winner, "Winner should be null with empty scores");
    }
} 