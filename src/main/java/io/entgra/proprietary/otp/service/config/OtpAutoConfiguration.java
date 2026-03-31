package io.entgra.proprietary.otp.service.config;


import io.entgra.proprietary.otp.service.config.Properties.OtpProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(OtpProperties.class)
public class OtpAutoConfiguration {

}
