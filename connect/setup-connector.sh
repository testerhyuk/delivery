#!/bin/bash

BASE_URL="http://kafka-connect:8083"
CONNECT_URL="$BASE_URL/connectors"

echo "1. Kafka Connect 서버 및 플러그인 상태 확인 중..."

until curl -s "$BASE_URL/connector-plugins" | grep -q "io.debezium.connector.postgresql.PostgresConnector"; do
  echo "대기 중: Kafka Connect 서버가 부팅 중이거나 Postgres 플러그인을 로드하고 있습니다... (5초 후 재시도)"
  sleep 5
done

echo "플러그인 로드 확인 완료"

register_connector() {
  NAME=$1
  CONFIG_FILE=$2

  echo "--------------------------------------------------"
  echo "작업 시작: 커넥터($NAME) 등록 여부 확인..."

  EXISTING=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/$NAME")

  if [ "$EXISTING" = "404" ]; then
    echo "등록된 커넥터($NAME)가 없습니다. 새로 등록을 시도합니다..."

    RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
      --data @"$CONFIG_FILE" \
      "$CONNECT_URL")

    if echo "$RESPONSE" | grep -q "$NAME"; then
      echo "커넥터($NAME) 등록 요청 성공"
    else
      echo "커넥터($NAME) 등록 실패: $RESPONSE"
      return 1
    fi
  else
    echo "이미 '$NAME'이(가) 등록되어 있습니다. 설정을 유지합니다."
  fi

  echo "커넥터($NAME) 실행 상태 점검 중..."
  sleep 3

  STATUS_RESPONSE=$(curl -s "$CONNECT_URL/$NAME/status")
  STATE=$(echo "$STATUS_RESPONSE" | grep -o '"state":"[^"]*"' | head -1 | cut -d'"' -f4)

  if [ "$STATE" = "RUNNING" ]; then
    echo "결과: 커넥터($NAME)가 정상 작동 중입니다. (상태: $STATE)"
  else
    echo "결과: 커넥터($NAME) 상태 이상 (현재 상태: $STATE)"
    echo "상세 로그: $STATUS_RESPONSE"
    return 1
  fi
}

# Order 등록
register_connector "order-outbox-connector" "/config/order-outbox-connector.json"
if [ $? -ne 0 ]; then exit 1; fi

# Pay 등록
register_connector "pay-outbox-connector" "/config/pay-outbox-connector.json"
if [ $? -ne 0 ]; then exit 1; fi

echo "--------------------------------------------------"
echo "모든 설정 완료! 모든 커넥터가 정상 가동 중입니다."
echo "--------------------------------------------------"