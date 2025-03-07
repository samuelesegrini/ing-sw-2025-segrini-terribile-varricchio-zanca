package it.polimi.ingsw.model.ship;

public class CargoHold extends Component {
    private int capacity;
    private List<GoodType> storeGoods;

    public boolean loadGood(GoodType){};
    public GoodType unloadGood(){};
    public boolean isEmpty(){};
}
