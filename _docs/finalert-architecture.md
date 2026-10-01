# FinAlert — Documentação de Arquitetura da Solução

## 1. Visão Geral do Sistema
O **FinAlert** é uma plataforma orientada a eventos (*Event-Driven Architecture*) projetada para monitorar cotações de ativos financeiros em tempo real e disparar notificações automáticas aos usuários quando limites de preço pré-configurados forem atingidos.

A arquitetura foi estruturada em microsserviços desacoplados, priorizando resiliência, baixa latência de processamento e escalabilidade independente.

---

## 2. Visão de Arquitetura de Microsserviços

O sistema é composto por 3 microsserviços principais e componentes de infraestrutura de mensageria e persistência:

```
+------------------------------+
| AwesomeAPI (externa)         |
| /json/available e /json/last |
+------------------------------+
                |
                | HTTP polling
                v
+--------------------------------------------+
| 02-market-data-ingestion-service           |
|                                            |
| MongoDB                                    |
| - market_instruments                       |
| - market_quotes                            |
+--------------------------------------------+
     | PriceUpdatedEvent e InstrumentCatalogUpdatedEvent
     v
+------------------------------------+
| RabbitMQ / exchange market.events  |
+------------------------------------+
                |
                v
+--------------------------------+
| 01-alert-engine-service        |
|                                |
| PostgreSQL                     |
| - tb_alerts                    |
| - tb_available_market_pairs    |
+--------------------------------+
                |
                | AlertTriggeredEvent
                v
+-----------------------------------+
| RabbitMQ / exchange alert.events  |
+-----------------------------------+
                |
                v
+--------------------------------+
| 03-notification-service        |
+--------------------------------+

POST /alerts valida o catálogo local do alert-engine-service.
```

O catálogo de pares válidos é obtido exclusivamente pelo `02-market-data-ingestion-service`. O `01-alert-engine-service` mantém uma projeção local desse catálogo para validar a criação de alertas sem consultar a AwesomeAPI ou o banco de outro serviço.

---

## 3. Descrição dos Microsserviços

### 3.1. `02-market-data-ingestion-service`
* **Papel:** Ingestão contínua de dados de mercado.
* **Mecanismo:** Executa consultas periódicas (*Polling*) via protocolo HTTP REST a APIs públicas de mercado utilizando o recurso `@Scheduled` do Spring Framework.
* **Fluxo de Dados:**
  1. Atualiza periodicamente o catálogo de pares disponíveis via `GET /json/available`.
  2. Persiste o catálogo e a última cotação conhecida no MongoDB (`market_instruments` e `market_quotes`).
  3. Publica `CatalogUpdatedEvent` quando uma nova versão do catálogo é obtida.
  4. Consulta a cotação dos símbolos monitorados via `GET /json/last/{symbols}`.
  5. Mapeia a resposta da API externa para o modelo interno e publica `PriceUpdatedEvent`.

### 3.2. `01-alert-engine-service`
* **Papel:** Core de negócios e avaliação de regras de alertas.
* **Mecanismo:** Gerencia o cadastro de alertas via interface REST (Swagger) e processa de forma assíncrona as variações de preço consumidas da mensageria.
* **Fluxo de Dados:**
  1. Consome `CatalogUpdatedEvent` e atualiza a projeção `tb_available_market_pairs` no PostgreSQL.
  2. Expõe endpoints para criação e consulta de regras de alertas de preços (`POST /alerts`, `GET /alerts`).
  3. Valida o símbolo solicitado contra sua projeção local antes de persistir a regra.
  4. Consome o evento `PriceUpdatedEvent`.
  5. Avalia as condições ativas (ex: `Preço Atual >= Preço Alvo`).
  6. Atualiza o status da regra para evitar disparos duplicados e publica o evento `AlertTriggeredEvent`.

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

