package io.entgra.proprietary.otp.service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpEmailDeliveryRequest {
    private String accountNo;
    private DistributionType distributionType;
    private String subject;
    private String messageTemplate;
    private String priority;
    private Long expiryAt;
    private List<Parameter> parameters;
    private String emailTemplate;
    private String senderEmailAddress;
    private String replyEmailAddress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionType {
        private String type;
        private List<String> values;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String key;
        private String value;
    }
}
