package io.entgra.proprietary.otp.service.provider.impl;

import io.entgra.proprietary.otp.service.provider.OtpQueryProvider;

public abstract class AbstractQueryProvider implements OtpQueryProvider {

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
        return "INSERT INTO " + tableName() +
                " (USERNAME, OTP, OTP_TYPE, METADATA, EXPIRY_TIME, DELIVERY_METHOD, DELIVERY_STATUS, IS_USED)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    @Override
    public String markOtpAsUsed(){
        return "UPDATE " + tableName() +
                " SET IS_USED = TRUE WHERE USERNAME = ? AND OTP_TYPE = ? AND OTP = ?";
    }
    
    @Override
    public String findActiveOtp(){
        return "SELECT OTP, EXPIRY_TIME, IS_USED, METADATA FROM " + tableName() +
                " WHERE USERNAME = ? AND OTP_TYPE = ? AND OTP = ? AND IS_USED = FALSE AND EXPIRY_TIME > ?";
    }
    
    @Override
    public String deleteAllOtps(){
        return "DELETE FROM " + tableName() + " WHERE USERNAME = ? AND OTP_TYPE = ?";
    }
    
    @Override
    public String deleteUnusedOtps(){
        return "DELETE FROM " + tableName() + " WHERE USERNAME = ? AND OTP_TYPE = ? AND IS_USED = FALSE";
    }
    
    @Override
    public String updateDeliveryStatus(){
        return "UPDATE " + tableName() + " SET DELIVERY_STATUS = ? WHERE USERNAME = ? AND OTP_TYPE = ?";
    }
    
    @Override
    public String cleanUpExpiredOtps(){
        return "DELETE FROM " + tableName() + " WHERE EXPIRY_TIME < ? OR IS_USED = TRUE";
    }
    
    @Override
    public String tableName(){
        return tableName;
    }
}
