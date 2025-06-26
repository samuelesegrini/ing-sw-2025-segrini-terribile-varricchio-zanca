package it.polimi.ingsw.server.model.domain.player;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerId implements Serializable {
    private final static long serialVersionUID = 1L;
    
    // Registry to ensure deterministic fromString conversion
    private static final Map<String, PlayerId> nicknameRegistry = new ConcurrentHashMap<>();

    private final UUID value;
    private final String nickname;

    public PlayerId(UUID value, String nickname) {
        if (value == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        this.value = value;
        this.nickname = nickname.trim();
    }

    /**
     * Return a String corresponding to the player's username
     * @return player's nickname
     */
    public String getNickname(){
        return this.nickname;
    }
    
    /**
     * Gets the UUID value of this PlayerId
     * @return The UUID value
     */
    public UUID getValue() {
        return this.value;
    }

    /**
     * Creates a PlayerId instance from a nickname string.
     * Returns the same PlayerId instance for the same nickname (deterministic).
     * This ensures consistent UUID mapping for each unique nickname.
     *
     * @param nickname The player's nickname.
     * @return A PlayerId instance (same instance for same nickname).
     * @throws IllegalArgumentException if nickname is null or empty.
     */
    public static PlayerId fromString(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty");
        }
        String trimmedNickname = nickname.trim();
        return nicknameRegistry.computeIfAbsent(trimmedNickname, 
            n -> new PlayerId(UUID.nameUUIDFromBytes(n.getBytes()), n));
    }

    @Override
    public String toString() {
        return nickname;
    }
    
    /**
     * Returns a unique string representation that includes both UUID and nickname
     * @return A string in format "nickname[uuid]"
     */
    public String toUniqueString() {
        return nickname + "[" + value.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlayerId playerId = (PlayerId) obj;
        // Use UUID for equality to ensure uniqueness
        return value.equals(playerId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    /**
     * Clears the nickname registry (for testing purposes)
     */
    public static void clearRegistry() {
        nicknameRegistry.clear();
    }
}
