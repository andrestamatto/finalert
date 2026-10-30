# Operação e teste da observabilidade local

Este roteiro usa o **Git Bash** como terminal de trabalho e encaminha o Docker para o WSL. A stack é opcional: o FinAlert continua funcionando com apenas `compose.yaml`.

## 1. Pré-requisitos no WSL

No terminal do WSL, confirme o Docker e instale apenas as ferramentas auxiliares:

```bash
sudo apt update
sudo apt install -y curl jq ca-certificates

docker version
docker compose version
```

Grafana, Prometheus, Loki, Tempo e Alloy não precisam ser instalados no WSL; são executados como containers com versões fixadas no overlay.

## 2. Prepare o Git Bash

Na raiz do repositório:

```bash
source ./.finalert-test-functions.sh
type docker finalert_check_observability finalert_run_e2e
```

A função `docker` encaminha os comandos para `wsl.exe docker`. Se o Docker não responder, abra o WSL e confirme que o daemon está ativo antes de continuar.

As credenciais locais do Grafana possuem estes padrões:

```bash
export FINALERT_GRAFANA_URL='http://localhost:3000'
export FINALERT_GRAFANA_USERNAME='admin'
export FINALERT_GRAFANA_PASSWORD='finalert-local'
```

Altere `GRAFANA_ADMIN_USER` e `GRAFANA_ADMIN_PASSWORD` ao subir o Compose se quiser substituir os valores padrão. Use os mesmos valores nas variáveis `FINALERT_GRAFANA_*` para executar as funções de teste.

## 3. Compile e suba o ambiente

Compile os três serviços com Java 21 disponível no Git Bash:

```bash
export JAVA_HOME='<caminho-do-jdk-21-no-formato-do-git-bash>'
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build
```

Suba a solução e o overlay:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  --profile observability \
  up --build -d
```

O RabbitMQ pode levar cerca de um minuto para recuperar o volume e ficar saudável. Acompanhe sem reiniciar os containers durante esse período:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  --profile observability \
  ps
```

## 4. Valide os quatro sinais

Depois que os serviços estiverem iniciados, execute:

```bash
finalert_check_observability
```

A função retorna código diferente de zero se algum serviço HTTP estiver indisponível, existir target Prometheus `DOWN`, faltar log de uma aplicação no Loki ou o Tempo ainda não possuir traces. Também é possível executar cada verificação separadamente:

```bash
finalert_observability_status
finalert_prometheus_targets
finalert_loki_services
finalert_tempo_traces
```

Se o Tempo ainda estiver vazio em uma instalação nova, execute o fluxo ponta a ponta e repita a verificação:

```bash
finalert_run_e2e
finalert_check_observability
```

## 5. Use o Grafana

Abra `http://localhost:3000`, autentique-se e acesse **Dashboards > FinAlert > FinAlert Overview**. Os datasources Prometheus, Loki e Tempo são provisionados automaticamente.

Consultas úteis no **Explore**:

- Prometheus: `up{job=~"finalert-.*"}`;
- Prometheus: `rabbitmq_queue_messages`;
- Prometheus: `finalert_outbox_events`;
- Prometheus: `finalert_outbox_oldest_age_seconds{status=~"PENDING|PROCESSING"}`;
- Prometheus: `finalert_market_price_last_success_age_seconds`;
- Prometheus: `finalert_notification_process_total`;
- Loki: `{service_name="alert-engine"}`;
- Loki: `{service_name="notification"} | json | level="ERROR"`;
- Tempo: selecione **Search** e filtre por `service.name`.

Prometheus, Loki e Tempo não publicam portas no host. Consultas automatizadas passam pelo proxy autenticado do Grafana, como fazem as funções do dotfile.

Antes da primeira cotação confirmada pelo RabbitMQ, `finalert_market_price_last_success_age_seconds` possui valor `NaN`. Isso representa ausência de amostra e evita um alerta falso logo após a inicialização. Depois de `finalert_run_e2e`, a métrica passa a informar a idade, em segundos, da última publicação confirmada.

## 6. Diagnóstico

Consulte primeiro o estado dos containers:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  --profile observability \
  ps
```

Depois consulte os logs da camada correspondente:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  --profile observability \
  logs --tail=200 grafana prometheus loki tempo alloy

docker compose logs --tail=200 \
  alert-engine market-data-ingestion notification rabbitmq
```

| Sintoma | Verificação |
|---|---|
| Grafana responde, mas o dashboard está vazio | intervalo de tempo e estado dos targets Prometheus |
| Target de aplicação está `DOWN` | `/actuator/prometheus`, health da aplicação e rede Compose |
| Loki não mostra um serviço | logs do Alloy e acesso read-only ao socket Docker |
| Tempo não mostra traces | sampling, endpoint OTLP e logs do Alloy/Tempo |
| Warning de envio para `/v1/metrics` | `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=false`; métricas usam scrape Prometheus |
| RabbitMQ demora a iniciar | aguarde o `start_period`; inspecione o volume antes de removê-lo |

## 7. Encerre o ambiente

Preserve os dados de telemetria:

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  --profile observability \
  down
```

Use `down -v` somente quando quiser apagar também históricos, dashboards locais e dados dos bancos. Essa operação é destrutiva.
