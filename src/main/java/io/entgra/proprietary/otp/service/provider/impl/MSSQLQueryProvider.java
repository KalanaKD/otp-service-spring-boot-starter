package io.entgra.proprietary.otp.service.provider.impl;

public class MSSQLQueryProvider extends AbstractQueryProvider {

    // Pass the table name to the base query provider initializer.
    public MSSQLQueryProvider(String tableName) {
        super(tableName);
    }

    @Override
    public String markOtpAsUsed() {
        return "UPDATE " + tableName() +
                " SET IS_USED = 1 WHERE USERNAME = ? AND OTP_TYPE = ? AND OTP = ?";
    }

    @Override
    public String findActiveOtp() {
        return "SELECT OTP, EXPIRY_TIME, IS_USED, METADATA FROM " + tableName() +
                " WHERE USERNAME = ? AND OTP_TYPE = ? AND OTP = ? AND IS_USED = 0 AND EXPIRY_TIME > ?";
    }

    @Override
    public String deleteUnusedOtps() {
        return "DELETE FROM " + tableName() +
                " WHERE USERNAME = ? AND OTP_TYPE = ? AND IS_USED = 0";
    }

    @Override
    public String cleanUpExpiredOtps() {
        return "DELETE FROM " + tableName() +
                " WHERE EXPIRY_TIME < ? OR IS_USED = 1";
    }

}
