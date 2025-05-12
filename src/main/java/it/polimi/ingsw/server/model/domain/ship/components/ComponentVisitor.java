package it.polimi.ingsw.server.model.domain.ship.components;

import it.polimi.ingsw.server.model.domain.ship.Ship;
import it.polimi.ingsw.server.model.enums.resource.GoodType;
import java.util.Map;

public interface ComponentVisitor {
    void useBattery(Ship ship, Battery battery, int quantity);
    void useCabin(Ship ship, Cabin cabin, int quantity);
    void useCargoHold(Ship ship, CargoHold cargoHold, Map<GoodType, Integer> goods);
    void useCannon(Ship ship, Cannon cannon);
    void useEngine(Ship ship, Engine engine);
    void useShield(Ship ship, Shield shield);
}
