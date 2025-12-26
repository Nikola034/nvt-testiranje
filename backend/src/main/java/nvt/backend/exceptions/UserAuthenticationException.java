package nvt.backend.exceptions;

import lombok.Getter;

public class UserAuthenticationException extends RuntimeException {

    public enum ErrorType {
        USER_NOT_FOUND,
        USER_SUSPENDED,
        ACCOUNT_NOT_VERIFIED,
        INVALID_CREDENTIALS
    }

    @Getter
    private final ErrorType errorType;

    public UserAuthenticationException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
}