# FinAlert — Documentação de Arquitetura da Solução

## 1. Visão geral

O **FinAlert** é uma plataforma orientada a eventos para cadastrar alertas de preço, monitorar cotações de mercado e, futuramente, notificar usuários quando as condições configuradas forem satisfeitas.

Este documento distingue o que está implementado atualmente do que ainda está planejado. Os nomes dos eventos, exchanges, routing keys, filas, endpoints e métodos abaixo refletem o código e os arquivos `application-local.yaml` dos serviços.

## 2. Arquitetura atual

```mermaid
flowchart LR
    classDef done fill:#d1fae5,stroke:#047857,color:#064e3b,stroke-width:2px
    classDef partial fill:#fef3c7,stroke:#b45309,color:#78350f,stroke-width:2px
    classDef todo fill:#f3f4f6,stroke:#6b7280,color:#374151,stroke-width:2px,stroke-dasharray:5 5
    classDef infra fill:#dbeafe,stroke:#1d4ed8,color:#1e3a8a

    User["Usuário"]

    subgraph AlertEngine["01 alert-engine-service"]
        CreateAlert --> AlertOutbox["✅ Outbox transacional<br/>AlertMonitoringRequestedEvent"]:::done
        CatalogConsumer["✅ Consumir catálogo<br/>e persistir projeção idempotente"]:::done
        PriceConsumer["⬜ Consumir<br/>MarketPriceUpdatedEvent"]:::todo
        EvaluateAlert["⬜ Avaliar condição<br/>e disparar alerta"]:::todo
        ReleaseEvent["⬜ Publicar<br/>AlertMonitoringReleasedEvent"]:::todo
        TriggerEvent["⬜ Publicar<br/>AlertTriggeredEvent"]:::todo

        PriceConsumer -.-> EvaluateAlert
        EvaluateAlert -.-> ReleaseEvent
        EvaluateAlert -.-> TriggerEvent
    end

    User -->|"POST /alerts/{symbol}"| CreateAlert
    AlertOutbox -->|"alert.monitoring.requested"| AlertExchange[("RabbitMQ<br/>alert.events")]:::infra

    subgraph MarketData["02 market-data-ingestion-service"]
        MonitoringConsumer["✅ Consumir solicitação<br/>e registrar monitored_alerts"]:::done
        CatalogRefresh["✅ Atualizar catálogo<br/>via /available"]:::done
        CatalogOutbox["✅ Outbox Mongo<br/>retry, redrive e cleanup"]:::done
        PricePolling["✅ Consultar cotações<br/>via /last/{symbols}"]:::done
        PricePublisher["✅ Publicar um evento<br/>por cotação"]:::done
        ReleaseConsumer["⬜ Consumir liberação<br/>e remover monitoramento"]:::todo

        CatalogRefresh --> CatalogOutbox
        PricePolling --> PricePublisher
    end

    AlertExchange -->|"fila market-price-alert"| MonitoringConsumer
    AlertExchange -.->|"alert.monitoring.released"| ReleaseConsumer

    Awesome["AwesomeAPI"]:::infra -->|"GET /available"| CatalogRefresh
    PricePolling -->|"GET /last/{symbols}"| Awesome

    CatalogOutbox -->|"market.catalog.updated"| MarketExchange[("RabbitMQ<br/>market.events")]:::infra
    PricePublisher -->|"market.price.updated"| MarketExchange
    MarketExchange -->|"fila market-catalog-updated"| CatalogConsumer
    MarketExchange -.->|"fila ainda não criada"| PriceConsumer

    MonitoringConsumer --> Mongo[("MongoDB<br/>monitored_alerts<br/>market_catalog<br/>market_catalog_outbox")]:::infra
    CatalogRefresh --> Mongo
    CatalogOutbox --> Mongo
    CreateAlert --> Postgres[("PostgreSQL<br/>tb_alerts<br/>tb_create_alert_requests<br/>tb_outbox_events<br/>tb_market_catalog")]:::infra
    AlertOutbox --> Postgres
    CatalogConsumer --> Postgres

    subgraph Notification["🟡 03 notification-service — módulo base criado"]
        TriggerConsumer["⬜ Consumir<br/>AlertTriggeredEvent"]:::todo
        SendNotification["⬜ Enviar notificação"]:::todo
        TriggerConsumer -.-> SendNotification
    end

    TriggerEvent -.->|"alert.triggered"| AlertExchange
    AlertExchange -.-> TriggerConsumer
```

**Legenda:** ✅ implementado no código · 🟡 estrutura parcial · ⬜ ainda não implementado. As setas contínuas representam integrações existentes; as tracejadas representam fluxos planejados.

