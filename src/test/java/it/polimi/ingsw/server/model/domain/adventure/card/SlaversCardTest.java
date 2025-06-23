package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


class SlaversCardTest {

    @Test
    void testSlaversCard() {


        SlaversCard card;

        card = new SlaversCard("10", CardLevel.LEVEL_II, "description", 3, 4, 5, 6);



        assertEquals("10", card.getId());

        assertEquals(CardLevel.LEVEL_II, card.getLevel());

        assertEquals("description", card.getDescription());

        assertEquals(3, card.getPowerLevel());

        assertEquals(4, card.getMovementPenalty());

        assertEquals(5, card.getCreditReward());

        assertEquals(6, card.getCrewLossAmount());

    }

}