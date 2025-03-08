package it.polimi.ingsw.model.domain.adventure.entity;

public class Planet {

    //which good types are on the planet and in what quantity
    private Map<GoodType, Integer> goodQuantities;
    private boolean visited;

    //constructor
    public Planet(Map<GoodType, Integer> goodQuantities) {
        this.goodQuantities = goodQuantities;
        visited = false;
    }

    //get which good types are on the planet and in what quantity
    public Map<GoodType, Integer> getGoodQuantities(){
        return null;
    }

    //in what quantity are on the planet for a given good type
    public int getQuantityByType(GoodType type){
        return 0;
    }

    // has the planet already been visited
    public boolean isVisited(){
        return false;
    }

    // how many goods are on the planet regardless of the type of the goods
    public int getTotalGoodsQuantity(){
        return 0;
    }
}
