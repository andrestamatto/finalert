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
  curl -sS http://localhost:8025/api/v1/messages \
    | tr -d '\r\n' \
    | sed -nE 's/.*"total"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/p'
}
