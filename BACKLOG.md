# Backlog — StockFlow (próxima revisão)

Pendências levantadas durante a implementação do scanner de QR Code NFC-e.
Prioridade: **P0** bloqueia uso real · **P1** importante · **P2** melhoria.

## Estado atual (já feito)

- Leitura de QR por imagem: `POST /api/v1/nfce/process/image` (ZXing)
- `POST /api/v1/nfce/process` passou a receber JSON `{"qrCode": "..."}`
- Chave de 44 dígitos extraída e validada (`NfceKey`); duplicata checada antes da consulta externa
- Proteção SSRF (`FiscalUrlValidator`): HTTPS + allowlist de domínios + IP público
- `reject` do `NfceController` agora chama o `InvoiceService`
- Testes: `NfceKeyTest`, `FiscalUrlValidatorTest`, `QrCodeImageDecoderTest`

Nenhuma dessas alterações foi commitada. Também estão sem commit alterações anteriores (README, docker-compose, pom, entidades, migration V1, seed).

---

## P0 — Antes de usar com dados reais

### 1. Parser dedicado por UF
`NfceHtmlParser` usa seletores genéricos e falha em silêncio (total `ZERO`, data `now()`, colunas por posição).
- Criar interface `NfceParser` + `NfceParserRegistry` escolhendo pela UF da chave (2 primeiros dígitos)
- Um parser por estado, mantendo o atual como fallback de baixa confiança
- Ler campos por rótulo, não por posição de coluna
- Validação cruzada: soma de `quantidade × unitário` deve bater com o total; senão, recusar a nota
- Falhar com `FiscalException` quando campo obrigatório faltar, em vez de usar valor padrão
- Fixtures reais em `src/test/resources/nfce/<uf>.html`
- **Bloqueio:** precisa do HTML real de uma nota de cada estado alvo. Definir quais UFs primeiro.

### 2. Validar o caminho de consulta com uma nota real
Nenhum fluxo ponta a ponta foi exercitado (QR → SEFAZ → Invoice). Os testes atuais cobrem só as partes isoladas.
- Testar com uma nota real, de preferência pelo endpoint de imagem
- Confirmar se `.gov.br` cobre os domínios dos estados usados; senão ajustar `fiscal.allowed-host-suffixes`

### 3. Contador de notas pendentes do dashboard está errado
`InvoiceRepository.countPendingByTenantId` conta `status = 'PENDING'`, mas o fluxo de QR grava `FETCHED` (aguardando confirmação). O contador nunca sobe.
- Decidir a semântica: "pendente" = `FETCHED`? ou `PENDING` + `FETCHED`?
- Corrigir a query e cobrir com teste

---

## P1 — Robustez e segurança

### 4. Provider principal por API estruturada
Raspar HTML é frágil. Verificar na documentação oficial se Focus NFe e Nuvem Fiscal devolvem itens de NFC-e na consulta por chave.
- Se sim, tornar um deles o provider principal e deixar a SEFAZ como fallback
- Hoje `FocusNfeProvider` e `NuvemFiscalProvider` ainda fazem GET de uma URL e passam pelo mesmo parser HTML; não usam API com token. Revisar se isso faz sentido
- Na prática eles nunca são escolhidos: `supports()` exige que a URL do QR contenha `focusnfe.com.br` / `nuvemfiscal.com.br`, e QR de nota fiscal aponta para o portal da SEFAZ. Só o `SefazProvider` roda de fato
- `nuvemfiscal.com.br` não está na allowlist padrão (`.gov.br,.focusnfe.com.br`), então o `NuvemFiscalProvider` seria bloqueado pelo `FiscalUrlValidator` mesmo se fosse escolhido. Ao integrar a API, decidir se o host entra na allowlist ou se a chamada à API sai do validador (a URL da API é fixa, não vem do usuário)

### 5. SSRF: resolução de DNS em duas etapas
O validador resolve o DNS, e o `WebClient` resolve de novo ao conectar. Com a allowlist o risco é baixo, mas existe janela de DNS rebinding.
- Fixar o IP validado na conexão (resolver customizado no Reactor Netty)
- Confirmar que `followRedirect` continua desligado no `WebClientConfig`

### 6. Resposta HTML sem limite de tamanho
`SefazProvider` lê o corpo inteiro como `String`. Uma resposta enorme consome memória.
- Limitar com `maxInMemorySize` / `DataBufferUtils.join` com teto

### 7. Rate limiting
`bucket4j-core` está no pom, mas não é usado em nenhum arquivo de `src`. O upload de imagem é custoso (decodificação) e dispara chamada externa.
- Limitar por tenant em `/process` e `/process/image`

### 8. Upload de imagem
- Hoje valida pelo conteúdo (ImageIO), não pelo `Content-Type` — aceitável, mas documentar
- Considerar redimensionar imagens grandes antes de decodificar para reduzir uso de CPU/memória
- Testar com fotos reais de celular (rotação EXIF, baixa luz, QR pequeno)

### 9. Status `ERROR` nunca é usado
`InvoiceStatus.ERROR` ("erro na consulta") existe, mas falhas na consulta só lançam exceção e não deixam rastro. Avaliar registrar a tentativa falha para o usuário ver e reprocessar.

---

## P2 — Qualidade e produto

### 10. Produtos sem EAN
Muitos estabelecimentos enviam código interno em vez de EAN. `findOrCreateByEan` pode criar produtos duplicados (um por nome). Definir estratégia de casamento: por nome normalizado, por código do fornecedor ou revisão manual.

### 11. Uso dos dados da chave
`NfceKey` já decodifica UF, mês, CNPJ, modelo, série e número. Hoje só usamos chave e CNPJ.
- Rejeitar modelos diferentes de 65 (NFC-e) se for a regra do produto
- Preencher número/série da nota na `Invoice` (exige migration)

### 12. Erro de relatório vazio (reportado, sem reprodução)
Foi relatado "Erro ao carregar relatório está vazio". O repositório não tem funcionalidade de relatório; o mais próximo é `GET /api/v1/dashboard`.
- Obter: onde aparece a mensagem, endpoint/tela, log do servidor, perfil (`dev`/`prod`)
- Lembrar que o seed `V100__dev_seed.sql` só carrega no perfil `dev`

### 13. Testes de integração do fluxo NFC-e
Só existe `AuthControllerIntegrationTest` (Testcontainers). Adicionar teste do `POST /nfce/process` e `/process/image` com `FiscalService` mockado, cobrindo: chave inválida, duplicata, imagem sem QR, host fora da allowlist.

### 14. Limpeza
- `NfceController.reject` duplica o `InvoiceController.reject`; manter um só
- Teste `FiscalUrlValidatorTest.shouldAcceptAllowedHostWithoutDnsWhenLiteralUnnecessary` depende de rede e tem asserção frouxa; trocar por resolver injetável
- README: documentar a mudança de contrato do `/nfce/process` (query param → JSON body)

---

## Mudança de contrato a comunicar

`POST /api/v1/nfce/process` não aceita mais `?qrCode=...`. Agora exige `Content-Type: application/json` com `{"qrCode": "..."}`. Qualquer cliente existente precisa ser atualizado.
