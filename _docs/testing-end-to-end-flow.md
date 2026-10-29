# Teste ponta a ponta do FinAlert

Este guia testa o fluxo completo de criação e disparo de um alerta até a entrega do e-mail no Mailpit.

O roteiro principal publica o catálogo e a cotação pelo HTTP API do RabbitMQ. Isso torna o teste determinístico e evita depender do horário do scheduler de catálogo ou da disponibilidade da AwesomeAPI.

## 1. Fluxo exercitado

```text
POST /alerts/{symbol}
    → alerta e AlertMonitoringRequestedEvent na outbox PostgreSQL
    → market-data-ingestion registra o alerta no MongoDB
    → MarketPriceUpdatedEvent
    → alert-engine marca o alerta como TRIGGERED
    → AlertTriggeredEvent na outbox PostgreSQL
    → notification-service envia o e-mail ao Mailpit
    → market-data-ingestion remove o alerta do monitoramento
```

## 2. Pré-requisitos

- Java 21;
- Docker com Docker Compose;
- PowerShell;
- portas `5432`, `5433`, `5672`, `8080`, `8081`, `8082`, `8025`, `1025`, `15672` e `27017` disponíveis.

Os comandos usam `curl.exe` para evitar o alias `curl` do Windows PowerShell.

O profile `local` não deve ser ativado nos containers. O arquivo `application-local.yaml` serve para executar o serviço pela IDE contra dependências expostas em `localhost`. No Compose, as variáveis de ambiente já apontam para os nomes internos dos containers.

