package io.entgra.proprietary.otp.service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpSMSDeliveryRequest {
    private DistributionType distributionType;
    private String msgTemplate;
    private String priority;
    private String smsGateway;
    private String expiryAt;
    private List<Param> params;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Param {
        private String key;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionType {
        private String type;
        private List<String> values;
    }

}
