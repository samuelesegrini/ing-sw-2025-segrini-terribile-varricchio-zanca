package it.polimi.ingsw.common.message.building;

import it.polimi.ingsw.common.message.BaseMessage;

import java.util.Objects;

/**
 * Event sent by the server to all players when the building timer is flipped
 * or its state changes (e.g., time remaining).
 */
public class BuildingTimerUpdatedEvent extends BaseMessage {
    private static final long serialVersionUID = 1L;

    private final int timerStage; // e.g., 0, 1, 2 for flips, or -1 if time's up. Or actual seconds.
    private final String description; // e.g., "Timer flipped to middle", "Final minute!"

    public BuildingTimerUpdatedEvent(int timerStage, String description) {
        super();
        this.timerStage = timerStage;
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    public int getTimerStage() {
        return timerStage;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "BuildingTimerUpdatedEvent{" +
                "timerStage=" + timerStage +
                ", description='" + description + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}