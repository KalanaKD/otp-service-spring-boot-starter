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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

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
        return switch (databaseType) {
            case MySQL -> new MySQLQueryProvider(tableName);
            case Oracle -> new OracleQueryProvider(tableName);
            case Mssql -> new MSSQLQueryProvider(tableName);
            case Postgresql -> new PostgreSQLQueryProvider(tableName);
            default -> throw new IllegalArgumentException("Unknown database type: " + databaseType);
        };
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

    @Bean
    @ConditionalOnMissingBean(type = {"org.flywaydb.core.Flyway", "liquibase.integration.spring.SpringLiquibase"})
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(prefix = "otp.schema-init", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DataSourceInitializer otpSchemaInitializer(DataSource dataSource, OtpProperties otpProperties, ResourceLoader resourceLoader) {
        OtpProperties.DatabaseType databaseType = otpProperties.getDatabaseType();
        if (databaseType == null) {
            databaseType = detectDatabaseType(dataSource);
        }
        String scriptName = switch (databaseType) {
            case MySQL -> "schema-mysql.sql";
            case Oracle -> "schema-oracle.sql";
            case Mssql -> "schema-mssql.sql";
            case Postgresql -> "schema-postgresql.sql";
        };

        // Loads the database specific schema SQL script from the classpath based on the detected/configured database type.
        Resource script = resourceLoader.getResource("classpath:otp-schema/" + scriptName);

        // Verifies the schema script.
        if (!script.exists()) {
            throw new IllegalStateException(String.format("Unable to locate schema resource '%s'", scriptName));
        }

        // Creates a schema populator instance used to execute the selected SQL script during datasource initialization.
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(false);
        populator.addScript(script);

        // Creates and binds the initializer to the current Datasource so Spring can run the OTP schema populator on application startup.
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}