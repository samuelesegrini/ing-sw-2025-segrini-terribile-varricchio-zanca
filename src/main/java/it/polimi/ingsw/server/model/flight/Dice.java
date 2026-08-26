package it.polimi.ingsw.server.model.flight;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * The two dice, rolled together.
 *
 * <p>Every roll in the game is a pair of dice read as one number, naming a row or a column
 * of a ship board (manual p.13). The sum runs 2 to 12 while the boards print columns 4 to
 * 10 and rows 5 to 9, so a roll naming no line at all is a normal outcome rather than an
 * error.
 *
 * <p>An interface so that tests can script a flight instead of retrying it until the dice
 * cooperate. A rules test that depends on chance is a rules test nobody trusts.
 */
@FunctionalInterface
public interface Dice {

    /**
     * Rolls both dice and reads them as one number.
     *
     * @return the sum, between 2 and 12
     */
    int roll();

    /**
     * Returns dice backed by a source of randomness.
     *
     * <p>Two independent dice rather than one number between 2 and 12: the distribution is
     * the point, and it is why sevens hit the middle of a ship far more often than twos hit
     * its edge.
     *
     * @param random where the randomness comes from
     * @return fair dice
     */
    static Dice fair(RandomGenerator random) {
        return () -> random.nextInt(1, 7) + random.nextInt(1, 7);
    }

    /**
     * Returns dice that produce the given sums in order, then repeat the last one.
     *
     * <p>For tests: a scripted flight is one whose outcome can be asserted.
     *
     * @param sums the sums to produce
     * @return scripted dice
     * @throws IllegalArgumentException if no sums are given, or one is impossible
     */
    static Dice scripted(int... sums) {
        if (sums.length == 0) {
            throw new IllegalArgumentException("scripted dice need at least one roll");
        }
        List<Integer> rolls = java.util.Arrays.stream(sums).boxed().toList();
        rolls.forEach(sum -> {
            if (sum < 2 || sum > 12) {
                throw new IllegalArgumentException("two dice cannot sum to " + sum);
            }
        });
        int[] next = {0};
        return () -> rolls.get(Math.min(next[0]++, rolls.size() - 1));
    }
}
