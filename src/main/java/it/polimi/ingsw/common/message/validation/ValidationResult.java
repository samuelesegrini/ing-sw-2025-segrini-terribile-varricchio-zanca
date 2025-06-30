package it.polimi.ingsw.common.message.validation;

/**
 * Result of message validation.
 */
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    private final String fieldName;

    public ValidationResult(boolean valid, String errorMessage, String fieldName) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.fieldName = fieldName;
    }

    /**
     *
     * @return a new valid result
     */

    public static ValidationResult success() {
        return new ValidationResult(true, null, null);
    }

    /**
     * @param errorMessage The message that signals the error
     * @return a new invalid result with its error message
     */

    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage, null);
    }

    /**
     *
     * @param errorMessage The message that signals the error
     * @return a new invalid result with its error message
     *
     */

    public static ValidationResult failure(String errorMessage, String fieldName) {
        return new ValidationResult(false, errorMessage, fieldName);
    }

    /**
     *
     * @return true if the result is valid
     */

    public boolean isValid() {
        return valid;
    }

    /**
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }


    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{valid=false, error='" + errorMessage +
               (fieldName != null ? "', field='" + fieldName : "") + "'}";
    }
}