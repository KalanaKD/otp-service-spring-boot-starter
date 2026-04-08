package io.entgra.proprietary.otp.service.provider.impl;

public class MSSQLQueryProvider extends AbstractQueryProvider {

    // Pass the table name to the base query provider initializer.
    public MSSQLQueryProvider(String tableName) {
        super(tableName);
    }

}
