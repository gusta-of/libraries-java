package com.example.infisical.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Propriedades de configuração do Infisical.
 *
 * <p>Uso em projetos dependentes:</p>
 * <pre>{@code
 * @Component
 * @ConfigurationProperties(prefix = "infisical")
 * public class MinhaConfig extends InfisicalProperties {
 *     // herda validação e defaults
 * }
 * }</pre>
 *
 * <p>Ou injeção direta:</p>
 * <pre>{@code
 * @Autowired
 * private InfisicalProperties infisicalProps;
 * }</pre>
 *
 * <p>Variáveis de ambiente suportadas (Spring Boot relaxed binding):</p>
 * <ul>
 *   <li>{@code INFISICAL_SITE_URL} ou {@code infisical.site-url}</li>
 *   <li>{@code INFISICAL_CLIENT_ID} ou {@code infisical.client-id}</li>
 *   <li>{@code INFISICAL_CLIENT_SECRET} ou {@code infisical.client-secret}</li>
 *   <li>{@code INFISICAL_PROJECT_ID} ou {@code infisical.project-id}</li>
 *   <li>{@code INFISICAL_ENVIRONMENT} ou {@code infisical.environment}</li>
 * </ul>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "infisical")
@Validated
public class InfisicalProperties {

    /**
     * URL base do servidor Infisical.
     * <p>Default: {@code http://localhost:8080}</p>
     */
    @NotBlank
    private String siteUrl = "http://localhost:8080";

    /**
     * Client ID gerado no Infisical (Project Settings → Client Credentials).
     * <p><b>Obrigatório</b> - não tem default. Configure via variável de ambiente {@code INFISICAL_CLIENT_ID}.</p>
     */
    @NotBlank
    private String clientId;

    /**
     * Client Secret gerado no Infisical.
     * <p><b>Obrigatório</b> - não tem default. Configure via variável de ambiente {@code INFISICAL_CLIENT_SECRET}.</p>
     */
    @NotBlank
    private String clientSecret;

    /**
     * ID do projeto no Infisical.
     * <p>Encontrado na URL: {@code https://app.infisical.com/project/<PROJECT_ID>/settings}</p>
     * <p><b>Obrigatório</b> - não tem default. Configure via variável de ambiente {@code INFISICAL_PROJECT_ID}.</p>
     */
    @NotBlank
    private String projectId;

    /**
     * Ambiente a ser usado (dev, staging, prod, etc).
     * <p>Default: {@code dev}</p>
     */
    @NotBlank
    private String environment = "dev";

    // ============================================
    // Getters e Setters
    // ============================================

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    // ============================================
    // Helpers
    // ============================================

    /**
     * Verifica se todas as propriedades obrigatórias estão preenchidas.
     *
     * @return true se clientId, clientSecret e projectId não estão vazios
     */
    public boolean isComplete() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank()
            && projectId != null && !projectId.isBlank();
    }

    /**
     * Lança exceção se configuração incompleta.
     * Útil para fail-fast no startup.
     *
     * @throws IllegalStateException se faltar alguma propriedade obrigatória
     */
    public void validateRequired() {
        if (!isComplete()) {
            StringBuilder missing = new StringBuilder();
            if (clientId == null || clientId.isBlank()) missing.append("clientId, ");
            if (clientSecret == null || clientSecret.isBlank()) missing.append("clientSecret, ");
            if (projectId == null || projectId.isBlank()) missing.append("projectId, ");
            throw new IllegalStateException(
                "InfisicalProperties incompleto - faltando: " + missing.substring(0, missing.length() - 2));
        }
    }
}