package io.entgra.proprietary.otp.service.service;

import io.entgra.proprietary.otp.service.dto.OtpDeliveryStatus;
import io.entgra.proprietary.otp.service.dto.OtpValidationResult;

import java.util.List;
import java.util.Map;

public interface OtpService {
    /**
     * Enum representing the various types of OTP use cases.
     */
    enum OtpType {
        LOGIN,
        PASSWORD_RESET,
        EMAIL_VERIFICATION,
        PHONE_VERIFICATION,
        MIGRATE_FROM_LEGACY,
        REGISTRATION_TOKEN,
        FLOW_TOKEN,
        ACCOUNT_LINKING,
        OWN_ACCOUNT_LINKING,
        SHARED_ACCOUNT_LINKING,
        USER_ID_LOGIN,
        USER_ID_UPDATE
    }

    /**
     * Generates a new OTP for the given user and OTP type, optionally considering delivery method and metadata.
     *
     * @param identifier The identifier of whom the OTP is generated. It can be a unique identifier such as username, mobile, email.
     * @param otpType The type of OTP to be generated
     * @param deliveryChannels List of delivery channel maps, each with keys 'channel' and 'address'
     * @param metadata Additional metadata such as IP address, device info, etc.
     * @return The generated OTP (typically returned for testing or direct delivery)
     */
    OtpDeliveryStatus generateAndDeliverOtp(String identifier, OtpType otpType, List<Map<String, String>> deliveryChannels, Map<String, Object> metadata);

    /**
     * Validates the provided OTP against the stored/generated value for the given user and OTP type.
     *
     * @param identifier The identifier associated with the OTP
     * @param otpType The type of OTP being validated
     * @param otp The OTP value provided by the user
     * @return true if the OTP is valid; false otherwise
     */
    OtpValidationResult validateOtp(String identifier, OtpType otpType, String otp);

    /**
     * Triggers a resend of the OTP for the given user and OTP type using the specified delivery method.
     *
     * @param identifier The identifier requesting the resend
     * @param otpType The type of OTP to resend
     * @param deliveryChannel Map with keys 'channel' and 'address'
     * @param metadata Additional metadata for context or auditing
     * @return true if the resend operation was successful; false otherwise
     */
    Boolean resendOtp(String identifier, OtpType otpType, Map<String, String> deliveryChannel, Map<String, Object> metadata);

}
