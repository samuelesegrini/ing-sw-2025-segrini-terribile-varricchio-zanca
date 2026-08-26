package it.polimi.ingsw.server.model.game;

/**
 * Turning the model's own complaints into something a player can read.
 *
 * <p>The model already refuses illegal moves, and it does so in sentences written for a
 * person: "there is no tile in your hand", "a cabin holds people or an alien, never both".
 * Restating those rules in the phases would be writing them twice and getting one of the two
 * wrong, so a phase lets the model refuse and passes on what it said.
 *
 * <p>This works because the model checks before it changes anything. A refused command leaves
 * the game as it was, which is what {@code Rejected} promises a client.
 */
final class Reasons {

    private Reasons() {
    }

    /**
     * Returns what to tell the player.
     *
     * @param refused what the model threw
     * @return the model's own words, or a fallback when it did not say anything
     */
    static String from(RuntimeException refused) {
        String said = refused.getMessage();
        return said == null || said.isBlank() ? "that is not something you can do now" : said;
    }
}
