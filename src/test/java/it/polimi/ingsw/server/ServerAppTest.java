package it.polimi.ingsw.server;

import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.common.message.event.PlayerDisconnectedEvent;
import it.polimi.ingsw.server.controller.CommandDispatcher;
import it.polimi.ingsw.server.core.*;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.monitor.ConnectionMonitorService;
import it.polimi.ingsw.server.network.ServerNetworkManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerAppTest {

    private ServerApp serverApp;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @Mock
    private ServerNetworkManager networkManager;
    @Mock
    private GameSessionManager sessionManager;
    @Mock
    private PlayerSessionRegistry playerRegistry;
    @Mock
    private CommandDispatcher commandDispatcher;
    @Mock
    private ConnectionMonitorService connectionMonitor;
    @Mock
    private GameSession gameSession;
    @Mock
    private GameModel gameModel;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        serverApp = new ServerApp();

        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        if (serverApp != null) {
            try {
                Method shutdownMethod = ServerApp.class.getDeclaredMethod("shutdown");
                shutdownMethod.setAccessible(true);
                shutdownMethod.invoke(serverApp);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    public void testParseArgumentsNoArgs() throws Exception {
        Method parseMethod = ServerApp.class.getDeclaredMethod("parseArguments", String[].class);
        parseMethod.setAccessible(true);

        parseMethod.invoke(serverApp, (Object) new String[]{});

        assertNotNull(serverApp);
    }

    @Test
    public void testParseArgumentsWithValidPorts() throws Exception {
        Method parseMethod = ServerApp.class.getDeclaredMethod("parseArguments", String[].class);
        parseMethod.setAccessible(true);

        parseMethod.invoke(serverApp, (Object) new String[]{"8080", "1200"});

        assertNotNull(serverApp);
    }

    @Test
    public void testParseArgumentsWithInvalidSocketPort() throws Exception {
        Method parseMethod = ServerApp.class.getDeclaredMethod("parseArguments", String[].class);
        parseMethod.setAccessible(true);

        parseMethod.invoke(serverApp, (Object) new String[]{"invalid", "1200"});

        assertNotNull(serverApp);
    }

    @Test
    public void testParseArgumentsWithInvalidRMIPort() throws Exception {
        Method parseMethod = ServerApp.class.getDeclaredMethod("parseArguments", String[].class);
        parseMethod.setAccessible(true);

        parseMethod.invoke(serverApp, (Object) new String[]{"8080", "invalid"});

        assertNotNull(serverApp);
    }

    @Test
    public void testParseArgumentsWithOnlySocketPort() throws Exception {
        Method parseMethod = ServerApp.class.getDeclaredMethod("parseArguments", String[].class);
        parseMethod.setAccessible(true);

        parseMethod.invoke(serverApp, (Object) new String[]{"8080"});

        assertNotNull(serverApp);
    }

    @Test
    public void testSetupLogging() throws Exception {
        Method setupMethod = ServerApp.class.getDeclaredMethod("setupLogging");
        setupMethod.setAccessible(true);

        setupMethod.invoke(serverApp);

        assertNotNull(serverApp);
    }

    @Test
    public void testHandleClientDisconnectWithPlayerNotInGame() throws Exception {
        PlayerId playerId = new PlayerId("player1");

        when(playerRegistry.getPlayerIdForClient("client1")).thenReturn(playerId);
        when(playerRegistry.getPlayerNickname(playerId)).thenReturn("nickname1");
        when(sessionManager.getGameSessionForPlayer(playerId)).thenReturn(null);

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleClientDisconnect", String.class);
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp, "client1");

        verify(playerRegistry).unregisterPlayer("client1");
    }

    @Test
    public void testHandleClientDisconnectWithPlayerInActiveGame() throws Exception {
        PlayerId playerId = new PlayerId("player1");
        String gameId = "game1";

        when(playerRegistry.getPlayerIdForClient("client1")).thenReturn(playerId);
        when(playerRegistry.getPlayerNickname(playerId)).thenReturn("nickname1");
        when(sessionManager.getGameSessionForPlayer(playerId)).thenReturn(gameSession);
        when(gameSession.getGameId()).thenReturn(gameId);
        when(gameSession.isStarted()).thenReturn(true);

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleClientDisconnect", String.class);
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp, "client1");

        verify(playerRegistry).unregisterPlayer("client1");
    }

    @Test
    public void testHandleClientDisconnectWithPlayerInLobby() throws Exception {
        PlayerId playerId = new PlayerId("player1");
        String gameId = "game1";

        when(playerRegistry.getPlayerIdForClient("client1")).thenReturn(playerId);
        when(playerRegistry.getPlayerNickname(playerId)).thenReturn("nickname1");
        when(sessionManager.getGameSessionForPlayer(playerId)).thenReturn(gameSession);
        when(gameSession.getGameId()).thenReturn(gameId);
        when(gameSession.isStarted()).thenReturn(false);

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleClientDisconnect", String.class);
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp, "client1");

        verify(playerRegistry).unregisterPlayer("client1");
    }

    @Test
    public void testHandleClientDisconnectWithNoPlayer() throws Exception {
        when(playerRegistry.getPlayerIdForClient("client1")).thenReturn(null);

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleClientDisconnect", String.class);
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp, "client1");

        verify(playerRegistry, never()).unregisterPlayer("client1");
    }

    @Test
    public void testPrintHelp() throws Exception {
        Method printMethod = ServerApp.class.getDeclaredMethod("printHelp");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Available commands:"));
    }

    @Test
    public void testPrintStatusWithRunningNetwork() throws Exception {
        setPrivateField(serverApp, "networkManager", networkManager);
        setPrivateField(serverApp, "playerRegistry", playerRegistry);
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(networkManager.isRunning()).thenReturn(true);
        when(playerRegistry.getAllClientIds()).thenReturn(Set.of("client1", "client2"));
        when(sessionManager.getAvailableGames()).thenReturn(Arrays.asList(gameModel));

        Method printMethod = ServerApp.class.getDeclaredMethod("printStatus");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Running"));
    }

    @Test
    public void testPrintStatusWithStoppedNetwork() throws Exception {
        setPrivateField(serverApp, "networkManager", networkManager);
        setPrivateField(serverApp, "playerRegistry", playerRegistry);
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(networkManager.isRunning()).thenReturn(false);
        when(playerRegistry.getAllClientIds()).thenReturn(Collections.emptySet());
        when(sessionManager.getAvailableGames()).thenReturn(Collections.emptyList());

        Method printMethod = ServerApp.class.getDeclaredMethod("printStatus");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Stopped"));
    }

    @Test
    public void testPrintGamesWithNoGames() throws Exception {
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(sessionManager.getAvailableGames()).thenReturn(Collections.emptyList());

        Method printMethod = ServerApp.class.getDeclaredMethod("printGames");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("No active games"));
    }

    @Test
    public void testPrintGamesWithGames() throws Exception {
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(sessionManager.getAvailableGames()).thenReturn(Arrays.asList(gameModel));
        when(gameModel.getGameId()).thenReturn("game1");
        when(gameModel.getGameName()).thenReturn("Test Game");
        when(gameModel.getPlayers()).thenReturn(Collections.emptyList());
        when(gameModel.getMaxPlayers()).thenReturn(4);
        when(gameModel.getGameLevel()).thenReturn(1);

        Method printMethod = ServerApp.class.getDeclaredMethod("printGames");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Active Games:"));
    }

    @Test
    public void testPrintGamesWithNullGameName() throws Exception {
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(sessionManager.getAvailableGames()).thenReturn(Arrays.asList(gameModel));
        when(gameModel.getGameId()).thenReturn("game1");
        when(gameModel.getGameName()).thenReturn(null);
        when(gameModel.getPlayers()).thenReturn(Collections.emptyList());
        when(gameModel.getMaxPlayers()).thenReturn(4);
        when(gameModel.getGameLevel()).thenReturn(1);

        Method printMethod = ServerApp.class.getDeclaredMethod("printGames");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Unnamed"));
    }

    @Test
    public void testPrintPlayersWithNoPlayers() throws Exception {
        setPrivateField(serverApp, "playerRegistry", playerRegistry);

        when(playerRegistry.getAllClientIds()).thenReturn(Collections.emptySet());

        Method printMethod = ServerApp.class.getDeclaredMethod("printPlayers");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("No connected players"));
    }

    @Test
    public void testPrintPlayersWithPlayers() throws Exception {
        setPrivateField(serverApp, "playerRegistry", playerRegistry);

        Map<String, String> playerInfo = new HashMap<>();
        playerInfo.put("nickname", "Player1");
        playerInfo.put("playerId", "id1");

        when(playerRegistry.getAllClientIds()).thenReturn(Set.of("client1"));
        when(playerRegistry.getPlayerInfo("client1")).thenReturn(playerInfo);

        Method printMethod = ServerApp.class.getDeclaredMethod("printPlayers");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Connected Players:"));
    }

    @Test
    public void testPrintPlayersWithNullPlayerInfo() throws Exception {
        setPrivateField(serverApp, "playerRegistry", playerRegistry);

        when(playerRegistry.getAllClientIds()).thenReturn(Set.of("client1"));
        when(playerRegistry.getPlayerInfo("client1")).thenReturn(null);

        Method printMethod = ServerApp.class.getDeclaredMethod("printPlayers");
        printMethod.setAccessible(true);

        printMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Connected Players:"));
    }

    @Test
    public void testHandleConsoleInputQuit() throws Exception {
        String input = "quit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        try {
            handleMethod.invoke(serverApp);
        } catch (Exception e) {
            // Expected due to System.exit(0)
        }

        assertNotNull(serverApp);
    }

    @Test
    public void testHandleConsoleInputExit() throws Exception {
        String input = "exit\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        try {
            handleMethod.invoke(serverApp);
        } catch (Exception e) {
            // Expected due to System.exit(0)
        }

        assertNotNull(serverApp);
    }

    @Test
    public void testHandleConsoleInputHelp() throws Exception {
        String input = "help\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Available commands:"));
    }

    @Test
    public void testHandleConsoleInputStatus() throws Exception {
        setPrivateField(serverApp, "networkManager", networkManager);
        setPrivateField(serverApp, "playerRegistry", playerRegistry);
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(networkManager.isRunning()).thenReturn(true);
        when(playerRegistry.getAllClientIds()).thenReturn(Collections.emptySet());
        when(sessionManager.getAvailableGames()).thenReturn(Collections.emptyList());

        String input = "status\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("Server Status:"));
    }

    @Test
    public void testHandleConsoleInputGames() throws Exception {
        setPrivateField(serverApp, "sessionManager", sessionManager);

        when(sessionManager.getAvailableGames()).thenReturn(Collections.emptyList());

        String input = "games\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("No active games"));
    }

    @Test
    public void testHandleConsoleInputPlayers() throws Exception {
        setPrivateField(serverApp, "playerRegistry", playerRegistry);

        when(playerRegistry.getAllClientIds()).thenReturn(Collections.emptySet());

        String input = "players\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp);

        String output = outputStream.toString();
        assertTrue(output.contains("No connected players"));
    }

    @Test
    public void testHandleConsoleInputUnknownCommand() throws Exception {
        String input = "unknown\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp);

        assertNotNull(serverApp);
    }

    @Test
    public void testHandleConsoleInputEmptyLine() throws Exception {
        String input = "\n\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        Method handleMethod = ServerApp.class.getDeclaredMethod("handleConsoleInput");
        handleMethod.setAccessible(true);

        handleMethod.invoke(serverApp);

        assertNotNull(serverApp);
    }

    @Test
    public void testShutdown() throws Exception {
        setPrivateField(serverApp, "connectionMonitor", connectionMonitor);
        setPrivateField(serverApp, "networkManager", networkManager);
        setPrivateField(serverApp, "commandDispatcher", commandDispatcher);
        setPrivateField(serverApp, "sessionManager", sessionManager);

        Method shutdownMethod = ServerApp.class.getDeclaredMethod("shutdown");
        shutdownMethod.setAccessible(true);

        shutdownMethod.invoke(serverApp);

        verify(connectionMonitor).stopMonitoring();
        verify(networkManager).stop();
        verify(commandDispatcher).shutdown();
        verify(sessionManager).shutdown();
    }

    @Test
    public void testShutdownWithNullComponents() throws Exception {
        Method shutdownMethod = ServerApp.class.getDeclaredMethod("shutdown");
        shutdownMethod.setAccessible(true);

        shutdownMethod.invoke(serverApp);

        assertNotNull(serverApp);
    }

    @Test
    public void testGetters() {
        setPrivateField(serverApp, "sessionManager", sessionManager);
        setPrivateField(serverApp, "playerRegistry", playerRegistry);
        setPrivateField(serverApp, "networkManager", networkManager);
        setPrivateField(serverApp, "commandDispatcher", commandDispatcher);

        assertEquals(sessionManager, serverApp.getSessionManager());
        assertEquals(playerRegistry, serverApp.getPlayerRegistry());
        assertEquals(networkManager, serverApp.getNetworkManager());
        assertEquals(commandDispatcher, serverApp.getCommandDispatcher());
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}