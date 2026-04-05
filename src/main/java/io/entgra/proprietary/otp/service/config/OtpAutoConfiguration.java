package io.entgra.proprietary.otp.service.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;
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

@AutoConfiguration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OtpService otpService(JdbcTemplate jdbcTemplate,
                                 OtpDeliveryService otpDeliveryService,
                                 OtpProperties otpProperties,
                                 ObjectMapper objectMapper) {
        return new OtpServiceImpl(jdbcTemplate, otpDeliveryService, otpProperties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public Normalizer normalizer(OtpProperties otpProperties) {
        return new Normalizer(otpProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "otp.delivery", name = "enabled", havingValue = "true")
    public OtpDeliveryService otpDeliveryService(OtpProperties otpProperties,
                                                 ObjectMapper objectMapper,
                                                 Normalizer normalizer) {
        return new OtpDeliveryServiceImpl(otpProperties, objectMapper, normalizer);
    }
}