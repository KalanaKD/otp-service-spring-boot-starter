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
import org.flywaydb.core.api.Location;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpAutoConfiguration {

    private static final String DEFAULT_OTP_TABLE_NAME = "OTP_STORE";
    private static final String FLYWAY_OTP_TABLE_PLACEHOLDER = "otp_table_name";

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
    // Chooses the right SQL query provider so JDBC operations target the correct OTP_STORE shape for the active database.
    public OtpQueryProvider otpQueryProvider(DataSource dataSource,
                                             OtpProperties otpProperties) {
        OtpProperties.DatabaseType databaseType = resolveDatabaseType(dataSource, otpProperties);
        String tableName = resolveTableName(otpProperties);
        return switch (databaseType) {
            case MySQL -> new MySQLQueryProvider(tableName);
            case Oracle -> new OracleQueryProvider(tableName);
            case Mssql -> new MSSQLQueryProvider(tableName);
            case Postgresql -> new PostgreSQLQueryProvider(tableName);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    // Tells Flyway which vendor folder to scan so the consumer app executes only the OTP migration for its database.
    public FlywayConfigurationCustomizer otpFlywayConfigurationCustomizer(DataSource dataSource,
                                                                          OtpProperties otpProperties) {
        OtpProperties.DatabaseType databaseType = resolveDatabaseType(dataSource, otpProperties);
        String tableName = resolveTableName(otpProperties);
        return configuration -> configuration
                .locations(flywayLocations(databaseType))
                // Keeps Flyway DDL and runtime query provider aligned when otp.table-name is overridden.
                .placeholders(Map.of(FLYWAY_OTP_TABLE_PLACEHOLDER, tableName));
    }

    private OtpProperties.DatabaseType resolveDatabaseType(DataSource dataSource, OtpProperties otpProperties) {
        OtpProperties.DatabaseType databaseType = otpProperties.getDatabaseType();
        if (databaseType != null) {
            return databaseType;
        }
        return detectDatabaseType(dataSource);
    }

    private String resolveTableName(OtpProperties otpProperties) {
        String configured = otpProperties.getTableName();
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_OTP_TABLE_NAME;
        }
        return configured;
    }

    private OtpProperties.DatabaseType detectDatabaseType(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            // Detect the database vendor from JDBC metadata so the starter can select the right SQL and Flyway path automatically.
            String rawProductName = conn.getMetaData().getDatabaseProductName();
            if (rawProductName == null || rawProductName.trim().isEmpty()) {
                throw new RuntimeException("Unable to detect database type: product name is null or empty");
            }
            // Normalize the product name to lowercase so vendor checks are case-insensitive across jdbc drivers.
            String productName = rawProductName.toLowerCase();
            if (productName.contains("mysql")) {
                return OtpProperties.DatabaseType.MySQL;
            }
            if (productName.contains("oracle")) {
                return OtpProperties.DatabaseType.Oracle;
            }
            if (productName.contains("microsoft sql server")) {
                return OtpProperties.DatabaseType.Mssql;
            }
            if (productName.contains("postgres")) {
                return OtpProperties.DatabaseType.Postgresql;
            }
            throw new IllegalStateException("Unsupported database type: " + productName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to detect database type.", e);
        }
    }

    // Maps each supported database to the matching Flyway migration folder under src/main/resources/db/migration/.
    private Location[] flywayLocations(OtpProperties.DatabaseType databaseType) {
        return switch (databaseType) {
            case MySQL -> new Location[]{new Location("classpath:db/migration/mysql")};
            case Oracle -> new Location[]{new Location("classpath:db/migration/oracle")};
            case Mssql -> new Location[]{new Location("classpath:db/migration/sqlserver")};
            case Postgresql -> new Location[]{new Location("classpath:db/migration/postgresql")};
        };
    }

}