# Stockflow

Backend SaaS multi-tenant para automação de estoque a partir de **NFC-e** (nota fiscal de consumidor eletrônica). O usuário envia o conteúdo do QR Code da nota, o sistema consulta os dados fiscais, o usuário revisa e confirma — e o estoque é atualizado automaticamente.

## Como funciona

```
QR Code (texto ou imagem) ──► POST /nfce/process[/image] ──► Invoice (FETCHED) ──► confirmar ──► CONFIRMED + movimentos ENTRY no estoque
                                                      └─► rejeitar  ──► REJECTED
```

Status da nota (`InvoiceStatus`): `PENDING` → `FETCHED` → `CONFIRMED` | `REJECTED` | `ERROR`.
Tipos de movimento (`MovementType`): `ENTRY`, `EXIT`, `ADJUSTMENT`, `RETURN`.

## Stack

- Java 21, Spring Boot 3.2.5
- Spring Web, Data JPA (Hibernate), Security, Validation, Actuator, Cache, WebFlux (WebClient)
- PostgreSQL 16 + Flyway (migrations)
- JWT (jjwt) com access e refresh token; senhas com BCrypt
- MapStruct + Lombok
- jsoup para parsing do HTML da NFC-e
- Bucket4j (rate limiting)
- SpringDoc OpenAPI (Swagger UI)
- Testes: JUnit, Spring Security Test, Testcontainers (PostgreSQL)

## Arquitetura

Código em `src/main/java/com/stockflow`:

| Pacote | Responsabilidade |
|--------|------------------|
| `controller` | Endpoints REST (`/api/v1/...`) |
| `service` | Regras de negócio (auth, produtos, estoque, notas, empresa, dashboard) |
| `usecase` | Fluxos da NFC-e: `ProcessNfceUseCase`, `ConfirmInvoiceUseCase` |
| `fiscal` | Consulta fiscal: `FiscalService`, parser HTML e providers (`SEFAZ`, `FOCUS_NFE`, `NUVEM_FISCAL`) |
| `tenant` | Isolamento multi-tenant (`TenantFilter` + `TenantContext`, tenant vem do JWT) |
| `security` / `config` | JWT, Spring Security, CORS, Swagger, WebClient, `CorrelationIdFilter` |
| `domain` | Entidades, DTOs e enums |
| `repository`, `mapper`, `exception`, `utils` | Persistência, MapStruct, tratamento global de erros, utilitários (ex.: CNPJ) |

**Providers fiscais:** o `FiscalService` escolhe o provider pela URL (`focusnfe.com.br` → Focus NFe; URLs `sefaz.`/`nfce.`/`portalsped.fazenda` → SEFAZ) e usa SEFAZ como fallback.

**Segurança do QR Code:** a chave de 44 dígitos é extraída do QR, validada (dígito verificador mod 11) e usada para checar duplicidade antes de qualquer chamada externa. A URL só é consultada se for HTTPS, estiver em um domínio permitido (`fiscal.allowed-host-suffixes`, padrão `.gov.br,.focusnfe.com.br`, ou env `FISCAL_ALLOWED_HOST_SUFFIXES`) e resolver para IP público (proteção contra SSRF).

**Modelo de dados** (migrations em `src/main/resources/db/migration`): `companies`, `users`, `products`, `invoices`, `invoice_items`, `stock_movements`. Perfis de usuário: `ADMIN`, `USER`, `VIEWER`.

Cada produto guarda o usuário que o cadastrou (`products.created_by`, V4). Produtos criados automaticamente pela NFC-e ficam vinculados ao usuário que processou a nota. Produtos anteriores à V4 ficam com `created_by` nulo.

## Executando

### Docker Compose (app + PostgreSQL, perfil `dev`)

```bash
docker compose up --build
```

- API: http://localhost:8080
- PostgreSQL exposto em `localhost:54329` (user/senha/db: `stockflow`)

### Local (Maven)

Requer JDK 21 e um PostgreSQL acessível (por padrão `localhost:54329`, ex.: subindo só o serviço `postgres` do compose):

```bash
docker compose up -d postgres
./mvnw spring-boot:run      # ou: mvn spring-boot:run
```

### Testes

```bash
mvn test
```

Os testes de integração usam Testcontainers, então precisam do Docker em execução. Se o Docker estiver rodando mas o `AuthControllerIntegrationTest` falhar com `Could not find a valid Docker environment` (HTTP 400 no npipe), é incompatibilidade entre o cliente do Testcontainers e a versão atual do Docker Desktop, não erro de código. Os demais testes rodam normalmente.

## Perfis e dados de exemplo

| Perfil | Comportamento |
|--------|---------------|
| `dev` (padrão) | SQL logado, logs `DEBUG`, e carrega também `db/seed` (`V100__dev_seed.sql`) com produtos, notas e movimentos de demonstração |
| `prod` | Logs `WARN`, sem seed de desenvolvimento, métricas Prometheus desativadas |

