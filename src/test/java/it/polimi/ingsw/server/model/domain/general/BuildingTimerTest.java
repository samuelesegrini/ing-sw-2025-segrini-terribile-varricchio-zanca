package it.polimi.ingsw.server.model.domain.general;

import it.polimi.ingsw.server.model.enums.GameLevel;
import org.junit.jupiter.api.Test;

public class BuildingTimerTest {

    @Test
    void test() {
        BuildingTimer timer = new BuildingTimer(GameLevel.LEVEL_II);
        timer.setEventListener((event, playerId, remainingTime) -> {
            // Stub: no-op listener
        });

        timer.startBuildingPhase(); // passa a IDLE, flippa automatico

        timer.flipTimer("A", false); // IDLE → FIRST_STAGE
        timer.flipTimer("B", false); // FIRST_STAGE → SECOND_STAGE
        timer.flipTimer("C", false); // SECOND_STAGE, no completed ship → niente
        timer.flipTimer("D", true);  // SECOND_STAGE → FINISHED (con nave completata)

        timer.getCurrentState();
        timer.getCurrentFlipperPlayerId();
        timer.getTimeRemaining();
        timer.isTimerActive();

        timer.forceEndBuildingPhase(); // forza fine (anche se già finito)
        timer.shutdown();
    }
}
