package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.GameLobbyInfoDTO;
import it.polimi.ingsw.common.dto.PlayerInfoDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Message;
import it.polimi.ingsw.common.model.GameSessionState;
import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateGameResponseEventTest {

    @Test
    void extendsBaseMessage() {
        assertTrue(BaseMessage.class.isAssignableFrom(CreateGameResponseEvent.class),
                "CreateGameResponseEvent should extend BaseMessage");
    }

    @Test
    void implementsMessageInterface() {
        assertTrue(Message.class.isAssignableFrom(CreateGameResponseEvent.class),
                "CreateGameResponseEvent should implement Message interface");
    }

    @Test
    void constructorForSuccessWithNoPlayers() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4, 
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        CreateGameResponseEvent response = new CreateGameResponseEvent("game-123", gameInfo);
        
        assertTrue(response.isSuccess(), "Response should indicate success");
        assertEquals("game-123", response.getSessionId());
        assertEquals(gameInfo, response.getNewGameInfo());
        assertNull(response.getErrorMessage(), "Error message should be null for successful creation");
        assertNotNull(response.getPlayersInLobby(), "Players list should not be null");
        assertEquals(0, response.getPlayersInLobby().size(), "Players list should be empty");
    }
    
    @Test
    void constructorForSuccessWithPlayers() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4, 
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Host Player", true)
        );
        
        CreateGameResponseEvent response = new CreateGameResponseEvent("game-123", gameInfo, players);
        
        assertTrue(response.isSuccess(), "Response should indicate success");
        assertEquals("game-123", response.getSessionId());
        assertEquals(gameInfo, response.getNewGameInfo());
        assertNull(response.getErrorMessage(), "Error message should be null for successful creation");
        assertEquals(1, response.getPlayersInLobby().size(), "Players list should have 1 entry");
        assertEquals("Host Player", response.getPlayersInLobby().get(0).getNickname());
    }
    
    @Test
    void constructorForFailure() {
        CreateGameResponseEvent response = new CreateGameResponseEvent("Invalid game settings");
        
        assertFalse(response.isSuccess(), "Response should indicate failure");
        assertNull(response.getSessionId(), "Session ID should be null for failed creation");
        assertNull(response.getNewGameInfo(), "Game info should be null for failed creation");
        assertEquals("Invalid game settings", response.getErrorMessage());
        assertNull(response.getPlayersInLobby(), "Players list should be null for failed creation");
    }
    
    @Test
    void nullSessionIdThrowsException() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        assertThrows(NullPointerException.class, () -> new CreateGameResponseEvent(null, gameInfo));
    }
    
    @Test
    void nullGameInfoThrowsException() {
        assertThrows(NullPointerException.class, () -> new CreateGameResponseEvent("game-123", null));
    }
    
    @Test
    void nullPlayersListThrowsException() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        assertThrows(NullPointerException.class, 
                () -> new CreateGameResponseEvent("game-123", gameInfo, null));
    }
    
    @Test
    void nullErrorMessageThrowsException() {
        assertThrows(NullPointerException.class, () -> new CreateGameResponseEvent(null));
    }
    
    @Test
    void playersListIsCopiedInConstructor() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        List<PlayerInfoDTO> originalList = new ArrayList<>();
        originalList.add(new PlayerInfoDTO("player1", "Host Player", true));
        
        CreateGameResponseEvent response = new CreateGameResponseEvent("game-123", gameInfo, originalList);
        
        // Add to original list should not affect response's list
        originalList.add(new PlayerInfoDTO("player2", "Player Two"));
        
        assertEquals(1, response.getPlayersInLobby().size(),
                "Response's player list should not be affected by changes to original list");
    }
    
    @Test
    void getPlayersInLobbyReturnsCopy() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Host Player", true)
        );
        
        CreateGameResponseEvent response = new CreateGameResponseEvent("game-123", gameInfo, players);
        
        List<PlayerInfoDTO> returnedList = response.getPlayersInLobby();
        
        // Verify original list and returned list are different objects
        assertNotSame(players, returnedList, "Returned list should be a new instance");
        
        // Modifying the returned list should be possible
        returnedList.add(new PlayerInfoDTO("player2", "Player Two"));
        
        // The next call should return a different list with only the original player
        List<PlayerInfoDTO> secondReturnedList = response.getPlayersInLobby();
        assertEquals(1, secondReturnedList.size(),
                "Original internal list should not be affected by modifications to the returned list");
    }
    
    @Test
    void toStringContainsAllFields() {
        GameLobbyInfoDTO gameInfo = new GameLobbyInfoDTO("game-123", "Test Game", 1, 4,
                GameSessionState.LOBBY, GameLevel.TEST_FLIGHT);
        
        List<PlayerInfoDTO> players = Arrays.asList(
                new PlayerInfoDTO("player1", "Host Player", true)
        );
        
        CreateGameResponseEvent successResponse = new CreateGameResponseEvent("game-123", gameInfo, players);
        String successString = successResponse.toString();
        
        CreateGameResponseEvent failureResponse = new CreateGameResponseEvent("Invalid settings");
        String failureString = failureResponse.toString();
        
        // Success response
        assertTrue(successString.contains("success=true"), "toString should indicate success");
        assertTrue(successString.contains("game-123"), "toString should include session ID");
        assertTrue(successString.contains("Test Game"), "toString should include game name via game info");
        assertTrue(successString.contains("1"), "toString should include player count");
        assertTrue(successString.contains("timestamp"), "toString should include timestamp");
        
        // Failure response
        assertTrue(failureString.contains("success=false"), "toString should indicate failure");
        assertTrue(failureString.contains("Invalid settings"), "toString should include error message");
        assertTrue(failureString.contains("timestamp"), "toString should include timestamp");
    }
} 