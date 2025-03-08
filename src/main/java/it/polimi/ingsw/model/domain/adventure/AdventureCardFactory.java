package it.polimi.ingsw.model.domain.adventure;

import java.util.List;
import java.util.Map;

import it.polimi.ingsw.model.enums.GameLevel;
import it.polimi.ingsw.model.enums.adventure.CardLevel;

import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;

public class AdventureCardFactory {
    private Map<CardLevel, List<AdventureCard>> cardsByLevel;
    private String cardsJsonPath;

    public AdventureCardFactory(){}
    public AdventureCardFactory(String jsonPath){}

    public void loadCardsFromJson(String path){}

    //TODO: check the JsonNode import and what to use to handle it
    public AdventureCard createCardFromJson(/*JsonNode cardNode,*/ CardLevel level){
        return null;
    }
    public AdventureDeck createDeckForGameLevel(GameLevel gameLevel){
        return null;
    }
    public List<AdventureCard> createPileForGameLevel(GameLevel gameLevel){
        return List.of();
    }
    public List<AdventureCard> selectRandomCards(CardLevel level, int count){
        return List.of();
    }
    public AdventureCard cloneCard(AdventureCard original){
        return original;
    }
}
