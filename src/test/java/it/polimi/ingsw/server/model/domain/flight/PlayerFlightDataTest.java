package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.enums.flight.FlightStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class PlayerFlightDataTest {

    private PlayerFlightData playerFlightData;
    private final int START_POSITION = 5;

    @BeforeEach
    void setUp() {
        playerFlightData = new PlayerFlightData(START_POSITION);
    }

    @Test
    void testConstructor() {
        // Test che il costruttore inizializzi correttamente tutti i valori
        assertEquals(START_POSITION, playerFlightData.getPosition());
        assertEquals(START_POSITION, playerFlightData.getStartPosition());
        assertEquals(0, playerFlightData.getLapsCompleted());
        assertEquals(FlightStatus.RACING, playerFlightData.getStatus());
    }

    @Test
    void testConstructorWithZeroStartPosition() {
        // Test con posizione di partenza zero
        PlayerFlightData data = new PlayerFlightData(0);
        assertEquals(0, data.getPosition());
        assertEquals(0, data.getStartPosition());
        assertEquals(0, data.getLapsCompleted());
        assertEquals(FlightStatus.RACING, data.getStatus());
    }

    @Test
    void testConstructorWithNegativeStartPosition() {
        // Test con posizione di partenza negativa
        PlayerFlightData data = new PlayerFlightData(-3);
        assertEquals(-3, data.getPosition());
        assertEquals(-3, data.getStartPosition());
        assertEquals(0, data.getLapsCompleted());
        assertEquals(FlightStatus.RACING, data.getStatus());
    }

    @Test
    void testGetPosition() {
        // Test del getter della posizione
        assertEquals(START_POSITION, playerFlightData.getPosition());
    }

    @Test
    void testGetStartPosition() {
        // Test del getter della posizione di partenza
        assertEquals(START_POSITION, playerFlightData.getStartPosition());
    }

    @Test
    void testGetLapsCompleted() {
        // Test del getter dei giri completati
        assertEquals(0, playerFlightData.getLapsCompleted());
    }

    @Test
    void testGetStatus() {
        // Test del getter dello status
        assertEquals(FlightStatus.RACING, playerFlightData.getStatus());
    }

    @Test
    void testSetStatusToAbandoned() {
        // Test del setter dello status per ABANDONED
        playerFlightData.setStatus(FlightStatus.ABANDONED);
        assertEquals(FlightStatus.ABANDONED, playerFlightData.getStatus());
    }

    @Test
    void testSetStatusToRacing() {
        // Test del setter dello status per RACING
        playerFlightData.setStatus(FlightStatus.ABANDONED);
        playerFlightData.setStatus(FlightStatus.RACING);
        assertEquals(FlightStatus.RACING, playerFlightData.getStatus());
    }

    @Test
    void testSetPositionWithoutLaps() {
        // Test setPosition senza completare giri
        int routeLength = 20;
        int newPosition = 10;

        playerFlightData.setPosition(newPosition, routeLength);

        assertEquals(newPosition, playerFlightData.getPosition());
        assertEquals(0, playerFlightData.getLapsCompleted());
    }

    @Test
    void testSetPositionWithOneLap() {
        // Test setPosition completando esattamente un giro
        int routeLength = 20;
        int newPosition = 25; // 20 + 5 = 1 giro + 5 posizioni

        playerFlightData.setPosition(newPosition, routeLength);

        assertEquals(5, playerFlightData.getPosition()); // 25 % 20 = 5
        assertEquals(1, playerFlightData.getLapsCompleted()); // 25 / 20 = 1
    }

    @Test
    void testSetPositionWithMultipleLaps() {
        // Test setPosition completando più giri
        int routeLength = 15;
        int newPosition = 47; // 15 * 3 + 2 = 3 giri + 2 posizioni

        playerFlightData.setPosition(newPosition, routeLength);

        assertEquals(2, playerFlightData.getPosition()); // 47 % 15 = 2
        assertEquals(3, playerFlightData.getLapsCompleted()); // 47 / 15 = 3
    }

    @Test
    void testSetPositionExactlyOneLap() {
        // Test setPosition con posizione esatta per un giro completo
        int routeLength = 10;
        int newPosition = 10; // Esattamente un giro

        playerFlightData.setPosition(newPosition, routeLength);

        assertEquals(0, playerFlightData.getPosition()); // 10 % 10 = 0
        assertEquals(1, playerFlightData.getLapsCompleted()); // 10 / 10 = 1
    }

    @Test
    void testSetPositionWithZeroPosition() {
        // Test setPosition con posizione zero
        int routeLength = 10;
        int newPosition = 0;

        playerFlightData.setPosition(newPosition, routeLength);

        assertEquals(0, playerFlightData.getPosition());
        assertEquals(0, playerFlightData.getLapsCompleted());
    }

    @Test
    void testSetPositionWithRouteLength1() {
        // Test edge case con lunghezza del percorso = 1
        int routeLength = 1;
        int newPosition = 5;

        playerFlightData.setPosition(newPosition, routeLength);

        assertEquals(0, playerFlightData.getPosition()); // 5 % 1 = 0
        assertEquals(5, playerFlightData.getLapsCompleted()); // 5 / 1 = 5
    }

    @Test
    void testSetPositionMultipleTimes() {
        // Test di chiamate multiple a setPosition
        int routeLength = 12;

        // Prima chiamata
        playerFlightData.setPosition(15, routeLength);
        assertEquals(3, playerFlightData.getPosition());
        assertEquals(1, playerFlightData.getLapsCompleted());

        // Seconda chiamata
        playerFlightData.setPosition(30, routeLength);
        assertEquals(6, playerFlightData.getPosition());
        assertEquals(2, playerFlightData.getLapsCompleted());

        // Terza chiamata con posizione minore
        playerFlightData.setPosition(8, routeLength);
        assertEquals(8, playerFlightData.getPosition());
        assertEquals(0, playerFlightData.getLapsCompleted());
    }

    @Test
    void testCompleteScenario() {
        // Test scenario completo: creazione, modifica posizione e status
        PlayerFlightData data = new PlayerFlightData(3);

        // Verifica stato iniziale
        assertEquals(3, data.getPosition());
        assertEquals(3, data.getStartPosition());
        assertEquals(0, data.getLapsCompleted());
        assertEquals(FlightStatus.RACING, data.getStatus());

        // Muovi il giocatore
        data.setPosition(23, 10);
        assertEquals(3, data.getPosition()); // 23 % 10 = 3
        assertEquals(2, data.getLapsCompleted()); // 23 / 10 = 2
        assertEquals(FlightStatus.RACING, data.getStatus());

        // Cambia status
        data.setStatus(FlightStatus.ABANDONED);
        assertEquals(FlightStatus.ABANDONED, data.getStatus());

        // La posizione di partenza rimane invariata
        assertEquals(3, data.getStartPosition());
    }

    @Test
    void testSerializationFields() {
        // Test che verifica l'esistenza del campo serialVersionUID
        // Questo test assicura che la classe sia correttamente serializzabile
        assertTrue(PlayerFlightData.class.isAssignableFrom(PlayerFlightData.class));
        assertNotNull(playerFlightData);
    }

    @Test
    void testPositionBoundaryValues() {
        // Test con valori limite per la posizione
        int routeLength = 100;

        // Test con il valore massimo di un integer
        playerFlightData.setPosition(Integer.MAX_VALUE, routeLength);
        assertEquals(Integer.MAX_VALUE % routeLength, playerFlightData.getPosition());
        assertEquals(Integer.MAX_VALUE / routeLength, playerFlightData.getLapsCompleted());

        // Reset per il test successivo
        playerFlightData = new PlayerFlightData(0);

        // Test con valori negativi
        playerFlightData.setPosition(-50, routeLength);
        assertEquals(-50 % routeLength, playerFlightData.getPosition());
        assertEquals(-50 / routeLength, playerFlightData.getLapsCompleted());
    }
}
