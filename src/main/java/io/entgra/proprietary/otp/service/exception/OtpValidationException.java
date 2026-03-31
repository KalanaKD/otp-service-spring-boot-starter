package io.entgra.proprietary.otp.service.exception;

public class OtpValidationException extends RuntimeException {

    public OtpValidationException() {
        super("Invalid or expired session. Please try again.");
    }

    public OtpValidationException(String message) {
        super(message);
    }

    public OtpValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}