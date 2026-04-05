package io.entgra.proprietary.otp.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;
import io.entgra.proprietary.otp.service.dto.OtpChannelAddressStatus;
import io.entgra.proprietary.otp.service.dto.OtpDeliveryStatus;
import io.entgra.proprietary.otp.service.dto.request.OtpEmailDeliveryRequest;
import io.entgra.proprietary.otp.service.dto.request.OtpSMSDeliveryRequest;
import io.entgra.proprietary.otp.service.exception.OtpDeliveryException;
import io.entgra.proprietary.otp.service.service.OtpDeliveryService;
import io.entgra.proprietary.otp.service.service.OtpService;
import io.entgra.proprietary.otp.service.validation.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OtpDeliveryServiceImpl implements OtpDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(OtpDeliveryServiceImpl.class);
    private static final String CHANNEL_EMAIL = "email";
    private static final String CHANNEL_SMS = "sms";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final OtpProperties otpProperties;
    private final Normalizer normalizer;

    public OtpDeliveryServiceImpl(OtpProperties otpProperties, ObjectMapper objectMapper, Normalizer normalizer) {
        this.otpProperties = otpProperties;
        this.objectMapper = objectMapper;
        this.normalizer = normalizer;
    }

    @Override
    public OtpDeliveryStatus sendOtp(String otp, OtpService.OtpType otpType, Map<String, String> deliveryChannel) {
        String channel = deliveryChannel.getOrDefault("channel", CHANNEL_SMS).toLowerCase();
        String address = deliveryChannel.get("address");

        OtpDeliveryStatus status = new OtpDeliveryStatus();

        List<String> recipients = resolveRecipients(address);
        if (recipients.isEmpty()) {
            logger.warn("OTP delivery address is missing for channel: {}", channel);
            status.setChannelStatus(channel, false);
            return status;
        }

        String message = generateMessage(otp, otpType);
        List<OtpChannelAddressStatus> successAddresses = new ArrayList<>();
        // Tracks whether any delivery attempt succeeded for the selected channel, so the final status can be set to true
        // only if at least one recipient was sent successfully.
        boolean success = false;

        try {
            if (CHANNEL_EMAIL.equals(channel)) {
                for (String recipient : recipients) {
                        sendEmailOtp(recipient, message);
                        successAddresses.add(new OtpChannelAddressStatus(channel, true, recipient));
                        success = true;
                }
                status.setChannelStatus(channel, success);
                status.setSuccessAddress(successAddresses);
                return status;
            }
            if (CHANNEL_SMS.equals(channel)) {
                for (String recipient : recipients) {
                        sendSmsOtp(recipient, message);
                        successAddresses.add(new OtpChannelAddressStatus(channel, true, recipient));
                        success = true;
                }
                status.setChannelStatus(channel, success);
                status.setSuccessAddress(successAddresses);
                return status;
            }
        } catch (RuntimeException e) {
            logger.error("OTP delivery failed for channel: {}", channel, e);
            status.setChannelStatus(channel, false);
            return status;
        }

        logger.warn("Unsupported OTP delivery channel: {}", channel);
        status.setChannelStatus(channel, false);
        return status;
    }

    private List<String> resolveRecipients(String address) {
        if (address == null || address.trim().isEmpty()) {
            return List.of();
        }
        return List.of(address);
    }

    private String generateMessage(String otp, OtpService.OtpType otpType) {
        switch (otpType) {
            case LOGIN:
                return "Your login verification code is: " + otp;
            case PASSWORD_RESET:
                return "Your password reset code is: " + otp;
            case EMAIL_VERIFICATION:
                return "Your email verification code is: " + otp;
            case PHONE_VERIFICATION:
                return "Your phone verification code is: " + otp;
            case MIGRATE_FROM_LEGACY:
                return "Your migration verification code is: " + otp;
            case USER_ID_UPDATE:
                return "Your User ID update verification code is: " + otp;
            case OWN_ACCOUNT_LINKING:
                return "Your account linking pin is: " + otp;
            case SHARED_ACCOUNT_LINKING:
                return "Your account linking shared pin is: " + otp;
            default:
                return "Your verification code is: " + otp;
        }
    }

    private void sendEmailOtp(String address, String message) {
        if (otpProperties.getEmailGatewayUrl() == null || otpProperties.getEmailGatewayUrl().isEmpty()) {
            throw new OtpDeliveryException("Email gateway URL is not configured.");
        }

        try {
            OtpEmailDeliveryRequest emailRequest = new OtpEmailDeliveryRequest();
            emailRequest.setAccountNo(otpProperties.getEmailAccountNo());

            OtpEmailDeliveryRequest.DistributionType distributionType = new OtpEmailDeliveryRequest.DistributionType();
            distributionType.setType("EmailAddress");
            distributionType.setValues(List.of(address));
            emailRequest.setDistributionType(distributionType);

            emailRequest.setSubject(otpProperties.getEmailSubject());
            emailRequest.setMessageTemplate(message);
            emailRequest.setPriority(otpProperties.getEmailPriority());
            emailRequest.setExpiryAt(System.currentTimeMillis() + (long) otpProperties.getExpiryMinutes() * 60 * 1000);

            emailRequest.setEmailTemplate(otpProperties.getEmailTemplateId());
            emailRequest.setSenderEmailAddress(otpProperties.getEmailSenderAddress());
            emailRequest.setReplyEmailAddress(otpProperties.getEmailReplyAddress());

            List<OtpEmailDeliveryRequest.Parameter> parameters = new ArrayList<>();
            parameters.add(new OtpEmailDeliveryRequest.Parameter("message", message));
            emailRequest.setParameters(parameters);

            String jsonBody = objectMapper.writeValueAsString(emailRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(otpProperties.getEmailGatewayUrl()))
                    .timeout(Duration.ofSeconds(otpProperties.getRequestTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OtpDeliveryException("Failed to send Email OTP: " + response.body());
            }
        } catch (Exception e) {
            throw new OtpDeliveryException("Failed to send Email OTP", e);
        }
    }

    private void sendSmsOtp(String address, String message) {
        if (otpProperties.getSmsGatewayUrl() == null || otpProperties.getSmsGatewayUrl().isEmpty()) {
            throw new OtpDeliveryException("SMS gateway URL is not configured.");
        }

        String normalizedAddress = normalizer.normalizeToGatewayFormat(address);
        if (normalizedAddress == null) {
            throw new OtpDeliveryException("Invalid SMS address format.");
        }

        try {
            OtpSMSDeliveryRequest smsRequest = new OtpSMSDeliveryRequest();
            OtpSMSDeliveryRequest.DistributionType distributionType = new OtpSMSDeliveryRequest.DistributionType();
            distributionType.setType("SMSNumber");
            distributionType.setValues(List.of(normalizedAddress));
            smsRequest.setDistributionType(distributionType);
            smsRequest.setMsgTemplate(message);
            smsRequest.setPriority(otpProperties.getSmsPriority());
            smsRequest.setSmsGateway(otpProperties.getSmsGatewayName());
            smsRequest.setExpiryAt(String.valueOf(System.currentTimeMillis() + (long) otpProperties.getExpiryMinutes() * 60 * 1000));
            smsRequest.setParams(new ArrayList<>());

            String jsonBody = objectMapper.writeValueAsString(smsRequest);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(otpProperties.getSmsGatewayUrl()))
                    .timeout(Duration.ofSeconds(otpProperties.getRequestTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new OtpDeliveryException("Failed to send SMS OTP: " + response.body());
            }
        } catch (Exception e) {
            throw new OtpDeliveryException("Failed to send SMS OTP", e);
        }
    }
}