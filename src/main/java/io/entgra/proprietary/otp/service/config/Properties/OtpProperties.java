package io.entgra.proprietary.otp.service.config.Properties;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {

    // OtpRequest DTO relate properties
    private int otpLength = 6;
    private String otpRegex;
    private String otpLengthMessage = "OTP must be {length} digits.";
    private String identifierRequiredMessage = "Identifier is required.";
    private String otpRequiredMessage = "OTP is required.";

    // OtpValidationRequest relate properties
    private String flowTypeRequiredMessage = "Flow type is required.";

    // Service runtime properties
    private int expiryMinutes = 5;
    private long cleanupIntervalMs = 300_000L;

    // Delivery configuration (default implementation)
    private String emailGatewayUrl;
    private String smsGatewayUrl;
    private String emailTemplateId;
    private String emailSenderAddress;
    private String emailReplyAddress;
    private String emailSubject = "OTP Verification";
    private String emailAccountNo;
    private String emailPriority = "1";
    private String smsGatewayName;
    private String smsPriority = "1";
    private int requestTimeoutSeconds = 10;

    // Mobile Number validation/normalization
    // Use E.126 format for country code and ensure regex patterns are consistent with that
    private String mobileApiRegex = "^(?:07|\\\\+947|947)\\\\d{8}$";
    private String mobileGatewayRegex = "^+947[0-9]{8}$";
    private String countryCode = "+94";
    private String coreDigits = "^7\\d{8}$";
    private int coreLength = 9;
    private String trunkPrefix = "0";

    public String getResolvedOtpRegex() {
        if (otpRegex != null && !otpRegex.trim().isEmpty()) {
            return otpRegex;
        }
        return "^\\d{" + otpLength + "}$";
    }

    public String getResolvedOtpLengthMessage() {
        if (otpLengthMessage == null || otpLengthMessage.trim().isEmpty()) {
            return "OTP must be " + otpLength + " digits.";
        }
        return otpLengthMessage.replace("{length}", String.valueOf(otpLength));
    }
}
