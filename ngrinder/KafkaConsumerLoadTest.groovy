import static net.grinder.script.Grinder.grinder

import net.grinder.script.GTest
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.AfterProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Test
import org.junit.runner.RunWith

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

/**
 * 구간 B: Kafka Consumer 부하 테스트
 *
 * pay-events 토픽에 CDC 포맷 메시지를 대량 발행하여
 * Order 서비스의 KafkaConsumer.onPayEvent() 처리 성능을 측정
 *
 * 사전 조건:
 *   1. 구간 A(CreateOrderLoadTest)로 주문을 미리 생성해둘 것
 *   2. nGrinder 스크립트 lib 폴더에 kafka-clients-3.7.x.jar 업로드 필요
 *      (Maven: org.apache.kafka:kafka-clients:3.7.1)
 *
 * nGrinder agent가 Docker delivery-network 안에 있으므로 kafka:29092 직접 접근 가능
 */
@RunWith(GrinderRunner)
class KafkaConsumerLoadTest {

    public static GTest test
    public static KafkaProducer<String, String> producer

    // ===== 설정값 =====
    public static final String KAFKA_BOOTSTRAP = "kafka:29092"

    // 구간 A에서 생성된 주문 ID 범위 (DB에서 조회하여 설정)
    public static final long ORDER_ID_START = 1L
    public static final long ORDER_ID_COUNT = 10000L

    // 테스트 대상 토픽 선택
    public static final String TOPIC_PAY = "pay-events.public.pay_outbox"
    public static final String TOPIC_SELLER = "seller-events.public.seller_outbox"
    public static final String TOPIC_RIDER = "rider-events.public.rider_outbox"

    @BeforeProcess
    public static void beforeProcess() {
        test = new GTest(1, "Kafka Pay Event 발행")

        Properties props = new Properties()
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP)
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName())
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName())
        props.put(ProducerConfig.ACKS_CONFIG, "1")
        props.put(ProducerConfig.LINGER_MS_CONFIG, "5")
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384")

        producer = new KafkaProducer<>(props)

        grinder.logger.info("=== Kafka Producer initialized (bootstrap: ${KAFKA_BOOTSTRAP}) ===")
    }

    @AfterProcess
    public static void afterProcess() {
        if (producer != null) {
            producer.flush()
            producer.close()
            grinder.logger.info("=== Kafka Producer closed ===")
        }
    }

    @BeforeThread
    public void beforeThread() {
        test.record(this, "testPayEvent")
        grinder.statistics.delayReports = true
    }

    /**
     * 결제 완료(DONE) 이벤트를 pay-events 토픽에 발행
     * → Order Consumer의 moneyPaid() 트리거
     */
    @Test
    public void testPayEvent() {
        long orderId = pickOrderId()
        String message = buildPayDoneMessage(orderId)

        try {
            ProducerRecord<String, String> record =
                new ProducerRecord<>(TOPIC_PAY, String.valueOf(orderId), message)

            producer.send(record).get()

            grinder.logger.info("pay DONE 이벤트 발행 - orderId: ${orderId}")
        } catch (Exception e) {
            grinder.logger.error("Kafka 발행 실패: ${e.message}")
            grinder.statistics.forLastTest.success = false
        }
    }

    // ===== 메시지 빌더 (Debezium CDC 포맷) =====

    private static String buildPayDoneMessage(long orderId) {
        String innerPayload = """{"orderId": ${orderId}}"""
        String escapedPayload = innerPayload.replace('"', '\\"')

        return """{
            "payload": {
                "after": {
                    "event_type": "DONE",
                    "payload": "${escapedPayload}"
                }
            }
        }"""
    }

    static String buildSellerCookingMessage(long orderId) {
        String innerPayload = """{"responseData": {"id": ${orderId}}}"""
        String escapedPayload = innerPayload.replace('"', '\\"')

        return """{
            "payload": {
                "after": {
                    "event_type": "COOKING",
                    "payload": "${escapedPayload}"
                }
            }
        }"""
    }

    static String buildRiderCompletedMessage(long orderId) {
        String innerPayload = """{"orderId": ${orderId}}"""
        String escapedPayload = innerPayload.replace('"', '\\"')

        return """{
            "payload": {
                "after": {
                    "event_type": "COMPLETED",
                    "payload": "${escapedPayload}"
                }
            }
        }"""
    }

    // ===== 유틸 =====

    private long pickOrderId() {
        int threadId = grinder.threadNumber
        int runNumber = grinder.runNumber
        return ORDER_ID_START + ((threadId * 10000L + runNumber) % ORDER_ID_COUNT)
    }
}
