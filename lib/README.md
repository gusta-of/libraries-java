# lib

Bibliotecas reutilizáveis compartilhadas entre os projetos. Cada subpasta é um módulo Maven independente, instalado no repositório local com `mvn clean install` e consumido como dependência pelos demais projetos.

| Módulo | Descrição |
|--------|-----------|
| [`infisical-config-provider`](#infisical-config-provider) | Integração com o Infisical (gerenciador de secrets) para aplicações Spring Boot |

---

## infisical-config-provider

Biblioteca que encapsula o [SDK Java do Infisical](https://central.sonatype.com/artifact/com.infisical/sdk) (`com.infisical:sdk`) e o expõe como componente do Spring Boot. A aplicação que a consome não constrói nem autentica o cliente: basta adicionar a dependência, configurar as propriedades `infisical.*` e injetar o serviço.

### Padrão de projeto: Spring Boot Auto-Configuration (estilo *starter*)

A biblioteca segue o padrão de **auto-configuração do Spring Boot**, o mesmo usado pelos `spring-boot-starter-*`. A ideia central é inverter a responsabilidade: quem configura os beans é a própria biblioteca, ativada por convenção e condições, e não a aplicação.

```
 aplicação ──depende de──▶ infisical-config-provider
                                  │
   META-INF/spring/...AutoConfiguration.imports
                                  │  (Spring Boot descobre no startup)
                                  ▼
                       InfisicalAutoConfiguration
                         ├─ lê  ──▶ InfisicalProperties   (infisical.*)
                         └─ cria ─▶ InfisicalSdk          (autenticado)
                                          ▲
                                          │ usa
                              InfisicalSecretService      (fachada)
```

Ele é composto por quatro peças, cada uma com um papel de padrão bem definido:

#### 1. Registro por convenção: `AutoConfiguration.imports`

O arquivo `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` lista a classe `InfisicalAutoConfiguration`. É assim que o Spring Boot 3.x descobre a configuração da biblioteca no classpath, sem `@ComponentScan` nem `@Import` na aplicação consumidora.

#### 2. Configuração condicional: `InfisicalAutoConfiguration`

Fábrica (*Factory*) do bean `InfisicalSdk`. O comportamento é controlado por condições:

| Condição | Efeito |
|----------|--------|
| `@ConditionalOnClass(InfisicalSdk.class)` | Só ativa se o SDK estiver no classpath |
| `@ConditionalOnProperty(infisical.enabled, matchIfMissing = true)` | Ativa por padrão; `infisical.enabled=false` desliga tudo |
| `@ConditionalOnMissingBean` | Se a aplicação definir o próprio `InfisicalSdk`, o da biblioteca recua (ponto de extensão) |

O método `infisicalClient()` valida as propriedades (*fail-fast*), instancia o `InfisicalSdk` com `SdkConfig.Builder` e faz o login via Universal Auth (`clientId`/`clientSecret`) antes de devolver o bean já autenticado.

#### 3. Configuração tipada: `InfisicalProperties`

Padrão **Typed Configuration Properties** (`@ConfigurationProperties(prefix = "infisical")` + `@Validated`). Em vez de espalhar `@Value` pelo código, todas as opções ficam em um único objeto tipado, com validação Bean Validation (`@NotBlank`) e valores padrão.

| Propriedade | Obrigatória | Padrão |
|-------------|-------------|--------|
| `infisical.site-url` | não | `http://localhost:8080` |
| `infisical.client-id` | sim | — |
| `infisical.client-secret` | sim | — |
| `infisical.project-id` | sim | — |
| `infisical.environment` | não | `dev` |
| `infisical.enabled` | não | `true` |

Além da validação declarativa, `validateRequired()` falha no startup com uma mensagem listando exatamente o que está faltando (*fail-fast*), em vez de errar mais tarde na primeira chamada.

O `spring-boot-configuration-processor` gera o `spring-configuration-metadata.json`, que dá autocomplete e documentação das propriedades na IDE da aplicação consumidora.

#### 4. Fachada: `InfisicalSecretService`

Padrão **Facade** sobre o SDK. Esconde a API de baixo nível (`Secrets().GetSecret(...)`, `ListSecrets(...)`, exceções verificadas, `projectId`/`environment`/`path` repetidos a cada chamada) atrás de uma interface pequena:

| Método | O que faz |
|--------|-----------|
| `getSecret(nome)` | Busca um secret; propaga erro se não existir |
| `getSecret(nome, padrao)` | Busca com valor padrão em caso de falha |
| `getAllSecrets()` | Todos os secrets do projeto/ambiente como `Map<String, String>` |
| `injectIntoSystemProperties()` | Copia os secrets para `System.getProperties()` (útil para HikariCP, AWS SDK etc.) |
| `injectIntoSystemProperties(prefixo)` | Idem, com prefixo |
| `getClient()` | Devolve o `InfisicalSdk` bruto para operações avançadas |

A fachada traduz `InfisicalException` (verificada) em `RuntimeException`, mais adequada ao estilo Spring.

### Decisões de projeto

- **Convenção sobre configuração:** adicionar a dependência e as propriedades é suficiente; nenhum código de integração na aplicação.
- **Substituível:** `@ConditionalOnMissingBean` permite sobrescrever qualquer bean sem alterar a biblioteca.
- **Não vaza dependências:** Lombok e o configuration-processor são `optional`, então não são transitivos para os consumidores.
- **Falha cedo:** configuração incompleta derruba o startup com mensagem clara.

### Uso

1. Instale a biblioteca (veja a seção de build abaixo) e adicione a dependência:

```xml
<dependency>
    <groupId>com.example.infisical</groupId>
    <artifactId>infisical-config-provider</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. Configure no `application.yml`, usando variáveis de ambiente para as credenciais (nunca valores reais no repositório):

```yaml
infisical:
  site-url: http://localhost:8080
  client-id: ${INFISICAL_CLIENT_ID}
  client-secret: ${INFISICAL_CLIENT_SECRET}
  project-id: ${INFISICAL_PROJECT_ID}
  environment: dev
```

3. Injete o serviço:

```java
@Service
public class MeuServico {

    private final InfisicalSecretService secrets;

    public MeuServico(InfisicalSecretService secrets) {
        this.secrets = secrets;
    }

    public void exemplo() {
        String dbUrl = secrets.getSecret("DATABASE_URL");
        secrets.injectIntoSystemProperties();
    }
}
```

### Build

Requer JDK 21 ou superior (o bytecode gerado é Java 21) e Maven. Na máquina de desenvolvimento, o `mvn2` usa o Maven 3.10 com o JDK 25 e o Maven Central, sem passar pelo Nexus interno:

```bash
cd lib/infisical-config-provider
mvn2 clean install
```

Observações:

- O Lombok precisa ser 1.18.42 ou superior para compilar com JDK 25.
- O artefato é instalado no repositório local usado pelo Maven que rodou o build. Com o `mvn2`, é o `~/.m2/repository-central`.

### Pontos de atenção

- **`InfisicalSecretService` não está registrado no `AutoConfiguration.imports`.** Hoje o arquivo lista só `InfisicalAutoConfiguration`, e o serviço é uma `@Configuration` em outra classe. Em uma aplicação em outro pacote ele não é encontrado por *component scan*, então o `InfisicalSecretService` só estará disponível se a aplicação o importar (`@Import`) ou se o registro for adicionado ao `.imports`. O ideal é declarar o `@Bean` do serviço dentro de `InfisicalAutoConfiguration`.
- **API do SDK:** a migração para o `com.infisical:sdk:3.0.8` mudou os tipos (`InfisicalClient` virou `InfisicalSdk`). `getClient()` agora devolve `InfisicalSdk`.
