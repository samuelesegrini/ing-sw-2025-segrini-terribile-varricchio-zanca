package it.polimi.ingsw.common.message.event;

import java.util.List;

/**
 * Broadcast by the server whenever dice are rolled to ensure all clients see the same random result.
 */
public class DiceRollEvent extends AbstractEvent {
    private final String purpose;
    private final List<Integer> diceValues;

    public DiceRollEvent(String gameId, String purpose, List<Integer> diceValues) {
        super(EventType.DICE_ROLLED, gameId, null);
        this.purpose = purpose;
        this.diceValues = List.copyOf(diceValues);
    }

    public String getPurpose() {
        return purpose;
    }

    public List<Integer> getDiceValues() {
        return diceValues;
    }

    @Override
    public void handleOnClient(ClientEventContext context) {
        // Client UI can show an animation or log message for the dice roll.
        // E.g., "Meteor strike location dice: 4, 5"
        // context.getGameUI().showDiceRoll(purpose, diceValues);
    }
}