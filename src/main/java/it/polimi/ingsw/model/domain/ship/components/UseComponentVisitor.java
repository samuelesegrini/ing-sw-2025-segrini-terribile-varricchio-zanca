package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Map;

public class UseComponentVisitor implements ComponentVisitor {
    @Override
    public void useBattery(Battery battery, int quantity) {

    }

    @Override
    public void useCabin(Cabin cabin, int quantity) {

    }

    @Override
    public void useCargoHold(CargoHold cargoHold, Map<GoodType, Integer> goods) {

    }

    @Override
    public void useEngine(Engine engine) {

    }

    @Override
    public void useShield(Shield shield) {

    }
}
