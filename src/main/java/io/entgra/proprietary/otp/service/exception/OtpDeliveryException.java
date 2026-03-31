package io.entgra.proprietary.otp.service.exception;

public class OtpDeliveryException extends RuntimeException {

    public OtpDeliveryException() {
        super("Failed to send OTP");
    }
    public OtpDeliveryException(String message) {
        super(message);
    }

}
