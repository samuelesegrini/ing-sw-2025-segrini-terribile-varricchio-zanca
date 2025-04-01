package it.polimi.ingsw.model.domain.flight;

import it.polimi.ingsw.model.domain.player.Player;
import it.polimi.ingsw.model.domain.player.PlayerId;
import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.flight.FlightStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlightBoardTest {

    private FlightBoard flightBoard;
    private GameLevel gameLevel;
    private Player player1, player2, player3;

    @BeforeEach
    void setUp() {
        gameLevel = GameLevel.TEST_FLIGHT;
        flightBoard = new FlightBoard(gameLevel);

        player1 = new Player(new PlayerId(UUID.randomUUID(), "Samuele"));
        player2 = new Player(new PlayerId(UUID.randomUUID(), "Diego"));
        player3 = new Player(new PlayerId(UUID.randomUUID(), "Manuela"));

        flightBoard.registerPlayer(player1);
        flightBoard.registerPlayer(player2);
        flightBoard.registerPlayer(player3);

        player1.getFlightData().setPosition(5, flightBoard.getRoute().getLength());
        player2.getFlightData().setPosition(8, flightBoard.getRoute().getLength());
        player3.getFlightData().setPosition(3, flightBoard.getRoute().getLength());
    }

    @Test
    void testFlightBoardCreation() {
        // Verifica che la FlightBoard sia creata correttamente
        assertNotNull(flightBoard, "La FlightBoard dovrebbe essere creata correttamente.");
    }

    @Test
    void testRouteCreationForTestFlight() {
        // Verifica che la route venga creata correttamente per TEST_FLIGHT
        if (gameLevel == GameLevel.TEST_FLIGHT) {
            assertNotNull(flightBoard.getRoute(), "La route dovrebbe essere creata correttamente.");
            assertEquals(18, flightBoard.getRoute().getLength(), "La lunghezza della route per TEST_FLIGHT dovrebbe essere 18.");
        }
    }

    @Test
    void testRouteCreationForLevelII() {
        gameLevel = GameLevel.LEVEL_II;
        flightBoard = new FlightBoard(gameLevel);

        // Verifica che la route venga creata correttamente per LEVEL_II
        if (gameLevel == GameLevel.LEVEL_II) {
            assertNotNull(flightBoard.getRoute(), "La route dovrebbe essere creata correttamente.");
            assertEquals(24, flightBoard.getRoute().getLength(), "La lunghezza della route per LEVEL_II dovrebbe essere 24.");
        }
    }

    @Test
    void testStartingPositionsForTestFlight() {
        // Verifica che le posizioni di partenza siano corrette per TEST_FLIGHT
        gameLevel = GameLevel.TEST_FLIGHT;
        flightBoard = new FlightBoard(gameLevel);

        if (gameLevel == GameLevel.TEST_FLIGHT) {
            List<Integer> expectedStartingPositions = Arrays.asList(0, 1, 2, 4);
            List<Integer> actualStartingPositions = flightBoard.getRoute().getStartingPositions();
            assertEquals(expectedStartingPositions, actualStartingPositions, "Le posizioni di partenza per TEST_FLIGHT non sono corrette.");
        }
    }

    @Test
    void testStartingPositionsForLevelII() {
        gameLevel = GameLevel.LEVEL_II;
        flightBoard = new FlightBoard(gameLevel);

        // Verifica che le posizioni di partenza siano corrette per LEVEL_II
        if (gameLevel == GameLevel.LEVEL_II) {
            List<Integer> expectedStartingPositions = Arrays.asList(0, 1, 3, 6);
            List<Integer> actualStartingPositions = flightBoard.getRoute().getStartingPositions();
            assertEquals(expectedStartingPositions, actualStartingPositions, "Le posizioni di partenza per LEVEL_II non sono corrette.");
        }
    }

    @Test
    void testRegisterPlayer() {
        Player player = new Player(new PlayerId(UUID.randomUUID(), "Player1"));
        flightBoard.registerPlayer(player);

        // Verifica che il giocatore sia stato registrato correttamente
        assertTrue(flightBoard.getCurrentOrder().contains(player), "Il giocatore dovrebbe essere registrato correttamente nella FlightBoard.");
        assertNotNull((flightBoard.getPlayerData(player)), "Sono stati inizializzati i dati di voto di player.");
    }

    @Test
    void testAbandonPlayer() {
        // Verifica che Player1 sia registrato inizialmente
        assertTrue(flightBoard.getCurrentOrder().contains(player1), "Player1 dovrebbe essere registrato.");

        // Abbandona Player1
        flightBoard.abandonPlayer(player1);

        // Verifica che Player1 sia stato rimosso correttamente
        assertFalse(flightBoard.getCurrentOrder().contains(player1), "Player1 non dovrebbe più essere registrato.");
    }

    @Test
    void testPlayerOrder() {
        // Verifica che i giocatori siano nell'ordine corretto
        assertEquals(3, flightBoard.getCurrentOrder().size(), "Dovrebbero esserci due giocatori registrati.");
        assertEquals(player1, flightBoard.getCurrentOrder().get(0), "Il primo giocatore nell'ordine dovrebbe essere Player1.");
        assertEquals(player2, flightBoard.getCurrentOrder().get(1), "Il secondo giocatore nell'ordine dovrebbe essere Player2.");
    }
    @Test
    void testUpdateCurrentOrder() {
        player1.getFlightData().setPosition(5, flightBoard.getRoute().getLength());
        player2.getFlightData().setPosition(8, flightBoard.getRoute().getLength());
        player3.getFlightData().setPosition(3, flightBoard.getRoute().getLength());

        flightBoard.updateCurrentOrder();

        // Verifica che l'ordine sia corretto (dovrebbero essere ordinati dalla posizione più alta alla più bassa)
        List<Player> order = flightBoard.getCurrentOrder();
        assertEquals(player2, order.get(0), "Player2 dovrebbe essere in prima posizione.");
        assertEquals(player1, order.get(1), "Player1 dovrebbe essere in seconda posizione.");
        assertEquals(player3, order.get(2), "Player3 dovrebbe essere in terza posizione.");
    }

    @Test
    void testGetPlayersAhead() {
        player1.getFlightData().setPosition(5, flightBoard.getRoute().getLength());
        player2.getFlightData().setPosition(8, flightBoard.getRoute().getLength());
        player3.getFlightData().setPosition(3, flightBoard.getRoute().getLength());

        // Verifica che i giocatori davanti a Player1 siano correttamente identificati
        List<Player> playersAhead = flightBoard.getPlayersAhead(player1, flightBoard.getRoute().getLength());
        assertEquals(1, playersAhead.size(), "Dovrebbe esserci un giocatore davanti a Player1.");
        assertTrue(playersAhead.contains(player2), "Player2 dovrebbe essere avanti a Player1.");
    }

    @Test
    void testGetPlayersBehind() {
        // Impostiamo le posizioni dei giocatori
        player1.getFlightData().setPosition(5, flightBoard.getRoute().getLength());
        player2.getFlightData().setPosition(8, flightBoard.getRoute().getLength());
        player3.getFlightData().setPosition(3, flightBoard.getRoute().getLength());

        // Verifica che i giocatori dietro Player1 siano correttamente identificati
        List<Player> playersBehind = flightBoard.getPlayersBehind(player1, flightBoard.getRoute().getLength());
        assertEquals(1, playersBehind.size(), "Dovrebbe esserci un giocatore dietro Player1.");
        assertTrue(playersBehind.contains(player3), "Player3 dovrebbe essere dietro a Player1.");
    }

    @Test
    void testMovePlayerForward() {
        player1.getFlightData().setPosition(5, flightBoard.getRoute().getLength());
        flightBoard.movePlayer(player1, 3, true);

        // Verifica che Player1 sia stato spostato correttamente condiderandoc he in poszione 8 c'è player2
        assertEquals(9, player1.getFlightData().getPosition(), "Player1 dovrebbe essere ora in posizione 9.");
    }

    @Test
    void testMovePlayerBackward() {
        // Impostiamo le posizioni dei giocatori
        player1.getFlightData().setPosition(5, flightBoard.getRoute().getLength());

        // Muoviamo Player1 indietro di 2 posizioni, considerando che la posizione 3 è occupata da player3
        flightBoard.movePlayer(player1, 2, false);

        // Verifica che Player1 sia stato spostato correttamente
        assertEquals(2, player1.getFlightData().getPosition(), "Player1 dovrebbe essere ora in posizione 3.");
    }
}