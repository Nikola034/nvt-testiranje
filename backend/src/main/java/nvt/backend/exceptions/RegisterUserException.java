package nvt.backend.exceptions;

public class RegisterUserException extends RuntimeException {
    private final ErrorType errorType;

    public enum ErrorType {
        USER_ALREADY_EXISTS,
    }

    public RegisterUserException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
