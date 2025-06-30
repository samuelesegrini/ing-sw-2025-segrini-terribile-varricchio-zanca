package it.polimi.ingsw.server.model.domain.adventure.entity;

import it.polimi.ingsw.server.model.domain.ship.components.Cannon;
import it.polimi.ingsw.server.model.enums.adventure.ShotIntensity;
import it.polimi.ingsw.server.model.enums.ship.ComponentType;
import it.polimi.ingsw.server.model.enums.ship.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CannonFireTest {

    @Test
    void setUpBlockable(){
        CannonFire cannonF;
        cannonF = new CannonFire(Direction.DOWN, ShotIntensity.LIGHT );

        assertEquals(Direction.DOWN, cannonF.getApproach());
        assertEquals(ShotIntensity.LIGHT, cannonF.getIntensity());
        assertTrue(cannonF.isBlockable());
    }

    @Test
    void isNotBlockable(){
        CannonFire cannonF;
        cannonF = new CannonFire(Direction.DOWN, ShotIntensity.HEAVY );

        assertEquals(Direction.DOWN, cannonF.getApproach());
        assertEquals(ShotIntensity.HEAVY, cannonF.getIntensity());
        assertFalse(cannonF.isBlockable());
    }
}