### 4.1. Evento: `CatalogUpdatedEvent`
* **Produtor:** `02-market-data-ingestion-service`
* **Exchange:** `market.events`
* **Routing key:** `market.instrument-catalog.updated`
* **Consumidor:** `01-alert-engine-service`, na fila própria `alert-engine.instrument-catalog`.
* **Semântica:** carrega um snapshot versionado do catálogo de pares válidos. O consumidor faz upsert dos pares recebidos, remove pares ausentes e registra a versão aplicada. O processamento deve ser idempotente.
* **Payload:**
```json
{
  "eventId": "426c7962-d29e-4fa1-9d6c-294fb15e8f48",
  "catalogVersion": 3,
  "generatedAt": "2026-10-01T10:00:00Z",
  "pairs": [
    {
      "symbol": "USD-BRL",
      "description": "Dólar Americano/Real Brasileiro"
    },
    {
      "symbol": "BTC-BRL",
      "description": "Bitcoin/Real Brasileiro"
    }
  ]
}
```

### 4.2. Evento: `PriceUpdatedEvent`
* **Produtor:** `02-market-data-ingestion-service`
* **Exchange:** `market.events`
* **Routing key:** `price.updated`
* **Consumidor:** `01-alert-engine-service`, na fila própria `alert-engine.price-updated`.
* **Payload:**
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "symbol": "USD-BRL",
  "currentPrice": 5.6025,
  "timestamp": "2026-09-29T10:00:00Z"
}
```

### 4.3. Evento: `AlertTriggeredEvent`
* **Produtor:** `01-alert-engine-service`
* **Exchange:** `alert.events`
* **Routing key:** `alert.triggered`
* **Consumidor:** `03-notification-service`, na fila própria `notification.alert-triggered`.
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

## 5. Modelagem de Dados

### 5.1. `02-market-data-ingestion-service` (MongoDB)

* **Collection:** `market_instruments`
  * `_id` (String, PK) — símbolo do par, por exemplo `USD-BRL`.
  * `description` (String)
  * `catalog_version` (Long)
  * `updated_at` (Instant)
* **Collection:** `market_quotes`
  * `_id` (String, PK) — símbolo do par.
  * `current_price` (Decimal128)
  * `quoted_at` (Instant)
  * `received_at` (Instant)

`market_instruments` é o catálogo interno do serviço de ingestão. `market_quotes` armazena apenas a última cotação de cada par; histórico de cotações, se necessário, deve usar uma collection própria.

### 5.2. `01-alert-engine-service` (PostgreSQL)

Entidade principal mantida no PostgreSQL:

* **Tabela:** `tb_alerts`
  * `id` (UUID, PK)
  * `user_email` (VARCHAR, Not Null)
  * `symbol` (VARCHAR, Not Null) — Ex: `USD-BRL`, `BTC-USD`
  * `target_price` (DECIMAL, Not Null)
  * `status` (VARCHAR, Not Null) — Valores: `PENDING`, `TRIGGERED`, `CANCELLED`
  * `created_at` (TIMESTAMP, Not Null)
  * `updated_at` (TIMESTAMP)

* **Tabela de projeção:** `tb_available_market_pairs`
  * `symbol` (VARCHAR, PK)
  * `description` (VARCHAR, Not Null)
  * `catalog_version` (BIGINT, Not Null)
  * `updated_at` (TIMESTAMP, Not Null)

Essa tabela é alimentada exclusivamente por `CatalogUpdatedEvent`. Ela é uma projeção de leitura local, e não uma fonte de verdade compartilhada entre os serviços.

---

## 6. Pilha Tecnológica e Infraestrutura

* **Linguagem & Framework:** Java 17/21 com Spring Boot 3.x
* **Acesso a Dados:** Spring Data JPA + Hibernate; Spring Data MongoDB
* **Banco de Dados Relacional:** PostgreSQL 15+
* **Banco de Dados Documental:** MongoDB 8+ para o `02-market-data-ingestion-service`
* **Mensageria:** RabbitMQ
* **Documentação de API:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
* **Conteinerização:** Docker & Docker Compose (para orquestração do ambiente de desenvolvimento local)
