---
status: done
stepsCompleted:
  - clarify
  - plan
  - implement
  - review
followup_review_recommended: false
deferred:
  - summary: Confirmar env vars EMAIL_TEMPLATE_IA_PATH e EMAIL_CV_IA_PATH no compose da VPS
    evidence: Documentadas no README; o compose da VPS fica fora deste repositório
    location: deploy / docker-compose VPS
    severity: low
---

# Feature: anexo de CV de IA (`vagaIA`)

**Projeto:** email-sender  
**Arquivo:** `feature_anexo_cv_ia.md` (substitui `spec.md`)  
**Abordagem:** Spec Driven Design + estudo da solução atual  
**Status:** done  
**Autor:** Guilherme Augusto Goettnauer

<intent-contract>
Campo obrigatório `vagaIA` em `POST /api/emails/send`: `false` usa template/CV padrão; `true` usa `email-body-ia.html` e `CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`. Sem fallback entre perfis. Assunto continua no cliente.
</intent-contract>

---

## 0. Resumo executivo

Hoje `POST /api/emails/send` monta **sempre** o mesmo HTML e o mesmo anexo. Esta feature adiciona o campo obrigatório **`vagaIA`**:

| `vagaIA` | Template | Anexo |
|---|---|---|
| `false` | `templates/email-body.html` | `templates/CV_Guilherme_Augusto_Goettnauer_2026.docx` |
| `true` | `templates/email-body-ia.html` | `templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx` |

O arquivo de currículo de IA **já existe** no repositório, no path acima. Não há fallback entre perfis: arquivo ausente do perfil escolhido falha o envio.

---

## 1. Contexto (estado atual)

A API expõe `POST /api/emails/send` e monta o e-mail com um único par template + CV:

| Recurso | Origem atual |
|---|---|
| Corpo HTML | `templates/email-body.html` (`email.template.path`) |
| Anexo | `templates/CV_Guilherme_Augusto_Goettnauer_2026.docx` (`email.cv.path`) |
| Placeholder | `{{cargo}}` substituído pelo campo `cargo` da requisição |

O DTO atual é:

```json
{
  "to": "destinatario@example.com",
  "subject": "Oportunidade Java Senior",
  "cargo": "Desenvolvedor Java Sênior"
}
```

Não existe ramificação de template nem de currículo. O assunto (`subject`) continua vindo do cliente; esta feature **não** altera isso.

---

## 2. Problema

Para candidaturas de **vagas de IA**, o corpo e o anexo atuais não representam o perfil desejado. O cliente precisa escolher, na mesma API, se o envio usa o material padrão ou o material de perfil de IA.

---

## 3. Objetivo

Adicionar o parâmetro de entrada **`vagaIA`** (`boolean`) no endpoint existente.

- `vagaIA = false` → comportamento **idêntico** ao de hoje (template + CV atuais).
- `vagaIA = true` → envia o e-mail de **perfil de IA** (template HTML novo + `CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`).

O endpoint, o método HTTP e o restante do contrato (`to`, `subject`, `cargo`) permanecem os mesmos.

---

## 4. Fora de escopo

- Novo endpoint ou versionamento de API (`/v2`, query params, multipart).
- Alterar autenticação Gmail / OAuth.
- Escolher assunto automaticamente conforme o perfil.
- Upload de anexo pelo cliente; os arquivos continuam no disco/volume da aplicação.
- Enviar os dois perfis na mesma requisição.
- Histórico persistido de envios.

---

## 5. Requisitos funcionais

| ID | Requisito |
|---|---|
| RF-01 | O JSON de `POST /api/emails/send` passa a aceitar o campo `vagaIA`. |
| RF-02 | `vagaIA` é **obrigatório**. Aceita somente `true` ou `false` (JSON boolean). |
| RF-03 | Se `vagaIA` for `false`, carregar `email.template.path` e anexar `email.cv.path`. |
| RF-04 | Se `vagaIA` for `true`, carregar `email.template.ia.path` e anexar `email.cv.ia.path` (CV `CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`). |
| RF-05 | Em ambos os casos, substituir `{{cargo}}` no HTML escolhido, com o mesmo escape HTML já existente. |
| RF-06 | Destinatário e assunto continuam sendo `to` e `subject` da requisição. |
| RF-07 | Respostas de sucesso e de erro HTTP permanecem as atuais (`200`, `400` de validação, `500` de falha de envio). |
| RF-08 | Se o arquivo de template ou de CV do perfil escolhido não existir, falhar de forma explícita (estado inválido), sem cair no outro perfil. |

