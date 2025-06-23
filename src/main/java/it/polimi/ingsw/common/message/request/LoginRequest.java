package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.event.PlayerConnectedEvent;
import it.polimi.ingsw.common.message.response.*;
import it.polimi.ingsw.common.message.response.LoginResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Request to authenticate a player with the server.
 */
public class LoginRequest extends AbstractRequest {
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");
    private final String nickname;

    public LoginRequest(String nickname) {
        super();
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public ValidationResult validate() {
        if (nickname == null || nickname.trim().isEmpty()) {
            return ValidationResult.failure("Nickname cannot be empty", "nickname");
        }

        String trimmed = nickname.trim();
        if (trimmed.length() < 3) {
            return ValidationResult.failure("Nickname too short (minimum 3 characters)", "nickname");
        }

        if (trimmed.length() > 20) {
            return ValidationResult.failure("Nickname too long (maximum 20 characters)", "nickname");
        }

        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.failure(
                    "Nickname contains invalid characters. Use only letters, numbers, underscore and hyphen",
                    "nickname"
            );
        }

        return ValidationResult.success();
    }

    @Override
    public Response execute(RequestContext context) {
        // Validate the request
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }

        // Check if already authenticated
        if (context.getPlayerId() != null) {
            return createErrorResponse("Already logged in", ErrorResponse.INVALID_STATE);
        }

        PlayerSessionRegistry registry = context.getPlayerRegistry();
        String trimmedNickname = nickname.trim();

        // Check if nickname is already in use
        if (registry.isNicknameInUse(trimmedNickname)) {
            return createErrorResponse(
                    "Nickname '" + trimmedNickname + "' is already taken",
                    ErrorResponse.VALIDATION_ERROR
            );
        }

        // Create player
        String playerId = UUID.randomUUID().toString();
        boolean registered = registry.registerPlayer(
                context.getSenderId(),
                playerId,
                trimmedNickname
        );

        if (!registered) {
            return createErrorResponse(
                    "Failed to register player",
                    ErrorResponse.INTERNAL_ERROR
            );
        }

        // Publish player connected event
        PlayerConnectedEvent event = new PlayerConnectedEvent(playerId, trimmedNickname);
        context.publishEvent(event);

        // Return success response
        return new LoginResponse(getCorrelationId(), playerId, trimmedNickname);
    }
}