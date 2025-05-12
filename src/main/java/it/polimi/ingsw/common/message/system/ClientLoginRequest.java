package it.polimi.ingsw.common.message.system;

import it.polimi.ingsw.common.message.BaseMessage;

public class ClientLoginRequest extends BaseMessage /* implements Message, Serializable */ {
    private static final long serialVersionUID = 1L; // Important for Serializable

    private final String nickname;

    public ClientLoginRequest(String nickname) {
        super(); // If extending BaseMessage
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Nickname cannot be null or empty.");
        }
        this.nickname = nickname.trim();
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public String toString() {
        return "ClientLoginRequest{" +
                "nickname='" + nickname + '\'' +
                ", timestamp=" + getTimestamp() + // If extending BaseMessage
                '}';
    }
}