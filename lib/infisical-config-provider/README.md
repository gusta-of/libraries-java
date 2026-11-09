# Infisical Config Provider

Biblioteca reutilizável para integração Spring Boot com Infisical.
Instala no seu `.m2` local e usa como dependência em qualquer projeto.

## Instalação no .m2 Local

```bash
cd lib/infisical-config-provider
mvn clean install
```

Isso instala:
- `com.example.infisical:infisical-config-provider:1.0.0-SNAPSHOT.jar`
- Sources JAR (`-sources.jar`) — para IDE mostrar código fonte
- Javadoc JAR (`-javadoc.jar`) — para documentação no IDE

## Uso em Projetos Dependentes

### 1. Adicione a dependência no `pom.xml`

```xml
<dependency>
    <groupId>com.example.infisical</groupId>
    <artifactId>infisical-config-provider</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure no `application.yml`

```yaml
infisical:
  site-url: http://localhost:8080        # default
  client-id: ${INFISICAL_CLIENT_ID}      # obrigatório
  client-secret: ${INFISICAL_CLIENT_SECRET}  # obrigatório
  project-id: ${INFISICAL_PROJECT_ID}    # obrigatório
  environment: ${INFISICAL_ENVIRONMENT:dev}  # default: dev
  enabled: true                          # default: true
```

### 3. Use no código

#### Opção A: Injeção direta do Service (recomendado)

```java
@Service
public class MeuServico {

    private final InfisicalSecretService secrets;

    public MeuServico(InfisicalSecretService secrets) {
        this.secrets = secrets;
    }

    public void conectarBanco() {
        // Busca secret específico
        String url = secrets.getSecret("DATABASE_URL");
        String user = secrets.getSecret("DATABASE_USER");
        String pass = secrets.getSecret("DATABASE_PASSWORD");

        // Ou injeta tudo no System.getProperties() para libs legadas
        secrets.injectIntoSystemProperties();
        // Agora System.getProperty("DATABASE_URL") funciona
    }
}
```

#### Opção B: Type-safe com @ConfigurationProperties

```java
@Component
@ConfigurationProperties(prefix = "infisical")
public class MinhaConfig extends InfisicalProperties {
    // Herda validação @NotBlank, defaults, helpers isComplete(), validateRequired()
}

@Service
public class OutroServico {

    private final MinhaConfig config;

    public OutroServico(MinhaConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        config.validateRequired(); // Fail-fast no startup
        String url = config.getSiteUrl(); // type-safe
    }
}
```

#### Opção C: @Value com placeholders (Spring padrão)

```java
@Component
public class LegacyIntegration {

    @Value("${infisical.client-id}")
    private String clientId;

    @Value("${infisical.project-id}")
    private String projectId;
}
```

## Auto-configuração

A biblioteca registra automaticamente (via `spring.factories`):

| Bean | Condição |
|------|----------|
| `InfisicalClient` | Se `infisical.enabled=true` (default) e propriedades completas |
| `InfisicalSecretService` | Sempre que `InfisicalClient` existir |
| `InfisicalProperties` | Sempre (bind de `infisical.*`) |

### Desabilitar auto-configuração

```yaml
infisical:
  enabled: false
```

Ou exclua no `@SpringBootApplication`:

```java
@SpringBootApplication(exclude = InfisicalAutoConfiguration.class)
public class MinhaApp { ... }
```

## Variáveis de Ambiente (Recomendado para Secrets)

Nunca commite valores reais. Use variáveis de ambiente:

```bash
export INFISICAL_CLIENT_ID="abc123..."
export INFISICAL_CLIENT_SECRET="xyz789..."
export INFISICAL_PROJECT_ID="proj-456..."
export INFISICAL_ENVIRONMENT="dev"
export INFISICAL_SITE_URL="http://localhost:8080"
```

No `application.yml` use placeholders:

```yaml
infisical:
  client-id: ${INFISICAL_CLIENT_ID}
  client-secret: ${INFISICAL_CLIENT_SECRET}
  project-id: ${INFISICAL_PROJECT_ID}
  environment: ${INFISICAL_ENVIRONMENT:dev}
  site-url: ${INFISICAL_SITE_URL:http://localhost:8080}
```

## API Principal

### InfisicalSecretService

| Método | Descrição |
|--------|-----------|
| `getSecret(String name)` | Busca um secret por nome |
| `getSecret(String name, String defaultValue)` | Busca com fallback |
| `getAllSecrets()` | Retorna `Map<String, String>` com todos |
| `injectIntoSystemProperties()` | Injeta todos em `System.getProperties()` |
| `injectIntoSystemProperties(String prefix)` | Injeta com prefixo customizado |
| `getClient()` | Acesso ao `InfisicalClient` bruto |
| `getProperties()` | Acesso ao `InfisicalProperties` |

### InfisicalProperties

| Propriedade | Default | Obrigatório |
|-------------|---------|-------------|
| `siteUrl` | `http://localhost:8080` | Não |
| `clientId` | — | **Sim** |
| `clientSecret` | — | **Sim** |
| `projectId` | — | **Sim** |
| `environment` | `dev` | Não |

| Método Helper | Descrição |
|---------------|-----------|
| `isComplete()` | Verifica se obrigatórios estão preenchidos |
| `validateRequired()` | Lança `IllegalStateException` se incompleto |

## Estrutura do Projeto

```
lib/infisical-config-provider/
├── pom.xml
└── src/main/
    ├── java/com/example/infisical/config/
    │   ├── InfisicalProperties.java          # @ConfigurationProperties
    │   ├── InfisicalAutoConfiguration.java   # @Configuration + @ConditionalOn*
    │   └── InfisicalSecretService.java       # Service principal + @Bean
    └── resources/
        └── META-INF/spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Atualizando a Dependência Local

Após mudanças no código da biblioteca:

```bash
cd lib/infisical-config-provider
mvn clean install
```

No projeto dependente, force update:

```bash
mvn dependency:purge-local-repository -DmanualInclude=com.example.infisical
# Ou apague ~/.m2/repository/com/example/infisical e rode mvn clean compile
```

## Publicando em Repositório Interno (Nexus/Artifactory)

Para compartilhar entre times, configure `distributionManagement` no pom.xml:

```xml
<distributionManagement>
    <snapshotRepository>
        <id>internal-snapshots</id>
        <url>https://nexus.seuaempresa.com/repository/maven-snapshots/</url>
    </snapshotRepository>
    <repository>
        <id>internal-releases</id>
        <url>https://nexus.seuaempresa.com/repository/maven-releases/</url>
    </repository>
</distributionManagement>
```

E deploy:

```bash
mvn clean deploy
```

## Troubleshooting

| Problema | Solução |
|----------|---------|
| `No bean named 'infisicalClient' available` | Verifique se `infisical.client-id`, `client-secret`, `project-id` estão configurados |
| `Could not resolve placeholder 'INFISICAL_CLIENT_ID'` | Exporte a variável de ambiente ou defina default no application.yml |
| `InfisicalProperties incompleto` | Chame `config.validateRequired()` no `@PostConstruct` para fail-fast claro |
| Auto-configuração não ativa | Verifique se `spring.factories` está no JAR (`jar tf target/*.jar \| grep spring.factories`) |
| IDE não mostra metadata | Rode `mvn clean install` (processador de anotação gera `META-INF/spring-configuration-metadata.json`) |