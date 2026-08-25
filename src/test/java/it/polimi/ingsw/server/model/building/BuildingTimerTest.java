package it.polimi.ingsw.server.model.building;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the hourglass of manual p.17, including the two rules that are easy to get
 * subtly wrong.
 *
 * <p>The sand running out does not end anything by itself: any player <em>may</em> turn
 * the glass, and if nobody wants to, building carries on. A timer that advanced on its
 * own would quietly delete a real tactical choice, since the player still building has
 * every reason to leave it alone.
 *
 * <p>And the final turn belongs to a player who has finished. Without that, someone still
 * building could start the last period at a moment that suits only them.
 *
 * <p>Components involved: {@link BuildingTimer}.
 */
class BuildingTimerTest {

    /** A clock the test moves by hand, so no test ever waits on real time. */
    private static final class TestClock implements InstantSource {
        private Instant now = Instant.parse("2025-06-27T09:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }
    }

    private final TestClock clock = new TestClock();
    private final Duration period = Duration.ofSeconds(90);

    /** A level II timer: three spaces, so the sand runs three times. */
    private BuildingTimer levelTwoTimer() {
        return new BuildingTimer(3, period, clock);
    }

    private void runOutTheSand() {
        clock.advance(period);
    }

    @Test
    @DisplayName("a test flight has no hourglass, and starting one is refused rather than silently doing nothing")
    void testFlight_hasNoHourglass() {
        BuildingTimer untimed = new BuildingTimer(0, period, clock);

        assertFalse(untimed.isInPlay());
        assertFalse(untimed.isRunning());
        assertFalse(untimed.isBuildingOver());
        assertThrows(IllegalStateException.class, untimed::start);
    }

    @Test
    @DisplayName("the level II board runs the sand three times, starting on the first space")
    void levelTwoBoard_runsTheSandThreeTimes() {
        BuildingTimer timer = levelTwoTimer();

        assertEquals(3, timer.spaces());
        assertEquals(-1, timer.space());

        timer.start();

        assertEquals(0, timer.space());
        assertTrue(timer.isRunning());
        assertEquals(period, timer.remaining());
    }

    @Test
    @DisplayName("the sand runs down and then stops at zero rather than going negative")
    void remainingTime_stopsAtZero() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();

        clock.advance(Duration.ofSeconds(30));
        assertEquals(Duration.ofSeconds(60), timer.remaining());
        assertFalse(timer.hasRunOut());

        clock.advance(Duration.ofSeconds(120));
        assertEquals(Duration.ZERO, timer.remaining());
        assertTrue(timer.hasRunOut());
    }

    @Test
    @DisplayName("turning the glass while the sand is still running is refused")
    void turningEarly_isRefused() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();
        clock.advance(Duration.ofSeconds(30));

        assertThrows(IllegalStateException.class, () -> timer.flip(true));
    }

    @Test
    @DisplayName("the sand running out does not end building: someone has to turn the glass")
    void sandRunningOut_doesNotAdvanceOnItsOwn() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();
        runOutTheSand();

        clock.advance(Duration.ofHours(1));

        assertEquals(0, timer.space(), "the glass is where it was left");
        assertFalse(timer.isBuildingOver(), "building carries on until someone turns it");
    }

    @Test
    @DisplayName("any player may make the first turn, finished or not")
    void firstTurn_isOpenToAnyone() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();
        runOutTheSand();

        timer.flip(false);

        assertEquals(1, timer.space());
        assertEquals(period, timer.remaining(), "the sand starts again from full");
    }

    @Test
    @DisplayName("the final turn is refused to a player who is still building")
    void finalTurn_isRefusedToAnUnfinishedPlayer() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();
        runOutTheSand();
        timer.flip(false);
        runOutTheSand();

        assertTrue(timer.nextIsFinalSpace());
        assertThrows(IllegalStateException.class, () -> timer.flip(false));
        assertEquals(1, timer.space(), "the refused turn leaves the glass where it was");
    }

    @Test
    @DisplayName("a player who has finished may make the final turn")
    void finalTurn_isAllowedToAFinishedPlayer() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();
        runOutTheSand();
        timer.flip(false);
        runOutTheSand();

        timer.flip(true);

        assertEquals(2, timer.space());
        assertFalse(timer.isBuildingOver(), "the last period has only just started");
    }

    @Test
    @DisplayName("building ends when the sand on the final space runs out")
    void buildingEnds_whenTheLastPeriodExpires() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();
        runOutTheSand();
        timer.flip(false);
        runOutTheSand();
        timer.flip(true);

        assertFalse(timer.isBuildingOver());

        runOutTheSand();

        assertTrue(timer.isBuildingOver());
        assertThrows(IllegalStateException.class, () -> timer.flip(true), "there is nowhere left to turn it");
    }

    @Test
    @DisplayName("nothing can be asked of a timer that has not been started")
    void unstartedTimer_answersNothing() {
        BuildingTimer timer = levelTwoTimer();

        assertFalse(timer.hasRunOut());
        assertFalse(timer.nextIsFinalSpace());
        assertFalse(timer.isBuildingOver());
        assertThrows(IllegalStateException.class, timer::remaining);
        assertThrows(IllegalStateException.class, () -> timer.flip(true));
    }

    @Test
    @DisplayName("starting the sand twice is refused, so a restart cannot buy anyone extra time")
    void startingTwice_isRefused() {
        BuildingTimer timer = levelTwoTimer();
        timer.start();

        assertThrows(IllegalStateException.class, timer::start);
    }

    @Test
    @DisplayName("a board with a single space has one period, and its only turn is the final one")
    void singleSpaceBoard_hasOnePeriod() {
        BuildingTimer timer = new BuildingTimer(1, period, clock);
        timer.start();
        runOutTheSand();

        assertTrue(timer.isBuildingOver());
        assertThrows(IllegalStateException.class, () -> timer.flip(true));
    }
}
