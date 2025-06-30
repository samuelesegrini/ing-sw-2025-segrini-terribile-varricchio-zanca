package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.player.Player;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.flight.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlightBoardTest {

    private FlightBoard flightBoard;
    private Route route;
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;

    @BeforeEach
    void setUp() {
        // Creo una route di test
        List<Integer> startingPositions = Arrays.asList(0, 1, 2, 3);
        route = new Route(GameLevel.TEST_FLIGHT, 3, startingPositions, null);
        flightBoard = new FlightBoard(GameLevel.TEST_FLIGHT, route, 3);

        // Creo i giocatori di test
        PlayerId playerId1 = new PlayerId(UUID.randomUUID(), "Player1");
        PlayerId playerId2 = new PlayerId(UUID.randomUUID(), "Player2");
        PlayerId playerId3 = new PlayerId(UUID.randomUUID(), "Player3");
        PlayerId playerId4 = new PlayerId(UUID.randomUUID(), "Player4");

        player1 = new Player(playerId1);
        player2 = new Player(playerId2);
        player3 = new Player(playerId3);
        player4 = new Player(playerId4);
    }

    @Test
    void testConstructor() {
        assertEquals(route, flightBoard.getRoute());
        assertEquals(3, flightBoard.getPlayerCount());
        assertTrue(flightBoard.getCurrentOrder().isEmpty());
        assertTrue(flightBoard.getPlayerDataMap().isEmpty());
    }

    @Test
    void testGetRoute() {
        assertEquals(route, flightBoard.getRoute());
    }

    @Test
    void testGetPlayerCount() {
        assertEquals(3, flightBoard.getPlayerCount());
    }

    @Test
    void testGetCurrentOrder() {
        List<Player> currentOrder = flightBoard.getCurrentOrder();
        assertTrue(currentOrder.isEmpty());

        flightBoard.registerPlayer(player1);
        assertEquals(1, currentOrder.size());
        assertTrue(currentOrder.contains(player1));
    }

    @Test
    void testGetPlayerData() {
        flightBoard.registerPlayer(player1);

        PlayerFlightData data = flightBoard.getPlayerData(player1);
        assertNotNull(data);
        assertEquals(FlightStatus.RACING, data.getStatus());
    }

    @Test
    void testGetPlayerDataMap() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        Map<Player, PlayerFlightData> dataMap = flightBoard.getPlayerDataMap();
        assertEquals(2, dataMap.size());
        assertTrue(dataMap.containsKey(player1));
        assertTrue(dataMap.containsKey(player2));
    }

    @Test
    void testGetLeadingPlayer() {
        // Test con nessun giocatore registrato
        assertNull(flightBoard.getLeadingPlayer());

        // Test con un giocatore
        flightBoard.registerPlayer(player1);
        assertEquals(player1, flightBoard.getLeadingPlayer());

        // Test con più giocatori - il primo nell'ordine corrente è il leader
        flightBoard.registerPlayer(player2);
        assertEquals(player1, flightBoard.getLeadingPlayer());
    }

    @Test
    void testUpdateCurrentOrder() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
        flightBoard.registerPlayer(player3);

        // Imposto posizioni diverse
        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(10, route.getLength());
        player3.getFlightData().setPosition(7, route.getLength());

        flightBoard.updateCurrentOrder();

        List<Player> currentOrder = flightBoard.getCurrentOrder();
        assertEquals(player2, currentOrder.get(0)); // posizione 10
        assertEquals(player3, currentOrder.get(1)); // posizione 7
        assertEquals(player1, currentOrder.get(2)); // posizione 5
    }

    @Test
    void testUpdateCurrentOrderWithSamePositions() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        // Imposto stessa posizione
        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(5, route.getLength());

        flightBoard.updateCurrentOrder();

        // L'ordine rimane invariato quando le posizioni sono uguali
        List<Player> currentOrder = flightBoard.getCurrentOrder();
        assertEquals(2, currentOrder.size());
    }

    @Test
    void testUpdateCurrentOrderAlreadySorted() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        // Imposto posizioni già ordinate
        player1.getFlightData().setPosition(10, route.getLength());
        player2.getFlightData().setPosition(5, route.getLength());

        flightBoard.updateCurrentOrder();

        List<Player> currentOrder = flightBoard.getCurrentOrder();
        assertEquals(player1, currentOrder.get(0));
        assertEquals(player2, currentOrder.get(1));
    }

    @Test
    void testGetPlayersAhead() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
        flightBoard.registerPlayer(player3);

        // Imposto posizioni
        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(8, route.getLength());
        player3.getFlightData().setPosition(12, route.getLength());

        List<Player> playersAhead = flightBoard.getPlayersAhead(player1, 5);
        assertEquals(1, playersAhead.size());
        assertTrue(playersAhead.contains(player2));
        assertFalse(playersAhead.contains(player3)); // troppo lontano
    }

    @Test
    void testGetPlayersAheadNoPlayers() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        player1.getFlightData().setPosition(10, route.getLength());
        player2.getFlightData().setPosition(5, route.getLength());

        List<Player> playersAhead = flightBoard.getPlayersAhead(player1, 3);
        assertTrue(playersAhead.isEmpty());
    }

    @Test
    void testGetPlayersAheadExactDistance() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(8, route.getLength());

        List<Player> playersAhead = flightBoard.getPlayersAhead(player1, 3);
        assertEquals(1, playersAhead.size());
        assertTrue(playersAhead.contains(player2));
    }

    @Test
    void testGetPlayersBehind() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
        flightBoard.registerPlayer(player3);

        // Imposto posizioni
        player1.getFlightData().setPosition(10, route.getLength());
        player2.getFlightData().setPosition(7, route.getLength());
        player3.getFlightData().setPosition(3, route.getLength());

        List<Player> playersBehind = flightBoard.getPlayersBehind(player1, 5);
        assertEquals(1, playersBehind.size());
        assertTrue(playersBehind.contains(player2));
        assertFalse(playersBehind.contains(player3)); // troppo lontano
    }

    @Test
    void testGetPlayersBehindNoPlayers() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(10, route.getLength());

        List<Player> playersBehind = flightBoard.getPlayersBehind(player1, 3);
        assertTrue(playersBehind.isEmpty());
    }

    @Test
    void testGetPlayersBehindExactDistance() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        player1.getFlightData().setPosition(10, route.getLength());
        player2.getFlightData().setPosition(7, route.getLength());

        List<Player> playersBehind = flightBoard.getPlayersBehind(player1, 3);
        assertEquals(1, playersBehind.size());
        assertTrue(playersBehind.contains(player2));
    }

    @Test
    void testMovePlayerForward() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        // Imposto posizioni iniziali
        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(7, route.getLength());

        int initialPosition = player1.getFlightData().getPosition();

        flightBoard.movePlayer(player1, 3, true);

        // Posizione finale dovrebbe essere iniziale + spazi + giocatori davanti
        int playersAhead = flightBoard.getPlayersAhead(player1, 3).size();
        assertEquals(initialPosition + 3 + playersAhead, player1.getFlightData().getPosition());
    }

    @Test
    void testMovePlayerBackward() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        // Imposto posizioni iniziali
        player1.getFlightData().setPosition(10, route.getLength());
        player2.getFlightData().setPosition(8, route.getLength());

        int initialPosition = player1.getFlightData().getPosition();

        flightBoard.movePlayer(player1, 3, false);

        // Posizione finale dovrebbe essere iniziale - spazi - giocatori dietro
        int playersBehind = flightBoard.getPlayersBehind(player1, 3).size();
        assertEquals(initialPosition - 3 - playersBehind, player1.getFlightData().getPosition());
    }

    @Test
    void testMovePlayerForwardWithNoPlayersAhead() {
        flightBoard.registerPlayer(player1);

        player1.getFlightData().setPosition(5, route.getLength());
        int initialPosition = player1.getFlightData().getPosition();

        flightBoard.movePlayer(player1, 3, true);

        assertEquals(initialPosition + 3, player1.getFlightData().getPosition());
    }

    @Test
    void testMovePlayerBackwardWithNoPlayersBehind() {
        flightBoard.registerPlayer(player1);

        player1.getFlightData().setPosition(10, route.getLength());
        int initialPosition = player1.getFlightData().getPosition();

        flightBoard.movePlayer(player1, 3, false);

        assertEquals(initialPosition - 3, player1.getFlightData().getPosition());
    }

    @Test
    void testIsPositionOccupied() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(8, route.getLength());

        assertTrue(flightBoard.isPositionOccupied(5));
        assertTrue(flightBoard.isPositionOccupied(8));
        assertFalse(flightBoard.isPositionOccupied(10));
    }

    @Test
    void testIsPositionOccupiedNoPlayers() {
        assertFalse(flightBoard.isPositionOccupied(5));
    }

    @Test
    void testRegisterPlayer() {
        assertEquals(3, flightBoard.getPlayerCount());
        assertTrue(flightBoard.getCurrentOrder().isEmpty());

        flightBoard.registerPlayer(player1);

        assertEquals(4, flightBoard.getPlayerCount());
        assertEquals(1, flightBoard.getCurrentOrder().size());
        assertTrue(flightBoard.getCurrentOrder().contains(player1));

        PlayerFlightData data = flightBoard.getPlayerData(player1);
        assertNotNull(data);
        assertEquals(FlightStatus.RACING, data.getStatus());
    }

    @Test
    void testRegisterMultiplePlayers() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
        flightBoard.registerPlayer(player3);

        assertEquals(6, flightBoard.getPlayerCount());
        assertEquals(3, flightBoard.getCurrentOrder().size());

        assertTrue(flightBoard.getCurrentOrder().contains(player1));
        assertTrue(flightBoard.getCurrentOrder().contains(player2));
        assertTrue(flightBoard.getCurrentOrder().contains(player3));
    }

    @Test
    void testAbandonPlayer() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        assertEquals(5, flightBoard.getPlayerCount());
        assertEquals(2, flightBoard.getCurrentOrder().size());

        flightBoard.abandonPlayer(player1);

        assertEquals(4, flightBoard.getPlayerCount());
        assertEquals(1, flightBoard.getCurrentOrder().size());
        assertFalse(flightBoard.getCurrentOrder().contains(player1));
        assertTrue(flightBoard.getCurrentOrder().contains(player2));

        PlayerFlightData data = flightBoard.getPlayerData(player1);
        assertEquals(FlightStatus.ABANDONED, data.getStatus());
    }

    @Test
    void testAbandonPlayerNotInOrder() {
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);

        // Rimuovo player1 dall'ordine manualmente per testare il caso edge
        flightBoard.getCurrentOrder().remove(player1);

        flightBoard.abandonPlayer(player1);

        assertEquals(4, flightBoard.getPlayerCount());
        assertEquals(FlightStatus.ABANDONED, flightBoard.getPlayerData(player1).getStatus());
    }

    @Test
    void testComplexScenario() {
        // Test di scenario complesso che combina più operazioni
        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
        flightBoard.registerPlayer(player3);

        // Imposto posizioni
        player1.getFlightData().setPosition(5, route.getLength());
        player2.getFlightData().setPosition(10, route.getLength());
        player3.getFlightData().setPosition(15, route.getLength());

        // Muovo player1 in avanti
        flightBoard.movePlayer(player1, 3, true);

        // Aggiorno l'ordine
        flightBoard.updateCurrentOrder();

        // Verifico che l'ordine sia corretto
        List<Player> currentOrder = flightBoard.getCurrentOrder();
        assertEquals(player3, currentOrder.get(0)); // posizione più alta

        // Abbandono un giocatore
        flightBoard.abandonPlayer(player2);

        assertEquals(5, flightBoard.getPlayerCount());
        assertEquals(FlightStatus.ABANDONED, flightBoard.getPlayerData(player2).getStatus());
        assertFalse(flightBoard.getCurrentOrder().contains(player2));
    }
}