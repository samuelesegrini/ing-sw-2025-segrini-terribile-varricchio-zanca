package it.polimi.ingsw.server.model.domain.adventure.entity;

import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeteorTest {

    @Test

            void test() {

        Meteor meteor;
        meteor = new Meteor(ShotIntensity.HEAVY, Direction.RIGHT);

        assertEquals(ShotIntensity.HEAVY, meteor.getShotIntensity());
        assertEquals(Direction.RIGHT, meteor.getApproach());
        assertEquals(ShotIntensity.HEAVY, meteor.getType());

    }

}