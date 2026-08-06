package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.response.SurveyReminderRes;
import devkor.com.teamcback.domain.notification.service.SurveyPushScheduleService;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static devkor.com.teamcback.global.response.ResultCode.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications/surveys")
public class SurveyNotificationController {

    private final SurveyPushScheduleService surveyPushScheduleService;

    @PostMapping("/{surveyKey}/reminders")
    public CommonResponse<SurveyReminderRes> remindAfterLater(
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @PathVariable String surveyKey
    ) {
        if (userDetail == null) {
            throw new GlobalException(UNAUTHORIZED);
        }

        return CommonResponse.success(surveyPushScheduleService.remindAfterLater(
                surveyKey,
                userDetail.getUser().getUserId()
        ));
    }
}
