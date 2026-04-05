package io.entgra.proprietary.otp.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpValidationResult {
    private boolean valid;
    private Map<String, Object> metadata;
    private String username;
}
