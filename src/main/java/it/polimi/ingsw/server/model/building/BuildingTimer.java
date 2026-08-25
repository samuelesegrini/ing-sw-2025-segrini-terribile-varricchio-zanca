package it.polimi.ingsw.server.model.building;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

/**
 * The hourglass that limits how long a ship may be built.
 *
 * <p>The level II board prints three spaces for it, so the sand runs three times
 * (manual p.17). Two of the rules around it are worth stating, because both are easy to
 * get subtly wrong:
 *
 * <ul>
 *   <li>Running out does not end anything by itself. Any player <em>may</em> turn the
 *       glass onto the next space, and if nobody wants to, everyone keeps building until
 *       somebody does or until they have all finished. A timer that advanced on its own
 *       would take away a real tactical choice — the player still building has every
 *       reason not to turn it.</li>
 *   <li>The last flip may only be made by a player who has <em>finished</em>. That is
 *       what stops someone still building from stalling the last period for themselves.</li>
 * </ul>
 *
 * <p>A test flight has no hourglass at all (manual p.8), which is modelled here as zero
 * spaces rather than as a missing object, so callers need no null check.
 */
public final class BuildingTimer {

    /**
     * How long the sand runs.
     *
     * <p>The manual gives no number: it ships a physical hourglass. Ninety seconds is
     * this project's choice, long enough to place several tiles under pressure and short
     * enough that three of them do not outlast anyone's patience.
     */
    public static final Duration DEFAULT_PERIOD = Duration.ofSeconds(90);

    private final int spaces;
    private final Duration period;
    private final InstantSource clock;

    private int space = -1;
    private Instant turnedAt;

    /**
     * Creates a timer for a board.
     *
     * @param spaces how many hourglass spaces the board prints; zero for an untimed flight
     * @param period how long the sand runs each time
     * @param clock  where the current time comes from
     * @throws IllegalArgumentException if the number of spaces is negative or the period is not positive
     */
    public BuildingTimer(int spaces, Duration period, InstantSource clock) {
        if (spaces < 0) {
            throw new IllegalArgumentException("a board cannot print " + spaces + " hourglass spaces");
        }
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("the sand has to run for a positive time, got " + period);
        }
        this.spaces = spaces;
        this.period = period;
        this.clock = clock;
    }

    /**
     * Creates a timer for a board, running for the default period.
     *
     * @param spaces how many hourglass spaces the board prints
     * @param clock  where the current time comes from
     */
    public BuildingTimer(int spaces, InstantSource clock) {
        this(spaces, DEFAULT_PERIOD, clock);
    }

    /**
     * Tells whether this flight is timed at all.
     *
     * @return {@code false} for a test flight, which has no hourglass
     */
    public boolean isInPlay() {
        return spaces > 0;
    }

    /**
     * Tells whether the sand has been started.
     *
     * @return {@code true} once building is under way
     */
    public boolean isRunning() {
        return space >= 0;
    }

    /**
     * Returns which space the glass is standing on, counting from zero.
     *
     * @return the space, or {@code -1} before building starts
     */
    public int space() {
        return space;
    }

    /**
     * Returns how many spaces the board prints.
     *
     * @return the number of times the sand can run
     */
    public int spaces() {
        return spaces;
    }

    /**
     * Starts the sand on the first space.
     *
     * @throws IllegalStateException if the flight is untimed or the sand is already running
     */
    public void start() {
        if (!isInPlay()) {
            throw new IllegalStateException("this flight has no hourglass");
        }
        if (isRunning()) {
            throw new IllegalStateException("the sand is already running");
        }
        space = 0;
        turnedAt = clock.instant();
    }

    /**
     * Tells whether the current period has run out.
     *
     * @return {@code true} when the sand has emptied and the glass may be turned
     */
    public boolean hasRunOut() {
        return isRunning() && !remaining().isPositive();
    }

    /**
     * Returns how much of the current period is left.
     *
     * @return the time remaining, never negative
     * @throws IllegalStateException if the sand has not been started
     */
    public Duration remaining() {
        requireRunning();
        Duration left = period.minus(Duration.between(turnedAt, clock.instant()));
        return left.isNegative() ? Duration.ZERO : left;
    }

    /**
     * Tells whether the next turn of the glass would be the last one.
     *
     * <p>Only a player who has finished building may make that one, so a client needs to
     * know which flip it is offering before it offers it.
     *
     * @return {@code true} when the glass would move onto the final space
     */
    public boolean nextIsFinalSpace() {
        return isRunning() && space == spaces - 2;
    }

    /**
     * Tells whether building has ended.
     *
     * <p>True once the sand on the final space runs out. Everyone still building has to
     * stop at that moment and take a start space (manual p.17).
     *
     * @return {@code true} when nobody may build any longer
     */
    public boolean isBuildingOver() {
        return isRunning() && space == spaces - 1 && hasRunOut();
    }

    /**
     * Turns the glass onto the next space.
     *
     * @param flipperHasFinishedBuilding whether the player doing it has finished their ship
     * @throws IllegalStateException if the sand is still running, the glass is already on
     *                               the last space, or the final turn is attempted by a
     *                               player who is still building
     */
    public void flip(boolean flipperHasFinishedBuilding) {
        requireRunning();
        if (!hasRunOut()) {
            throw new IllegalStateException("the sand is still running: " + remaining() + " left");
        }
        if (space >= spaces - 1) {
            throw new IllegalStateException("the glass is on the last space and building is over");
        }
        if (nextIsFinalSpace() && !flipperHasFinishedBuilding) {
            throw new IllegalStateException(
                    "only a player who has finished building may turn the glass onto the last space");
        }
        space++;
        turnedAt = clock.instant();
    }

    private void requireRunning() {
        if (!isRunning()) {
            throw new IllegalStateException("the sand has not been started");
        }
    }
}