Cada serviço possui configuração e credenciais próprias. No host, o PostgreSQL do alert engine usa a porta `5432`, enquanto o PostgreSQL de notificações usa `5433`. Os conteúdos esperados dos arquivos locais estão documentados em [Configuração dos arquivos `.local.env`](#configuração-dos-arquivos-localenv).

Os arquivos `application-local.example.yaml` de cada módulo são templates versionados. Copie-os para `application-local.yaml` quando preparar uma nova estação. O `compose.override.example.yaml` segue a mesma regra para o Compose local. Os arquivos sem `.example` permanecem ignorados pelo Git.

Execute os comandos a partir da raiz do repositório.

## 3. Compile os serviços

Configure o JDK 21 para a sessão atual, se necessário:

```powershell
$env:JAVA_HOME = 'C:\Users\André Stamatto\.jdks\azul-21.0.12.1'
```

Compile e execute todos os testes:

```powershell
.\gradlew.bat clean build
```

O resultado esperado é:

```text
BUILD SUCCESSFUL
```

Também devem existir estes artefatos:

```text
01-alert-engine-service/build/libs/app.jar
02-market-data-ingestion-service/build/libs/app.jar
03-notification-service/build/libs/app.jar
```

## 4. Suba o ambiente

Para preservar os dados já existentes:

```powershell
docker compose -f compose.yaml up --build -d
```

Para um teste totalmente limpo, remova antes os containers e volumes:

```powershell
docker compose -f compose.yaml down -v
docker compose -f compose.yaml up --build -d
```

> Atenção: `down -v` apaga todos os dados locais do PostgreSQL, MongoDB e RabbitMQ.

Confira o estado dos containers:

```powershell
docker compose -f compose.yaml ps
```

Os containers de infraestrutura devem ficar `healthy`. A primeira inicialização do MongoDB pode levar cerca de 30 segundos por causa da criação do replica set.

Confira o RabbitMQ:

```powershell
curl.exe -sS `
  -u "finalert:finalert-local" `
  http://localhost:15672/api/overview
```

Se algum serviço não iniciar, consulte os logs:

```powershell
docker compose -f compose.yaml logs --tail=200 `
  alert-engine-postgres notification-postgres mongodb rabbitmq `
  alert-engine market-data-ingestion notification
```

## 5. Publique um catálogo conhecido

O alert engine só permite criar alertas para símbolos presentes em sua projeção do catálogo. Publique manualmente um catálogo contendo `USD-BRL`:

```powershell
$catalogEventId = [guid]::NewGuid().ToString()
$catalogUpdatedAt = [DateTimeOffset]::UtcNow.ToString('o')
$catalogVersion = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$catalogEvent = @{
    eventId   = $catalogEventId
    catalog   = @{
        'USD-BRL' = 'USD/BRL'
    }
    updatedAt = $catalogUpdatedAt
    version   = $catalogVersion
} | ConvertTo-Json -Compress

$catalogPublication = @{
    properties       = @{
        content_type = 'application/json'
    }
    routing_key      = 'market.catalog.updated'
    payload          = $catalogEvent
    payload_encoding = 'string'
} | ConvertTo-Json -Compress

curl.exe -sS `
  -u "finalert:finalert-local" `
  -H "Content-Type: application/json" `
  -X POST `
  --data-raw $catalogPublication `
  "http://localhost:15672/api/exchanges/%2F/market.events/publish"
```

Resposta esperada:

```json
{"routed":true}
```

Aguarde o consumo:

```powershell
Start-Sleep -Seconds 2
```

Se a resposta contiver `"routed":false`, confirme que o `alert-engine` iniciou e declarou a fila `market-catalog-updated`.

## 6. Crie um alerta

Use um preço alto para impedir que uma cotação real da AwesomeAPI dispare o alerta antes da publicação manual:

```powershell
$idempotencyKey = 'e2e-' + [guid]::NewGuid().ToString()

$alertRequest = @{
    userEmail       = 'user@example.com'
    targetPrice     = 999.00
    triggerOperator = 'GTE'
} | ConvertTo-Json -Compress

curl.exe -i -sS `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: $idempotencyKey" `
  -X POST `
  --data-raw $alertRequest `
  "http://localhost:8080/alerts/USD-BRL"
```

Resultado esperado:

```text
HTTP/1.1 201 Created
```

O corpo contém o `alertId`. Após alguns segundos, o `AlertMonitoringRequestedEvent` deve ter sido publicado pela outbox e consumido pelo serviço de ingestão.

Confira os logs:

```powershell
docker compose -f compose.yaml logs --tail=100 alert-engine market-data-ingestion
```

## 7. Publique uma cotação que satisfaça o alerta

O preço `1000.00` satisfaz a condição `GTE 999.00`:

```powershell
$priceEventId = [guid]::NewGuid().ToString()
$quotedAt = [DateTimeOffset]::UtcNow.ToString('o')

$priceEvent = @{
    eventId      = $priceEventId
    symbol       = 'USD-BRL'
    currentPrice = 1000.00
    occurredAt   = $quotedAt
} | ConvertTo-Json -Compress

$pricePublication = @{
    properties       = @{
        content_type = 'application/json'
    }
    routing_key      = 'market.price.updated'
    payload          = $priceEvent
    payload_encoding = 'string'
} | ConvertTo-Json -Compress

curl.exe -sS `
  -u "finalert:finalert-local" `
  -H "Content-Type: application/json" `
  -X POST `
  --data-raw $pricePublication `
  "http://localhost:15672/api/exchanges/%2F/market.events/publish"
```

Resposta esperada:

```json
{"routed":true}
```

Aguarde a outbox e os dois consumidores de `AlertTriggeredEvent`:

```powershell
Start-Sleep -Seconds 5
```

## 8. Verifique o e-mail

Liste as mensagens capturadas pelo Mailpit:

```powershell
curl.exe -sS http://localhost:8025/api/v1/messages
```

Para exibir os principais campos:

```powershell
$mailbox = curl.exe -sS http://localhost:8025/api/v1/messages |
    ConvertFrom-Json

$mailbox.messages |
    Select-Object Subject, From, To, Created
```

Deve existir uma mensagem para `user@example.com` com assunto semelhante a:

```text
FinAlert: alerta de USD-BRL atingido
```

A interface visual está disponível em:

```text
http://localhost:8025
```

## 9. Teste a idempotência

Guarde a quantidade atual de mensagens:

```powershell
$totalBeforeReplay = (
    curl.exe -sS http://localhost:8025/api/v1/messages |
        ConvertFrom-Json
).total
```

Publique exatamente o mesmo `MarketPriceUpdatedEvent` novamente:

```powershell
curl.exe -sS `
  -u "finalert:finalert-local" `
  -H "Content-Type: application/json" `
  -X POST `
  --data-raw $pricePublication `
  "http://localhost:15672/api/exchanges/%2F/market.events/publish"

Start-Sleep -Seconds 5
```

Consulte novamente:

```powershell
$totalAfterReplay = (
    curl.exe -sS http://localhost:8025/api/v1/messages |
        ConvertFrom-Json
).total

[PSCustomObject]@{
    BeforeReplay = $totalBeforeReplay
    AfterReplay  = $totalAfterReplay
    Idempotent   = $totalBeforeReplay -eq $totalAfterReplay
}
```

Resultado esperado:

```text
Idempotent = True
```

O alerta já está `TRIGGERED`, portanto a cotação repetida não produz um segundo evento nem outro e-mail.

Também é possível repetir o `POST /alerts/USD-BRL` com a mesma `Idempotency-Key` e o mesmo corpo. O serviço deve retornar o mesmo `alertId`. Reutilizar a chave com outro corpo deve retornar `409 Conflict`.

## 10. Verifique as dead-letter queues

```powershell
$queues = @(
    'market-price-updated.dlq',
    'market-catalog-updated.dlq',
    'market-price-alert.dlq',
    'alert-triggered-monitoring-release.dlq',
    'alert-triggered-notification.dlq'
)

foreach ($queue in $queues) {
    $encodedQueue = [uri]::EscapeDataString($queue)

    $result = curl.exe -sS `
        -u "finalert:finalert-local" `
        "http://localhost:15672/api/queues/%2F/$encodedQueue" |
        ConvertFrom-Json

    [PSCustomObject]@{
        Queue    = $queue
        Messages = $result.messages
    }
}
```

Todas as filas devem apresentar `Messages = 0`.

## 11. Teste opcional com a AwesomeAPI

O roteiro anterior substitui apenas a obtenção externa da cotação. Para testar também o scheduler e o publisher do `market-data-ingestion-service`, crie outro alerta que qualquer cotação real de `USD-BRL` satisfaça:

```powershell
$automaticKey = 'e2e-awesome-' + [guid]::NewGuid().ToString()

$automaticAlert = @{
    userEmail       = 'awesome@example.com'
    targetPrice     = 999999.00
    triggerOperator = 'LTE'
} | ConvertTo-Json -Compress

curl.exe -i -sS `
  -H "Content-Type: application/json" `
  -H "Idempotency-Key: $automaticKey" `
  -X POST `
  --data-raw $automaticAlert `
  "http://localhost:8080/alerts/USD-BRL"
```

O scheduler consulta preços a cada 30 segundos. Aguarde até 40 segundos e consulte o Mailpit:

```powershell
Start-Sleep -Seconds 40
curl.exe -sS http://localhost:8025/api/v1/messages
```

Esse teste depende da conectividade com a AwesomeAPI e de sua disponibilidade.

## 12. Diagnóstico

### MongoDB não inicia

Confira os logs:

```powershell
docker logs finalert-mongodb --tail 300
```

Recrie somente o container, preservando o volume:

```powershell
docker compose -f compose.yaml up -d --force-recreate mongodb
```

Se o volume estiver parcialmente inicializado e a recriação não resolver, use um ambiente limpo:

```powershell
docker compose -f compose.yaml down -v
docker compose -f compose.yaml up --build -d
```

### Criação do alerta retorna `422`

O símbolo ainda não chegou à projeção local do catálogo. Repita a etapa 5, confirme `"routed":true`, aguarde dois segundos e tente novamente.

### Publicação retorna `"routed":false`

A fila correspondente ainda não foi declarada. Confira se os serviços estão ativos e inspecione os logs:

```powershell
docker compose -f compose.yaml ps
docker compose -f compose.yaml logs --tail=200 alert-engine market-data-ingestion notification
```

### O e-mail não aparece

Verifique, nesta ordem:

```powershell
docker compose -f compose.yaml logs --tail=200 alert-engine
docker compose -f compose.yaml logs --tail=200 notification
curl.exe -sS http://localhost:8025/api/v1/messages
```

Depois confira `alert-triggered-notification.dlq` usando a etapa 10.

## 13. Encerramento

Pare os containers preservando os dados:

```powershell
docker compose -f compose.yaml down
```

Pare os containers e remova todos os dados locais:

```powershell
docker compose -f compose.yaml down -v
```

## Configuração dos arquivos `.local.env`

### `alert-engine-service.local.env`

```properties
ALERT_ENGINE_DB_URL=jdbc:postgresql://localhost:5432/finalert_alert_engine
ALERT_ENGINE_DB_USERNAME=finalert_alert_engine
ALERT_ENGINE_DB_PASSWORD=finalert-local
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=finalert
RABBITMQ_PASSWORD=finalert-local
```

### `market-data-ingestion-service.local.env`

```properties
MARKET_DATA_MONGODB_URI=mongodb://finalert:finalert-local@localhost:27017/finalert_market_data?authSource=admin&uuidRepresentation=standard&replicaSet=rs0&directConnection=true
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=finalert
RABBITMQ_PASSWORD=finalert-local
AWESOMEAPI_KEY=
AWESOMEAPI_URL=https://economia.awesomeapi.com.br/json
```

### `notification-service.local.env`

```properties
NOTIFICATION_DB_URL=jdbc:postgresql://localhost:5433/finalert_notification
NOTIFICATION_DB_USERNAME=finalert_notification
NOTIFICATION_DB_PASSWORD=finalert-local
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=finalert
RABBITMQ_PASSWORD=finalert-local
MAIL_HOST=localhost
MAIL_PORT=1025
NOTIFICATION_FROM_ADDRESS=alerts@finalert.local
```
