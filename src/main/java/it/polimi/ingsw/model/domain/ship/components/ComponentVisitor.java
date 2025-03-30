package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.resource.GoodType;
import java.util.Map;

public interface ComponentVisitor {
    void useBattery(Battery battery, int quantity);
    void useCabin(Cabin cabin, int quantity);
    void useCargoHold(CargoHold cargoHold, Map<GoodType, Integer> goods);
    void useCannon(Cannon cannon);
    void useEngine(Engine engine);
    void useShield(Shield shield);
}