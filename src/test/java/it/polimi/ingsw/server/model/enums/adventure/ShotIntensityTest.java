package it.polimi.ingsw.server.model.enums.adventure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShotIntensityTest {

    @Test

    void test() {

        ShotIntensity shotIntensity;
        shotIntensity = ShotIntensity.LIGHT;

        assertEquals(ShotIntensity.LIGHT, shotIntensity);

    }


}


