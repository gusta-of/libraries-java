package com.example.infisical.config;

import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.util.InfisicalException;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service principal para acesso a secrets do Infisical.
 *
 * <p>Registrado como bean via {@link InfisicalAutoConfiguration}.</p>
 *
 * <p>Uso típico:</p>
 * <pre>{@code
 * @Service
 * public class MeuServico {
 *
 *     private final InfisicalSecretService secrets;
 *
 *     public MeuServico(InfisicalSecretService secrets) {
 *         this.secrets = secrets;
 *     }
 *
 *     public void exemplo() {
 *         // Busca um secret específico
 *         String dbUrl = secrets.getSecret("DATABASE_URL");
 *
 *         // Busca todos como mapa
 *         Map<String, String> all = secrets.getAllSecrets();
 *
 *         // Injeta no System.getProperties() para libs legadas (HikariCP, AWS SDK, etc)
 *         secrets.injectIntoSystemProperties();
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class InfisicalSecretService {

    private final InfisicalSdk infisicalClient;
    private final InfisicalProperties properties;

    public InfisicalSecretService(InfisicalSdk infisicalClient, InfisicalProperties properties) {
        this.infisicalClient = infisicalClient;
        this.properties = properties;
    }

    /**
     * Busca um secret específico pelo nome.
     *
     * @param secretName nome do secret (ex: "DATABASE_URL")
     * @return valor do secret
     * @throws IllegalStateException se projectId não configurado
     * @throws RuntimeException se secret não encontrado (propaga do SDK)
     */
    public String getSecret(String secretName) {
        validateProjectId();

        try {
            return infisicalClient.Secrets()
                .GetSecret(secretName, properties.getProjectId(), properties.getEnvironment(),
                    "/", false, false, null)
                .getSecretValue();
        } catch (InfisicalException e) {
            throw new RuntimeException("Falha ao buscar secret '" + secretName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Busca um secret com fallback para valor padrão.
     *
     * @param secretName nome do secret
     * @param defaultValue valor se secret não existir
     * @return valor do secret ou defaultValue
     */
    public String getSecret(String secretName, @Nullable String defaultValue) {
        try {
            return getSecret(secretName);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Busca todos os secrets do projeto/ambiente configurado.
     *
     * @return mapa com todos os secrets (chave -> valor)
     */
    public Map<String, String> getAllSecrets() {
        validateProjectId();

        List<Secret> secrets;
        try {
            secrets = infisicalClient.Secrets()
                .ListSecrets(properties.getProjectId(), properties.getEnvironment(),
                    "/", false, false, false);
        } catch (InfisicalException e) {
            throw new RuntimeException("Falha ao listar secrets: " + e.getMessage(), e);
        }

        return secrets.stream()
            .collect(Collectors.toMap(Secret::getSecretKey, Secret::getSecretValue));
    }

    /**
     * Injeta todos os secrets no {@link System#getProperties()}.
     *
     * <p>Útil para bibliotecas que leem configuração via {@code System.getProperty()}
     * ou {@code System.getenv()} — como drivers JDBC (HikariCP), AWS SDK, clients HTTP, etc.</p>
     *
     * <p>Chame uma vez no startup (ex: {@code @PostConstruct} ou {@code CommandLineRunner}).</p>
     *
     * @see #getAllSecrets()
     */
    public void injectIntoSystemProperties() {
        Map<String, String> secrets = getAllSecrets();
        secrets.forEach(System::setProperty);
    }

    /**
     * Injeta secrets no {@link System#getProperties()} com prefixo customizado.
     *
     * @param prefix prefixo a adicionar (ex: "app.infisical." vira "app.infisical.DATABASE_URL")
     */
    public void injectIntoSystemProperties(String prefix) {
        Map<String, String> secrets = getAllSecrets();
        secrets.forEach((k, v) -> System.setProperty(prefix + k, v));
    }

    /**
     * Retorna o {@link InfisicalSdk} bruto para operações avançadas.
     */
    public InfisicalSdk getClient() {
        return infisicalClient;
    }

    /**
     * Retorna as propriedades de configuração ativas.
     */
    public InfisicalProperties getProperties() {
        return properties;
    }

    private void validateProjectId() {
        if (properties.getProjectId() == null || properties.getProjectId().isBlank()) {
            throw new IllegalStateException(
                "infisical.project-id não configurado. Defina via application.yml ou variável de ambiente INFISICAL_PROJECT_ID");
        }
    }
}
