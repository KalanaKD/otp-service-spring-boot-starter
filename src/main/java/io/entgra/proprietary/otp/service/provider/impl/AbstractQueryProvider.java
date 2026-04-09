package io.entgra.proprietary.otp.service.provider.impl;

import io.entgra.proprietary.otp.service.provider.OtpQueryProvider;

public abstract class AbstractQueryProvider implements OtpQueryProvider {

    // Default table name used by the library when the consumer does not override otp.table-name.
    private static final String DEFAULT_TABLE_NAME = "OTP_STORE";

    private final String tableName;

    // Initialize with the default table name.
    protected AbstractQueryProvider() {
        this(DEFAULT_TABLE_NAME);
    }

    // Use the default table name when input is null or empty, otherwise use a trimmed custom name.
    protected AbstractQueryProvider(String tableName){
        this.tableName = (tableName == null || tableName.trim().isEmpty())
                ? DEFAULT_TABLE_NAME
                : tableName.trim();
    }

    @Override
    public String insertOtp(){
        // Inserts one OTP row after generation so validation and cleanup can work against the same persisted record.
        return "INSERT INTO " + tableName() +
                " (USERNAME, OTP, OTP_TYPE, METADATA, EXPIRY_TIME, DELIVERY_METHOD, DELIVERY_STATUS, IS_USED)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    @Override
    public String markOtpAsUsed(){
        // Marks the stored OTP as consumed so the same code cannot be validated twice.
        return "UPDATE " + tableName() +
                " SET IS_USED = TRUE WHERE USERNAME = ? AND OTP_TYPE = ? AND OTP = ?";
    }
    
    @Override
    public String findActiveOtp(){
        // Finds only unexpired and unused OTPs so validation rejects stale or already-used codes.
        return "SELECT OTP, EXPIRY_TIME, IS_USED, METADATA FROM " + tableName() +
                " WHERE USERNAME = ? AND OTP_TYPE = ? AND OTP = ? AND IS_USED = FALSE AND EXPIRY_TIME > ?";
    }
    
    @Override
    public String deleteAllOtps(){
        // Removes all existing OTPs for a user/type before issuing a fresh one.
        return "DELETE FROM " + tableName() + " WHERE USERNAME = ? AND OTP_TYPE = ?";
    }
    
    @Override
    public String deleteUnusedOtps(){
        // Clears only unused OTPs so a new code can replace older pending records.
        return "DELETE FROM " + tableName() + " WHERE USERNAME = ? AND OTP_TYPE = ? AND IS_USED = FALSE";
    }
    
    @Override
    public String updateDeliveryStatus(){
        // Persists delivery result metadata so the consumer can inspect which channel succeeded or failed.
        return "UPDATE " + tableName() + " SET DELIVERY_STATUS = ? WHERE USERNAME = ? AND OTP_TYPE = ?";
    }
    
    @Override
    public String cleanUpExpiredOtps(){
        // Removes expired or already-used records so the OTP_STORE table does not grow forever.
        return "DELETE FROM " + tableName() + " WHERE EXPIRY_TIME < ? OR IS_USED = TRUE";
    }
    
    @Override
    public String tableName(){
        return tableName;
    }
}
