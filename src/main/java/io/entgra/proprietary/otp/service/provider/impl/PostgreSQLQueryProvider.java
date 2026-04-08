package io.entgra.proprietary.otp.service.provider.impl;

public class PostgreSQLQueryProvider extends AbstractQueryProvider{

    // Pass the table name to the base query provider initializer.
    public PostgreSQLQueryProvider(String tableName) {
        super(tableName);
    }

}
