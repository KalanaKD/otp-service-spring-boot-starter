package io.entgra.proprietary.otp.service.provider.impl;

public class MySQLQueryProvider extends AbstractQueryProvider{

    // Pass the table name to the base query provider initializer.
    public MySQLQueryProvider(String tableName) {
        super(tableName);
    }

}
