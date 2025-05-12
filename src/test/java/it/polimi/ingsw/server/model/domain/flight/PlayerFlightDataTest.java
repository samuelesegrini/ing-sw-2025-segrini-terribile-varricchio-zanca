package it.polimi.ingsw.server.model.domain.flight;

import it.polimi.ingsw.server.model.domain.flight.PlayerFlightData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerFlightDataTest {

    private PlayerFlightData playerFlightData;

    @BeforeEach
    void setUp() {
        playerFlightData = new PlayerFlightData(0); // inizializzo con la posizione di partenza 0
    }

    @Test
    void testSetPosition_WithinRouteLength() {
        // Testa una posizione che è inferiore alla lunghezza del percorso
        int routeLength = 10;
        playerFlightData.setPosition(5, routeLength);

        assertEquals(5, playerFlightData.getPosition(), "La posizione dovrebbe essere 5.");
        assertEquals(0, playerFlightData.getLapsCompleted(), "Il numero di giri completati dovrebbe essere 0.");
    }

    @Test
    void testSetPosition_ExceedsRouteLength_WithoutMultiple() {
        // Testa una posizione che supera il percorso ma non è un multiplo esatto
        int routeLength = 10;
        playerFlightData.setPosition(12, routeLength);

        assertEquals(2, playerFlightData.getPosition(), "La posizione dovrebbe essere 2.");
        assertEquals(1, playerFlightData.getLapsCompleted(), "Il numero di giri completati dovrebbe essere 1.");
    }

    @Test
    void testSetPosition_ExceedsRouteLength_ExactMultiple() {
        // Testa una posizione che è un multiplo esatto della lunghezza del percorso
        int routeLength = 10;
        playerFlightData.setPosition(20, routeLength);

        assertEquals(0, playerFlightData.getPosition(), "La posizione dovrebbe essere 0.");
        assertEquals(2, playerFlightData.getLapsCompleted(), "Il numero di giri completati dovrebbe essere 2.");
    }

    @Test
    void testSetPosition_LargePosition() {
        // Testa una posizione molto grande che supera di molto la lunghezza del percorso
        int routeLength = 10;
        playerFlightData.setPosition(105, routeLength);

        assertEquals(5, playerFlightData.getPosition(), "La posizione dovrebbe essere 5.");
        assertEquals(10, playerFlightData.getLapsCompleted(), "Il numero di giri completati dovrebbe essere 10.");
    }
}