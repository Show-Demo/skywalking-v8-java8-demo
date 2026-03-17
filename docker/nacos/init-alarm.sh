#!/bin/sh
set -eu

NACOS_URL="http://nacos:8848/nacos/v1/cs/configs"
NACOS_AUTH_URL="http://nacos:8848/nacos/v1/auth/login"
DATA_ID="alarm.default.alarm-settings"
LEGACY_DATA_ID="alarm.default"
GROUP="skywalking"
CONTENT_FILE="/config/alarm-settings.yml"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"

echo "[nacos-init] waiting for Nacos..."
for i in $(seq 1 120); do
  if curl -fsS "http://nacos:8848/nacos/v1/console/health/liveness" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

if ! curl -fsS "http://nacos:8848/nacos/v1/console/health/liveness" >/dev/null 2>&1; then
  echo "[nacos-init] Nacos not ready"
  exit 1
fi

echo "[nacos-init] login..."
TOKEN="$(curl -fsS -X POST "${NACOS_AUTH_URL}" \
  --data-urlencode "username=${NACOS_USERNAME}" \
  --data-urlencode "password=${NACOS_PASSWORD}" \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')"

if [ -z "${TOKEN}" ]; then
  echo "[nacos-init] failed to get accessToken"
  exit 1
fi

echo "[nacos-init] publishing ${DATA_ID} to group ${GROUP}"
curl -fsS -X POST "${NACOS_URL}?accessToken=${TOKEN}" \
  --data-urlencode "dataId=${DATA_ID}" \
  --data-urlencode "group=${GROUP}" \
  --data-urlencode "content=$(cat "${CONTENT_FILE}")" >/dev/null

echo "[nacos-init] deleting legacy ${LEGACY_DATA_ID} if exists"
curl -fsS -X DELETE "${NACOS_URL}?dataId=${LEGACY_DATA_ID}&group=${GROUP}&accessToken=${TOKEN}" >/dev/null || true

echo "[nacos-init] done"
