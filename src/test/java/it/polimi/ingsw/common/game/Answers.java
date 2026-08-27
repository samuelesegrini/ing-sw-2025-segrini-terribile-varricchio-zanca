package it.polimi.ingsw.common.game;

/**
 * The least interesting answer to any question a card can ask.
 *
 * <p>Take nothing, power nothing, keep the first piece. For tests whose point is to reach the
 * end of a flight rather than to play one well — what the cards do when answered properly was
 * settled card by card in M3, and a test that also tried to play cleverly would be testing two
 * things and telling you neither.
 *
 * <p>Which is the same question the server has to answer when a player drops, so this is now
 * {@link SkippedTurn} under another name. Kept as a name because that is what these tests mean
 * — they want to reach the end of a flight, not to play one well — and because a test reading
 * {@code SkippedTurn.answerFor} would suggest somebody had disconnected.
 */
public final class Answers {

    private Answers() {
    }

    /**
     * Returns the simplest legal answer to a question.
     *
     * @param prompt what is being asked
     * @return an answer that does as little as possible
     */
    public static PlayerChoice simplestTo(PlayerPrompt prompt) {
        // One definition, in the main sources, because the server needs it for real: it is what
        // a game answers on behalf of somebody who has dropped. A second copy here would be a
        // second thing to keep in step with the rules.
        return SkippedTurn.answerFor(prompt);
    }
}
