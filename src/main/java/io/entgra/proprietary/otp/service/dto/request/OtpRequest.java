package io.entgra.proprietary.otp.service.dto.request;

import io.entgra.proprietary.otp.service.validation.ValidOtp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {

    @NotBlank(message = "${otp.identifier-required-message:Identifier is required.}")
    private String identifier;

    @NotBlank(message = "${otp.otp-required-message:OTP is required.}")
    @ValidOtp
    private String otp;

}
