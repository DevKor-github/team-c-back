package devkor.com.teamcback.domain.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import devkor.com.teamcback.domain.notification.dto.request.NotificationTestReq;
import devkor.com.teamcback.domain.notification.entity.PushDispatch;
import devkor.com.teamcback.domain.notification.entity.PushInstallation;
import devkor.com.teamcback.domain.notification.entity.PushMessage;
import devkor.com.teamcback.domain.notification.entity.type.AppVariant;
import devkor.com.teamcback.domain.notification.entity.type.PushActionType;
import devkor.com.teamcback.domain.notification.entity.type.PushDispatchStatus;
import devkor.com.teamcback.domain.notification.entity.type.PushMessageStatus;
import devkor.com.teamcback.domain.notification.repository.PushDispatchRepository;
import devkor.com.teamcback.domain.notification.repository.PushInstallationRepository;
import devkor.com.teamcback.domain.notification.repository.PushMessageRepository;
import devkor.com.teamcback.domain.notification.service.NotificationTestService;
import devkor.com.teamcback.domain.notification.service.PushMessageDispatchWorker;
import devkor.com.teamcback.domain.notification.service.PushReceiptWorker;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:push_pipeline;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR,END;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never",
        "spring.cache.type=simple",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=test",
        "push.worker.enabled=false",
        "push.receipt-worker.enabled=false",
        "jwt.secret.key=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=",
        "jwt.admin.token=test-admin-token",
        "jwt.social.kakao.iss=test-kakao-iss",
        "jwt.social.kakao.aud=test-kakao-aud",
        "jwt.social.google.iss=test-google-iss",
        "jwt.social.google.aud=test-google-aud",
        "jwt.social.apple.iss=test-apple-iss",
        "jwt.social.apple.aud=test-apple-aud",
        "jwt.social.apple.dev-aud=test-apple-dev-aud",
        "metrics.environment=test",
        "staff.emails=test@example.com",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2",
        "date.api.holiday.end-point=http://localhost",
        "date.api.holiday.encoded-key=test-encoded-key",
        "date.api.holiday.decoded-key=test-decoded-key",
        "spring.mail.host=localhost",
        "management.health.mail.enabled=false"
})
class PushNotificationPipelineIntegrationTest {

    private static final String EXPO_PUSH_TOKEN = "ExponentPushToken[pipeline-test-token]";
    private static final String TICKET_ID = "ticket-pipeline-1";
    private static final MockExpoServer EXPO_SERVER = new MockExpoServer();

    @Autowired
    private NotificationTestService notificationTestService;

    @Autowired
    private PushMessageDispatchWorker pushMessageDispatchWorker;

    @Autowired
    private PushReceiptWorker pushReceiptWorker;

    @Autowired
    private PushInstallationRepository pushInstallationRepository;

    @Autowired
    private PushDispatchRepository pushDispatchRepository;

    @Autowired
    private PushMessageRepository pushMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedissonClient redissonClient;

    @DynamicPropertySource
    static void expoProperties(DynamicPropertyRegistry registry) {
        EXPO_SERVER.start();
        registry.add("push.expo.base-url", EXPO_SERVER::baseUrl);
    }

    @AfterAll
    static void stopExpoServer() {
        EXPO_SERVER.stop();
    }

    @BeforeEach
    void setUp() {
        EXPO_SERVER.reset();
        pushMessageRepository.deleteAll();
        pushDispatchRepository.deleteAll();
        pushInstallationRepository.deleteAll();
    }

    @Test
    void testNotificationPipelineQueuesSendsReceiptsAndCompletesDispatch() throws Exception {
        PushInstallation installation = pushInstallationRepository.save(new PushInstallation(
                1L,
                "install-pipeline",
                EXPO_PUSH_TOKEN,
                AppVariant.DEV
        ));

        notificationTestService.sendTest(
                1L,
                UUID.randomUUID().toString(),
                new NotificationTestReq(1, installation.getInstallationId())
        );

        List<PushDispatch> dispatches = pushDispatchRepository.findAll();
        List<PushMessage> messages = pushMessageRepository.findAll();
        assertThat(dispatches).hasSize(1);
        assertThat(messages).hasSize(1);

        PushDispatch dispatch = dispatches.get(0);
        PushMessage queuedMessage = messages.get(0);
        assertThat(dispatch.getRecipientCount()).isEqualTo(1);
        assertThat(queuedMessage.getStatus()).isEqualTo(PushMessageStatus.QUEUED);
        assertThat(EXPO_SERVER.requests()).isEmpty();

        EXPO_SERVER.enqueueJson(200, """
                {"data":[{"status":"ok","id":"ticket-pipeline-1"}]}
                """);
        pushMessageDispatchWorker.dispatchPending();

        PushMessage receiptPendingMessage = pushMessageRepository.findById(queuedMessage.getPushMessageId()).orElseThrow();
        assertThat(receiptPendingMessage.getStatus()).isEqualTo(PushMessageStatus.RECEIPT_PENDING);
        assertThat(receiptPendingMessage.getExpoTicketId()).isEqualTo(TICKET_ID);
        assertThat(receiptPendingMessage.getReceiptAvailableAt()).isNotNull();

        RecordedRequest sendRequest = EXPO_SERVER.takeOnlyRequest();
        assertThat(sendRequest.path()).isEqualTo("/send");
        JsonNode sendBody = objectMapper.readTree(sendRequest.body());
        JsonNode firstSend = sendBody.get(0);
        assertThat(firstSend.get("to").asText()).isEqualTo(EXPO_PUSH_TOKEN);
        assertThat(firstSend.at("/data/action/type").asText()).isEqualTo(PushActionType.TEST.name());
        assertThat(firstSend.at("/data/notificationId").asText())
                .isEqualTo(String.valueOf(receiptPendingMessage.getPushMessageId()));

        ReflectionTestUtils.setField(
                receiptPendingMessage,
                "receiptAvailableAt",
                LocalDateTime.now().minusMinutes(1)
        );
        pushMessageRepository.saveAndFlush(receiptPendingMessage);

        EXPO_SERVER.reset();
        EXPO_SERVER.enqueueJson(200, """
                {"data":{"ticket-pipeline-1":{"status":"ok"}}}
                """);
        pushReceiptWorker.checkPendingReceipts();

        PushMessage deliveredMessage = pushMessageRepository.findById(queuedMessage.getPushMessageId()).orElseThrow();
        PushDispatch completedDispatch = pushDispatchRepository.findById(dispatch.getPushDispatchId()).orElseThrow();
        assertThat(deliveredMessage.getStatus()).isEqualTo(PushMessageStatus.DELIVERED);
        assertThat(deliveredMessage.getReceiptStatus()).isEqualTo("ok");
        assertThat(deliveredMessage.getReceiptCheckedAt()).isNotNull();
        assertThat(completedDispatch.getStatus()).isEqualTo(PushDispatchStatus.COMPLETED);
        assertThat(pushMessageRepository.findById(deliveredMessage.getPushMessageId())).isPresent();
        assertThat(pushDispatchRepository.findById(completedDispatch.getPushDispatchId())).isPresent();

        RecordedRequest receiptRequest = EXPO_SERVER.takeOnlyRequest();
        assertThat(receiptRequest.path()).isEqualTo("/getReceipts");
        JsonNode receiptBody = objectMapper.readTree(receiptRequest.body());
        JsonNode ids = receiptBody.get("ids");
        assertThat(ids)
                .as("receipt request body: " + receiptRequest.body())
                .isNotNull();
        assertThat(ids.isArray()).isTrue();
        assertThat(ids.get(0).asText()).isEqualTo(TICKET_ID);
    }

