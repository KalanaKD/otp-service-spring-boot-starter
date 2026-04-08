package io.entgra.proprietary.otp.service.provider;

public interface OtpQueryProvider {

    // Returns the SQL query used to insert a new OTP record into the database.
    String insertOtp();

    // Returns the SQL query used to mark an OTP as used.
    String markOtpAsUsed();

    // Returns the SQL query used to find an active OTP for validation.
    String findActiveOtp();

    // Returns the SQL query used to delete all OTP records from the table.
    String deleteAllOtps();

    // Returns the SQL query used to delete OTPs that were never used.
    String deleteUnusedOtps();

    // Returns the SQL query used to update the delivery status of an OTP.
    String updateDeliveryStatus();

    // Returns the SQL query used to remove expired or already used OTP records.
    String cleanUpExpiredOtps();

    // Returns the name of the OTP table used by the database implementation.
    String tableName();
}
