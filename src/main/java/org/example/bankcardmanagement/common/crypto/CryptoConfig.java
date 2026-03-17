package org.example.bankcardmanagement.common.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CardCryptoProperties.class)
public class CryptoConfig {
}
