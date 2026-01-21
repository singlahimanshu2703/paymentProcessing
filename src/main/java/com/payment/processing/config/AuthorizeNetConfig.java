package com.payment.processing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "authorize-net")
public class AuthorizeNetConfig {
    private String apiLoginId;
    private String transactionKey;
    private boolean sandbox = true;
}
