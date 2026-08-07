package devkor.com.teamcback.domain.usagesurvey.controller;

import devkor.com.teamcback.domain.usagesurvey.dto.request.RecordUsageSurveyDismissalReq;
import devkor.com.teamcback.domain.usagesurvey.dto.request.SubmitUsageSurveyResponseReq;
import devkor.com.teamcback.domain.usagesurvey.dto.response.SubmitUsageSurveyResponseRes;
import devkor.com.teamcback.domain.usagesurvey.dto.response.UsageSurveyStatusRes;
import devkor.com.teamcback.domain.usagesurvey.service.UsageSurveyService;
import devkor.com.teamcback.global.response.CommonResponse;
import devkor.com.teamcback.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usage-surveys")
public class UsageSurveyController {
    private final UsageSurveyService usageSurveyService;

    @GetMapping("/status")
    public CommonResponse<UsageSurveyStatusRes> getStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetail
    ) {
        return CommonResponse.success(
                usageSurveyService.getStatus(userDetail.getUser().getUserId())
        );
    }

    @PostMapping("/responses")
    public CommonResponse<SubmitUsageSurveyResponseRes> submitResponse(
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @RequestBody SubmitUsageSurveyResponseReq req
    ) {
        return CommonResponse.success(usageSurveyService.submitResponse(
                userDetail.getUser().getUserId(),
                req
        ));
    }

    @PostMapping("/dismissals")
    public CommonResponse<Void> recordDismissal(
            @AuthenticationPrincipal UserDetailsImpl userDetail,
            @RequestBody RecordUsageSurveyDismissalReq req
    ) {
        usageSurveyService.recordDismissal(userDetail.getUser().getUserId(), req);
        return CommonResponse.success(null);
    }
}
