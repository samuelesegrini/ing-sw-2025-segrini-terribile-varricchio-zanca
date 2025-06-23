package it.polimi.ingsw.common.message.request;

import it.polimi.ingsw.common.message.response.*;
import it.polimi.ingsw.common.message.response.LoginResponse;
import it.polimi.ingsw.common.message.validation.ValidationResult;
import it.polimi.ingsw.server.core.PlayerSessionRegistry;

import java.util.UUID;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Request to authenticate a player with the server.
 */
public class LoginRequest extends AbstractRequest {
    private static final Logger LOGGER = Logger.getLogger(LoginRequest.class.getName());
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
        LOGGER.info("🔐 LOGIN REQUEST - Starting authentication for nickname: '" + nickname + "' from client: " + context.getSenderId());
        
        // Validate the request
        ValidationResult validation = validate();
        if (!validation.isValid()) {
            LOGGER.warning("❌ LOGIN FAILED - Validation error for nickname '" + nickname + "': " + validation.getErrorMessage());
            return createErrorResponse(validation.getErrorMessage(), ErrorResponse.VALIDATION_ERROR);
        }
        LOGGER.fine("✅ LOGIN VALIDATION - Nickname '" + nickname + "' passed validation checks");

        // Check if already authenticated
        if (context.getPlayerId() != null) {
            LOGGER.warning("❌ LOGIN FAILED - Client " + context.getSenderId() + " already authenticated as player: " + context.getPlayerId());
            return createErrorResponse("Already logged in", ErrorResponse.INVALID_STATE);
        }
        LOGGER.fine("✅ AUTH STATE - Client " + context.getSenderId() + " is not yet authenticated");

        PlayerSessionRegistry registry = context.getPlayerRegistry();
        String trimmedNickname = nickname.trim();
        LOGGER.fine("🔍 NICKNAME CHECK - Checking availability of nickname: '" + trimmedNickname + "'");

        // Check if nickname is already in use
        if (registry.isNicknameInUse(trimmedNickname)) {
            LOGGER.warning("❌ LOGIN FAILED - Nickname '" + trimmedNickname + "' is already taken");
            return createErrorResponse(
                    "Nickname '" + trimmedNickname + "' is already taken",
                    ErrorResponse.VALIDATION_ERROR
            );
        }
        LOGGER.fine("✅ NICKNAME AVAILABLE - Nickname '" + trimmedNickname + "' is available");

        // Create player
        String playerId = UUID.randomUUID().toString();
        LOGGER.info("👤 PLAYER CREATION - Generated playerId: " + playerId + " for nickname: '" + trimmedNickname + "'");
        
        boolean registered = registry.registerPlayer(
                context.getSenderId(),
                playerId,
                trimmedNickname
        );

        if (!registered) {
            LOGGER.severe("❌ LOGIN FAILED - Failed to register player with ID: " + playerId + " and nickname: '" + trimmedNickname + "'");
            return createErrorResponse(
                    "Failed to register player",
                    ErrorResponse.INTERNAL_ERROR
            );
        }
        LOGGER.info("✅ PLAYER REGISTERED - Successfully registered player: " + playerId + " ('" + trimmedNickname + "') for client: " + context.getSenderId());


        // Return success response
        LOGGER.info("🎉 LOGIN SUCCESS - Returning LoginResponse for player: " + playerId + " ('" + trimmedNickname + "') to client: " + context.getSenderId());
        return new LoginResponse(getCorrelationId(), playerId, trimmedNickname);
    }
}