package it.polimi.ingsw.common.message.setup;

import it.polimi.ingsw.common.dto.GameSettingsDTO;
import it.polimi.ingsw.common.message.BaseMessage;
import it.polimi.ingsw.common.message.Command;

import java.util.Objects;

/**
 * Command sent by a client to request the creation of a new game session.
 */
public class CreateGameRequestCommand extends BaseMessage implements Command {
    private static final long serialVersionUID = 1L;

    private final GameSettingsDTO settings;

    public CreateGameRequestCommand(GameSettingsDTO settings) {
        super();
        this.settings = Objects.requireNonNull(settings, "settings cannot be null");
    }

    public GameSettingsDTO getSettings() {
        return settings;
    }

    @Override
    public String toString() {
        return "CreateGameRequestCommand{" +
                "settings=" + settings +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}