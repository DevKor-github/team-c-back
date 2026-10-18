package devkor.com.teamcback.domain.notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.redisson.api.RedissonClient;
import jakarta.persistence.EntityManagerFactory;
import devkor.com.teamcback.domain.notification.repository.*;
import devkor.com.teamcback.domain.notification.entity.*;
import devkor.com.teamcback.domain.notification.entity.type.*;
import devkor.com.teamcback.domain.user.entity.*;
import devkor.com.teamcback.domain.user.repository.UserRepository;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="KODAERO_PUSH_PG_TEST", matches="true")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "push.storage=neon",
        "push.audience=INTERNAL_TEST",
        "push.service-ddl-auto=create-drop",
        "push.ddl-auto=create-drop",
        "push.datasource.url=jdbc:postgresql://127.0.0.1:55439/push_test",
        "push.datasource.username=push_test",
        "push.datasource.password=local-test-only",
        "push.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:push_members;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR,END;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=never",
        "spring.cache.type=simple",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=55440",
        "spring.data.redis.password=",
        "push.idle.namespace=kodaero-local-pg-integration",
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
        "cloud.aws.region.static=ap-southeast-2",
        "date.api.holiday.end-point=http://localhost",
        "date.api.holiday.encoded-key=test-encoded-key",
        "date.api.holiday.decoded-key=test-decoded-key",
        "spring.mail.host=localhost",
        "management.health.mail.enabled=false"
})
class NeonPushStoreIntegrationTest {
    @MockBean RedissonClient redissonClient;
    @Autowired devkor.com.teamcback.domain.notification.scheduler.PushWorkSignals workSignals;
    @Autowired org.springframework.data.redis.connection.RedisConnectionFactory redisFactory;
    @Autowired UserRepository users;
    @Autowired PushInstallationRepository installations;
    @Autowired PushDispatchRepository dispatches;
    @Autowired PushMessageRepository messages;
    @Autowired @Qualifier("entityManagerFactory") EntityManagerFactory service;
    @Autowired @Qualifier("pushEntityManagerFactory") EntityManagerFactory push;
    @Autowired @Qualifier("pushTransactionManager") org.springframework.transaction.PlatformTransactionManager tx;

    @Test void databasesAreSeparatedAndProfileSearchUsesPrimaryMembers() {
        assertThat(service.getMetamodel().getEntities()).noneMatch(e -> e.getJavaType()==PushInstallation.class);
        assertThat(push.getMetamodel().getEntities()).noneMatch(e -> e.getJavaType()==User.class);
        var user=users.saveAndFlush(new User("neon_admin", "neon-admin@example.test", Role.ADMIN, Provider.GOOGLE));
        var device=installations.saveAndFlush(new PushInstallation(user.getUserId(), "neon-admin-device", "ExponentPushToken[neon-admin]", AppVariant.PRODUCTION));
        assertThat(installations.searchByUserProfile("%neon!_admin%",AppVariant.PRODUCTION,true,org.springframework.data.domain.PageRequest.of(0,200)))
                .extracting(PushInstallation::getInstallationId).containsExactly(device.getInstallationId());
        assertThat(installations.findUserProfiles(List.of(user.getUserId()))).extracting(PushInstallationRepository.UserProfile::getEmail).containsExactly("neon-admin@example.test");
        assertThat(installations.findAdminUserIds(List.of(user.getUserId()))).contains(user.getUserId());
    }
    @Test void postgresClaimQueriesExecuteInPushTransaction() {
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(status -> {
            assertThat(messages.findDueQueuedForUpdateSkipLocked(java.time.LocalDateTime.now(),10)).isEmpty();
            assertThat(messages.findDueReceiptPendingForUpdateSkipLocked(java.time.LocalDateTime.now(),10)).isEmpty();
        });
    }

    @Autowired devkor.com.teamcback.domain.notification.service.PushDispatchService dispatchService;
    @Autowired devkor.com.teamcback.domain.notification.service.PushEventFlagService flags;
    @Autowired PushEventSettingRepository settings;
    @Autowired SurveyPushScheduleRepository schedules;
    @Autowired devkor.com.teamcback.domain.notification.outbox.PushDomainEventRepository events;
    @Autowired org.springframework.context.ApplicationEventPublisher publisher;
    @Autowired @Qualifier("transactionManager") org.springframework.transaction.PlatformTransactionManager serviceTx;

