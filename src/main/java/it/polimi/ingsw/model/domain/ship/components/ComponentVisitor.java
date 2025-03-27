package it.polimi.ingsw.model.domain.ship.components;

import it.polimi.ingsw.model.enums.resource.GoodType;
import java.util.Map;

public interface ComponentVisitor {
    public void useBattery(Battery battery, int quantity);
    public void useCabin(Cabin cabin, int quantity);
    public void useCargoHold(CargoHold cargoHold, Map<GoodType, Integer> goods);
    public void useCannon(Cannon cannon);
    public void useEngine(Engine engine);
    public void useShield(Shield shield);
}