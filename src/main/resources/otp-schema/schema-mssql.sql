IF OBJECT_ID('OTP_STORE', 'U') IS NULL
BEGIN
CREATE TABLE OTP_STORE (
                           USERNAME VARCHAR(255) NOT NULL,
                           OTP VARCHAR(16) NOT NULL,
                           OTP_TYPE VARCHAR(64) NOT NULL,
                           METADATA NVARCHAR(MAX) NULL,
                           EXPIRY_TIME DATETIME2 NOT NULL,
                           DELIVERY_METHOD VARCHAR(255) NULL,
                           DELIVERY_STATUS VARCHAR(255) NULL,
                           IS_USED BIT NOT NULL DEFAULT 0
);
END;
GO