import static net.grinder.script.Grinder.grinder
import static org.junit.Assert.*

import net.grinder.script.GTest
import net.grinder.script.Grinder
import net.grinder.scriptengine.groovy.junit.GrinderRunner
import net.grinder.scriptengine.groovy.junit.annotation.BeforeProcess
import net.grinder.scriptengine.groovy.junit.annotation.BeforeThread
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import HTTPClient.HTTPResponse
import HTTPClient.NVPair
import net.grinder.plugin.http.HTTPRequest
import net.grinder.plugin.http.HTTPPluginControl

/**
 * 결제 구간 부하 테스트
 *
 * 대상: 
 * 1. POST /pay-service/pay/ready (결제 준비)
 * 2. POST /pay-service/pay/confirm (결제 승인 - WireMock으로 Toss API 모킹)
 *
 * 사전 조건:
 * - pay-service(9094) 실행 중
 * - WireMock(9999) 실행 중 (docker-compose로 시작)
 * - application.yml에 toss.api.url: http://localhost:9999/v1/payments 설정
 *
 * nGrinder agent가 Docker 안에 있으므로 host.docker.internal 사용
 */
@RunWith(GrinderRunner)
class PaymentLoadTest {

    public static GTest testReady
    public static GTest testConfirm
    public static HTTPRequest request
    public static NVPair[] headers

    // ===== 설정값 =====
    public static final String PAY_HOST = "host.docker.internal:9094"
    public static final int AMOUNT = 20000

    @BeforeProcess
    public static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
        testReady = new GTest(1, "결제 준비 API")
        testConfirm = new GTest(2, "결제 승인 API")
        request = new HTTPRequest()

        headers = [
            new NVPair("Content-Type", "application/json"),
        ] as NVPair[]

        grinder.logger.info("=== Payment Load Test initialized ===")
    }

    @BeforeThread
    public static void beforeThread() {
        testReady.record(request)
        testConfirm.record(request)
        grinder.statistics.delayReports = true
    }

    @Test
    public void test() {
        int threadId = grinder.threadNumber
        int runNumber = grinder.runNumber
        String uuid = UUID.randomUUID().toString().substring(0, 8)

        // orderId 생성 (완전히 유일한 값)
        String orderId = "order-${threadId}-${uuid}"

        // ===== 1. 결제 준비 =====
        String readyBody = """{
            "orderId": "${orderId}",
            "amount": ${AMOUNT}
        }"""

        NVPair[] payHeaders = [
            new NVPair("Content-Type", "application/json"),
        ] as NVPair[]

        testReady.record(request)
        HTTPResponse readyResponse = request.POST(
            "http://${PAY_HOST}/pay-service/pay/ready",
            readyBody.getBytes("UTF-8"),
            payHeaders
        )

        int readyStatusCode = readyResponse.statusCode
        if (readyStatusCode != 200) {
            grinder.logger.warn("결제 준비 실패 - status: ${readyStatusCode}, body: ${readyResponse.getText()}")
            grinder.statistics.forLastTest.setSuccess(false)
            return
        }

        // ===== 2. 결제 승인 (WireMock 호출) =====
        String confirmBody = """{
            "orderId": "${orderId}",
            "amount": ${AMOUNT},
            "paymentKey": "test_payment_key_${threadId}_${runNumber}"
        }"""

        testConfirm.record(request)
        HTTPResponse confirmResponse = request.POST(
            "http://${PAY_HOST}/pay-service/pay/confirm",
            confirmBody.getBytes("UTF-8"),
            payHeaders
        )

        int confirmStatusCode = confirmResponse.statusCode
        if (confirmStatusCode != 200) {
            grinder.logger.warn("결제 승인 실패 - status: ${confirmStatusCode}, body: ${confirmResponse.getText()}")
            grinder.statistics.forLastTest.setSuccess(false)
            return
        }

        grinder.logger.info("결제 성공 - orderId: ${orderId}, amount: ${AMOUNT}")
    }
}
