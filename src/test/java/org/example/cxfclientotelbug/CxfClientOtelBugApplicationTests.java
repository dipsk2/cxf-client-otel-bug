package org.example.cxfclientotelbug;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.opentelemetry.sdk.trace.SpanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.example.cxfclientotelbug.controller.SampleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.example.cxfclientotelbug.TracingWireMockInitializer.TRACING_URI;
import static org.example.cxfclientotelbug.TracingWireMockInitializer.TRACING_WIREMOCK_BEAN;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@ContextConfiguration(initializers = TracingWireMockInitializer.class)
@Slf4j
class CxfClientOtelBugApplicationTests {

    @LocalServerPort
    private int port;
    @Autowired
    @Qualifier(TRACING_WIREMOCK_BEAN)
    private WireMockServer mockServer;
    private final TestRestTemplate restTemplate = new TestRestTemplate();
    @Autowired
    private SpanProcessor spanProcessor;
    @Autowired
    private SampleController controller;

    @Test
    void callingControllerDirectly() throws InterruptedException {
        mockServer.resetRequests();
        controller.greet();
        controller.greet();
        controller.greet2Way();
        controller.greet2Way();

        getTraces();

        /*
         * In this case, we are calling the controller method directly, new traces are not generated on every call.
         * (This is to simulate for e.g. a JMSListener which is not auto-instrumented by micrometer / otel
         * - spring-boot v3.1.4 doesn't support it yet)
         *
         * In this case, it can be observed that a new trace is not created on each call. All client cxf calls are appended
         * to the same traceId.
         * Moreover, in zipkin, we see only two spans - one for each of the two-way call. None for the one-way calls.
         *
         * The traceId/spanId are also visible in logs
         */
    }

    @Test
    void callingEndpoints() throws InterruptedException {
        mockServer.resetRequests();
        callOneWay();
        callOneWay();
        callTwoWay();
        callTwoWay();

        getTraces();

        /*
         * In this case, since we are calling the endpoints directly, new traces are generated on every call.
         * (This is to simulate for e.g. if you have a RestController which is auto-instrumented by micrometer / otel)
         *
         * In this case, it can be observed that there is a new trace on each call.
         * It can also be observed that the one-way client call spans are not sent to jaeger/zipkin. Only the two-way.
         *
         * The traceId/spanId are also visible in logs
         */
    }

    private void callOneWay() {
        restTemplate.getForEntity("http://localhost:" + port + "/one-way", String.class);
    }

    private void callTwoWay() {
        restTemplate.getForEntity("http://localhost:" + port + "/two-way", String.class);
    }

    private void getTraces() throws InterruptedException {
        Thread.currentThread().join(2_000);
        spanProcessor.forceFlush();
        Thread.currentThread().join(2_000);

        // when get tracing data
        var result = mockServer.findRequestsMatching(postRequestedFor(urlEqualTo(TRACING_URI)).build());
        result.getRequests().forEach(r -> log.info("sent to tracing: {} ", r.getBodyAsString()));
    }

}
