package com.example.infisical.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import com.infisical.sdk.util.InfisicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuração do InfisicalSdk.
 *
 * <p>Ativa automaticamente quando:</p>
 * <ul>
 *   <li>Infisical SDK está no classpath ({@code ConditionalOnClass})</li>
 *   <li>Propriedades {@code infisical.*} estão configuradas ({@code ConditionalOnProperty})</li>
 *   <li>Não há outro bean {@link InfisicalSdk} definido ({@code ConditionalOnMissingBean})</li>
 * </ul>
 *
 * <p>Para desabilitar: {@code infisical.enabled=false}</p>
 *
 * @see InfisicalProperties
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(InfisicalSdk.class)
@ConditionalOnProperty(prefix = "infisical", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(InfisicalProperties.class)
public class InfisicalAutoConfiguration {

    @Autowired
    private InfisicalProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public InfisicalSdk infisicalClient() {
        properties.validateRequired();

        InfisicalSdk sdk = new InfisicalSdk(
            new SdkConfig.Builder()
                .withSiteUrl(properties.getSiteUrl())
                .build());

        try {
            sdk.Auth().UniversalAuthLogin(properties.getClientId(), properties.getClientSecret());
        } catch (InfisicalException e) {
            throw new IllegalStateException("Falha ao autenticar no Infisical: " + e.getMessage(), e);
        }

        return sdk;
    }

    @Bean
    @ConditionalOnMissingBean
    public InfisicalSecretService infisicalSecretService(InfisicalSdk client, InfisicalProperties props) {
        return new InfisicalSecretService(client, props);
    }
}
