#!/bin/sh
set -eu

NACOS_URL="http://nacos:8848/nacos/v1/cs/configs"
DATA_ID="alarm.default.alarm-settings"
GROUP="skywalking"
CONTENT_FILE="/config/alarm-settings.yml"

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

echo "[nacos-init] publishing ${DATA_ID} to group ${GROUP}"
curl -fsS -X POST "${NACOS_URL}" \
  --data-urlencode "dataId=${DATA_ID}" \
  --data-urlencode "group=${GROUP}" \
  --data-urlencode "content=$(cat "${CONTENT_FILE}")" >/dev/null

echo "[nacos-init] done"
