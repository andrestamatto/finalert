#!/usr/bin/env bash

# Funcoes auxiliares para executar o roteiro de testes do FinAlert no Git Bash.
# Importe-as a partir da raiz do repositorio:
#   source ./.finalert-test-functions.sh

docker() {
  wsl.exe docker "$@"
}

new_uuid() {
  local hex
  hex="$(openssl rand -hex 16)"
  printf '%s-%s-%s-%s-%s\n' \
    "${hex:0:8}" \
    "${hex:8:4}" \
    "${hex:12:4}" \
    "${hex:16:4}" \
    "${hex:20:12}"
}

mail_total() {
  curl -sS "${FINALERT_MAILPIT_URL:-http://localhost:8025}/api/v1/messages" \
    | tr -d '\r\n' \
    | sed -nE 's/.*"total"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/p'
}

_finalert_publish_event() {
  local exchange="$1"
  local routing_key="$2"
  local event="$3"
  local payload publication response
  local rabbitmq_url="${FINALERT_RABBITMQ_API_URL:-http://localhost:15672/api}"
  local rabbitmq_user="${FINALERT_RABBITMQ_USERNAME:-finalert}"
  local rabbitmq_password="${FINALERT_RABBITMQ_PASSWORD:-finalert-local}"

  payload="$(printf '%s' "$event" | base64 | tr -d '\r\n')"
  publication="$(printf \
    '{"properties":{"content_type":"application/json"},"routing_key":"%s","payload":"%s","payload_encoding":"base64"}' \
    "$routing_key" \
    "$payload")"

  response="$(
    curl -fsS \
      -u "$rabbitmq_user:$rabbitmq_password" \
      -H 'Content-Type: application/json' \
      -X POST \
      --data-raw "$publication" \
      "$rabbitmq_url/exchanges/%2F/$exchange/publish"
  )" || return 1

  printf '%s\n' "$response"
  [[ "$response" == *'"routed":true'* ]]
}

finalert_publish_catalog() {
  local symbol="${1:-USD-BRL}"
  local provider_symbol="${2:-USD/BRL}"
  local event_id updated_at version

  event_id="$(new_uuid)" || return 1
  updated_at="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  version="$(date +%s)000"
  FINALERT_TEST_CATALOG_EVENT="$(printf \
    '{"eventId":"%s","catalog":{"%s":"%s"},"updatedAt":"%s","version":%s}' \
    "$event_id" \
    "$symbol" \
    "$provider_symbol" \
    "$updated_at" \
    "$version")"

  printf 'Publicando catalogo %s -> %s...\n' "$symbol" "$provider_symbol"
  _finalert_publish_event \
    'market.events' \
    'market.catalog.updated' \
    "$FINALERT_TEST_CATALOG_EVENT" || return 1
  sleep 2
}

finalert_create_alert() {
  local email="${1:-user@example.com}"
  local symbol="${2:-USD-BRL}"
  local target_price="${3:-999.00}"
  local trigger_operator="${4:-GTE}"
  local response http_code
  local alert_engine_url="${FINALERT_ALERT_ENGINE_URL:-http://localhost:8080}"

  FINALERT_TEST_IDEMPOTENCY_KEY="e2e-$(new_uuid)" || return 1
  FINALERT_TEST_ALERT_REQUEST="$(printf \
    '{"userEmail":"%s","targetPrice":%s,"triggerOperator":"%s"}' \
    "$email" \
    "$target_price" \
    "$trigger_operator")"

  response="$(
    curl -sS \
      -H 'Content-Type: application/json' \
      -H "Idempotency-Key: $FINALERT_TEST_IDEMPOTENCY_KEY" \
      -X POST \
      --data-raw "$FINALERT_TEST_ALERT_REQUEST" \
      -w $'\n%{http_code}' \
      "$alert_engine_url/alerts/$symbol"
  )" || return 1

  http_code="${response##*$'\n'}"
  FINALERT_TEST_ALERT_RESPONSE="${response%$'\n'*}"
  FINALERT_TEST_ALERT_ID="$(
    printf '%s' "$FINALERT_TEST_ALERT_RESPONSE" \
      | sed -nE 's/.*"alertId"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/p'
  )"

  printf 'HTTP %s\n%s\n' "$http_code" "$FINALERT_TEST_ALERT_RESPONSE"
  [[ "$http_code" == '201' ]] || return 1
  sleep 2
}

