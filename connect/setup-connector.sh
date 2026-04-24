#!/bin/bash

BASE_URL="http://kafka-connect:8083"
CONNECT_URL="$BASE_URL/connectors"

echo "0. PostgreSQL 연결 대기 중..."
until curl -s --connect-timeout 3 http://kafka-connect:8083/ > /dev/null 2>&1; do
  echo "kafka-connect 대기 중... (5초 후 재시도)"
  sleep 5
done

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
  EXISTING=$(curl -s -o /dev/null -w "%{http_code}" "$CONNECT_URL/$NAME")

  if [ "$EXISTING" = "200" ]; then
    STATUS_RESPONSE=$(curl -s "$CONNECT_URL/$NAME/status")
    # connector 상태가 아니라 task 상태 확인
    TASK_STATE=$(echo "$STATUS_RESPONSE" | grep -o '"state":"[^"]*"' | sed -n '2p' | cut -d'"' -f4)

    if [ "$TASK_STATE" = "RUNNING" ]; then
      echo "커넥터($NAME) 정상 실행 중. 스킵."
      return 0
    else
      echo "커넥터($NAME) task 상태 이상($TASK_STATE). 삭제 후 재등록..."
      curl -s -X DELETE "$CONNECT_URL/$NAME"
      sleep 5
    fi
  fi

  echo "커넥터($NAME) 등록 중..."
  RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
    --data @"$CONFIG_FILE" \
    "$CONNECT_URL")

  if echo "$RESPONSE" | grep -q "$NAME"; then
    echo "커넥터($NAME) 등록 성공"
  else
    echo "커넥터($NAME) 등록 실패: $RESPONSE"
    return 1
  fi

  sleep 5

  STATUS_RESPONSE=$(curl -s "$CONNECT_URL/$NAME/status")
  TASK_STATE=$(echo "$STATUS_RESPONSE" | grep -o '"state":"[^"]*"' | sed -n '2p' | cut -d'"' -f4)

  if [ "$TASK_STATE" = "RUNNING" ]; then
    echo "커넥터($NAME) 정상 작동 중"
  else
    echo "커넥터($NAME) 상태 이상: $TASK_STATE"
    echo "$STATUS_RESPONSE"
    return 1
  fi
}

# Order 등록
register_connector "order-outbox-connector" "/config/order-outbox-connector.json"
if [ $? -ne 0 ]; then exit 1; fi

# Pay 등록
register_connector "pay-outbox-connector" "/config/pay-outbox-connector.json"
if [ $? -ne 0 ]; then exit 1; fi

# Seller 등록
register_connector "seller-outbox-connector" "/config/seller-outbox-connector.json"
if [ $? -ne 0 ]; then exit 1; fi

# Rider 등록
register_connector "rider-outbox-connector" "/config/rider-outbox-connector.json"
if [ $? -ne 0 ]; then exit 1; fi

echo "--------------------------------------------------"
echo "모든 설정 완료! 모든 커넥터가 정상 가동 중입니다."
echo "--------------------------------------------------"