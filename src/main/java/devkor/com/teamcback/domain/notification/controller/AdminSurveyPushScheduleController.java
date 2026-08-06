package devkor.com.teamcback.domain.notification.controller;

import devkor.com.teamcback.domain.notification.dto.request.AdminSurveyPushScheduleReq;
import devkor.com.teamcback.domain.notification.dto.response.AdminSurveyPushScheduleRes;
import devkor.com.teamcback.domain.notification.service.SurveyPushScheduleService;
import devkor.com.teamcback.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications/survey-schedules")
public class AdminSurveyPushScheduleController {

    private final SurveyPushScheduleService surveyPushScheduleService;

    @PutMapping("/{surveyKey}")
    public CommonResponse<AdminSurveyPushScheduleRes> upsertSurveySchedules(
            @PathVariable String surveyKey,
            @RequestBody AdminSurveyPushScheduleReq request
    ) {
        return CommonResponse.success(surveyPushScheduleService.upsertAdminSchedules(surveyKey, request));
    }

    @GetMapping("/{surveyKey}")
    public CommonResponse<AdminSurveyPushScheduleRes> getSurveySchedules(
            @PathVariable String surveyKey
    ) {
        return CommonResponse.success(surveyPushScheduleService.getAdminSchedules(surveyKey));
    }

    @DeleteMapping("/{surveyKey}")
    public CommonResponse<AdminSurveyPushScheduleRes> cancelSurveySchedules(
            @PathVariable String surveyKey
    ) {
        return CommonResponse.success(surveyPushScheduleService.cancelSchedules(surveyKey));
    }
}
