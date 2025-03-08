package it.polimi.ingsw.model.adventure;

import java.util.List;
import java.util.Map;

import it.polimi.ingsw.model.enums.CardLevel;

import it.polimi.ingsw.model.domain.adventure.card.AdventureCard;

public class AdventureCardFactory {
    private Map<CardLevel, List<AdventureCard>> cardsByLevel;
    private String cardsJsonPath;

    public AdventureCardFactory(){}
    public AdventureCardFactory(String jsonPath){}

    public void loadCardsFromJson(String path){}
    public AdventureCard createCardFromJson(JsonNode cardNode, CardLevel level){
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
