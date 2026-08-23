# Email Sender

Microserviço desenvolvido em **Java 21 + Spring Boot** para envio automatizado de e-mails através da **Gmail API**, utilizando **OAuth 2.0** para autenticação.

A aplicação foi projetada para executar de forma independente, podendo ser containerizada com Docker e utilizada como serviço de infraestrutura para aplicações que precisam enviar e-mails utilizando uma conta Gmail autorizada.

---

## 📋 Sobre o projeto

O **Email Sender** é uma aplicação backend que expõe uma API REST para envio de e-mails.

A aplicação:

* recebe uma requisição HTTP;
* carrega um template HTML;
* substitui parâmetros dinâmicos no template;
* adiciona um arquivo como anexo;
* constrói uma mensagem MIME;
* codifica a mensagem no formato exigido pela Gmail API;
* envia o e-mail através da API oficial do Gmail.

A autenticação é realizada utilizando **OAuth 2.0**, com armazenamento local do token de autorização para evitar a necessidade de realizar o processo de autorização a cada inicialização.

---

## 🏗️ Arquitetura

Fluxo simplificado da aplicação:

```text
                  Cliente
                     │
                     │ HTTP POST
                     ▼
             ┌─────────────────┐
             │ EmailController │
             └────────┬────────┘
                      │
                      ▼
             ┌─────────────────┐
             │  EmailService   │
             └────────┬────────┘
                      │
          ┌───────────┼───────────┐
          │           │           │
          ▼           ▼           ▼
      Template       MIME        Anexo
        HTML        Message       CV
          │           │           │
          └───────────┼───────────┘
                      │
                      ▼
              ┌───────────────┐
              │   Gmail API   │
              └───────┬───────┘
                      │
                      ▼
                   Gmail
```

---

## 🔐 Autenticação OAuth 2.0

A aplicação utiliza o fluxo OAuth 2.0 disponibilizado pelas bibliotecas oficiais do Google.

O escopo utilizado atualmente é:

```text
https://www.googleapis.com/auth/gmail.send
```

Esse escopo concede à aplicação permissão para enviar e-mails através da conta Gmail autorizada.

### Fluxo de autorização

Na primeira execução, caso não exista um token previamente armazenado:

```text
Email Sender
     │
     ▼
Google OAuth 2.0
     │
     ▼
Usuário autoriza aplicação
     │
     ▼
Callback localhost:8888
     │
     ▼
Token armazenado
     │
     ▼
Gmail API
```

A aplicação utiliza:

```java
LocalServerReceiver
```

na porta:

```text
8888
```

para receber o callback OAuth.

Após a autorização inicial, o token é armazenado no diretório configurado pela aplicação:

```text
tokens/
```

Dessa forma, execuções posteriores podem reutilizar as credenciais armazenadas.

---

# 🚀 Tecnologias

## Backend

* Java 21
* Spring Boot 4.1.0
* Spring Web MVC
* Spring Actuator

## Integração

* Gmail API
* Google OAuth 2.0
* Google API Client
* JavaMail / Angus Mail

## Build

* Maven
* Spring Boot Maven Plugin

## Infraestrutura

* Docker
* Docker Compose
* Linux
* VPS

---

# 📦 Dependências principais

O projeto utiliza, entre outras, as seguintes dependências:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-gmail</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>org.eclipse.angus</groupId>
    <artifactId>angus-mail</artifactId>
</dependency>
```

---

# 📁 Estrutura do projeto

Uma estrutura aproximada:

```text
email-sender/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── goett/
│   │               └── emailsender/
│   │                   │
│   │                   ├── config/
│   │                   │   └── GmailConfig.java
│   │                   │
│   │                   ├── controller/
│   │                   │   └── EmailController.java
│   │                   │
│   │                   ├── dto/
│   │                   │   └── SendEmailRequest.java
│   │                   │
│   │                   └── service/
│   │                       └── EmailService.java
│   │
│   └── resources/
│
├── credentials/
│   └── google-oauth-client.json
│
├── tokens/
│   └── StoredCredential
│
├── templates/
│   ├── email-body.html
│   └── CV_Guilherme_Augusto_Goettnauer_2026.docx
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

> Os arquivos contendo credenciais e tokens **não devem ser versionados no Git**.

---

# 📡 API

## Enviar e-mail

### Endpoint

```http
POST /api/emails/send
```

### Content-Type

```http
application/json
```

### Request

```json
{
  "to": "destinatario@example.com",
  "subject": "Oportunidade Java Senior",
  "cargo": "Desenvolvedor Java Sênior"
}
```

### Response de sucesso

```text
E-mail enviado com sucesso.
```

HTTP:

```text
200 OK
```

