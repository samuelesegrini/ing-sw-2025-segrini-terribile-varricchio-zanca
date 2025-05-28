package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PiratesCardTest {
    @Test
    void getName() {
        PiratesCard card;
        List<CannonFire> attackPattern;
        CannonFire cannon1, cannon2, cannon3;
        cannon1 = new CannonFire(Direction.LEFT, ShotIntensity.HEAVY);
        cannon2 = new CannonFire(Direction.RIGHT, ShotIntensity.HEAVY);
        cannon3 = new CannonFire(Direction.UP, ShotIntensity.LIGHT);
        attackPattern = new ArrayList<>();
        attackPattern.add(cannon1);
        attackPattern.add(cannon2);
        attackPattern.add(cannon3);
        card = new PiratesCard("10", CardLevel.LEVEL_II, "description", 2, 3, 5, attackPattern);
        assertEquals("10", card.getId());
        assertEquals(CardLevel.LEVEL_II, card.getLevel());
        assertEquals("description", card.getDescription());
        assertEquals(2, card.getPowerLevel());
        assertEquals(3, card.getMovementPenalty());
        assertEquals(5, card.getCreditReward());
        assertEquals(attackPattern, card.getAttackPattern());

    }

}