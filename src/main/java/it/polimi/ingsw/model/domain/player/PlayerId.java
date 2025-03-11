package it.polimi.ingsw.model.domain.player;

import java.util.UUID;

public class PlayerId {
    private UUID value;
    private String nickname;

    public PlayerId(UUID value, String nickname) {
        this.value = value;
        this.nickname = nickname;
    }

    /**
     * Concatenate the player's nickname and UUID into a single string.
     @return A new string which is the result of concatenating {@code value} and {@code nickname}
     */
    public String stringToString(){
        return value.toString().concat(nickname);
    }

    /**
     * Generate a Universally Unique Identifier for each player.
     * @return a 36-character string that includes numbers and letters
     */
    public static PlayerId generatePlayerId(){
        UUID uniqueId = UUID.randomUUID();
        return new PlayerId(uniqueId, uniqueId.toString());
    }

    /**
     * Return a String corresponding to the player's username
     * @return player's nickname
     */
    public String getNickname(){
        return this.nickname;
    }
}
