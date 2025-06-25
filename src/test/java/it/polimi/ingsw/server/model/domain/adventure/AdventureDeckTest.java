package it.polimi.ingsw.server.model.domain.adventure;

import it.polimi.ingsw.server.model.domain.adventure.card.*;
import it.polimi.ingsw.server.model.domain.general.GameModel;
import it.polimi.ingsw.server.model.domain.player.PlayerId;
import it.polimi.ingsw.server.model.enums.GameLevel;
import it.polimi.ingsw.server.model.enums.adventure.AdventureType;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.util.PileIdentifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
class AdventureDeckTest {

    @Test

    void test(){

        AdventureDeck deck;

        SlaversCard card1, card2;
        EpidemicCard card3, card4, card5, card6;
        card1 = new SlaversCard("10", CardLevel.LEVEL_II, "description", 4, 5, 6, 7 );
        card2 = new SlaversCard("11", CardLevel.LEVEL_II, "description",  4, 5, 6, 7);
        card3 = new EpidemicCard("12", CardLevel.LEVEL_II, "description");
        card4 = new EpidemicCard("13", CardLevel.LEVEL_II, "description");
        card5 = new EpidemicCard("14", CardLevel.LEVEL_II, "description");
        card6 = new EpidemicCard("15", CardLevel.LEVEL_II, "description");


        List<AdventureCard> initialCoveredOrUnknownPile, deck1, deck2;
        deck1 = new ArrayList<>();
        deck2 = new ArrayList<>();
        initialCoveredOrUnknownPile = new ArrayList<>();
        deck1.add(card1);
        deck1.add(card2);
        deck2.add(card3);
        deck2.add(card4);
        initialCoveredOrUnknownPile.add(card5);
        initialCoveredOrUnknownPile.add(card6);

        List<List<AdventureCard>> initialUncoveredPiles;
        initialUncoveredPiles = new ArrayList<>();
        initialUncoveredPiles.add(deck1);
        initialUncoveredPiles.add(deck2);

        deck = new AdventureDeck(GameLevel.TEST_FLIGHT, initialUncoveredPiles, initialCoveredOrUnknownPile);

        PileIdentifier pileid;
        PlayerId playerid;
        UUID uuid = UUID.randomUUID();
        playerid = new PlayerId(uuid, "name");
        pileid = PileIdentifier.BOTTOM_LEFT;
        assertFalse(deck.canPlayerViewPile(playerid, pileid));



    }



}