### Exemplo utilizando cURL

```bash
curl -X POST http://localhost:8080/api/emails/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": "destinatario@example.com",
    "subject": "Oportunidade Java Senior",
    "cargo": "Desenvolvedor Java Sênior"
  }'
```

---

# ✉️ Processo de envio

O envio segue aproximadamente o seguinte fluxo:

```text
POST /api/emails/send
          │
          ▼
    SendEmailRequest
          │
          ▼
     EmailService
          │
          ├── Carrega template HTML
          │
          ├── Substitui {{cargo}}
          │
          ├── Valida currículo
          │
          ├── Cria MimeMessage
          │
          ├── Adiciona HTML
          │
          ├── Adiciona currículo
          │
          ├── Codifica MIME em Base64 URL-safe
          │
          ▼
       Gmail API
          │
          ▼
       Gmail
```

---

# 📝 Template de e-mail

O corpo do e-mail é carregado de um arquivo HTML configurável.

Exemplo:

```html
<html>
<body>

<p>Olá,</p>

<p>
Tenho interesse na oportunidade de
<strong>{{cargo}}</strong>.
</p>

<p>
Fico à disposição para conversarmos.
</p>

</body>
</html>
```

A aplicação substitui:

```text
{{cargo}}
```

pelo valor recebido na requisição.

Antes da substituição, o valor é tratado através de escaping HTML para evitar que conteúdo fornecido pelo usuário seja interpretado como HTML.

---

# 📎 Anexo

A aplicação também permite adicionar um arquivo ao e-mail.

Atualmente o projeto está configurado para utilizar um currículo como anexo:

```text
CV_Guilherme_Augusto_Goettnauer_2026.docx
```

O caminho é configurável através da propriedade:

```properties
email.cv.path
```

---

# ⚙️ Configuração

As principais propriedades utilizadas pela aplicação são:

```properties
email.template.path=/app/templates/email-body.html
email.cv.path=/app/templates/CV_Guilherme_Augusto_Goettnauer_2026.docx
```

Em ambiente Docker, esses arquivos podem ser disponibilizados através de volumes.

Exemplo:

```yaml
volumes:
  - ./projects/email-sender/templates:/app/templates:ro
```

---

# 🔑 Configuração do Google Cloud

Para executar a aplicação é necessário criar um projeto no **Google Cloud** e habilitar a Gmail API.

## 1. Criar um projeto

Crie um projeto no Google Cloud Console.

## 2. Habilitar Gmail API

Habilite:

```text
Gmail API
```

## 3. Configurar OAuth Consent Screen

Configure a tela de consentimento OAuth.

Durante o desenvolvimento, a aplicação pode permanecer em modo de teste.

## 4. Criar credenciais OAuth

Crie credenciais para aplicação compatível com o fluxo utilizado pelo projeto.

O arquivo JSON fornecido pelo Google deve ser disponibilizado no caminho:

```text
credentials/google-oauth-client.json
```

### ⚠️ Importante

Nunca faça commit desse arquivo.

Adicione ao `.gitignore`:

```gitignore
credentials/
tokens/
```

---

# 🐳 Docker

A aplicação pode ser executada dentro de um container Docker.

Exemplo de serviço no Docker Compose:

```yaml
email-sender:
  build:
    context: ./projects/email-sender
    dockerfile: Dockerfile

  container_name: email-sender

  restart: unless-stopped

  volumes:
    - ./projects/email-sender/credentials:/app/credentials:ro
    - ./projects/email-sender/tokens:/app/tokens
    - ./projects/email-sender/templates:/app/templates:ro

  environment:
    EMAIL_TEMPLATE_PATH: /app/templates/email-body.html
    EMAIL_CV_PATH: /app/templates/CV_Guilherme_Augusto_Goettnauer_2026.docx
```

A aplicação executa o servidor HTTP na porta:

```text
8080
```

---

# 🔄 OAuth em ambiente remoto / VPS

Quando a aplicação é executada em uma VPS, o callback OAuth utiliza:

```text
localhost:8888
```

Como o navegador normalmente está sendo executado na máquina do desenvolvedor e não na VPS, pode ser utilizado um túnel SSH.

Exemplo:

```bash
ssh -L 8888:localhost:8888 usuario@IP_DA_VPS
```

Com isso:

```text
Navegador local
      │
      │ localhost:8888
      ▼
SSH Tunnel
      │
      ▼
VPS
      │
      ▼
Docker
      │
      ▼
email-sender:8888
```

Isso permite realizar o fluxo OAuth sem expor publicamente a porta 8888 da aplicação.

---

# 🩺 Health Check

O projeto utiliza Spring Boot Actuator.

Endpoint:

```http
GET /actuator/health
```

