---
name: stockflow
description: Atuação no projeto Stockflow/HomeStock (backend Spring Boot 3.2.5 SaaS multi-tenant de estoque via NFC-e). Use ao criar ou alterar código, endpoints, entidades, migrations, testes, fluxo fiscal (QR Code/SEFAZ), segurança/JWT, ou ao escolher a próxima tarefa do BACKLOG.md.
---

# Stockflow — regras de atuação no projeto

Backend SaaS multi-tenant: o usuário envia o QR Code de uma NFC-e, o sistema consulta os dados fiscais, o usuário revisa e confirma, e o estoque é atualizado com movimentos `ENTRY`.

Fonte de verdade para o estado do projeto: [README.md](../../../README.md) (como rodar, API, perfis) e [BACKLOG.md](../../../BACKLOG.md) (pendências P0/P1/P2). Antes de propor trabalho novo, confira se já está no backlog.

## Stack e comandos

- Java 21, Spring Boot 3.2.5, Maven (não há `mvnw` no repositório; use `mvn`).
- PostgreSQL 16 via Docker Compose, porta **54329** (`docker compose up -d postgres`).
- Rodar: `mvn spring-boot:run` (perfil `dev` por padrão).
- Testes: `mvn test`. Os testes de integração usam Testcontainers e **exigem Docker rodando**; se o Docker estiver parado, diga isso em vez de tratar a falha como bug de código.
- Build de produção: `docker build -t stockflow:1.0.0 .`

Antes de dizer que algo funciona, rode `mvn test` (ou o teste específico) e reporte a saída real.

## Arquitetura — onde colocar cada coisa

Código em `src/main/java/com/stockflow`:

| Pacote | Uso |
|---|---|
| `controller` | REST em `/api/v1/...`. Só recebe a requisição, extrai o `tenantId` via `SecurityUtils` e delega ao service. |
| `service` | Regras de negócio CRUD (Auth, Product, Company, Invoice, StockMovement, Dashboard). |
| `usecase` | Fluxos que cruzam serviços: `ProcessNfceUseCase` (QR → Invoice `FETCHED`), `ConfirmInvoiceUseCase` (→ `CONFIRMED` + movimentos). |
| `fiscal` | Consulta fiscal: `FiscalService`, `NfceKey`, `FiscalUrlValidator` (SSRF), `QrCodeImageDecoder` (ZXing), parser jsoup, providers (SEFAZ, Focus NFe, Nuvem Fiscal). |
| `domain/entity`, `domain/dto`, `domain/enums` | Entidades JPA (herdam `BaseEntity`), DTOs em **records**, enums. |
| `repository` | Spring Data JPA; `specification/ProductSpecification` para filtros dinâmicos. |
| `mapper` | MapStruct. Não escreva conversão manual se houver mapper. |
| `security`, `config` | JWT, Spring Security (stateless), CORS, Swagger, WebClient, `CorrelationIdFilter`. |
| `tenant` | `TenantFilter` + `TenantContext` (ThreadLocal). |
| `exception` | `BusinessException(msg, status)`, `ResourceNotFoundException`, `DuplicateResourceException`, `FiscalException`; tratamento central em `GlobalExceptionHandler`. |

Fluxo de dependência: controller → service/usecase → repository. Controller não acessa repository.

## Convenções obrigatórias

**Multi-tenancy (crítico):**
- Todo acesso a dado de negócio filtra por `tenantId`. Ex.: `findByIdAndTenantIdAndDeletedAtIsNull`.
- O `tenantId` vem sempre do JWT (`SecurityUtils.getCurrentTenantId(jwtTokenProvider, request)`), nunca do body, query param ou path.
- Ao criar entidade, defina `tenantId` explicitamente (ver `ProductService.create`).
- Nunca retorne um recurso de outro tenant; prefira `ResourceNotFoundException` (404) a 403, para não vazar existência.

**Soft delete:** entidades usam `deletedAt` (`softDelete()`). Queries de leitura incluem `DeletedAtIsNull`. `DELETE` não remove a linha.

**Respostas HTTP:** corpos de sucesso e erro usam `ApiResponseDTO<T>` (`ApiResponseDTO.ok(...)` / `error(...)`). Exceção: `DELETE` retorna `204 No Content` sem corpo. Erros de negócio devem lançar `BusinessException` com o status HTTP correto, não retornar `ResponseEntity` de erro manualmente.

**Entidades:**
- Use `@SuperBuilder` + `@NoArgsConstructor` (não `@Builder` simples, quebra a herança de `BaseEntity`). Não use `@AllArgsConstructor` nelas.
- Colunas de texto de tamanho fixo: `length = n`, **não** `columnDefinition = "CHAR(n)"` (quebra `ddl-auto: validate` no PostgreSQL).
- `ddl-auto` é `validate` em todos os perfis: o **Flyway é dono do schema**. Mudou entidade? Crie migration.

