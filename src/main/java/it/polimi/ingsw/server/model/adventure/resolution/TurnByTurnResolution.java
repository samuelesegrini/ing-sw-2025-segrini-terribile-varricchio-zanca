package it.polimi.ingsw.server.model.adventure.resolution;


import it.polimi.ingsw.common.game.PlayerChoice;
import it.polimi.ingsw.common.game.PlayerColor;
import it.polimi.ingsw.common.game.PlayerPrompt;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * The skeleton most adventure cards share: work down a queue of players, asking each one
 * in turn, and stop when the card says to.
 *
 * <p>Three of the manual's shapes fall out of the same loop, and the only thing that
 * separates them is what {@link #apply} returns:
 *
 * <ul>
 *   <li><b>Everyone acts</b> — Planets, Open Space, Stardust. Never stop early.</li>
 *   <li><b>First taker only</b> — an abandoned ship or station. Stop as soon as somebody
 *       accepts; the rest are cut out (manual p.12).</li>
 *   <li><b>Until defeated</b> — Smugglers, Pirates, Slavers. Stop as soon as somebody
 *       beats them; nobody behind is attacked (manual p.12).</li>
 * </ul>
 *
 * <p>The queue is fixed when the card starts. Route order can change while a card is
 * being resolved — somebody advances in Open Space, somebody falls back on Planets — but
 * whose turn it is was settled when the card was turned over. Combat Zone is the
 * exception, and it re-reads the order between its lines by building a fresh queue for
 * each one (manual p.13).
 */
public abstract class TurnByTurnResolution implements AdventureResolution {

    private final Deque<PlayerColor> queue = new ArrayDeque<>();

    private boolean started;
    private boolean finished;
    private PlayerPrompt current;
    private PlayerPrompt followUp;

    /**
     * Creates a resolution that will work down the given order.
     *
     * @param order the players to ask, in the order to ask them
     */
    protected TurnByTurnResolution(List<PlayerColor> order) {
        queue.addAll(order);
    }

    /**
     * Returns what to ask a player, or empty to pass over them.
     *
     * <p>Passing over is a normal outcome, not a refusal: a player without the crew an
     * abandoned station demands is simply not offered it.
     *
     * @param player whose turn it is
     * @return the question for them, or empty to move on
     */
    protected abstract Optional<PlayerPrompt> promptFor(PlayerColor player);

    /**
     * Applies a player's answer.
     *
     * @param choice what they answered
     * @return {@code true} to end the card here, {@code false} to carry on down the queue
     */
    protected abstract boolean apply(PlayerChoice choice);

    /**
     * Runs whatever the card does once nobody else will be asked.
     *
     * <p>Where the effects that happen to everybody at once belong — Planets moves every
     * player who landed, in reverse route order, only after all of them have chosen
     * (manual p.12). Called exactly once.
     */
    protected void afterEveryone() {
        // Most cards have nothing left to do.
    }

    @Override
    public final Optional<PlayerPrompt> pending() {
        ensureStarted();
        return Optional.ofNullable(current);
    }

    @Override
    public final void submit(PlayerChoice choice) {
        ensureStarted();
        PlayerPrompt prompt = current;
        if (prompt == null) {
            throw new IllegalStateException("this card is finished and is waiting for nothing");
        }
        if (choice.player() != prompt.player()) {
            throw new IllegalArgumentException(
                    "the card is waiting on the " + prompt.player() + " player, not the " + choice.player());
        }
        // Asked of the prompt, once, rather than of the card. Every card used to check this
        // for itself in the default arm of its own switch, which wrote the pairing of question
        // to answer down eight times over with nothing holding the eight in agreement.
        if (!prompt.accepts(choice)) {
            throw new IllegalArgumentException("a " + prompt.getClass().getSimpleName()
                    + " cannot be answered with a " + choice.getClass().getSimpleName());
        }
        // The answer is applied while the question is still outstanding, so that an answer
        // the card refuses leaves the question standing. Clearing it first and applying
        // afterwards loses the prompt on every rejected answer: the card would then be
        // waiting for nobody while players were still queued behind it, and the flight would
        // walk straight past them.
        boolean noFurtherPlayers = apply(choice);

        current = null;
        if (noFurtherPlayers) {
            queue.clear();
        }
        if (followUp != null) {
            current = followUp;
            followUp = null;
        } else {
            advance();
        }
    }

    /**
     * Starts another pass down a fresh list of players.
     *
     * <p>Called from {@link #afterEveryone}, for cards that work through more than one
     * round. The Pirates fight down the route and then fire on everybody they beat;
     * Combat Zone evaluates three lines, each its own pass with the route order read
     * again. {@link #afterEveryone} runs again when the new pass is exhausted, so a card
     * that re-queues has to know which round it is on.
     *
     * @param players the players to work through next, in order
     */
    protected final void queueAgain(List<PlayerColor> players) {
        queue.addAll(players);
        finished = false;
    }

    /**
     * Asks the same player a second question before moving on.
     *
     * <p>Several cards need two answers from one player. An abandoned station is claimed
     * and then loaded; an enemy is fought and then, if beaten, its reward is taken or left.
     * Calling this from {@link #apply} keeps the follow-up with the player who earned it,
     * and it composes with returning {@code true}: the queue is emptied and the follow-up
     * is still asked, which is exactly what happens when somebody wins a fight.
     *
     * @param prompt the second question
     */
    protected final void askAgain(PlayerPrompt prompt) {
        followUp = prompt;
    }

    private void ensureStarted() {
        if (!started) {
            started = true;
            advance();
        }
    }

    /**
     * Works down the queue to the next question.
     *
     * <p>Loops rather than falling through, because {@link #afterEveryone} may start
     * another pass: the Pirates fire on everybody they beat only once the fight is over,
     * and Combat Zone reads the route order again for each of its lines. A single pass
     * would queue the new round and then return without ever asking it anything.
     */
    private void advance() {
        while (true) {
            while (current == null && !queue.isEmpty()) {
                current = promptFor(queue.poll()).orElse(null);
            }
            if (current != null || finished) {
                return;
            }
            finished = true;
            afterEveryone();
        }
    }
}
