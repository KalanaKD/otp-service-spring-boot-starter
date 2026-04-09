DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'OTP_STORE';
    IF v_count = 0 THEN
            EXECUTE IMMEDIATE '
                CREATE TABLE OTP_STORE (
                    USERNAME VARCHAR2(255) NOT NULL,
                    OTP VARCHAR2(16) NOT NULL,
                    OTP_TYPE VARCHAR2(64) NOT NULL,
                    METADATA CLOB NULL,
                    EXPIRY_TIME TIMESTAMP NOT NULL,
                    DELIVERY_METHOD VARCHAR2(255) NULL,
                    DELIVERY_STATUS VARCHAR2(255) NULL,
                    IS_USED NUMBER(1) DEFAULT 0 NOT NULL
                )';
    END IF;
END;