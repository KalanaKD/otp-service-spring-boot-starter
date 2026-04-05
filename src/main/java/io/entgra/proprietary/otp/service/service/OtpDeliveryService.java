package io.entgra.proprietary.otp.service.service;

import io.entgra.proprietary.otp.service.dto.OtpChannelAddressStatus;
import io.entgra.proprietary.otp.service.dto.OtpDeliveryStatus;

import java.util.Map;

public interface OtpDeliveryService {

    OtpDeliveryStatus sendOtp(String otp, OtpService.OtpType otpType, Map<String, String> channel);
}