| Fluxo funcional | Estado atual |
|---|---|
| Criação idempotente do alerta e publicação de `AlertMonitoringRequestedEvent` | ✅ Implementado de ponta a ponta |
| Registro dos alertas monitorados e consulta periódica de preços | ✅ Implementado |
| Atualização, outbox e consumo do catálogo de mercado | ✅ Implementado de ponta a ponta |
| Publicação de `MarketPriceUpdatedEvent` | ✅ Implementado até o RabbitMQ |
| Consumo do preço, avaliação da condição e disparo do alerta | ⬜ Pendente |
| Liberação do monitoramento após disparo ou cancelamento | ⬜ Pendente |
| Consumo de `AlertTriggeredEvent` e envio da notificação | 🟡 Módulo criado; comportamento pendente |
| Persistência de histórico de cotações (`market_quotes`) | ⬜ Pendente |

O `01-alert-engine-service` persiste o alerta e o evento de integração na mesma transação por meio do transactional outbox. O worker do outbox publica `AlertMonitoringRequestedEvent` em `alert.events` com a routing key `alert.monitoring.requested`.

O `02-market-data-ingestion-service` consome esse evento pela fila `market-price-alert`, mantém a projeção `monitored_alerts`, consulta a AwesomeAPI e publica atualizações em `market.events`. A atualização do catálogo usa uma outbox Mongo transacional, com claim atômico, retry, redrive e limpeza dos eventos publicados.

O consumidor de `market.catalog.updated` já está implementado no `01-alert-engine-service` e mantém `tb_market_catalog` de forma idempotente. O consumidor de `market.price.updated` e a avaliação dos alertas ainda não estão implementados.

### 2.1. Sequência implementada

```mermaid
sequenceDiagram
    actor U as Usuário
    participant C as CreateAlertController
    participant CAS as CreateAlertService
    participant O as Outbox
    participant R as RabbitMQ
    participant RC as RabbitMqAlertMonitoringRequestedEventConsumer
    participant RMS as RegisterMonitoredMarketPriceService
    participant CPS as CheckUpdatedPriceService
    participant API as AwesomeAPI

    U->>C: POST /alerts/{symbol}
    C->>CAS: execute(CreateAlertCommand)
    CAS->>CAS: persiste Alert PENDING
    CAS->>O: enqueue(AlertMonitoringRequestedEvent)
    CAS-->>C: Alert
    O->>R: alert.events / alert.monitoring.requested
    R->>RC: fila market-price-alert
    RC->>RMS: execute(MarketPriceAlert)
    RMS->>RMS: upsert em monitored_alerts

    loop a cada 30 segundos
        CPS->>CPS: busca símbolos monitorados
        CPS->>API: GET /last/{symbols}
        API-->>CPS: cotações atuais
        CPS->>R: MarketPriceUpdatedEvent<br/>market.events / market.price.updated
    end

    Note over R,CAS: O consumo de market.price.updated pelo alert-engine-service ainda será implementado.
```

### 2.2. Métodos de entrada atuais

| Entrada | Método chamado | Responsabilidade |
|---|---|---|
| `POST /alerts/{symbol}` | `CreateAlertService.execute(CreateAlertCommand)` | Cria o alerta idempotentemente e enfileira `AlertMonitoringRequestedEvent` no outbox. |
| `alert.monitoring.requested` | `RabbitMqAlertMonitoringRequestedEventConsumer.consume(AlertMonitoringRequestedEvent)` | Converte o evento e chama o caso de uso de registro. |
| Registro do monitoramento | `RegisterMonitoredMarketPriceService.execute(MarketPriceAlert)` | Faz upsert do alerta monitorado no MongoDB. |
| Scheduler de catálogo | `RefreshMarketCatalogService.execute()` | Atualiza `market_catalog` e grava `MarketCatalogUpdatedEvent` na outbox. |
| `market.catalog.updated` | `RabbitMqMarketCatalogUpdatedEvent.consume(MarketCatalogUpdatedMessage)` | Persiste o snapshot versionado em `tb_market_catalog`, ignorando o mesmo `eventId` quando reentregue. |
| Scheduler de cotações | `CheckUpdatedPriceService.execute()` | Consulta cotações e publica `MarketPriceUpdatedEvent`. |

## 3. Microsserviços

### 3.1. `01-alert-engine-service`

Responsabilidades implementadas:

1. Expõe `POST /alerts/{symbol}`.
2. Exige o header `Idempotency-Key`.
3. Persiste alertas em `tb_alerts`.
4. Controla idempotência em `tb_create_alert_requests`.
5. Persiste eventos em `tb_outbox_events` na mesma transação do alerta.
6. Publica `AlertMonitoringRequestedEvent` de forma assíncrona.
7. Aplica retry persistente, limite de tentativas e circuit breaker na publicação.
8. Consome `MarketCatalogUpdatedEvent` pela fila `market-catalog-updated`.
9. Mantém o histórico idempotente do catálogo em `tb_market_catalog`.

Ainda não implementado:

- consumo de `MarketPriceUpdatedEvent`;
- avaliação e disparo do alerta;
- `AlertMonitoringReleasedEvent`;
- `AlertTriggeredEvent`.

### 3.2. `02-market-data-ingestion-service`

Responsabilidades implementadas:

