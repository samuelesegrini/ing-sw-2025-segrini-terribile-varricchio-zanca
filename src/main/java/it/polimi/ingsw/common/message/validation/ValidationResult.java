package it.polimi.ingsw.common.message.validation;

/**
 * Result of message validation.
 */
public class ValidationResult {
    private final boolean valid;
    private final String errorMessage;
    private final String fieldName;
    
    private ValidationResult(boolean valid, String errorMessage, String fieldName) {
        this.valid = valid;
        this.errorMessage = errorMessage;
        this.fieldName = fieldName;
    }
    
    public static ValidationResult success() {
        return new ValidationResult(true, null, null);
    }
    
    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage, null);
    }
    
    public static ValidationResult failure(String errorMessage, String fieldName) {
        return new ValidationResult(false, errorMessage, fieldName);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
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