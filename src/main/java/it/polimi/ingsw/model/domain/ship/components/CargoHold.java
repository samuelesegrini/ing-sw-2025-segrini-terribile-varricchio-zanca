package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Map;

public class CargoHold extends Component {
    private int capacity;
    private int occupiedCapacity;
    private Map<GoodType, Integer> storedGoods;

    @Override
    public void accept(ComponentVisitor v) {
        Map<GoodType, Integer> goods = null;
        v.useCargoHold(super.ship, this, goods);
    }

    @Override
    public void count(Ship s) {
        Map<GoodType, Integer> goods = s.getResources();

        for (GoodType type : GoodType.values()) {
            goods.put(type, goods.get(type) + storedGoods.get(type));
        }
        s.setResources(goods);

        // Updates specialGoods and normalGoods in Ship
        s.setSpecialGoods(s.getSpecialGoods() + storedGoods.get(GoodType.RED));
        s.setNormalGoods(s.getNormalGoods() + storedGoods.get(GoodType.BLUE));
        s.setNormalGoods(s.getNormalGoods() + storedGoods.get(GoodType.GREEN));
        s.setNormalGoods(s.getNormalGoods() + storedGoods.get(GoodType.YELLOW));
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

    public void setStoredGoods(Map<GoodType, Integer> storedGoods) {
        this.storedGoods = storedGoods;
    }


    public void storeGoodsOfType(GoodType type, int quantity) {
        if (occupiedCapacity + quantity <= capacity) {
            storedGoods.put(type, storedGoods.get(type) + quantity);
            occupiedCapacity += quantity;
        }
        else {
            System.out.println("Not enough free capacity");
        }
    }

    public void removeGoodsOfType(GoodType type, int quantity) {
        if (storedGoods.get(type) - quantity >= 0) {
            storedGoods.put(type, storedGoods.get(type) - quantity);
            occupiedCapacity -= quantity;
        }
        else {
            System.out.println("Not enough goods of this type");
        }
    }
}
