# Teste ponta a ponta do FinAlert

Este guia testa o fluxo completo de criação e disparo de um alerta até a entrega do e-mail no Mailpit.

Os comandos são orientados ao Git Bash no Windows. O Docker é executado pelo WSL, onde estão as imagens e os containers. O catálogo e a cotação são publicados pelo HTTP API do RabbitMQ para tornar o teste determinístico, sem depender do horário do scheduler ou da disponibilidade da AwesomeAPI.

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

- Git Bash;
- Java 21;
- WSL 2 com Docker e Docker Compose;
- OpenSSL, incluído no Git for Windows;
- portas `5432`, `5433`, `5672`, `8080`, `8081`, `8082`, `8025`, `1025`, `15672` e `27017` disponíveis.

Execute todos os comandos a partir da raiz do repositório.

### Importe as funções de teste

O dotfile `.finalert-test-functions.sh` contém a função que encaminha o Docker ao WSL e as funções auxiliares usadas neste roteiro. Importe-o uma vez em cada nova sessão do Git Bash:

```bash
source ./.finalert-test-functions.sh
```

O WSL herda o diretório atual do Git Bash. Confirme o acesso ao daemon e valide o Compose:

```bash
docker version
docker compose version
docker compose -f compose.yaml config --quiet
```

Se o Docker já estiver diretamente disponível no Git Bash, remova ou sobrescreva a função `docker` após importar o dotfile.

### Configuração local

O profile `local` não deve ser ativado nos containers. O arquivo `application-local.yaml` serve para executar um serviço pela IDE contra dependências expostas em `localhost`. No Compose, as variáveis já apontam para os nomes internos dos containers.

Cada serviço possui configuração e credenciais próprias. No host, o PostgreSQL do alert engine usa a porta `5432`, enquanto o PostgreSQL de notificações usa `5433`.

Os arquivos `application-local.example.yaml` são templates versionados. Copie-os para `application-local.yaml` ao preparar uma nova estação. O `compose.override.example.yaml` segue a mesma regra para o Compose local. Os arquivos sem `.example` permanecem ignorados pelo Git.

