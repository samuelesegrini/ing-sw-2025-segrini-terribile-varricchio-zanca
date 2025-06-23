package it.polimi.ingsw.server.model.enums.adventure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PenaltyTypeTest {

    @Test

            void test() {

        PenaltyType penaltyType;

        penaltyType = PenaltyType.FLIGHT_DAYS_LOSS;

                assertEquals(penaltyType, PenaltyType.FLIGHT_DAYS_LOSS);

    }


}


