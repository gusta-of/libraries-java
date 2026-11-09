# Infisical Local com Docker

## Início Rápido

```bash
# 1. Entre na pasta
cd docker

# 2. Copie e edite as variáveis
cp .env.example .env
# Edite .env com suas chaves (veja abaixo como gerar)

# 3. Suba os containers
docker compose up -d

# 4. Acesse
# UI: http://localhost:8080/admin   (path raiz é /admin)
# API: http://localhost:8080/api
```

## Serviços Incluídos

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **Infisical** | 8080 | UI + API |
| **PostgreSQL** | 5432 (interno) | Banco de dados principal |
| **Redis** | 6379 (interno) | Cache/sessões (obrigatório pelo Infisical) |

## Gerar Chaves Seguras

```bash
# AUTH_SECRET (qualquer tamanho, base64)
openssl rand -base64 32

# ENCRYPTION_KEY (DEVE ter exatamente 32 caracteres)
openssl rand -base64 32 | cut -c1-32

# POSTGRES_PASSWORD
openssl rand -base64 24
```

## Primeiro Acesso

1. Abra **http://localhost:8080/admin** (o path raiz `/` redireciona para `/admin`)
2. Crie sua conta (primeiro usuário vira admin)
3. Crie um **Project** → **Environment** (dev, staging, prod)
4. Adicione **Secrets** (chave/valor)
5. Gere **Client Credentials** em Project Settings → Client Credentials

## Integração Java (Spring Boot)

### 1. Dependências (Maven)

```xml
<dependency>
    <groupId>com.infisical</groupId>
    <artifactId>infisical-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configuração

```java
@Configuration
public class InfisicalConfig {

    @Value("${infisical.client-id}")
    private String clientId;

    @Value("${infisical.client-secret}")
    private String clientSecret;

    @Value("${infisical.site-url:http://localhost:8080}")
    private String siteUrl;

    @Bean
    public InfisicalClient infisicalClient() {
        return InfisicalClient.builder()
            .siteUrl(siteUrl)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build();
    }
}
```

```yaml
# application.yml
infisical:
  site-url: http://localhost:8080
  client-id: ${INFISICAL_CLIENT_ID}
  client-secret: ${INFISICAL_CLIENT_SECRET}
```

### 3. Uso

```java
@Service
public class ConfigService {

    private final InfisicalClient infisical;

    public ConfigService(InfisicalClient infisical) {
        this.infisical = infisical;
    }

    public String getDatabaseUrl() {
        return infisical.secrets().getSecret(GetSecretOptions.builder()
            .secretName("DATABASE_URL")
            .projectId("seu-project-id")
            .environment("dev")
            .build()).getSecretValue();
    }

    // Ou carrega tudo de uma vez
    public Map<String, String> loadAllSecrets() {
        return infisical.secrets().listSecrets(ListSecretsOptions.builder()
            .projectId("seu-project-id")
            .environment("dev")
            .build()).getSecrets().stream()
            .collect(Collectors.toMap(Secret::getSecretKey, Secret::getSecretValue));
    }
}
```

## Integração Java (Spring Cloud Vault - Alternativa)

Se preferir usar o ecossistema Spring nativo com Vault:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    vault:
      uri: http://localhost:8200
      token: dev-token
      kv:
        enabled: true
        backend: secret
        profile: dev
        default-context: myapp
```

## Comandos Úteis

```bash
# Ver logs
docker compose logs -f infisical

# Parar
docker compose down

# Parar e remover volumes (CUIDADO: apaga dados!)
docker compose down -v

# Backup do banco
docker exec infisical-db pg_dump -U postgres infisical > backup.sql

# Restore
cat backup.sql | docker exec -i infisical-db psql -U postgres infisical
```

## Estrutura

```
docker/
├── docker-compose.yml    # Orquestração (Infisical + PostgreSQL + Redis)
├── .env.example          # Template de variáveis
├── .env                  # Suas variáveis (não versionar!)
└── README.md             # Este arquivo
```

## Troubleshooting

| Problema | Solução |
|----------|---------|
| `ENCRYPTION_KEY must be 32 characters` | Gere com `openssl rand -base64 32 \| cut -c1-32` |
| Healthcheck falha | Aguarde 30s (start_period), verifique `docker compose logs infisical` |
| Porta 8080 ocupada | Mude em `docker-compose.yml`: `"8081:8080"` |
| Esqueci a senha do admin | `docker exec -it infisical npx prisma db seed` (reset) |
| `Either REDIS_URL... must be defined` | Redis é obrigatório — o compose já inclui o serviço `redis` |
| Redis connection refused | Verifique `docker compose logs redis` e healthcheck |