1. Consome `AlertMonitoringRequestedEvent`.
2. Mantém a projeção `monitored_alerts` no MongoDB.
3. Atualiza `market_catalog` a partir de `GET /available` da AwesomeAPI.
4. Publica `MarketCatalogUpdatedEvent`.
5. Consulta `GET /last/{symbols}` para os símbolos monitorados.
6. Publica um `MarketPriceUpdatedEvent` para cada cotação recebida.
7. Persiste catálogo e evento na mesma transação Mongo por meio de outbox.
8. Publica a outbox com retry, limite de tentativas, recuperação de locks, redrive e retenção configuráveis.

Ainda não implementado:

- persistência de `market_quotes`;
- consumo de `AlertMonitoringReleasedEvent`;
- remoção do monitoramento após disparo ou cancelamento do alerta.

### 3.3. `03-notification-service`

O módulo existe e possui dependências para RabbitMQ e e-mail, mas o consumo de `AlertTriggeredEvent` e o envio da notificação ainda não estão implementados.

## 4. Contratos de eventos implementados

### 4.1. `AlertMonitoringRequestedEvent`

- **Produtor:** `01-alert-engine-service`
- **Exchange:** `alert.events`
- **Routing key:** `alert.monitoring.requested`
- **Consumidor:** `02-market-data-ingestion-service`
- **Fila:** `market-price-alert`
- **Semântica:** informa que um alerta ativo precisa ter seu símbolo monitorado. O consumidor faz upsert por `alertId`.

```json
{
  "eventId": "9296af84-0c44-43df-9b61-62d8534ecddd",
  "alertId": "8f8373b0-2b1b-4f81-80d5-12a8a7051b9e",
  "symbol": "USD-BRL",
  "targetValue": 5.6000,
  "triggerOperator": "GTE",
  "occurredAt": "2026-10-02T10:00:00Z"
}
```

### 4.2. `MarketCatalogUpdatedEvent`

- **Produtor:** `02-market-data-ingestion-service`
- **Exchange:** `market.events`
- **Routing key:** `market.catalog.updated`
- **Consumidor:** `01-alert-engine-service`, onde o payload é recebido como `MarketCatalogUpdatedMessage`
- **Fila:** `market-catalog-updated`
- **Semântica:** representa um snapshot versionado do catálogo disponível no serviço de ingestão.

```json
{
  "eventId": "426c7962-d29e-4fa1-9d6c-294fb15e8f48",
  "catalog": {
    "USD-BRL": "Dólar Americano/Real Brasileiro",
    "BTC-BRL": "Bitcoin/Real Brasileiro"
  },
  "updatedAt": "2026-10-01T10:00:00Z",
  "version": 0
}
```

### 4.3. `MarketPriceUpdatedEvent`

- **Produtor:** `02-market-data-ingestion-service`
- **Exchange:** `market.events`
- **Routing key:** `market.price.updated`
- **Consumidor atual:** nenhum
- **Semântica:** representa a cotação mais recente de um único símbolo.

```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "symbol": "USD-BRL",
  "currentPrice": 5.6025,
  "occurredAt": "2026-09-29T10:00:00Z"
}
```

## 5. Contratos planejados

Os contratos abaixo descrevem a evolução pretendida, mas ainda não possuem produtor e consumidor completos no código atual:

| Evento | Exchange | Routing key | Finalidade |
|---|---|---|---|
| `AlertMonitoringReleasedEvent` | `alert.events` | `alert.monitoring.released` | Encerrar o monitoramento depois do disparo ou cancelamento. |
| `AlertTriggeredEvent` | `alert.events` | `alert.triggered` | Solicitar a entrega da notificação ao usuário. |

## 6. Persistência atual

### 6.1. MongoDB — `02-market-data-ingestion-service`

- `market_catalog`: histórico de snapshots do catálogo, com `_id` próprio e `catalogVersion` único;
- `market_catalog_outbox`: eventos do catálogo nos estados `PENDING`, `PROCESSING`, `PUBLISHED` ou `FAILED`;
- `monitored_alerts`: projeção dos alertas recebidos por `AlertMonitoringRequestedEvent`, identificada por `alertId`.

Não existe atualmente uma collection persistente de histórico ou de última cotação de preços (`market_quotes`).

### 6.2. PostgreSQL — `01-alert-engine-service`

- `tb_alerts`: alertas cadastrados;
- `tb_create_alert_requests`: controle de idempotência das requisições;
- `tb_outbox_events`: eventos pendentes, publicados ou definitivamente falhos;
- `tb_market_catalog`: histórico idempotente dos snapshots de catálogo, identificado pelo `eventId`.

## 7. Tecnologias atuais

- Java 21;
- Spring Boot 4.x;
- Spring Data JPA e Hibernate;
- Spring Data MongoDB;
- PostgreSQL 17;
- MongoDB 8;
- RabbitMQ 4;
- Resilience4j;
- Flyway;
- Docker e Docker Compose.
