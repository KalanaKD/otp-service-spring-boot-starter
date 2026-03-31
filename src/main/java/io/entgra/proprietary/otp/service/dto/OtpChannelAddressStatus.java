package io.entgra.proprietary.otp.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpChannelAddressStatus {
    private String channel;
    private boolean success;
    private String address;
}
