package io.entgra.proprietary.otp.service.validation;

import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class OtpValidator implements ConstraintValidator<ValidOtp, String> {

    private final OtpProperties properties;

    public OtpValidator(OtpProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String otp, ConstraintValidatorContext context) {
        if (otp == null) return false;

        return otp.matches(properties.getOtpRegex());
    }
}
