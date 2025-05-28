package it.polimi.ingsw.server.model.domain.adventure.card;

import it.polimi.ingsw.server.model.domain.adventure.entity.CannonFire;
import it.polimi.ingsw.server.model.domain.adventure.entity.CombatCheck;
import it.polimi.ingsw.server.model.enums.adventure.CardLevel;
import it.polimi.ingsw.server.model.enums.adventure.CombatAttributeType;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombatZoneCardTest {

    @Test

    void test() {

        CombatZoneCard card;
        card = new CombatZoneCard("10", CardLevel.LEVEL_II, "description");

        CannonFire cannonFire1;
        CannonFire cannonFire2;
        CannonFire cannonFire3;

        cannonFire1 = new CannonFire(Direction.RIGHT, ShotIntensity.LIGHT);
        cannonFire2 = new CannonFire(Direction.LEFT, ShotIntensity.HEAVY);
        cannonFire3 = new CannonFire(Direction.UP, ShotIntensity.LIGHT);

        List<CannonFire> cannonFires;
        cannonFires = new ArrayList<>();
        cannonFires.add(cannonFire1);
        cannonFires.add(cannonFire2);
        cannonFires.add(cannonFire3);

        CombatCheck check;
        check = new CombatCheck(CombatAttributeType.CANNON_STRENGTH, cannonFires);

        card.addCombatCheck(check);

        assertEquals(check, card.getCombatChecks().get(0));

        assertEquals(cannonFire1, card.getCombatChecks().get(0).getCannonFires().get(0));

    }


}













