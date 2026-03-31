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

    private String otpRegex = "^\\d{6}$";
    private String otpLengthMessage = "OTP must be 6 digits.";
    private String identifierRequiredMessage = "Identifier is required.";
    private String otpRequiredMessage = "OTP is required.";

}
