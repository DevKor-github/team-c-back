package devkor.com.teamcback.domain.notification.dto.response;

import java.util.List;

public record UpdateNoticeListRes(
        String minimumRequiredVersion,
        List<UpdateNoticeRes> notices
) {
}
