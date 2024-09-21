package devkor.com.teamcback.domain.koyeon.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class SearchFreePubInfoListRes {
    List<SearchFreePubInfoRes> list;

    public SearchFreePubInfoListRes(List<SearchFreePubInfoRes> list) {
        this.list = list;
    }
}