    @org.junit.jupiter.api.BeforeEach void clearPushData() {
        messages.deleteAll(); dispatches.deleteAll(); installations.deleteAll(); settings.deleteAll(); schedules.deleteAll(); events.deleteAll();
        if ("true".equals(System.getenv("KODAERO_PUSH_REDIS_TEST"))) {
            var redis = new org.springframework.data.redis.core.StringRedisTemplate(redisFactory);
            redis.delete(redis.keys("push:wake:v1:kodaero-local-pg-integration:*"));
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="KODAERO_PUSH_REDIS_TEST", matches="true")
    void committedEnqueueWakesWorkerButRolledBackEnqueueDoesNot() {
        String suffix = UUID.randomUUID().toString();
        var user = users.saveAndFlush(new User("commit-"+suffix, "commit-"+suffix+"@example.test", Role.ADMIN, Provider.GOOGLE));
        installations.saveAndFlush(new PushInstallation(user.getUserId(), "commit-"+suffix, "ExponentPushToken["+suffix+"]", AppVariant.PRODUCTION));
        var command = new devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand(
                NotificationType.GENERAL, PushMode.ACTUAL, AppVariant.PRODUCTION, PushTargetType.USER, user.getUserId().toString(),
                "제목", "내용", PushActionType.HOME, Map.of(), "commit-"+suffix, 0L);
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(status -> {
            dispatchService.enqueue(command);
            status.setRollbackOnly();
        });
        assertThat(messages.count()).isZero();
        assertThat(workSignals.claim(devkor.com.teamcback.domain.notification.scheduler.PushWorkSignals.Work.DISPATCH)).isNull();
        dispatchService.enqueue(command);
        assertThat(messages.count()).isEqualTo(1);
        var work = devkor.com.teamcback.domain.notification.scheduler.PushWorkSignals.Work.DISPATCH;
        String lease = workSignals.claim(work);
        assertThat(lease).isNotNull();
        workSignals.release(work, lease);
    }

    @Test void enqueueIsAtomicScopedAndIdempotentUnderConcurrentRetry() throws Exception {
        String suffix=UUID.randomUUID().toString();
        var user=users.saveAndFlush(new User("admin-"+suffix,"admin-"+suffix+"@example.test",Role.ADMIN,Provider.GOOGLE));
        installations.saveAndFlush(new PushInstallation(user.getUserId(),"device-"+suffix,"ExponentPushToken["+suffix+"]",AppVariant.PRODUCTION));
        var command = new devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand(
                NotificationType.GENERAL,PushMode.ACTUAL,AppVariant.PRODUCTION,PushTargetType.USER,user.getUserId().toString(),
                "테스트 제목","테스트 내용",PushActionType.HOME,Map.of(),"neon-"+suffix,0L);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first=executor.submit(() -> dispatchService.enqueue(command));
            var second=executor.submit(() -> dispatchService.enqueue(command));
            assertThat(first.get().getDispatchId()).isEqualTo(second.get().getDispatchId());
            assertThat(dispatches.count()).isEqualTo(1);
            assertThat(messages.count()).isEqualTo(1);
            assertThat(dispatches.findAll().get(0).getAudience()).isEqualTo(PushAudience.INTERNAL_TEST);
        } finally { executor.shutdownNow(); }
        var badCommand=new devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand(
                NotificationType.GENERAL,PushMode.ACTUAL,AppVariant.PRODUCTION,PushTargetType.USER,"99999999",
                "없는 대상","테스트",PushActionType.HOME,Map.of(),"missing-"+suffix,0L);
        assertThatThrownBy(() -> dispatchService.enqueue(badCommand)).isInstanceOf(RuntimeException.class);
        assertThat(dispatches.count()).isEqualTo(1);
    }

    @Test void productionEnqueueIsRejectedByInternalOnlyServer() {
        var command=new devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand(
                NotificationType.GENERAL,PushMode.ACTUAL,AppVariant.PRODUCTION,PushTargetType.ALL,"ALL",null,
                "제목","내용",null,PushActionType.HOME,Map.of(),"forbidden-live",0L,false);
        assertThatThrownBy(() -> dispatchService.enqueue(command)).isInstanceOf(devkor.com.teamcback.global.exception.exception.GlobalException.class);
        assertThat(dispatches.count()).isZero();
    }

