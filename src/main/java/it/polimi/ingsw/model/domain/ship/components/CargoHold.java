package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.domain.ship.Ship;
import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.enums.ship.ComponentType;
import it.polimi.ingsw.model.enums.ship.ConnectorType;
import it.polimi.ingsw.model.enums.ship.Direction;

import java.util.HashMap;
import java.util.Map;

public class CargoHold extends Component {
    private int capacity;
    private int occupiedCapacity;
    private Map<GoodType, Integer> storedGoods;


    public CargoHold(ComponentType type, Map<Direction, ConnectorType> connectors, int capacity) {
        super(type, connectors);
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

    public void storeGoodsOfType(GoodType type, int quantity) {
        if (occupiedCapacity + quantity <= capacity) {
            Integer currentAmount = storedGoods.getOrDefault(type, 0);
            //System.out.println(currentAmount);
            storedGoods.put(type, currentAmount + quantity);
            occupiedCapacity += quantity;
        }
        else {
            System.out.println("Not enough free capacity");
        }
    }

    public void removeGoodsOfType(GoodType type, int quantity) {
        if (storedGoods.get(type) - quantity >= 0) {
            Integer currentAmount = storedGoods.getOrDefault(type, 0);
            storedGoods.put(type, currentAmount - quantity);
            occupiedCapacity -= quantity;
        }
        else {
            System.out.println("Not enough goods of this type");
        }
    }
}
