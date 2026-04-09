package io.entgra.proprietary.otp.service.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;
import io.entgra.proprietary.otp.service.dto.OtpChannelAddressStatus;
import io.entgra.proprietary.otp.service.dto.OtpDeliveryStatus;
import io.entgra.proprietary.otp.service.dto.OtpValidationResult;
import io.entgra.proprietary.otp.service.provider.OtpQueryProvider;
import io.entgra.proprietary.otp.service.service.OtpDeliveryService;
import io.entgra.proprietary.otp.service.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OtpServiceImpl implements OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final SecureRandom secureRandom = new SecureRandom();
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final OtpDeliveryService otpDeliveryService;
    private final OtpProperties otpProperties;
    private final OtpQueryProvider otpQueryProvider;

    public OtpServiceImpl(JdbcTemplate jdbcTemplate,
                          OtpDeliveryService otpDeliveryService,
                          OtpProperties otpProperties,
                          ObjectMapper objectMapper,
                          OtpQueryProvider otpQueryProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.otpDeliveryService = otpDeliveryService;
        this.otpProperties = otpProperties;
        this.objectMapper = objectMapper;
        this.otpQueryProvider = otpQueryProvider;
    }

    @Override
    @Transactional
    public OtpDeliveryStatus generateAndDeliverOtp(String identifier,
                                                   OtpType otpType,
                                                   List<Map<String, String>> deliveryChannels,
                                                   Map<String, Object> metadata) {
        // Delete existing OTPs before inserting a new one to avoid multiple active OTPs per user/type.
        if (otpType == OtpType.FLOW_TOKEN) {
            logger.info("Deleting all existing FLOW_TOKENs for identifier: {}", identifier);
            deleteOtp(identifier, otpType, true);
        } else {
            deleteOtp(identifier, otpType, false);
        }

        // Generate customized-legth otp
        String otp = generateNumericOtp(otpProperties.getOtpLength());

        // Set expiry time based on configuration
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpProperties.getExpiryMinutes());

        String deliveryMethod;
        List<String> availableChannels = new ArrayList<>();
        if (otpType == OtpType.FLOW_TOKEN) {
            deliveryMethod = "API";
        } else if (deliveryChannels != null && !deliveryChannels.isEmpty()) {
            availableChannels = deliveryChannels.stream()
                    .map(channel -> channel.get("channel"))
                    .filter(ch -> ch != null && !ch.trim().isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            deliveryMethod = String.join(",", availableChannels);
        } else {
            deliveryMethod = "PENDING";
        }

        String metadataJson = null;
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                logger.warn("Failed to serialize OTP metadata for identifier: {}", identifier, e);
            }
        }

        // Store the newly generated OTP in the consumer database through the configured query provider.
        jdbcTemplate.update(
                otpQueryProvider.insertOtp(),
                identifier,
                otp,
                otpType.name(),
                metadataJson,
                expiryTime,
                deliveryMethod,
                "PENDING",
                false
        );

        OtpDeliveryStatus otpDeliveryStatus = new OtpDeliveryStatus();
        otpDeliveryStatus.setOtp(otp);

        if (otpType == OtpType.FLOW_TOKEN) {
            otpDeliveryStatus.setChannelStatus("API", true);
            updateDeliveryStatus(identifier, otpType, otpDeliveryStatus);
            return otpDeliveryStatus;
        }

        if (deliveryChannels == null || deliveryChannels.isEmpty()) {
            // No delivery channels provided by the consumer; return status with OTP only
            return otpDeliveryStatus;
        }

        List<OtpChannelAddressStatus> allSuccessAddresses = new ArrayList<>();
        for (Map<String, String> deliveryChannel : deliveryChannels) {
            String channel = deliveryChannel.get("channel");
            String address = deliveryChannel.get("address");

            if (channel == null || channel.trim().isEmpty()) {
                logger.warn("Channel is null or empty for identifier: {}", identifier);
                otpDeliveryStatus.setChannelStatus("unknown", false);
                continue;
            }

            String normalizedChannel = channel.toLowerCase();
            if (address == null || address.trim().isEmpty()) {
                logger.warn("Address is null or empty for identifier: {} channel: {}", identifier, channel);
                otpDeliveryStatus.setChannelStatus(normalizedChannel, false);
                continue;
            }

            try {
                OtpDeliveryStatus channelStatus = otpDeliveryService.sendOtp(otp, otpType, deliveryChannel);
                boolean success = channelStatus.getChannelStatus().getOrDefault(normalizedChannel, false);
                otpDeliveryStatus.setChannelStatus(normalizedChannel, success);
                if (channelStatus.getSuccessAddress() != null) {
                    allSuccessAddresses.addAll(channelStatus.getSuccessAddress());
                }
            } catch (RuntimeException e) {
                logger.error("Failed to send OTP via {} for user {}", normalizedChannel, identifier, e);
                otpDeliveryStatus.setChannelStatus(normalizedChannel, false);
            }
        }

        otpDeliveryStatus.setSuccessAddress(allSuccessAddresses);
        updateDeliveryStatus(identifier, otpType, otpDeliveryStatus);
        return otpDeliveryStatus;
    }

    @Override
    @Transactional
    public OtpValidationResult validateOtp(String identifier, OtpType otpType, String otp) {

        // First, fetch the OTP data to get metadata before marking as used
        Optional<OtpData> otpData = getActiveOtpWithCode(identifier, otpType, otp);
        if (otpData.isEmpty()) {
            logger.warn("Invalid, expired, or already used OTP for identifier: {} and type: {}", identifier, otpType);
            return new OtpValidationResult(false, null, identifier);
        }

        // Mark the validated row as used so the same code cannot be replayed.
        jdbcTemplate.update(
                otpQueryProvider.markOtpAsUsed(),
                identifier,
                otpType.name(),
                otp
        );

        logger.info("OTP validated successfully for identifier: {} and type: {}", identifier, otpType);
        return new OtpValidationResult(true, otpData.get().getMetadata(objectMapper), identifier);
    }

    @Override
    @Transactional
    public Boolean resendOtp(String identifier,
                             OtpType otpType,
                             Map<String, String> deliveryChannel,
                             Map<String, Object> metadata) {
        // Resend is essentially a new generation - reuse the optimized generateAndDeliverOTP
        OtpDeliveryStatus status = generateAndDeliverOtp(identifier, otpType, List.of(deliveryChannel), metadata);
        logger.info("OTP resent for identifier: {} and type: {}", identifier, otpType);
        return status.isSuccess();
    }

    @Scheduled(fixedRateString = "${otp.cleanup-interval-ms:300000}") // Run every 5 minutes by default
    public void cleanupExpiredOtps() {
        try {
            // Periodic cleanup keeps the OTP table small by deleting expired or consumed records.
            int deletedCount = jdbcTemplate.update(
                    otpQueryProvider.cleanUpExpiredOtps(),
                    LocalDateTime.now()
            );
            logger.debug("Cleaned up {} expired/used OTPs", deletedCount);
        } catch (Exception e) {
            logger.error("Error cleaning up expired/used OTPs", e);
        }
    }

    private void updateDeliveryStatus(String identifier, OtpType otpType, OtpDeliveryStatus otpDeliveryStatus) {
        // Compress delivery outcomes into one string so the result can be stored in a simple text column.
        StringBuilder statusStr = new StringBuilder();
        otpDeliveryStatus.getChannelStatus().forEach((ch, st) -> statusStr.append(ch).append(":").append(st).append(","));
        if (statusStr.length() > 0) {
            statusStr.setLength(statusStr.length() - 1);
        }

        try {
            jdbcTemplate.update(
                    otpQueryProvider.updateDeliveryStatus(),
                    statusStr.toString(),
                    identifier,
                    otpType.name()
            );
            logger.debug("Updated delivery status for user {}: {}", identifier, statusStr.toString());
        } catch (Exception e) {
            logger.error("Failed to update delivery status for identifier: {} and type: {}", identifier, otpType, e);
            throw new IllegalStateException("Failed to update delivery status", e);
        }
    }

    private Optional<OtpData> getActiveOtpWithCode(String identifier, OtpType otpType, String otp) {
        return jdbcTemplate.query(
                otpQueryProvider.findActiveOtp(),
                new Object[]{identifier, otpType.name(), otp, LocalDateTime.now()},
                otpRowMapper
        ).stream().findFirst();
    }

    private void deleteOtp(String identifier, OtpType otpType, boolean deleteAllOtp) {
        if (deleteAllOtp) {
            jdbcTemplate.update(
                    otpQueryProvider.deleteAllOtps(),
                    identifier,
                    otpType.name()
            );
        } else {
            jdbcTemplate.update(
                    otpQueryProvider.deleteUnusedOtps(),
                    identifier,
                    otpType.name()
            );
        }
    }

    private String generateNumericOtp(int length) {
        int maxExclusive = (int) Math.pow(10, length);
        int number = secureRandom.nextInt(maxExclusive);
        return String.format("%0" + length + "d", number);
    }

    private static final RowMapper<OtpData> otpRowMapper = (rs, rowNum) -> new OtpData(
            // Read the persisted OTP row back from the schema created by Flyway.
            rs.getString("OTP"),
            rs.getTimestamp("EXPIRY_TIME").toLocalDateTime(),
            rs.getBoolean("IS_USED"),
            rs.getString("METADATA")
    );

    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiryTime;
        private final boolean isUsed;
        private final String metadataJson;

        private OtpData(String otp, LocalDateTime expiryTime, boolean isUsed, String metadataJson) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.isUsed = isUsed;
            this.metadataJson = metadataJson;
        }

        private Map<String, Object> getMetadata(ObjectMapper objectMapper) {
            try {
                if (metadataJson == null) {
                    return null;
                }
                return objectMapper.readValue(metadataJson, Map.class);
            } catch (Exception e) {
                logger.warn("Failed to parse OTP metadata JSON", e);
                return null;
            }
        }
    }
}
