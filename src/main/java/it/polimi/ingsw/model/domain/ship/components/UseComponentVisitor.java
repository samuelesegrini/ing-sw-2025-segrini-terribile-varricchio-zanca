package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.resource.GoodType;
import it.polimi.ingsw.model.enums.ship.ComponentType;

import java.util.HashMap;
import java.util.Map;

public class UseComponentVisitor implements ComponentVisitor {
    // TODO: Restituisci e sistema le batterie rimanenti se ci sono & Chiama count()?
    @Override
    public void useBattery(Battery battery, int quantity) {
        int result = battery.getCurrentBatteries() + quantity;

        if (result < 0) {
            System.out.println("Not enough batteries to use");

            while (battery.getCurrentBatteries() > 0) {
                battery.setCurrentBatteries(battery.getCurrentBatteries() - 1);
            }
        }
        // Serve poter aggiungere batterie?
        else if (result > battery.getMaxBatteries()) {
            System.out.println("Not enough space for all the batteries");

            while (battery.getCurrentBatteries() < battery.getMaxBatteries()) {
                battery.setCurrentBatteries(battery.getCurrentBatteries() + 1);
            }
        } else {
            battery.setCurrentBatteries(result);
        }
    }

    // TODO: Restituisci e sistema l'equipaggio rimanente se c'è & Chiama count()?
    @Override
    public void useCabin(Cabin cabin, int quantity) {
        int result = cabin.getCurrentCrew() + quantity;

        if (result < 0) {
            System.out.println("Not enough crew to use");

            while (cabin.getCurrentCrew() > 0) {
                cabin.setCurrentCrew(cabin.getCurrentCrew() - 1);
            }
        }
        // Serve poter aggiungere batterie?
        else if (result > cabin.getMaxCrew()) {
            System.out.println("Not enough space for all the batteries");

            while (cabin.getCurrentCrew() < cabin.getMaxCrew()) {
                cabin.setCurrentCrew(cabin.getCurrentCrew() + 1);
            }
        } else {
            cabin.setCurrentCrew(result);
        }
    }

    // Assumes that all values of goods have the same sign (positive to store, negative to remove)
    // TODO: Restituisci e sistema le merci rimanenti se c'è & Chiama count()?
    @Override
    public void useCargoHold(CargoHold cargoHold, Map<GoodType, Integer> goods) {
        // Checks if cargoHold is compatible with goods
        if (goods.get(GoodType.RED) != 0 && ((Component) cargoHold).type == ComponentType.CARGO_HOLD) {
            System.out.println("This Cargo Hold is not special, red goods can't be used");
        } else {
            int result = 0;
            for (GoodType goodType : GoodType.values()) {
                result += goods.get(goodType);
            }

            // Stores goods
            if (result > cargoHold.getOccupiedCapacity()) {
                if (result > cargoHold.getCapacity()) {
                    System.out.println("Not enough free capacity for all the goods");
                }
                for (GoodType type : GoodType.values()) {
                    while (cargoHold.getOccupiedCapacity() < cargoHold.getCapacity() && goods.get(type) > 0) {
                        cargoHold.storeGood(type);
                        goods.put(type, goods.get(type) - 1);
                    }
                }
            }
            // Removes goods
            else if (result < cargoHold.getOccupiedCapacity()) {
                if (result < 0) {
                    System.out.println("Not enough goods to use");
                }
                for (GoodType type : GoodType.values()) {
                    while (cargoHold.getOccupiedCapacity() > 0 && goods.get(type) < 0) {
                        cargoHold.removeGood(type);
                        goods.put(type, goods.get(type) + 1);
                    }
                }
            }
        }
    }

    @Override
    public void useCannon(Cannon cannon) {
        // TODO
    }

    @Override
    public void useEngine(Engine engine) {
        // TODO
    }

    @Override
    public void useShield(Shield shield) {
        // TODO
    }
}
