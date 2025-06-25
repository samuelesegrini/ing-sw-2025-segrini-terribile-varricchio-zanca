package it.polimi.ingsw.server.model.enums.ship;

import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorTypeTest {

    @Test

    void test() {

        ConnectorType type;

        type = ConnectorType.DOUBLE;

        assertEquals(ConnectorType.DOUBLE, type);

        ConnectorType type2;
        type2 = ConnectorType.DOUBLE;
        assertEquals(ConnectorType.DOUBLE, type2);

        assertTrue(type.canConnectTo(type2));

        type = ConnectorType.PLAIN;

        assertFalse(type.canConnectTo(type2));

        type2 = ConnectorType.PLAIN;

        assertTrue(type.canConnectTo(type2));

        type = type2 = ConnectorType.UNIVERSAL;

        assertTrue(type.canConnectTo(type2));


    }


}