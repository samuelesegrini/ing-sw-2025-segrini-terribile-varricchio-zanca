package it.polimi.ingsw.server.model.enums.adventure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdventureTypeTest {

    @Test

    void test() {

        AdventureType type;

        type = AdventureType.PIRATES;

                assertEquals(AdventureType.PIRATES, type);

    }

}