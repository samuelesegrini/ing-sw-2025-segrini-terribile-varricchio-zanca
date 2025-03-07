package it.polimi.ingsw.model.adventure;

public class AdventureCard {
    private String id;
    private CardLevel level;
    private String description;
    private AdventureType type;
    public AdventureCard(String id, CardLevel level, String description, AdventureType type){
        this.id = id;
        this.level = level;
        this.description = description;
        this.type = type;
    }
    public String getId(){}
    public CardLevel getLevel(){}
    public String getDescription(){}
    public AdventureType getType(){}
}