    @Test void wakeTimesFollowPendingWorkAndDisappearWhenCompleted() {
        String suffix = UUID.randomUUID().toString();
        var user = users.saveAndFlush(new User("wake-"+suffix, "wake-"+suffix+"@example.test", Role.ADMIN, Provider.GOOGLE));
        installations.saveAndFlush(new PushInstallation(user.getUserId(), "wake-"+suffix, "ExponentPushToken["+suffix+"]", AppVariant.PRODUCTION));
        dispatchService.enqueue(new devkor.com.teamcback.domain.notification.dto.request.PushDispatchCommand(
                NotificationType.GENERAL, PushMode.ACTUAL, AppVariant.PRODUCTION, PushTargetType.USER, user.getUserId().toString(),
                "제목", "내용", PushActionType.HOME, Map.of(), "wake-"+suffix, 0L));
        assertThat(messages.findNextQueuedAt()).isNotNull();
        assertThat(messages.findNextReceiptAt()).isNull();
        var now = java.time.LocalDateTime.now().withNano(0);
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(status ->
                messages.findAll().get(0).recordTicket("ok", "local-ticket", null, now));
        assertThat(messages.findNextQueuedAt()).isNull();
        assertThat(messages.findNextReceiptAt()).isEqualTo(now.plusMinutes(15));
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(status ->
                messages.findAll().get(0).recordReceipt("ok", null, now.plusMinutes(15)));
        assertThat(messages.findNextReceiptAt()).isNull();
        schedules.saveAndFlush(new SurveyPushSchedule("wake-survey", SurveyNotificationStage.STARTED, null,
                now.plusDays(3), 0, "wake-survey", now).withAudience(PushAudience.INTERNAL_TEST));
        assertThat(schedules.findNextPendingAt(PushAudience.INTERNAL_TEST)).isEqualTo(now.plusDays(3));
        assertThat(schedules.findNextPendingAt(PushAudience.LIVE)).isNull();
    }

    @Test void settingsAndSchedulesAreIsolatedByAudience() {
        flags.updateFlag(PushEventType.CHARACTER,true);
        assertThat(flags.isEnabled(PushEventType.CHARACTER)).isTrue();
        org.springframework.test.util.ReflectionTestUtils.setField(flags,"audience","LIVE");
        try { assertThat(flags.isEnabled(PushEventType.CHARACTER)).isFalse(); }
        finally { org.springframework.test.util.ReflectionTestUtils.setField(flags,"audience","INTERNAL_TEST"); }
        for(var audience:List.of(PushAudience.INTERNAL_TEST,PushAudience.LIVE)) {
            schedules.saveAndFlush(new SurveyPushSchedule("same-survey",SurveyNotificationStage.STARTED,null,
                    java.time.LocalDateTime.now().minusMinutes(1),0,"same-key",java.time.LocalDateTime.now()).withAudience(audience));
        }
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(status -> {
            assertThat(schedules.findDuePendingForUpdateSkipLocked(java.time.LocalDateTime.now(),10,PushAudience.INTERNAL_TEST))
                    .hasSize(1).allMatch(row -> row.getAudience()==PushAudience.INTERNAL_TEST);
        });
    }

    @Test void businessCommitPersistsOutboxAndRollbackDoesNot() {
        var transaction=new org.springframework.transaction.support.TransactionTemplate(serviceTx);
        transaction.executeWithoutResult(status -> {
            publisher.publishEvent(new devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent(10L,20L,30L,"테스트"));
            status.setRollbackOnly();
        });
        assertThat(events.count()).isZero();
        transaction.executeWithoutResult(status -> publisher.publishEvent(
                new devkor.com.teamcback.domain.character.event.CharacterUnlockedEvent(10L,20L,30L,"테스트")));
        assertThat(events.findAll()).hasSize(1).allMatch(event -> event.getCompletedAt()==null && event.getAudience().equals("INTERNAL_TEST"));
        assertThat(dispatches.count()).isZero();
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class ExportSchema {
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor exportPushSchema() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                public Object postProcessBeforeInitialization(Object bean, String name) {
                    if(name.equals("pushEntityManagerFactory") && bean instanceof org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean factory) {
                        try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of("build/neon-push-schema.sql")); } catch(java.io.IOException e) { throw new IllegalStateException(e); }
                        factory.getJpaPropertyMap().put("jakarta.persistence.schema-generation.database.action","drop-and-create");
                        factory.getJpaPropertyMap().put("jakarta.persistence.schema-generation.scripts.action","create");
                        factory.getJpaPropertyMap().put("jakarta.persistence.schema-generation.scripts.create-target","build/neon-push-schema.sql");
                    }
                    return bean;
                }
            };
        }
    }
}