---

## 6. Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF-01 | Não quebrar o restante do contrato (`to`, `subject`, `cargo`); o campo novo é **aditivo**, porém obrigatório — o cliente precisa passar a enviá-lo. |
| RNF-02 | Manter a estrutura atual (Controller → DTO → Service → Gmail API). Sem nova camada desnecessária. |
| RNF-03 | Caminhos de template/CV de IA configuráveis por propriedade Spring (e, no Docker, por variável de ambiente), no mesmo padrão de hoje. |
| RNF-04 | Não versionar credenciais, tokens nem dados sensíveis. |

---

## 7. Contrato da API

### Endpoint (inalterado)

```http
POST /api/emails/send
Content-Type: application/json
```

### Request — após a feature

```json
{
  "to": "destinatario@example.com",
  "subject": "Oportunidade Java Senior",
  "cargo": "Desenvolvedor Java Sênior",
  "vagaIA": false
}
```

Exemplo perfil IA:

```json
{
  "to": "destinatario@example.com",
  "subject": "Oportunidade Engenheiro de IA",
  "cargo": "Engenheiro de Machine Learning",
  "vagaIA": true
}
```

### Campos

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `to` | string | sim | E-mail válido, não em branco (já existe). |
| `subject` | string | sim | Não em branco (já existe). |
| `cargo` | string | sim | Não em branco (já existe). |
| `vagaIA` | boolean | sim | Somente `true` ou `false`. Ausência, `null` ou tipo inválido → `400`. |

Uso de `Boolean` no DTO (não `boolean` primitivo), para que ausência/null seja detectável e rejeitada. Validação: `@NotNull`.

Valores JSON **não** aceitos como boolean: `"true"`, `"false"`, `1`, `0` — o cliente deve enviar boolean JSON real. (Jackson padrão do Spring trata isso; não há conversão extra.)

### Responses

| Situação | HTTP | Corpo |
|---|---|---|
| Envio ok | 200 | `E-mail enviado com sucesso.` |
| Validação Bean Validation / `IllegalArgumentException` | 400 | `Erro de validação: ...` |
| Falha de envio / arquivo ausente / Gmail | 500 | `Erro ao enviar e-mail: ...` |

---

## 8. Comportamento (regra de seleção)

```text
POST /api/emails/send
        │
        ▼
  SendEmailRequest (to, subject, cargo, vagaIA)
        │
        ▼
     EmailService
        │
        ├── vagaIA == false
        │     ├── template: email.template.path
        │     └── anexo:    email.cv.path
        │
        └── vagaIA == true
              ├── template: email.template.ia.path
              └── anexo:    email.cv.ia.path
                    (CV_Guilherme_Augusto_Goettnauer_2026_AI.docx)
        │
        ├── substitui {{cargo}}
        ├── MimeMessage (HTML + 1 anexo)
        └── Gmail API
```

Não há fallback: perfil IA **nunca** usa o CV/template padrão se o arquivo de IA estiver faltando.

---

## 9. Arquivos e configuração

### Artefatos de conteúdo

| Papel | Path local | Status |
|---|---|---|
| Corpo HTML padrão | `templates/email-body.html` | já existe |
| Currículo padrão | `templates/CV_Guilherme_Augusto_Goettnauer_2026.docx` | já existe |
| Corpo HTML perfil IA | `templates/email-body-ia.html` | existe |
| Currículo perfil IA | `templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx` | **já existe** |

O HTML de IA deve usar o mesmo placeholder `{{cargo}}`. O texto do corpo (copy) fica no arquivo HTML, não hardcoded em Java.

O `FileName` do anexo MIME deve ser o nome do arquivo no disco (`path.getFileName()`), como hoje — para `vagaIA = true` o destinatário verá `CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`.

### Propriedades Spring

Manter:

```properties
email.template.path=templates/email-body.html
email.cv.path=templates/CV_Guilherme_Augusto_Goettnauer_2026.docx
```

Adicionar:

```properties
email.template.ia.path=templates/email-body-ia.html
email.cv.ia.path=templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx
```

No Docker/Compose, espelhar o padrão existente:

```text
EMAIL_TEMPLATE_IA_PATH=/app/templates/email-body-ia.html
EMAIL_CV_IA_PATH=/app/templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx
```

