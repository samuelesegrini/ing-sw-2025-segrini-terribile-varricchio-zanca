package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.adventure.entity.Meteor;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeteorSwarmCardTest {

    @Test
    void test() {
        MeteorSwarmCard card;
        Meteor meteor1 = new Meteor(ShotIntensity.LIGHT, Direction.UP);
        Meteor meteor2 = new Meteor(ShotIntensity.HEAVY, Direction.DOWN);
        List<Meteor> meteors = new ArrayList<>();
        meteors.add(meteor1);
        meteors.add(meteor2);
        card = new MeteorSwarmCard("10", CardLevel.TEST_FLIGHT, "description", meteors );

        assertEquals("10", card.getId());
        assertEquals(CardLevel.TEST_FLIGHT, card.getLevel());
        assertEquals("description", card.getDescription());
        assertEquals(meteors, card.getMeteorPattern());
        assertEquals(AdventureType.METEOR_SWARM, card.getType());


    }
}