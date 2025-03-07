package it.polimi.ingsw.model.adventure;

import java.util.List;

public class AdventureDeck {
    //game level
    private GameLevel gameLevel;
    //covered deck
    private List<List<AdventureCard>> uncoveredPiles;
    //uncovered deck
    private List<AdventureCard> coveredPile;
    //number of seen cards
    private int currentIndex;
    //for every player which card deck they are looking at, because a single deck can not be viewed by more than one player at the same time
    private Map<PlayerId, PileIdentifier> playerViewing;
    private boolean isFlightPhase;
    //constructor
    public AdventureDeck(GameLevel, List<List<AdventureCard>>, List<AdventureCard>){}
    //can the player view the pile or is another player already viewing it
    public boolean canPlayerViewPile(PlayerId, PileIdentifier){}
    //view every card in an uncovered pile
    public <List<AdventureCard>> viewPile(PlayerId, PileIdentifier){}
    //stop viewing pile and put it back on the table ready to be used by other players
    public AdventureDeck stopViewingPile(PlayerId){}
    //get current card while playing to check again the card that is being played
    public Optional<AdventureCard> getCurrentCard(){}
    //draw next card if there is at least one card left
    public Optional<AdventureCard> drawNextCard(){}
    //start flight phase and create a single deck out of the previous decks
    public AdventureDeck startFlightPhase(){}
    //there are no cards left
    public boolean isExhausted(){}
    //bohhh
    public PileIdentifier[]{} getValidPileIdentifiers(){}
    //get level of the game
    public GameLevel getGameLevel(){}
}
