package it.polimi.ingsw.model.adventure;

public class AdventureCardFactory {
    private Map<CardLevel, List<AdventureCard>> cardsByLevel;
    private String cardsJsonPath

            //no constructor??

    public void AdventureCardFactory(){}
    public void AdventureCardFactory(String jsonPath){}
    public void loadCardsFromJson(String path){}
    public void AdventureCard createCardFromJson(JsonNode cardNode, CardLevel level){}
    public void AdventureDeck createDeckForGameLevel(GameLevel gameLevel){}
    public List<AdventureCard> createPileForGameLevel(GameLevel gameLevel){}
    public List<AdventureCard> selectRandomCards(CardLevel level, int count){}
    public void AdventureCard cloneCard(AdventureCard original){}
}