Os conteúdos esperados dos `.local.env` estão em [Configuração dos arquivos `.local.env`](#configuração-dos-arquivos-localenv).

## 3. Compile os serviços

Configure o JDK 21 para a sessão do Git Bash, ajustando o caminho se necessário:

```bash
export JAVA_HOME='/c/Users/André Stamatto/.jdks/azul-21.0.12.1'
export PATH="$JAVA_HOME/bin:$PATH"
```

Compile e execute todos os testes:

```bash
./gradlew clean build
```

O resultado esperado é:

```text
BUILD SUCCESSFUL
```

Também devem existir:

```text
01-alert-engine-service/build/libs/app.jar
02-market-data-ingestion-service/build/libs/app.jar
03-notification-service/build/libs/app.jar
```

## 4. Suba o ambiente

Para preservar os dados existentes:

```bash
docker compose -f compose.yaml up --build -d
```

Para um teste totalmente limpo:

```bash
docker compose -f compose.yaml down -v --remove-orphans
docker compose -f compose.yaml up --build -d
```

> Atenção: `down -v` apaga todos os dados locais do PostgreSQL, MongoDB e RabbitMQ.

Confira o estado dos containers:

```bash
docker compose -f compose.yaml ps
```

Os containers de infraestrutura devem ficar `healthy`. A primeira inicialização do MongoDB pode levar cerca de 30 segundos por causa da criação do replica set.

Confira o RabbitMQ:

```bash
curl -sS \
  -u 'finalert:finalert-local' \
  http://localhost:15672/api/overview
```

Se algum serviço não iniciar, consulte os logs:

```bash
docker compose -f compose.yaml logs --tail=200 \
  alert-engine-postgres notification-postgres mongodb rabbitmq \
  alert-engine market-data-ingestion notification
```

## 5. Funções auxiliares disponíveis

O dotfile importado na etapa 2 disponibiliza:

- `docker`: encaminha os comandos Docker ao WSL;
- `new_uuid`: produz identificadores aceitos por `UUID.fromString` usando o OpenSSL;
- `mail_total`: extrai o total de mensagens retornado pelo Mailpit sem exigir `jq`.

Confirme que as funções estão disponíveis na sessão:

```bash
type docker new_uuid mail_total
```

## 6. Publique um catálogo conhecido

O alert engine só aceita símbolos presentes em sua projeção do catálogo. Monte um evento contendo `USD-BRL`:

```bash
catalog_event_id="$(new_uuid)"
catalog_updated_at="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
catalog_version="$(date +%s)000"

catalog_event="$(printf \
  '{"eventId":"%s","catalog":{"USD-BRL":"USD/BRL"},"updatedAt":"%s","version":%s}' \
  "$catalog_event_id" \
  "$catalog_updated_at" \
  "$catalog_version")"
```

O HTTP API do RabbitMQ recebe o evento interno em Base64, evitando escapes JSON aninhados:

```bash
catalog_payload="$(printf '%s' "$catalog_event" | base64 | tr -d '\r\n')"

catalog_publication="$(printf \
  '{"properties":{"content_type":"application/json"},"routing_key":"market.catalog.updated","payload":"%s","payload_encoding":"base64"}' \
  "$catalog_payload")"

curl -sS \
  -u 'finalert:finalert-local' \
  -H 'Content-Type: application/json' \
  -X POST \
  --data-raw "$catalog_publication" \
  'http://localhost:15672/api/exchanges/%2F/market.events/publish'
```

Resposta esperada:

```json
{"routed":true}
```

Aguarde o consumo:

```bash
sleep 2
```

Se a resposta contiver `"routed":false`, confirme que o `alert-engine` iniciou e declarou a fila `market-catalog-updated`.

## 7. Crie um alerta

Use um preço alto para impedir que uma cotação real da AwesomeAPI dispare o alerta antes da publicação manual:

```bash
idempotency_key="e2e-$(new_uuid)"
alert_request='{"userEmail":"user@example.com","targetPrice":999.00,"triggerOperator":"GTE"}'

curl -i -sS \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $idempotency_key" \
  -X POST \
  --data-raw "$alert_request" \
  'http://localhost:8080/alerts/USD-BRL'
```

Resultado esperado:

```text
HTTP/1.1 201 Created
```

O corpo contém o `alertId`. Após alguns segundos, o `AlertMonitoringRequestedEvent` deve ter sido publicado pela outbox e consumido pelo serviço de ingestão.

Confira os logs:

```bash
docker compose -f compose.yaml logs --tail=100 \
  alert-engine market-data-ingestion
```

## 8. Publique uma cotação que satisfaça o alerta

O preço `1000.00` satisfaz `GTE 999.00`:

```bash
price_event_id="$(new_uuid)"
quoted_at="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"

price_event="$(printf \
  '{"eventId":"%s","symbol":"USD-BRL","currentPrice":1000.00,"occurredAt":"%s"}' \
  "$price_event_id" \
  "$quoted_at")"

price_payload="$(printf '%s' "$price_event" | base64 | tr -d '\r\n')"

price_publication="$(printf \
  '{"properties":{"content_type":"application/json"},"routing_key":"market.price.updated","payload":"%s","payload_encoding":"base64"}' \
  "$price_payload")"

curl -sS \
  -u 'finalert:finalert-local' \
  -H 'Content-Type: application/json' \
  -X POST \
  --data-raw "$price_publication" \
  'http://localhost:15672/api/exchanges/%2F/market.events/publish'
```

Resposta esperada:

```json
{"routed":true}
```

Aguarde a outbox e os dois consumidores de `AlertTriggeredEvent`:

```bash
sleep 5
```

## 9. Verifique o e-mail

Liste as mensagens capturadas pelo Mailpit:

```bash
curl -sS http://localhost:8025/api/v1/messages
```

Se `jq` estiver instalado, exiba apenas os campos principais:

```bash
curl -sS http://localhost:8025/api/v1/messages \
  | jq '.messages[] | {Subject, From, To, Created}'
```

Deve existir uma mensagem para `user@example.com` com assunto semelhante a:

```text
FinAlert: alerta de USD-BRL atingido
```

A interface visual está em:

```text
http://localhost:8025
```

## 10. Teste a idempotência

Guarde a quantidade atual de mensagens:

```bash
total_before_replay="$(mail_total)"
```

Publique exatamente o mesmo `MarketPriceUpdatedEvent` novamente:

```bash
curl -sS \
  -u 'finalert:finalert-local' \
  -H 'Content-Type: application/json' \
  -X POST \
  --data-raw "$price_publication" \
  'http://localhost:15672/api/exchanges/%2F/market.events/publish'

sleep 5
```

Compare o total:

```bash
total_after_replay="$(mail_total)"

printf 'Antes do replay: %s\n' "$total_before_replay"
printf 'Depois do replay: %s\n' "$total_after_replay"

if [[ "$total_before_replay" == "$total_after_replay" ]]; then
  echo 'Idempotente: true'
else
  echo 'Idempotente: false'
fi
```

Resultado esperado:

```text
Idempotente: true
```

O alerta já está `TRIGGERED`; portanto, a cotação repetida não produz um segundo evento nem outro e-mail.

Também é possível repetir o `POST /alerts/USD-BRL` com a mesma `Idempotency-Key` e o mesmo corpo. O serviço deve retornar o mesmo `alertId`. Reutilizar a chave com outro corpo deve retornar `409 Conflict`.

## 11. Verifique as dead-letter queues

```bash
queues=(
  'market-price-updated.dlq'
  'market-catalog-updated.dlq'
  'market-price-alert.dlq'
  'alert-triggered-monitoring-release.dlq'
  'alert-triggered-notification.dlq'
)

for queue in "${queues[@]}"; do
  messages="$(
    curl -sS \
      -u 'finalert:finalert-local' \
      "http://localhost:15672/api/queues/%2F/$queue" \
      | tr -d '\r\n' \
      | sed -nE 's/.*"messages"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/p'
  )"

  printf '%-45s %s\n' "$queue" "${messages:-indisponível}"
done
```

Todas as filas devem apresentar `0`.

## 12. Teste opcional com a AwesomeAPI

O roteiro anterior substitui somente a obtenção externa da cotação. Para testar também o scheduler e o publisher do `market-data-ingestion-service`, crie outro alerta que qualquer cotação real de `USD-BRL` satisfaça:

```bash
automatic_key="e2e-awesome-$(new_uuid)"
automatic_alert='{"userEmail":"awesome@example.com","targetPrice":999999.00,"triggerOperator":"LTE"}'

curl -i -sS \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $automatic_key" \
  -X POST \
  --data-raw "$automatic_alert" \
  'http://localhost:8080/alerts/USD-BRL'
```

O scheduler consulta preços a cada 30 segundos. Aguarde até 40 segundos e consulte o Mailpit:

```bash
sleep 40
curl -sS http://localhost:8025/api/v1/messages
```

Esse teste depende da conectividade e disponibilidade da AwesomeAPI.

## 13. Diagnóstico

### O Compose informa `dependency alert-engine-postgres failed to start`

Na primeira criação do volume, o PostgreSQL pode levar mais de um minuto para inicializar no WSL. O health check possui uma janela inicial de 90 segundos para acomodar esse processo.

Confira se o banco terminou a inicialização:

```bash
docker compose -f compose.yaml ps alert-engine-postgres
docker logs finalert-alert-engine-postgres --tail 300
```

Se o container já estiver `healthy`, execute novamente o comando de subida para iniciar os serviços que ficaram no estado `Created` quando o Compose interrompeu a primeira tentativa:

```bash
docker compose -f compose.yaml up -d
```

Não remova o volume para esse caso: a segunda execução reutiliza o banco que acabou de ser inicializado.

### MongoDB não inicia

```bash
docker logs finalert-mongodb --tail 300
```

Recrie somente o container, preservando o volume:

```bash
docker compose -f compose.yaml up -d --force-recreate mongodb
```

Se o volume estiver parcialmente inicializado:

```bash
docker compose -f compose.yaml down -v --remove-orphans
docker compose -f compose.yaml up --build -d
```

### Criação do alerta retorna `422`

O símbolo ainda não chegou à projeção do catálogo. Repita a etapa 6, confirme `"routed":true`, aguarde dois segundos e tente novamente.

### Publicação retorna `"routed":false`

A fila ainda não foi declarada. Confira os serviços e os logs:

```bash
docker compose -f compose.yaml ps
docker compose -f compose.yaml logs --tail=200 \
  alert-engine market-data-ingestion notification
```

### O e-mail não aparece

```bash
docker compose -f compose.yaml logs --tail=200 alert-engine
docker compose -f compose.yaml logs --tail=200 notification
curl -sS http://localhost:8025/api/v1/messages
```

Depois confira `alert-triggered-notification.dlq` conforme a etapa 11.

## 14. Encerramento

Pare os containers preservando os dados:

```bash
docker compose -f compose.yaml down --remove-orphans
```

Pare os containers e remova todos os dados locais:

```bash
docker compose -f compose.yaml down -v --remove-orphans
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
