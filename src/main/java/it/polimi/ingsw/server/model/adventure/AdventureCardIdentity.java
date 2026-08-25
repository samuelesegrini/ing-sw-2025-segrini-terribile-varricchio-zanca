package it.polimi.ingsw.server.model.adventure;

/**
 * What deck building needs to know about a card, without knowing what it does.
 *
 * <p>The test flight mark is a property of a level I card, not a level of its own: the
 * eight cards used for a test flight are drawn from the level I deck and also appear
 * in level II flights (manual p.9).
 *
 * @param id         the card's identifier, taken from its artwork file name
 * @param type       the adventure it presents
 * @param level      the difficulty band printed on it
 * @param testFlight whether it carries the L mark
 */
public record AdventureCardIdentity(String id, AdventureCardType type, CardLevel level, boolean testFlight) {

    /**
     * Validates the identity.
     *
     * @throws IllegalArgumentException if the identifier is blank, or a card is marked
     *                                  for the test flight without being a level I card
     *                                  of a type the test flight uses
     * @throws NullPointerException     if the type or the level is {@code null}
     */
    public AdventureCardIdentity {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("an adventure card needs an identifier");
        }
        if (testFlight && level != CardLevel.LEVEL_I) {
            throw new IllegalArgumentException(id + ": the test flight deck is drawn from level I cards");
        }
        if (testFlight && !type.inTestFlight()) {
            throw new IllegalArgumentException(id + ": " + type + " does not appear in the test flight");
        }
    }
}
