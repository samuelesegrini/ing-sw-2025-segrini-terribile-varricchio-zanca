package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameListResponseEventTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(GameListResponseEvent.class),
                "GameListResponseEvent should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(GameListResponseEvent.class),
                "GameListResponseEvent should implement Message interface");
    }

    @Test
    void constructorSetsAllFields() {
        List<GameLobbyInfoDTO> joinableGames = Arrays.asList(
                new GameLobbyInfoDTO("game-1", "First Game", 2, 4, GameSessionState.LOBBY, GameLevel.TEST_FLIGHT),
                new GameLobbyInfoDTO("game-2", "Second Game", 1, 3, GameSessionState.LOBBY, GameLevel.LEVEL_II)
        );
        
        List<GameLobbyInfoDTO> runningGames = List.of(
                new GameLobbyInfoDTO("game-3", "Running Game", 3, 3, GameSessionState.FLIGHT, GameLevel.TEST_FLIGHT)
        );
        
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        
        assertEquals(2, response.getJoinableGames().size());
        assertEquals(1, response.getRunningGames().size());
        
        assertEquals("First Game", response.getJoinableGames().get(0).getGameName());
        assertEquals("Second Game", response.getJoinableGames().get(1).getGameName());
        assertEquals("Running Game", response.getRunningGames().get(0).getGameName());
    }
    
    @Test
    void nullJoinableGamesThrowsException() {
        List<GameLobbyInfoDTO> runningGames = new ArrayList<>();
        
        assertThrows(NullPointerException.class, 
                () -> new GameListResponseEvent(null, runningGames));
    }
    
    @Test
    void nullRunningGamesThrowsException() {
        List<GameLobbyInfoDTO> joinableGames = new ArrayList<>();
        
        assertThrows(NullPointerException.class, 
                () -> new GameListResponseEvent(joinableGames, null));
    }
    
    @Test
    void joinableGamesListIsCopiedInConstructor() {
        List<GameLobbyInfoDTO> originalJoinable = new ArrayList<>();
        originalJoinable.add(new GameLobbyInfoDTO("game-1", "First Game", 2, 4, 
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT));
        
        List<GameLobbyInfoDTO> runningGames = new ArrayList<>();
        
        GameListResponseEvent response = new GameListResponseEvent(originalJoinable, runningGames);
        
        // Add to original list should not affect response's list
        originalJoinable.add(new GameLobbyInfoDTO("game-2", "Second Game", 1, 3, 
                GameSessionState.LOBBY, GameLevel.LEVEL_II));
        
        assertEquals(1, response.getJoinableGames().size(),
                "Response's joinable games list should not be affected by changes to original list");
    }
    
    @Test
    void runningGamesListIsCopiedInConstructor() {
        List<GameLobbyInfoDTO> joinableGames = new ArrayList<>();
        
        List<GameLobbyInfoDTO> originalRunning = new ArrayList<>();
        originalRunning.add(new GameLobbyInfoDTO("game-3", "Running Game", 3, 3, 
                GameSessionState.FLIGHT, GameLevel.TEST_FLIGHT));
        
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, originalRunning);
        
        // Add to original list should not affect response's list
        originalRunning.add(new GameLobbyInfoDTO("game-4", "Another Running Game", 2, 2, 
                GameSessionState.FLIGHT, GameLevel.LEVEL_II));
        
        assertEquals(1, response.getRunningGames().size(),
                "Response's running games list should not be affected by changes to original list");
    }
    
    @Test
    void getJoinableGamesReturnsCopy() {
        List<GameLobbyInfoDTO> joinableGames = Arrays.asList(
                new GameLobbyInfoDTO("game-1", "First Game", 2, 4, GameSessionState.LOBBY, GameLevel.TEST_FLIGHT)
        );
        
        List<GameLobbyInfoDTO> runningGames = new ArrayList<>();
        
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        
        List<GameLobbyInfoDTO> returnedList = response.getJoinableGames();
        
        // Verify original list and returned list are different objects
        assertNotSame(joinableGames, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible
        returnedList.add(new GameLobbyInfoDTO("game-2", "Second Game", 1, 3, 
                GameSessionState.LOBBY, GameLevel.LEVEL_II));
        
        // The next call should return a different list with only the original element
        List<GameLobbyInfoDTO> secondReturnedList = response.getJoinableGames();
        assertEquals(1, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void getRunningGamesReturnsCopy() {
        List<GameLobbyInfoDTO> joinableGames = new ArrayList<>();
        
        List<GameLobbyInfoDTO> runningGames = Arrays.asList(
                new GameLobbyInfoDTO("game-3", "Running Game", 3, 3, GameSessionState.FLIGHT, GameLevel.TEST_FLIGHT)
        );
        
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        
        List<GameLobbyInfoDTO> returnedList = response.getRunningGames();
        
        // Verify original list and returned list are different objects
        assertNotSame(runningGames, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible
        returnedList.add(new GameLobbyInfoDTO("game-4", "Another Running Game", 2, 2, 
                GameSessionState.FLIGHT, GameLevel.LEVEL_II));
        
        // The next call should return a different list with only the original element
        List<GameLobbyInfoDTO> secondReturnedList = response.getRunningGames();
        assertEquals(1, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsCountsAndTimestamp() {
        List<GameLobbyInfoDTO> joinableGames = Arrays.asList(
                new GameLobbyInfoDTO("game-1", "First Game", 2, 4, GameSessionState.LOBBY, GameLevel.TEST_FLIGHT),
                new GameLobbyInfoDTO("game-2", "Second Game", 1, 3, GameSessionState.LOBBY, GameLevel.LEVEL_II)
        );
        
        List<GameLobbyInfoDTO> runningGames = List.of(
                new GameLobbyInfoDTO("game-3", "Running Game", 3, 3, GameSessionState.FLIGHT, GameLevel.TEST_FLIGHT)
        );
        
        GameListResponseEvent response = new GameListResponseEvent(joinableGames, runningGames);
        String toString = response.toString();
        
        assertTrue(toString.contains("joinableGamesCount=2"), "toString should include joinable games count");
        assertTrue(toString.contains("runningGamesCount=1"), "toString should include running games count");
        assertTrue(toString.contains("timestamp"), "toString should include timestamp");
    }
} 