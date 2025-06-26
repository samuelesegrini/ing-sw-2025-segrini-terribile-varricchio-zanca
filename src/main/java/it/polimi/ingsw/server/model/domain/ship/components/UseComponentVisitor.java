package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import java.util.Map;

// Galaxy Trucker component usage - batteries are consumed (trashed) when used
public class UseComponentVisitor implements ComponentVisitor {
    private int quantity;
    private Map<GoodType, Integer> goods;


    @Override
    public void useBattery(Ship ship, Battery battery, int quantity) {
        // Galaxy Trucker rules: batteries are consumed directly, no intermediate state
        int consumed = battery.consumeBatteries(quantity);
        
        if (consumed < quantity) {
            System.out.println("Warning: Only " + consumed + " batteries available, needed " + quantity);
        }
        
        // Update ship's total battery count
        ship.updateStats();
    }

    @Override
    public void useCabin(Ship ship, Cabin cabin, int quantity) {
        int result = cabin.getCurrentCrew() - quantity;

        if (result < 0) {
            System.out.println("Not enough crew to use");
            return;
        }

        cabin.setCurrentCrew(cabin.getCurrentCrew() - quantity);
        ship.setCrew(ship.getCrew() - quantity);
    }

    // Assumes all values have the same sign (positive to store, negative to remove)
    @Override
    public void useCargoHold(Ship ship, CargoHold cargoHold, Map<GoodType, Integer> goods) {
        // Checks if cargoHold is compatible with goods
        if (goods.get(GoodType.RED) != 0 && cargoHold.type == ComponentType.CARGO_HOLD) {
            System.out.println("This Cargo Hold is not special, red goods can't be used");
            return;
        }

        int result = 0;
        for (GoodType goodType : GoodType.values()) {
            result += goods.get(goodType);
        }

        if (result > cargoHold.getCapacity()) {
            System.out.println("Not enough free capacity for all the goods");
            return;
        }
        else if (result < 0) {
            System.out.println("Not enough goods to use");
            return;
        }

        // Stores goods
        if (result > cargoHold.getOccupiedCapacity()) {
            for (GoodType type : GoodType.values()) {
                cargoHold.storeGoodsOfType(type, goods.get(type));

                // Update ship's resources
                ship.getResources().put(type, ship.getResources().get(type) + goods.get(type));

                if (type == GoodType.RED) {
                    ship.setSpecialGoods(ship.getSpecialGoods() + goods.get(type));
                }
                else {
                    ship.setNormalGoods(ship.getNormalGoods() + goods.get(type));
                }
            }
        }
        // Removes goods
        else if (result < cargoHold.getOccupiedCapacity()) {
            for (GoodType type : GoodType.values()) {
                cargoHold.removeGoodsOfType(type, goods.get(type));

                // Update ship's resources
                ship.getResources().put(type, ship.getResources().get(type) - goods.get(type));

                if (type == GoodType.RED) {
                    ship.setSpecialGoods(ship.getSpecialGoods() - goods.get(type));
                }
                else {
                    ship.setNormalGoods(ship.getNormalGoods() - goods.get(type));
                }
            }
        }
    }

    @Override
    public void useCannon(Ship ship, Cannon cannon) {
        // Galaxy Trucker: No pre-charging of cannons
        // Batteries are consumed during combat strength calculation
        System.out.println("Cannon usage: Batteries consumed during combat calculation");
    }

    @Override
    public void useEngine(Ship ship, Engine engine) {
        // Galaxy Trucker: No pre-charging of engines
        // Batteries are consumed during movement strength calculation
        System.out.println("Engine usage: Batteries consumed during movement calculation");
    }

    @Override
    public void useShield(Ship ship, Shield shield) {
        // Galaxy Trucker: No pre-charging of shields
        // Batteries are consumed when shields are used to block meteors
        System.out.println("Shield usage: Batteries consumed when blocking meteors");
    }


    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Map<GoodType, Integer> getGoods() {
        return goods;
    }

    public void setGoods(Map<GoodType, Integer> goods) {
        this.goods = goods;
    }
}
