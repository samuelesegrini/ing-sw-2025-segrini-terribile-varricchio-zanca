package it.polimi.ingsw.common.transport;

import java.time.Duration;

/**
 * How hard a channel tries to notice that the other end has gone.
 *
 * <p>A dropped TCP connection does not announce itself. Writes succeed into a kernel buffer,
 * reads block, and both ends sit there believing in each other for as long as the operating
 * system's own timeout allows — which is measured in tens of minutes. An RMI call fails
 * faster but only when there is a call to make, and a player who is thinking makes none.
 *
 * <p>So silence has to be made detectable on purpose: send nothing at a fixed rate, and give
 * up when nothing has come back for a while.
 *
 * @param keepAliveEvery how often to send a {@link Envelope.KeepAlive}
 * @param silenceAllowed how long to hear nothing before deciding the other end has gone
 */
public record Liveness(Duration keepAliveEvery, Duration silenceAllowed) {

    /**
     * The settings a real game uses: a beat every two seconds, and six seconds of patience.
     *
     * <p>Six seconds is three missed beats. One missed beat is a scheduling hiccup, two is
     * a slow network, three is somebody's laptop lid.
     */
    public static final Liveness DEFAULT =
            new Liveness(Duration.ofSeconds(2), Duration.ofSeconds(6));

    /**
     * Validates the settings.
     *
     * @throws NullPointerException     if either duration is {@code null}
     * @throws IllegalArgumentException if a duration is not positive, or the patience is
     *                                  shorter than the beat
     */
    public Liveness {
        if (keepAliveEvery == null || silenceAllowed == null) {
            throw new NullPointerException("liveness needs both durations");
        }
        if (keepAliveEvery.isNegative() || keepAliveEvery.isZero()) {
            throw new IllegalArgumentException("a heartbeat needs a positive period");
        }
        if (silenceAllowed.compareTo(keepAliveEvery) <= 0) {
            throw new IllegalArgumentException(
                    "allowing " + silenceAllowed + " of silence between beats of "
                            + keepAliveEvery + " would close every working connection");
        }
    }

    /**
     * Returns how many beats of silence this allows.
     *
     * @return the number of missed beats before a connection is given up on
     */
    public long missedBeatsAllowed() {
        return silenceAllowed.toMillis() / keepAliveEvery.toMillis();
    }
}