**Migrations:**
- Não edite `V1`, `V2`, `V3` (já aplicadas). Crie `V4__descricao.sql` (próximo número livre) em `src/main/resources/db/migration`.
- Seed de desenvolvimento vai em `db/seed` (`V100__dev_seed.sql`), só carregado no perfil `dev`. Não coloque dado de demo em `db/migration`.

**DTOs e validação:** records com Bean Validation (`@Valid` no controller). Use DTOs de request/response; não exponha entidades JPA na API.

**Logs:** SLF4J via `@Slf4j`, com IDs (tenantId, invoiceKey) e sem dados sensíveis. Nunca logue senha, token, `JWT_SECRET` ou conteúdo completo de QR com credenciais.

**Idioma:** código e comentários técnicos em inglês (padrão atual dos arquivos); README, BACKLOG e mensagens de negócio em português. Mantenha o estilo do arquivo vizinho.

## Domínio NFC-e (invariantes)

- Status da nota (`InvoiceStatus`): `PENDING` → `FETCHED` → `CONFIRMED` | `REJECTED` | `ERROR`. Confirmar só é permitido a partir de `FETCHED` (senão `422`).
- Tipos de movimento (`MovementType`): `ENTRY`, `EXIT`, `ADJUSTMENT`, `RETURN`. A confirmação gera `ENTRY` por item, com referência `INVOICE#<id>`.
- A chave de 44 dígitos do QR é **autoritativa**: é validada (`NfceKey.parse`, dígito verificador mod 11) e checada contra duplicidade **antes** de qualquer chamada externa. Preserve essa ordem.
- Consulta externa só para URL HTTPS, em host permitido (`fiscal.allowed-host-suffixes`, padrão `.gov.br,.focusnfe.com.br`) e que resolva para IP público (SSRF). Não relaxe `FiscalUrlValidator` nem ligue `followRedirect` no `WebClientConfig`.
- Produto sem EAN: `findOrCreateByEan` cria produto automaticamente. Esse ponto tem pendência conhecida (BACKLOG item 10).
- Contrato atual: `POST /api/v1/nfce/process` recebe JSON `{"qrCode": "..."}` (não mais query param). `POST /process/image` recebe multipart `file` até 5 MB.

## Segurança

- Stateless, JWT (jjwt 0.12.5): access 15 min, refresh 7 dias. Senhas com BCrypt.
- Rotas públicas: só `/api/v1/auth/**`, Swagger e `/actuator/health|info`. Qualquer endpoint novo fica autenticado por padrão; mantenha assim.
- Não adicione segredo real em arquivo versionado. Segredos vão em variável de ambiente (`.env`, ignorado pelo git).
- A senha `Demo@1234` no README é só de desenvolvimento (tenant `Demo Company`). Não a reutilize em produção nem em testes de outro contexto.

## Como trabalhar nas tarefas

1. **Escolha de tarefa:** se o pedido for vago ("melhora isso"), olhe o BACKLOG.md e proponha a menor tarefa P0/P1 que resolve o pedido. Não execute itens P2 sem pedido.
2. **Bloqueios conhecidos:** o parser por UF (BACKLOG 1) depende de HTML real de notas; sem fixture, não invente seletores nem HTML de exemplo que pareça oficial.
3. **Mudança mínima:** siga o padrão do pacote existente antes de criar abstração nova. Não refatore fora do escopo.
4. **Testes:** toda regra nova em service/usecase ganha teste unitário (JUnit 5 + Mockito, como `ProductServiceTest`). Mudança de endpoint ou de schema pede teste de integração ou verificação manual (Swagger em `http://localhost:8080/swagger-ui.html`).
5. **Verificação antes de concluir:** `mvn test` passando (ou o motivo real de não rodar, como Docker parado). Se não puder rodar, diga explicitamente que a verificação foi só por leitura.
6. **Docs:** mudança de contrato da API ou de comportamento de configuração atualiza o README. Pendência resolvida sai do BACKLOG.md.

## Não mexer

- `target/`, `bin/` e `src/bin/`: artefatos de build antigos, ignorados pelo git. Não edite nem use como referência.
- `.idea/`, `.env`: configuração local do IDE e segredos.
- Migrations `V1`–`V3` já aplicadas.

## Git

- Histórico usa Conventional Commits em minúsculas (`feat(nfce): ...`, `fix(db): ...`, `docs: ...`, `chore(dev): ...`).
- Não faça commit nem push sem pedido explícito. Se estiver na `main`, crie branch antes de qualquer commit.
