package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.List;

public class CargoHold extends Component {
    private int capacity;
    private List<GoodType> storeGoods;

    /**
     * Loads a resource into the cargo and decrements the total cargo capacity.
     * @param goodType indicates the type of resource to load into the cargo
     * @return true if the remaining capacity is at least one, false otherwise
     */
    public boolean loadGood(GoodType goodType){ return false; }

    /**
     * Unloads a resource from the cargo and increments the total cargo capacity.
     * @param goodType indicates the type of resource to remove from the cargo
     * @return true if at least one unit of type goodType was stored in the cargo, false otherwise
     */
    public boolean unloadGood(GoodType goodType){ return false; }

    /**
     * Checks if the cargo is empty, meaning there are no stored resources.
     * @return true if the cargo is empty, false otherwise
     */
    public boolean isEmpty(){ return false; }
}
