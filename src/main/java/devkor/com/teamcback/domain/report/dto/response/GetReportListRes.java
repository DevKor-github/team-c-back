package devkor.com.teamcback.domain.report.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class GetReportListRes {
    List<GetReportRes> reportList;

    public GetReportListRes(List<GetReportRes> reportList) {
        this.reportList = reportList;
    }
}
