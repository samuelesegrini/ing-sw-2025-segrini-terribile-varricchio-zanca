package it.polimi.ingsw.model.player;

import java.util.UUID;

public class PlayerId {
    private UUID value;
    private String nickname;


    /**
     * Concatenate the player's nickname and UUID into a single string.
     */
    public void StringToString(){};

    /**
     * Generate a Universally Unique Identifier for each player.
     * @return a 36-character string that includes numbers and letters
     */
    public static PlayerId generatePlayerId(){};
}
