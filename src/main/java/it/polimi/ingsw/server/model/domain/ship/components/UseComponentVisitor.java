package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;

import java.util.Map;

// Removes batteries to charge cannons/engines/shields
public class UseComponentVisitor implements ComponentVisitor {
    private int quantity;
    private Map<GoodType, Integer> goods;


    @Override
    public void useBattery(Ship ship, Battery battery, int quantity) {
        int result = battery.getCurrentBatteries() - quantity;

        if (result < 0) {
            System.out.println("Not enough batteries to use");
            return;
        }

        battery.setCurrentBatteries(battery.getCurrentBatteries() - quantity);
        ship.setBatteries(ship.getBatteries() - quantity);
        ship.setChargingBatteries(ship.getChargingBatteries() + quantity);
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
        if (cannon.getType() == ComponentType.CANNON_SINGLE) {
            System.out.println("This cannon is single, it cannot be charged");
            return;
        }

        if (ship.getChargingBatteries() > 0) {
            ship.setChargingBatteries(ship.getChargingBatteries() - 1);
            cannon.setCharged(true);
            cannon.count(ship);
            // Spengo qui il cannone?
        }
        else {
            System.out.println("There is no charging battery to use");
        }
    }

    @Override
    public void useEngine(Ship ship, Engine engine) {
        if (engine.getType() == ComponentType.ENGINE_SINGLE) {
            System.out.println("This engine is single, it cannot be charged");
            return;
        }

        if (ship.getChargingBatteries() > 0) {
            ship.setChargingBatteries(ship.getChargingBatteries() - 1);
            engine.setCharged(true);
            engine.count(ship);
            // Spengo qui il motore?
        }
        else {
            System.out.println("There is no charging battery to use");
        }
    }

    @Override
    public void useShield(Ship ship, Shield shield) {
        if (ship.getChargingBatteries() > 0) {
            ship.setChargingBatteries(ship.getChargingBatteries() - 1);
            shield.setCharged(true);
            // Spengo qui lo scudo?
        }
        else {
            System.out.println("There is no charging battery to use");
        }
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