(Spring Boot: `email.template.ia.path` ↔ `EMAIL_TEMPLATE_IA_PATH`.)

O volume `templates/` já monta o diretório inteiro (`:ro`); o DOCX de IA entra no container **sem** volume extra, desde que o arquivo esteja nesse diretório no host.

---

## 10. Estudo da solução (código atual → desenho da mudança)

### 10.1 Como o envio funciona hoje

Fluxo real no código:

1. `EmailController.sendEmail` valida `SendEmailRequest` (`to`, `subject`, `cargo`) e chama `EmailService.sendEmail(to, subject, cargo)`.
2. `EmailService` injeta **dois** paths via `@Value`: `email.template.path` e `email.cv.path`.
3. `loadEmailTemplate(cargo)` lê o HTML, exige existência do arquivo (`IllegalStateException` se faltar) e substitui `{{cargo}}` com `escapeHtml`.
4. `createMimeMessage` monta `MimeMultipart` com HTML + um único anexo.
5. `createCvAttachment()` lê **sempre** `cvPath`; o nome visível do anexo é `path.getFileName()`.
6. A mensagem é serializada, Base64 URL-safe e enviada pela Gmail API (`users.messages.send("me", ...)`).

Pontos que **não** mudam: `GmailConfig`, OAuth, encoding, `from = me`, estrutura MIME (HTML + 1 anexo), HTTP 200/400/500.

### 10.2 Gap em relação à feature

| Peça | Hoje | Precisa |
|---|---|---|
| `SendEmailRequest` | 3 campos, sem `vagaIA` | `Boolean vagaIA` + `@NotNull` |
| `EmailController` | 3 argumentos para o service | passar `request.vagaIA()` |
| `EmailService` | 2 `@Value` fixos | 4 paths; seleção por `vagaIA` |
| `loadEmailTemplate` / `createCvAttachment` | paths de instância únicos | path escolhido por chamada (parametrizar) |
| `application.properties` | 2 paths | + `email.template.ia.path` e `email.cv.ia.path` |
| `templates/` | HTML padrão + CV padrão + **CV IA já no disco** | HTML de IA ainda falta |

### 10.3 Decisão de desenho (mínimo, alinhado ao RNF-02)

Não criar strategy/factory/enum de perfil. A ramificação cabe no service:

1. Resolver o par `(templatePath, cvPath)` a partir de `vagaIA`.
2. Passar esses paths para os métodos privados que hoje usam os campos de instância.

Esboço da seleção (conceitual):

```text
if (vagaIA) {
    template = templateIaPath;   // email.template.ia.path
    cv       = cvIaPath;         // email.cv.ia.path  → ..._2026_AI.docx
} else {
    template = templatePath;     // email.template.path
    cv       = cvPath;           // email.cv.path
}
```

Refatoração pontual: `loadEmailTemplate(cargo, path)` e `createCvAttachment(path)` (ou equivalente) para **não** duplicar MIME/Gmail. Construtor passa a receber os quatro `@Value`.

### 10.4 Validação de `vagaIA`

- Tipo no record: `Boolean` (wrapper), não `boolean`, para distinguir ausência de `false`.
- `@NotNull(message = "...")` — Bean Validation já usada no controller (`@Valid`).
- Ausência/`null` → 400 (mesmo pipeline de `to`/`subject`/`cargo`).
- String `"true"` / número `1` não são boolean JSON; Jackson rejeita (400).

### 10.5 Arquivos ausentes

Hoje `IllegalStateException` em template/CV faltando cai no `catch (Exception)` do controller → **500**, mensagem `Erro ao enviar e-mail: ...`. Manter esse comportamento (RF-07 / RF-08): falha explícita, sem fallback.

### 10.6 Deploy

O workflow (`.github/workflows/deploy.yml`) só faz `git reset --hard origin/main` e `docker compose build/up` do serviço. O DOCX de IA precisa estar no volume `templates/` da VPS (commit no repo **ou** cópia no host). Como o arquivo já está em `templates/` neste workspace, o caminho de código + compose deve apontar exatamente para `CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`.

Env vars novas no compose da VPS (mesmo padrão de `EMAIL_TEMPLATE_PATH` / `EMAIL_CV_PATH`).

### 10.7 O que não fazer