Exemplo:

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:

```json
{
  "status": "UP"
}
```

Esse endpoint também pode ser utilizado posteriormente em mecanismos de monitoramento e deploy automatizado.

---

# 🧪 Testes

O projeto possui suporte às dependências de teste do Spring Boot.

Para executar os testes:

```bash
./mvnw test
```

ou:

```bash
mvn test
```

---

# 🔨 Build

Para gerar o JAR:

```bash
./mvnw clean package
```

O artefato será gerado em:

```text
target/email-sender-0.0.1-SNAPSHOT.jar
```

Para executar diretamente:

```bash
java -jar target/email-sender-0.0.1-SNAPSHOT.jar
```

---

# 🖥️ Execução local

Clone o projeto:

```bash
git clone <URL_DO_REPOSITORIO>
```

Entre no diretório:

```bash
cd email-sender
```

Configure as credenciais do Google:

```text
credentials/google-oauth-client.json
```

Configure os templates:

```text
templates/email-body.html
templates/CV_Guilherme_Augusto_Goettnauer_2026.docx
```

Execute:

```bash
./mvnw spring-boot:run
```

A aplicação estará disponível em:

```text
http://localhost:8080
```

---

# 🔐 Segurança

O projeto foi desenvolvido considerando a separação entre código da aplicação e informações sensíveis.

Não devem ser versionados:

```text
credentials/
tokens/
.env
```

O arquivo:

```text
google-oauth-client.json
```

contém informações relacionadas à configuração OAuth da aplicação e deve ser tratado como informação sensível.

O token OAuth armazenado em:

```text
tokens/StoredCredential
```

também não deve ser publicado.

---

# 🌐 Deploy

A aplicação pode ser executada em uma VPS utilizando Docker Compose.

Arquitetura de deployment:

```text
                    Git
                     │
                     ▼
                Desenvolvedor
                     │
                     │ deploy
                     ▼
                    VPS
                     │
              Docker Compose
                     │
                     ▼
              email-sender
                     │
                     ▼
                 Gmail API
```

Uma evolução natural do projeto é automatizar esse processo utilizando:

```text
GitHub
   │
   ▼
GitHub Actions
   │
   ├── Build
   ├── Test
   ├── Docker Build
   └── Deploy
          │
          ▼
         VPS
```

---

# 📈 Possíveis evoluções

O projeto foi desenvolvido de forma incremental e pode evoluir para uma arquitetura mais robusta.

Algumas evoluções planejadas:

* [ ] CI/CD com GitHub Actions
* [ ] Build automático da imagem Docker
* [ ] Deploy automático na VPS
* [ ] Container Registry
* [ ] Testes unitários
* [ ] Testes de integração
* [ ] Testcontainers
* [ ] Validação dos campos da API
* [ ] Tratamento global de exceções com `@RestControllerAdvice`
* [ ] Respostas de erro padronizadas
* [ ] Logging estruturado
* [ ] Correlation ID
* [ ] Métricas através do Actuator
* [ ] Observabilidade com Prometheus/Grafana
* [ ] Retry para falhas temporárias da Gmail API
* [ ] Rate limiting
* [ ] Fila para processamento assíncrono
* [ ] Persistência de histórico de mensagens
* [ ] Suporte a múltiplos templates
* [ ] Configuração de múltiplos anexos
* [ ] Autenticação da própria API
* [ ] OpenAPI/Swagger

---

# 🎯 Objetivo técnico

O principal objetivo do projeto é demonstrar a construção de um serviço backend utilizando práticas e tecnologias presentes em ambientes profissionais:

```text
Java 21
   │
   ▼
Spring Boot
   │
   ▼
REST API
   │
   ▼
Integração com API externa
   │
   ▼
OAuth 2.0
   │
   ▼
Docker
   │
   ▼
Linux / VPS
   │
   ▼
CI/CD
```

Além da implementação da API, o projeto aborda aspectos importantes de engenharia de software, como:

* integração com serviços externos;
* autenticação OAuth 2.0;
* gerenciamento de credenciais;
* containerização;
* configuração por ambiente;
* health checks;
* deployment;
* automação de infraestrutura.

---

# 👨‍💻 Autor

**Guilherme Augusto Goettnauer**

Desenvolvedor Java / Backend

Principais áreas de interesse:

```text
Java
Spring Boot
Microservices
REST APIs
Cloud
Docker
Kubernetes
CI/CD
Arquitetura de Software
Observabilidade
Integração de sistemas
```

---

# 📄 Licença

Este projeto está disponível para fins de estudo, demonstração técnica e portfólio.

Caso seja adicionada uma licença específica ao repositório, esta seção deverá ser atualizada de acordo com os termos escolhidos.
