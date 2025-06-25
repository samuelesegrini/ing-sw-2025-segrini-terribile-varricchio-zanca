package it.polimi.ingsw.server.model.domain.player;

import java.util.UUID;

public class PlayerId {
    private UUID value;
    private String nickname;

    public PlayerId(UUID value, String nickname) {
        this.value = value;
        this.nickname = nickname;
    }

    /**
     * Return a String corresponding to the player's username
     * @return player's nickname
     */
    public String getNickname(){
        return this.nickname;
    }

    /**
     * Creates a PlayerId instance from a nickname string.
     * Generates a new UUID for the internal value.
     * Assumes the nickname is the primary identifier for lookup/comparison purposes.
     *
     * @param nickname The player's nickname.
     * @return A new PlayerId instance.
     * @throws IllegalArgumentException if nickname is null or empty.
     */
    public static PlayerId fromString(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        // Generate a new UUID, as the original UUID isn't recoverable from just the nickname
        return new PlayerId(UUID.randomUUID(), nickname.trim());
    }

    @Override
    public String toString() {
        return nickname;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlayerId playerId = (PlayerId) obj;
        return nickname.equals(playerId.nickname);
    }

    @Override
    public int hashCode() {
        return nickname.hashCode();
    }
}
