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
 * 구간 A: 주문 생성 HTTP 부하 테스트
 *
 * 대상: POST /order-service/order (order-service 직접 호출, gateway 우회)
 * 사전 조건: order-service(9100), restaurant-service(9101), pay-service(9094) 실행 중
 *
 * nGrinder agent가 Docker 안에 있으므로 host.docker.internal 사용
 */
@RunWith(GrinderRunner)
class CreateOrderLoadTest {

    public static GTest test
    public static HTTPRequest request
    public static NVPair[] headers

    // ===== 설정값 (환경에 맞게 수정) =====
    public static final String TARGET_HOST = "host.docker.internal:9100"
    public static final String RESTAURANT_ID = "1"
    public static final String MENU_ID = "1"
    public static final String MENU_NAME = "치킨"
    public static final int MENU_PRICE = 20000

    @BeforeProcess
    public static void beforeProcess() {
        HTTPPluginControl.getConnectionDefaults().timeout = 10000
        test = new GTest(1, "주문 생성 API")
        request = new HTTPRequest()

        headers = [
            new NVPair("Content-Type", "application/json"),
        ] as NVPair[]

        grinder.logger.info("=== CreateOrder Load Test initialized ===")
    }

    @BeforeThread
    public void beforeThread() {
        test.record(request)
        grinder.statistics.delayReports = true
    }

    @Test
    public void test() {
        int threadId = grinder.threadNumber
        int runNumber = grinder.runNumber

        String userId = "load-test-user-${threadId}"

        int quantity = (runNumber % 3) + 1

        String body = """{
            "restaurantId": "${RESTAURANT_ID}",
            "deliveryAddress": "서울시 강남구 테헤란로 ${threadId}길 ${runNumber}",
            "userLatitude": 37.5065,
            "userLongitude": 127.0536,
            "orderItems": [
                {
                    "menuId": "${MENU_ID}",
                    "menuName": "${MENU_NAME}",
                    "price": ${MENU_PRICE},
                    "quantity": ${quantity}
                }
            ]
        }"""

        NVPair[] requestHeaders = [
            new NVPair("Content-Type", "application/json"),
            new NVPair("userId", userId),
        ] as NVPair[]

        HTTPResponse response = request.POST(
            "http://${TARGET_HOST}/order-service/order",
            body.getBytes("UTF-8"),
            requestHeaders
        )

        int statusCode = response.statusCode
        if (statusCode != 201) {
            grinder.logger.warn("주문 생성 실패 - status: ${statusCode}, body: ${response.getText()}")
            grinder.statistics.forLastTest.setSuccess(false)
        }
    }
}
