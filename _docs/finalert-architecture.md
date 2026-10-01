# FinAlert — Documentação de Arquitetura da Solução

## 1. Visão Geral do Sistema
O **FinAlert** é uma plataforma orientada a eventos (*Event-Driven Architecture*) projetada para monitorar cotações de ativos financeiros em tempo real e disparar notificações automáticas aos usuários quando limites de preço pré-configurados forem atingidos.

A arquitetura foi estruturada em microsserviços desacoplados, priorizando resiliência, baixa latência de processamento e escalabilidade independente.

---

## 2. Visão de Arquitetura de Microsserviços

O sistema é composto por 3 microsserviços principais e componentes de infraestrutura de mensageria e persistência:

```
+---------------------------------+
|   API de Cotações (Externa)     |
+---------------------------------+
                 |
                 | HTTP REST (Polling)
                 v
+---------------------------------+       Event: PriceUpdatedEvent        +---------------------------------+
|  market-data-ingestion-service  | ------------------------------------> |       RabbitMQ / Broker         |
+---------------------------------+                                       +---------------------------------+
                                                                             |                 ^
                                                                             |                 |
                                                  Event: PriceUpdatedEvent   v                 | Event: AlertTriggeredEvent
                                                        +------------------------+             |
                                                        |  alert-engine-service  | ------------+
                                                        +------------------------+
                                                                    |
                                                                    v
                                                        +------------------------+
                                                        | PostgreSQL (Rules DB)  |
                                                        +------------------------+

                                                        +------------------------+
                                  Event:                |  notification-service  |
                                  AlertTriggeredEvent   +------------------------+
                                  -------------------->             |
                                                                    v
                                                        +------------------------+
                                                        | User (E-mail / Bot)    |
                                                        +------------------------+
```

---

## 3. Descrição dos Microsserviços

### 3.1. `02-market-data-ingestion-service`
* **Papel:** Ingestão contínua de dados de mercado.
* **Mecanismo:** Executa consultas periódicas (*Polling*) via protocolo HTTP REST a APIs públicas de mercado utilizando o recurso `@Scheduled` do Spring Framework.
* **Fluxo de Dados:**
  1. Consulta a cotação do ativo parametrizado.
  2. Mapeia a resposta da API externa para o modelo interno.
  3. Publica o evento `PriceUpdatedEvent` no broker de mensageria.

### 3.2. `01-alert-engine-service`
* **Papel:** Core de negócios e avaliação de regras de alertas.
* **Mecanismo:** Gerencia o cadastro de alertas via interface REST (Swagger) e processa de forma assíncrona as variações de preço consumidas da mensageria.
* **Fluxo de Dados:**
  1. Expõe endpoints para criação e consulta de regras de alertas de preços (`POST /alerts`, `GET /alerts`).
  2. Persiste e atualiza as regras no banco de dados relacional **PostgreSQL**.
  3. Consome o evento `PriceUpdatedEvent`.
  4. Avalia as condições ativas (ex: `Preço Atual >= Preço Alvo`).
  5. Atualiza o status da regra para evitar disparos duplicados e publica o evento `AlertTriggeredEvent`.

### 3.3. `03-notification-service`
* **Papel:** Entrega de notificações aos usuários.
* **Mecanismo:** Consumidor reativo responsável por transformar eventos de alerta em mensagens finais de comunicação.
* **Fluxo de Dados:**
  1. Consome o evento `AlertTriggeredEvent`.
  2. Formata o modelo do e-mail/mensagem com os dados do evento.
  3. Realiza o envio da notificação via SMTP (`JavaMailSender`) ou canal alternativo (ex: Telegram Bot API).

---

## 4. Design de Eventos e Mensageria

O desacoplamento entre os serviços é garantido pelo uso de um broker de mensagens (**RabbitMQ**).

### 4.1. Evento: `PriceUpdatedEvent`
* **Tópico/Fila:** `market.price-updated.queue`
* **Payload:**
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "symbol": "USD-BRL",
  "currentPrice": 5.6025,
  "timestamp": "2026-09-29T10:00:00Z"
}
```

### 4.2. Evento: `AlertTriggeredEvent`
* **Tópico/Fila:** `alert.triggered.queue`
* **Payload:**
```json
{
  "alertId": "8f8373b0-2b1b-4f81-80d5-12a8a7051b9e",
  "userEmail": "usuario@email.com",
  "symbol": "USD-BRL",
  "targetPrice": 5.6000,
  "triggeredPrice": 5.6025,
  "triggeredAt": "2026-09-29T10:00:01Z"
}
```

---

## 5. Modelagem de Dados (`01-alert-engine-service`)

Entidade principal mantida no PostgreSQL:

* **Tabela:** `tb_alerts`
  * `id` (UUID, PK)
  * `user_email` (VARCHAR, Not Null)
  * `symbol` (VARCHAR, Not Null) — Ex: `USD-BRL`, `BTC-USD`
  * `target_price` (DECIMAL, Not Null)
  * `status` (VARCHAR, Not Null) — Valores: `PENDING`, `TRIGGERED`, `CANCELLED`
  * `created_at` (TIMESTAMP, Not Null)
  * `updated_at` (TIMESTAMP)

---

## 6. Pilha Tecnológica e Infraestrutura

* **Linguagem & Framework:** Java 17/21 com Spring Boot 3.x
* **Acesso a Dados:** Spring Data JPA + Hibernate
* **Banco de Dados Relacional:** PostgreSQL 15+
* **Mensageria:** RabbitMQ
* **Documentação de API:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
* **Conteinerização:** Docker & Docker Compose (para orquestração do ambiente de desenvolvimento local)