    @Test
    void receiptDeviceNotRegisteredFailsMessageAndDeactivatesInstallation() {
        PushInstallation installation = pushInstallationRepository.save(new PushInstallation(
                1L,
                "install-device-not-registered",
                EXPO_PUSH_TOKEN,
                AppVariant.DEV
        ));

        notificationTestService.sendTest(
                1L,
                UUID.randomUUID().toString(),
                new NotificationTestReq(1, installation.getInstallationId())
        );

        EXPO_SERVER.enqueueJson(200, """
                {"data":[{"status":"ok","id":"ticket-pipeline-1"}]}
                """);
        pushMessageDispatchWorker.dispatchPending();

        PushMessage receiptPendingMessage = pushMessageRepository.findAll().get(0);
        ReflectionTestUtils.setField(
                receiptPendingMessage,
                "receiptAvailableAt",
                LocalDateTime.now().minusMinutes(1)
        );
        pushMessageRepository.saveAndFlush(receiptPendingMessage);

        EXPO_SERVER.reset();
        EXPO_SERVER.enqueueJson(200, """
                {"data":{"ticket-pipeline-1":{"status":"error","details":{"error":"DeviceNotRegistered"}}}}
                """);
        pushReceiptWorker.checkPendingReceipts();

        PushMessage failedMessage = pushMessageRepository.findById(receiptPendingMessage.getPushMessageId()).orElseThrow();
        PushInstallation deactivatedInstallation = pushInstallationRepository.findById(installation.getPushInstallationId())
                .orElseThrow();
        PushDispatch failedDispatch = pushDispatchRepository.findAll().get(0);

        assertThat(failedMessage.getStatus()).isEqualTo(PushMessageStatus.FAILED);
        assertThat(failedMessage.getReceiptStatus()).isEqualTo("error");
        assertThat(failedMessage.getReceiptError()).isEqualTo("DeviceNotRegistered");
        assertThat(deactivatedInstallation.isActive()).isFalse();
        assertThat(failedDispatch.getStatus()).isEqualTo(PushDispatchStatus.FAILED);
    }

    private record RecordedRequest(String path, String body) {
    }

    private static final class MockExpoServer {

        private final ConcurrentLinkedQueue<String> responses = new ConcurrentLinkedQueue<>();
        private final List<RecordedRequest> requests = new ArrayList<>();
        private HttpServer server;

        void start() {
            if (server != null) {
                return;
            }

            try {
                server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            server.createContext("/", this::handle);
            server.start();
        }

        void stop() {
            if (server != null) {
                server.stop(0);
            }
        }

        String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        void reset() {
            responses.clear();
            synchronized (requests) {
                requests.clear();
            }
        }

        void enqueueJson(
                int status,
                String body
        ) {
            responses.add(status + "\n" + body);
        }

        List<RecordedRequest> requests() {
            synchronized (requests) {
                return List.copyOf(requests);
            }
        }

        RecordedRequest takeOnlyRequest() {
            List<RecordedRequest> snapshot = requests();
            assertThat(snapshot).hasSize(1);
            return snapshot.get(0);
        }

        private void handle(HttpExchange exchange) throws IOException {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            synchronized (requests) {
                requests.add(new RecordedRequest(exchange.getRequestURI().getPath(), requestBody));
            }

            String rawResponse = responses.poll();
            if (rawResponse == null) {
                write(exchange, 500, "{\"errors\":[{\"message\":\"missing mock response\"}]}");
                return;
            }

            int delimiter = rawResponse.indexOf('\n');
            int status = Integer.parseInt(rawResponse.substring(0, delimiter));
            String body = rawResponse.substring(delimiter + 1);
            write(exchange, status, body);
        }

        private void write(
                HttpExchange exchange,
                int status,
                String body
        ) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(bytes);
            }
        }
    }
}