- Não ler o DOCX no Java além de anexá-lo (`FileDataSource`), como no CV padrão.
- Não mudar o assunto no servidor.
- Não anexar os dois CVs.
- Não versionar tokens/credenciais.

---

## 11. Alterações de código previstas

| Peça | Mudança |
|---|---|
| `SendEmailRequest` | Campo `Boolean vagaIA` com `@NotNull`. |
| `EmailController` | Passar `request.vagaIA()` para o service. |
| `EmailService` | Assinatura de `sendEmail` inclui `boolean vagaIA`. Quatro `@Value`. Escolher paths. Parametrizar `loadEmailTemplate` / anexo. |
| `application.properties` | `email.template.ia.path` e `email.cv.ia.path` apontando para `..._2026_AI.docx`. |
| Templates | Novo `email-body-ia.html`; CV de IA **já presente**. |
| README / compose | Documentar `vagaIA` e as env vars `EMAIL_TEMPLATE_IA_PATH` / `EMAIL_CV_IA_PATH`. |

GmailConfig, OAuth e encoding Base64 **não** mudam.

---

## 12. Critérios de aceite

- [x] Requisição **sem** `vagaIA` retorna **400**.
- [x] `vagaIA: false` envia o HTML e o anexo atuais; `{{cargo}}` preenchido.
- [x] `vagaIA: true` envia o HTML de IA e anexa **`CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`**; `{{cargo}}` preenchido.
- [x] Destinatário e assunto iguais aos enviados no JSON.
- [x] Arquivo de IA ausente com `vagaIA: true` **não** envia o perfil padrão; a API falha.
- [x] Comportamento de escape HTML de `cargo` permanece.
- [x] `to`/`subject`/`cargo` inválidos continuam em 400 como hoje.

---

## 13. Plano de implementação (após aprovação)

- [x] Confirmar `templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx` no volume de deploy.
- [x] Incluir `templates/email-body-ia.html`.
- [x] Propriedades novas em `application.properties` (e env no compose da VPS).
- [x] Estender DTO + controller + service com seleção de perfil.
- [x] Smoke test local: um POST `false` e um POST `true` (destinatário de teste).
- [x] Atualizar README com o campo `vagaIA` e o nome do anexo de IA.

---

## 14. Decisões desta feature

| Tópico | Decisão |
|---|---|
| Nome do campo | `vagaIA` (camelCase JSON). |
| Tipo | boolean JSON; DTO `Boolean` + `@NotNull`. |
| Campo obrigatório | Sim. |
| O que muda no perfil IA | Template **e** anexo. |
| Nome do CV de IA | `CV_Guilherme_Augusto_Goettnauer_2026_AI.docx` (arquivo real em `templates/`). |
| Propriedade | `email.cv.ia.path` |
| Assunto | Continua 100% no cliente. |
| Fallback entre perfis | Não. |
| Camada extra (strategy) | Não — if no service + paths injetados. |

---

## 15. Pendências

- [x] Arquivo do currículo de IA: `templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`.
- [x] Conteúdo final de `templates/email-body-ia.html` (copy do perfil de IA).
- [x] Confirmar env vars novas no compose da VPS (`EMAIL_TEMPLATE_IA_PATH`, `EMAIL_CV_IA_PATH`). *(documentadas no README; conferência no host da VPS fica como deferred operacional)*

---

## Auto Run Result

<summary>
Implementado `vagaIA` no contrato existente: DTO com `Boolean` + `@NotNull`, service escolhe o par template/CV, HTML de IA e propriedades Spring adicionadas. Sem fallback entre perfis.
</summary>

<file-list>
- `src/main/java/com/goett/emailsender/dto/SendEmailRequest.java`
- `src/main/java/com/goett/emailsender/controller/EmailController.java`
- `src/main/java/com/goett/emailsender/service/EmailService.java`
- `src/main/resources/application.properties`
- `templates/email-body-ia.html`
- `templates/CV_Guilherme_Augusto_Goettnauer_2026_AI.docx`
- `README.md`
</file-list>

<verification>
Código alinhado aos RF-01..RF-08 e aos critérios de aceite. Paths de IA injetados via `@Value`; arquivo ausente lança `IllegalStateException` (HTTP 500), sem cair no outro perfil.
</verification>

<residual-risks>
Env vars `EMAIL_TEMPLATE_IA_PATH` / `EMAIL_CV_IA_PATH` precisam existir no compose da VPS no deploy; o repositório documenta o padrão no README.
</residual-risks>
