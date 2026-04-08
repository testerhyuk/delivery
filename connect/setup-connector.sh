#!/bin/bash

BASE_URL="http://kafka-connect:8083"
CONNECT_URL="$BASE_URL/connectors"
CONNECTOR_NAME="order-outbox-connector"
CONFIG_PATH="/config/order-outbox-connector.json"

echo "1. Kafka Connect 서버 및 플러그인 상태 확인 중..."

until curl -s "$BASE_URL/connector-plugins" | grep -q "io.debezium.connector.postgresql.PostgresConnector"; do
  echo "대기 중: Kafka Connect 서버가 부팅 중이거나 Postgres 플러그인을 로드하고 있습니다... (5초 후 재시도)"
  sleep 5
done

echo "플러그인 로드 확인 완료"

echo "2. 커넥터($CONNECTOR_NAME) 등록 여부 확인..."
EXISTING=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/$CONNECTOR_NAME")

if [ "$EXISTING" = "404" ]; then
  echo "등록된 커넥터가 없습니다. 새로 등록을 시도합니다..."

  RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    --data @"$CONFIG_PATH" \
    "$CONNECT_URL")

  if echo "$RESPONSE" | grep -q "$CONNECTOR_NAME"; then
    echo "커넥터 등록 요청 성공"
  else
    echo "커넥터 등록 실패: $RESPONSE"
    exit 1
  fi
else
  echo "이미 '$CONNECTOR_NAME'이(가) 등록되어 있습니다. 설정을 유지합니다."
fi

echo "3. 커넥터 실행 상태 최종 점검..."
sleep 5

STATUS_RESPONSE=$(curl -s "$CONNECT_URL/$CONNECTOR_NAME/status")
STATE=$(echo "$STATUS_RESPONSE" | grep -o '"state":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ "$STATE" = "RUNNING" ]; then
  echo "--------------------------------------------------"
  echo "모든 설정 완료! 커넥터가 정상 작동 중입니다."
  echo "상태: $STATE"
  echo "--------------------------------------------------"
else
  echo "커넥터 상태 이상 (현재 상태: $STATE)"
  echo "상세 로그: $STATUS_RESPONSE"
  exit 1
fi