package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Map;

public class CargoHold extends Component {
    private int capacity;
    private Map<GoodType, Integer> storedGoods;

    @Override
    public void accept(ComponentVisitor v) {
        Map<GoodType, Integer> goods = null;
        v.useCargoHold(this, goods);
    }

    @Override
    public void count(Ship s) {
        // TODO
    }


    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public Map<GoodType, Integer> getStoredGoods() {
        return storedGoods;
    }

    public void setStoredGoods(Map<GoodType, Integer> storedGoods) {
        this.storedGoods = storedGoods;
    }
}

























/*
public class CargoHold extends Component {
    private int capacity;
    private Map<GoodType, Integer> storedGoods;

    /**
     * Loads a resource into the cargo and decrements the total cargo capacity.
     * @param goodType indicates the type of resource to load into the cargo
     * @return {@code true} if the remaining capacity is at least one, {@code false} otherwise

public boolean loadGood(GoodType goodType){ return false; }

        /**
         * Unloads a resource from the cargo and increments the total cargo capacity.
         * @param goodType indicates the type of resource to remove from the cargo
         * @return {@code true} if at least one unit of type goodType was stored in the cargo, {@code false} otherwise

        public boolean unloadGood(GoodType goodType){ return false; }

        /**
         * Checks if the cargo is empty, meaning there are no stored resources.
         * @return {@code true} if the cargo is empty, {@code false} otherwise

        public boolean isEmpty(){ return false; }
}
 */
