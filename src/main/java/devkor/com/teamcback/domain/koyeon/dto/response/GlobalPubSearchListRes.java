package devkor.com.teamcback.domain.koyeon.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Schema(description = "주점검색 결과")
@Getter
public class GlobalPubSearchListRes {
    List<GlobalPubSearchRes> list;

    public GlobalPubSearchListRes(List<GlobalPubSearchRes> list) {
        this.list = list;
    }
}
