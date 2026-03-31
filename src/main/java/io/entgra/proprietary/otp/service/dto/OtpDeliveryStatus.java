package io.entgra.proprietary.otp.service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpDeliveryStatus {
    private final Map<String, Boolean> channelStatus =  new HashMap<>();
    private List<String> failedChannels = new ArrayList<>();
    private List<OtpChannelAddressStatus> successAddress;
    private String otp;

    public void setChannelStatus(String channel, boolean success) {
        channelStatus.put(channel, success);
    }

    public boolean isFullySuccessful() {
        return !getSuccessfulChannels().isEmpty() && getSuccessfulChannels().size() == 2;
    }

    public boolean isPartiallySuccessful() {
        return getSuccessfulChannels() != null && getSuccessfulChannels().size() == 1;
    }

    public boolean isSuccess() {
        return channelStatus.containsValue(true);
    }
    public List<String> getFailedChannels() {
        List<String> failed = new ArrayList<>();
        channelStatus.forEach((channel, success) -> {
            if (!success) failed.add(channel);
        });
        return failed;
    }
    public List<String> getSuccessfulChannels() {
        List<String> success = new ArrayList<>();
        channelStatus.forEach((channel, s) -> {
            if (s) success.add(channel);
        });
        return success;
    }
}
