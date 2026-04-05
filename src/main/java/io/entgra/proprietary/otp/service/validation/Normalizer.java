package io.entgra.proprietary.otp.service.validation;


import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;


public class Normalizer {

    private static final String DEFAULT_MOBILE_API_REGEX = "^(\\+?94|0)?7\\d{8}$";
    private static final String DEFAULT_MOBILE_GATEWAY_REGEX = "^(\\+?94)7\\d{8}$";

    private final OtpProperties otpProperties;

    public Normalizer(OtpProperties otpProperties) {
        this.otpProperties = otpProperties;
    }

    /**
     * Validates a mobile number against the SMS gateway regex.
     * This checks if the number is already in the gateway format.
     *
     * @param mobileNumber The mobile number to validate
     * @return true if the mobile number matches the gateway regex, false otherwise
     */
    public boolean isValidMobileGatewayFormat(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return false;
        }
        String gatewayRegex = getGatewayRegex();
        return mobileNumber.matches(gatewayRegex);
    }

    /**
     * Normalizes a mobile number to the SMS gateway format.
     * Converts various input formats (07, +947, 947) to the configured gateway format.
     * Reads the gateway format from configuration and normalizes accordingly.
     *
     * @param mobileNumber The mobile number to normalize
     * @return The normalized mobile number in gateway format, or null if invalid
     */
    public String normalizeToGatewayFormat(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return null;
        }

        // Remove any spaces, dashes, or other non-digit characters except +
        String cleaned = mobileNumber.replaceAll("[\\s\\-\\(\\)]", "");

        // Get the regex to determine the target format
        String apiRegex = getApiRegex();
        String gatewayRegex = getGatewayRegex();

        if (!cleaned.matches(apiRegex)) {
            return null;
        }

        if (cleaned.matches(gatewayRegex)) {
            return cleaned;
        }

        String coreDigits = extractCoreMobileNumber(cleaned);
        if (coreDigits == null || !coreDigits.matches("^7\\d{8}$")) {
            return null;
        }

        // Determine the target format based on gateway regex
        String normalized;
        if (gatewayRegex.contains("\\+94")) {
            normalized = "+94" + coreDigits;
        } else if (gatewayRegex.contains("94")) {
            normalized = "94" + coreDigits;
        } else if (gatewayRegex.contains("07")) {
            normalized = "0" + coreDigits;
        } else {
            normalized = "94" + coreDigits;
        }

        // Verify it matches the gateway regex
        if (isValidMobileGatewayFormat(normalized)) {
            return normalized;
        }
        return null;
    }

    /**
     * Extracts the core mobile number (without country code or leading zero) for consistent searching.
     * Converts formats like "077XXXXXXX", "9477XXXXXXX", "+9477XXXXXXX" to "77XXXXXXX".
     *
     * @param mobileNumber The mobile number to extract core from
     * @return The core mobile number in format "7XXXXXXXX" or null if invalid
     */
    public static String extractCoreMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return null;
        }

        // Remove any spaces, dashes, or other non-digit characters except +
        String cleaned = mobileNumber.replaceAll("[\\s\\-\\(\\)]", "");

        if (cleaned.startsWith("+94") && cleaned.length() == 12 && cleaned.matches("\\+947\\d{8}")) {
            // Format: +947XXXXXXXX -> extract 7XXXXXXXX
            return cleaned.substring(3);
        } else if (cleaned.startsWith("94") && cleaned.length() == 11 && cleaned.matches("947\\d{8}")) {
            // Format: 947XXXXXXXX -> extract 7XXXXXXXX
            return cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 10 && cleaned.matches("0\\d{9}")) {
            // Format: 07XXXXXXXX -> extract 7XXXXXXXX
            return cleaned.substring(1);
        }

        return null;
    }

    private String getApiRegex() {
        String configured = otpProperties.getMobileApiRegex();
        return (configured == null || configured.trim().isEmpty())
                ? DEFAULT_MOBILE_API_REGEX
                : configured;
    }

    private String getGatewayRegex() {
        String configured = otpProperties.getMobileGatewayRegex();
        return (configured == null || configured.trim().isEmpty())
                ? DEFAULT_MOBILE_GATEWAY_REGEX
                : configured;
    }
}