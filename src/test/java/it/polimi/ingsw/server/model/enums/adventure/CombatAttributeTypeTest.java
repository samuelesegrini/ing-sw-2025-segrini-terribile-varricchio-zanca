package it.polimi.ingsw.server.model.enums.adventure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatAttributeTypeTest {

    @Test

    void test() {

        CombatAttributeType test;
        test = CombatAttributeType.CANNON_STRENGTH;

                assertEquals(CombatAttributeType.CANNON_STRENGTH, test);

    }



}


