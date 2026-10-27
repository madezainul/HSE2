package ahqpck.hse.safety.exception;

/**
 * Thrown when a request is valid but a business rule forbids it,
 * for example deleting your own account or removing the last administrator.
 * The message is written for the person using the page and can be shown to them.
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }

    public OperationNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