Usuários de demonstração (tenant `Demo Company`), senha **`Demo@1234`**:

| E-mail | Perfil |
|--------|--------|
| `demo@stockflow.com` | ADMIN |
| `user@stockflow.com` | USER |
| `viewer@stockflow.com` | VIEWER |

> As senhas de demonstração são apenas para desenvolvimento.

### Banco de dev já existente após a V4

A seed `V100__dev_seed.sql` roda só no perfil `dev`. Se o banco de dev já tinha a V100 aplicada antes da V4 existir, a aplicação não sobe com `Detected resolved migration not applied to database: 4`. Não existe migration a corrigir: o banco local precisa ser recriado. Isso apaga os dados de dev, inclusive os que você tiver criado:

```bash
docker compose down -v
docker compose up -d postgres
```

Em produção não há esse problema, porque a seed não é carregada.

## Configuração

Variáveis de ambiente (veja [.env.example](.env.example)):

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SPRING_PROFILES_ACTIVE` | Perfil ativo | `dev` |
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:54329/stockflow` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciais do banco | `stockflow` / `stockflow` |
| `JWT_SECRET` | Segredo do JWT (mín. 256 bits) | valor de exemplo — **troque em produção** |
| `JWT_ACCESS_EXP_MS` | Validade do access token | `900000` (15 min) |
| `JWT_REFRESH_EXP_MS` | Validade do refresh token | `604800000` (7 dias) |

## Produção

```bash
docker build -t stockflow:1.0.0 .
cp .env.example .env     # ajuste DB_*, JWT_SECRET e APP_VERSION
docker compose -f docker-compose-prod.yml --env-file .env up -d
```

O [Dockerfile](Dockerfile) usa build multi-stage (JDK 21 → JRE 21 Alpine) e roda com usuário não-root.

## API

Documentação interativa: http://localhost:8080/swagger-ui.html (OpenAPI em `/v3/api-docs`).
Todos os endpoints exigem `Authorization: Bearer <token>`, exceto `/api/v1/auth/**`, Swagger e `/actuator/health|info`.

| Grupo | Endpoints |
|-------|-----------|
| **Auth** `/api/v1/auth` | `POST /register`, `POST /login`, `POST /refresh`, `POST /logout` |
| **Company** `/api/v1/company` | `GET`, `PUT` |
| **Products** `/api/v1/products` | `POST`, `GET` (com filtros/paginação; `mine=true` lista só os produtos cadastrados pelo usuário logado), `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| **User Profile** `/api/v1/users/me` | `GET`, `PUT` (altera `name`) |
| **NFC-e** `/api/v1/nfce` | `POST /process` (JSON `{"qrCode": "..."}`), `POST /process/image` (multipart `file`, até 5MB), `POST /{invoiceId}/confirm`, `POST /{invoiceId}/reject` |
| **Invoices** `/api/v1/invoices` | `GET`, `GET /{id}`, `POST /{id}/reject`, `DELETE /{id}` |
| **Stock Movements** `/api/v1/stock-movements` | `POST /adjust`, `GET`, `GET /product/{productId}` |
| **Dashboard** `/api/v1/dashboard` | `GET` (KPIs, produtos mais movimentados, movimentos recentes) |

### Perfil do usuário

`GET /api/v1/users/me` retorna `id`, `name`, `email` e `role` do usuário do token. `PUT /api/v1/users/me` recebe `{"name": "..."}` (obrigatório, até 100 caracteres) e devolve o perfil atualizado. O e-mail, o perfil de acesso e a senha não são alterados por este endpoint.

Esta rota fica fora de `/api/v1/auth/**` de propósito: `/auth/**` é público no `SecurityConfig`.

### Produtos vinculados ao usuário

- `POST /api/v1/products` grava o usuário do token em `createdBy`. Esse campo também aparece em `GET` e `GET /{id}`.
- `GET /api/v1/products?mine=true` lista só os produtos cadastrados pelo usuário logado. Pode ser combinado com os demais filtros e com a paginação.
- Edição e exclusão não são restritas ao criador: qualquer usuário do tenant continua podendo alterar ou excluir produtos de outros usuários.

### Exemplo rápido

```bash
# login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@stockflow.com","password":"Demo@1234"}'

# listar produtos
curl http://localhost:8080/api/v1/products -H "Authorization: Bearer <accessToken>"

# listar só os produtos cadastrados pelo usuário logado
curl "http://localhost:8080/api/v1/products?mine=true" -H "Authorization: Bearer <accessToken>"

# perfil do usuário logado
curl http://localhost:8080/api/v1/users/me -H "Authorization: Bearer <accessToken>"
```

## Observabilidade

Actuator em `/actuator` (`health`, `info`, `metrics`; `prometheus` no perfil `dev`). Cada requisição recebe um `correlationId` presente nos logs.
