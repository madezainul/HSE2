package ahqpck.hse.safety.util;

import org.springframework.validation.BindingResult;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Web Utility Class
 * Provides common utility methods for web layer operations
 */
public class WebUtil {

    /**
     * Extracts validation errors from BindingResult into a single string.
     * Format: "Field1: Error1 | Field2: Error2"
     */
    public static String getErrorMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof org.springframework.validation.FieldError)
                            ? ((org.springframework.validation.FieldError) error).getField()
                            : "Input";
                    String message = error.getDefaultMessage();
                    return field + ": " + message;
                })
                .collect(Collectors.joining(" | "));
    }

    /**
     * Returns true if errors exist and are non-empty
     */
    public static boolean hasErrors(BindingResult bindingResult) {
        return bindingResult.hasErrors();
    }

    /**
     * Generates a unique activation token using UUID
     * @return 36-character unique token
     */
    public static String generateActivationToken() {
        return UUID.randomUUID().toString();
    }
}
