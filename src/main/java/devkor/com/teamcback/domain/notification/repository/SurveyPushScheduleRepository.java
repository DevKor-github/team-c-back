package devkor.com.teamcback.domain.notification.repository;

import devkor.com.teamcback.domain.notification.entity.SurveyPushSchedule;
import devkor.com.teamcback.domain.notification.entity.type.SurveyNotificationStage;
import devkor.com.teamcback.domain.notification.entity.type.SurveyPushScheduleStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@devkor.com.teamcback.domain.notification.persistence.PushStore
public interface SurveyPushScheduleRepository extends JpaRepository<SurveyPushSchedule, Long> {

    Optional<SurveyPushSchedule> findByIdempotencyKey(String idempotencyKey);

    List<SurveyPushSchedule> findAllBySurveyKeyOrderByNotificationStageAscSurveyPushScheduleIdAsc(String surveyKey);

    List<SurveyPushSchedule> findAllBySurveyKeyAndNotificationStageIn(
            String surveyKey,
            Collection<SurveyNotificationStage> notificationStages
    );

    Optional<SurveyPushSchedule> findBySurveyKeyAndNotificationStage(
            String surveyKey,
            SurveyNotificationStage notificationStage
    );

    @Query(
            value = """
                    SELECT *
                    FROM tb_survey_push_schedule
                    WHERE status = 'PENDING'
                      AND scheduled_at <= :now
                    ORDER BY scheduled_at ASC, survey_push_schedule_id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<SurveyPushSchedule> findDuePendingForUpdateSkipLocked(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );

    List<SurveyPushSchedule> findAllByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAscSurveyPushScheduleIdAsc(
            SurveyPushScheduleStatus status,
            LocalDateTime now
    );

    @Query("select s from SurveyPushSchedule s where s.idempotencyKey = :key and s.audience = :audience")
    Optional<SurveyPushSchedule> findByIdempotencyKey(@Param("key") String key, @Param("audience") devkor.com.teamcback.domain.notification.entity.type.PushAudience audience);
    @Query("select s from SurveyPushSchedule s where s.surveyKey = :key and s.audience = :audience order by s.notificationStage, s.surveyPushScheduleId")
    List<SurveyPushSchedule> findAllBySurveyKeyOrderByNotificationStageAscSurveyPushScheduleIdAsc(@Param("key") String key, @Param("audience") devkor.com.teamcback.domain.notification.entity.type.PushAudience audience);
    @Query("select s from SurveyPushSchedule s where s.surveyKey = :key and s.notificationStage in :stages and s.audience = :audience")
    List<SurveyPushSchedule> findAllBySurveyKeyAndNotificationStageIn(@Param("key") String key, @Param("stages") Collection<SurveyNotificationStage> stages, @Param("audience") devkor.com.teamcback.domain.notification.entity.type.PushAudience audience);
    @Query("select s from SurveyPushSchedule s where s.surveyKey = :key and s.notificationStage = :stage and s.audience = :audience")
    Optional<SurveyPushSchedule> findBySurveyKeyAndNotificationStage(@Param("key") String key, @Param("stage") SurveyNotificationStage stage, @Param("audience") devkor.com.teamcback.domain.notification.entity.type.PushAudience audience);
    @Query(value="SELECT * FROM tb_survey_push_schedule WHERE status='PENDING' AND scheduled_at <= :now AND audience = :#{#audience.name()} ORDER BY scheduled_at, survey_push_schedule_id LIMIT :limit FOR UPDATE SKIP LOCKED",nativeQuery=true)
    List<SurveyPushSchedule> findDuePendingForUpdateSkipLocked(@Param("now") LocalDateTime now, @Param("limit") int limit, @Param("audience") devkor.com.teamcback.domain.notification.entity.type.PushAudience audience);
}
