package io.entgra.proprietary.otp.service.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;
import io.entgra.proprietary.otp.service.provider.OtpQueryProvider;
import io.entgra.proprietary.otp.service.provider.impl.MSSQLQueryProvider;
import io.entgra.proprietary.otp.service.provider.impl.MySQLQueryProvider;
import io.entgra.proprietary.otp.service.provider.impl.OracleQueryProvider;
import io.entgra.proprietary.otp.service.provider.impl.PostgreSQLQueryProvider;
import io.entgra.proprietary.otp.service.service.OtpDeliveryService;
import io.entgra.proprietary.otp.service.service.OtpService;
import io.entgra.proprietary.otp.service.service.impl.OtpDeliveryServiceImpl;
import io.entgra.proprietary.otp.service.service.impl.OtpServiceImpl;
import io.entgra.proprietary.otp.service.validation.Normalizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@AutoConfiguration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OtpService otpService(JdbcTemplate jdbcTemplate,
                                 OtpDeliveryService otpDeliveryService,
                                 OtpProperties otpProperties,
                                 ObjectMapper objectMapper,
                                 OtpQueryProvider otpQueryProvider) {
        return new OtpServiceImpl(jdbcTemplate, otpDeliveryService, otpProperties, objectMapper, otpQueryProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public Normalizer normalizer(OtpProperties otpProperties) {
        return new Normalizer(otpProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    // Creates the OTP delivery service bean only if 'otp.delivery.enabled=true' (or if the property is missing, since matchIfMissing=true).
    // Allows enabling/disabling OTP delivery via configuration.
    @ConditionalOnProperty(prefix = "otp.delivery", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OtpDeliveryService otpDeliveryService(OtpProperties otpProperties,
                                                 ObjectMapper objectMapper,
                                                 Normalizer normalizer) {
        return new OtpDeliveryServiceImpl(otpProperties, objectMapper, normalizer);
    }

    @Bean
    @ConditionalOnMissingBean
    public OtpQueryProvider otpQueryProvider(DataSource dataSource,
                                             OtpProperties otpProperties) {
        OtpProperties.DatabaseType databaseType = otpProperties.getDatabaseType();
        if (databaseType == null) {
            databaseType = detectDatabaseType(dataSource);
        }
        String tableName = otpProperties.getTableName();
        switch (databaseType) {
            case MySQL:
                return new MySQLQueryProvider(tableName);
            case Oracle:
                return new OracleQueryProvider(tableName);
            case Mssql:
                return new MSSQLQueryProvider(tableName);
            case Postgresql:
                return new PostgreSQLQueryProvider(tableName);
            default:
                throw new IllegalArgumentException("Unknown database type: " + databaseType);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public OtpProperties.DatabaseType detectDatabaseType(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()){
            String productName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (productName == null) {
                throw new RuntimeException("Unable to detect database type : product name is null");
            }
            if (productName.contains("mysql")) {
                return OtpProperties.DatabaseType.MySQL;
            }
            if (productName.contains("oracle")) {
                return OtpProperties.DatabaseType.Oracle;
            }
            if (productName.contains("postgres")) {
                return OtpProperties.DatabaseType.Postgresql;
            }
            if (productName.contains("microsoft sql server")) {
                return OtpProperties.DatabaseType.Mssql;
            }
            throw new IllegalStateException("Unsupported database type : " + productName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to detect Database type.",e);
        }
    }
}