finalert_publish_market_price() {
  local symbol="${1:-USD-BRL}"
  local current_price="${2:-1000.00}"
  local event_id occurred_at

  event_id="$(new_uuid)" || return 1
  occurred_at="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  FINALERT_TEST_PRICE_EVENT="$(printf \
    '{"eventId":"%s","symbol":"%s","currentPrice":%s,"occurredAt":"%s"}' \
    "$event_id" \
    "$symbol" \
    "$current_price" \
    "$occurred_at")"

  printf 'Publicando cotacao %s = %s...\n' "$symbol" "$current_price"
  _finalert_publish_event \
    'market.events' \
    'market.price.updated' \
    "$FINALERT_TEST_PRICE_EVENT" || return 1
  sleep 5
}

finalert_list_emails() {
  local messages
  messages="$(curl -fsS "${FINALERT_MAILPIT_URL:-http://localhost:8025}/api/v1/messages")" || return 1

  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$messages" | jq '.messages[] | {Subject, From, To, Created}'
  else
    printf '%s\n' "$messages"
  fi
}

finalert_test_idempotency() {
  local total_before_replay total_after_replay

  if [[ -z "${FINALERT_TEST_PRICE_EVENT:-}" ]]; then
    printf '%s\n' 'Execute finalert_publish_market_price antes do teste de idempotencia.' >&2
    return 1
  fi

  total_before_replay="$(mail_total)" || return 1
  [[ "$total_before_replay" =~ ^[0-9]+$ ]] || return 1

  printf '%s\n' 'Republicando o mesmo MarketPriceUpdatedEvent...'
  _finalert_publish_event \
    'market.events' \
    'market.price.updated' \
    "$FINALERT_TEST_PRICE_EVENT" || return 1
  sleep 5

  total_after_replay="$(mail_total)" || return 1
  [[ "$total_after_replay" =~ ^[0-9]+$ ]] || return 1

  printf 'Antes do replay: %s\n' "$total_before_replay"
  printf 'Depois do replay: %s\n' "$total_after_replay"

  if [[ "$total_before_replay" == "$total_after_replay" ]]; then
    printf '%s\n' 'Idempotente: true'
    return 0
  fi

  printf '%s\n' 'Idempotente: false'
  return 1
}

finalert_check_dlqs() {
  local queues=(
    'market-price-updated.dlq'
    'market-catalog-updated.dlq'
    'market-price-alert.dlq'
    'alert-triggered-monitoring-release.dlq'
    'alert-triggered-notification.dlq'
  )
  local queue response messages
  local result=0
  local rabbitmq_url="${FINALERT_RABBITMQ_API_URL:-http://localhost:15672/api}"
  local rabbitmq_user="${FINALERT_RABBITMQ_USERNAME:-finalert}"
  local rabbitmq_password="${FINALERT_RABBITMQ_PASSWORD:-finalert-local}"

  for queue in "${queues[@]}"; do
    response="$(
      curl -fsS \
        -u "$rabbitmq_user:$rabbitmq_password" \
        "$rabbitmq_url/queues/%2F/$queue"
    )" || {
      printf '%-45s %s\n' "$queue" 'indisponivel'
      result=1
      continue
    }

    messages="$(
      printf '%s' "$response" \
        | tr -d '\r\n' \
        | sed -nE 's/.*"messages"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/p'
    )"
    printf '%-45s %s\n' "$queue" "${messages:-indisponivel}"

    if [[ ! "$messages" =~ ^[0-9]+$ ]] || (( messages != 0 )); then
      result=1
    fi
  done

  return "$result"
}

finalert_run_e2e() {
  local emails_before emails_after

  emails_before="$(mail_total)" || return 1
  [[ "$emails_before" =~ ^[0-9]+$ ]] || return 1

  finalert_publish_catalog || return 1
  finalert_create_alert || return 1
  finalert_publish_market_price || return 1

  emails_after="$(mail_total)" || return 1
  [[ "$emails_after" =~ ^[0-9]+$ ]] || return 1
  if (( emails_after <= emails_before )); then
    printf 'Nenhum novo e-mail foi entregue: antes=%s, depois=%s.\n' \
      "$emails_before" \
      "$emails_after" >&2
    return 1
  fi

  finalert_list_emails || return 1
  finalert_test_idempotency || return 1
  finalert_check_dlqs
}
