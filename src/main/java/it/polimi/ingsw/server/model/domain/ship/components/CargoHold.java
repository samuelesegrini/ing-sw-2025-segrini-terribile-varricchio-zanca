package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.ConnectorType;
import it.polimi.ingsw.server.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.Map;

public class CargoHold extends Component {
    private static final long serialVersionUID = 1L;
    
    private int capacity;
    private int occupiedCapacity;
    private Map<GoodType, Integer> storedGoods;


    public CargoHold(ComponentType type, Map<Direction, ConnectorType> connectors, int capacity, String id) {
        super(type, connectors, id);
        this.capacity = capacity;
        this.occupiedCapacity = 0;

        this.storedGoods = new HashMap<>();
        for (GoodType goodType : GoodType.values()) {
            storedGoods.put(goodType, 0);
        }
    }


    @Override
    public void use(UseComponentVisitor v) {
        v.useCargoHold(this.getShip(), this, v.getGoods());
    }

    @Override
    public void count(Ship ship) {
        Map<GoodType, Integer> goods = ship.getResources();

        for (GoodType type : GoodType.values()) {
            goods.put(type, goods.get(type) + storedGoods.get(type));
        }
        ship.setResources(goods);

        // Updates specialGoods and normalGoods in Ship
        ship.setSpecialGoods(ship.getSpecialGoods() + storedGoods.get(GoodType.RED));
        ship.setNormalGoods(ship.getNormalGoods() + storedGoods.get(GoodType.BLUE));
        ship.setNormalGoods(ship.getNormalGoods() + storedGoods.get(GoodType.GREEN));
        ship.setNormalGoods(ship.getNormalGoods() + storedGoods.get(GoodType.YELLOW));

    }


    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getOccupiedCapacity() {
        return occupiedCapacity;
    }

    public void setOccupiedCapacity(int occupiedCapacity) {
        this.occupiedCapacity = occupiedCapacity;
    }

    public Map<GoodType, Integer> getStoredGoods() {
        return storedGoods;
    }

    public void setStoredGoods(Map<GoodType, Integer> goods) {
        storedGoods = goods;

        occupiedCapacity = 0;
        for (GoodType type : GoodType.values()) {
            occupiedCapacity += goods.get(type);
        }
    }

    public boolean storeGoodsOfType(GoodType type, int quantity) {
        if (quantity <= 0) {
            return true; // No goods to store
        }
        
        if (occupiedCapacity + quantity <= capacity) {
            Integer currentAmount = storedGoods.getOrDefault(type, 0);
            storedGoods.put(type, currentAmount + quantity);
            occupiedCapacity += quantity;
            return true;
        }
        return false;
    }

    public boolean removeGoodsOfType(GoodType type, int quantity) {
        if (quantity <= 0) {
            return true; // No goods to remove
        }
        
        Integer currentAmount = storedGoods.getOrDefault(type, 0);
        if (currentAmount >= quantity) {
            storedGoods.put(type, currentAmount - quantity);
            occupiedCapacity -= quantity;
            return true;
        }
        return false;
    }
    
    /**
     * Attempts to remove as many goods as possible of the specified type.
     * @param type The type of goods to remove
     * @param quantity The maximum quantity to remove
     * @return The actual quantity removed
     */
    public int removeAvailableGoods(GoodType type, int quantity) {
        if (quantity <= 0) {
            return 0;
        }
        
        Integer currentAmount = storedGoods.getOrDefault(type, 0);
        int amountToRemove = Math.min(currentAmount, quantity);
        
        if (amountToRemove > 0) {
            storedGoods.put(type, currentAmount - amountToRemove);
            occupiedCapacity -= amountToRemove;
        }
        
        return amountToRemove;
    }
    
    /**
     * Checks if this cargo hold can store RED goods (special cargo only).
     * @return true if this is a special cargo hold that can store RED goods
     */
    public boolean canStoreRedGoods() {
        return this.getType() == ComponentType.CARGO_HOLD_SPECIAL;
    }
    
    /**
     * Gets the available free capacity in this cargo hold.
     * @return The number of goods that can still be stored
     */
    public int getFreeCapacity() {
        return capacity - occupiedCapacity;
    }
}
