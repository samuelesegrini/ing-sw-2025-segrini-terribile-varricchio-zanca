package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.resource.GoodType;

import java.util.Map;

public class UseComponentVisitor implements ComponentVisitor {
    @Override
    public void useBattery(Battery battery, int quantity) {
        // TODO
    }

    @Override
    public void useCabin(Cabin cabin, int quantity) {
        // TODO
    }

    @Override
    public void useCargoHold(CargoHold cargoHold, Map<GoodType, Integer> goods) {
        // TODO
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
