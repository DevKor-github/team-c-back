package devkor.com.teamcback.domain.operatingtime.service;

import devkor.com.teamcback.domain.operatingtime.dto.HolidayResDto;
import devkor.com.teamcback.domain.operatingtime.entity.Holiday;
import devkor.com.teamcback.domain.operatingtime.repositoy.HolidayRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j(topic = "Holiday API")
@RequiredArgsConstructor
@Service
public class HolidayService {
    private final RestTemplate restTemplate;

    private final HolidayRepository holidayRepository;

    @Value("${data.api.holiday.end-point}")
    private String endPoint;

    @Value("${data.api.holiday.encoded-key}")
    private String encodedKey;

    @Value("${data.api.holiday.decoded-key}")
    private String decodedKey;

    public void updateHolidays(int year, int month) throws URISyntaxException {
        // 요청 URL 만들기
        String uriString = String.format("%s?solYear=%d&solMonth=%02d&ServiceKey=%s&_type=json&numOfRows=30",
            endPoint, year, month, encodedKey);
        URI uri = new URI(uriString);

//        log.info("uri = " + uri);

        RequestEntity<Void> requestEntity = RequestEntity
            .get(uri)
            .header("Accept", "application/json")
            .build();

        ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

        log.info("responseEntity: {}", responseEntity);

        List<Holiday> holidayList = fromJSONtoItems(responseEntity.getBody()).stream().map(holidayResDto -> new Holiday(holidayResDto)).toList();

        holidayRepository.saveAll(holidayList);
    }

    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByDateAndIsHoliday(date, true);
    }

    private List<HolidayResDto> fromJSONtoItems(String responseEntity) {
        JSONObject jsonObject = new JSONObject(responseEntity);
        JSONObject responseBody = jsonObject.getJSONObject("response").getJSONObject("body");

        List<HolidayResDto> holidayList = new ArrayList<>();

        // items 객체 가져오기 및 검사
        if (responseBody.has("items")) {
            Object itemsObject = responseBody.get("items");

            if (itemsObject instanceof JSONObject) {
                JSONObject items = (JSONObject) itemsObject;

                // items에 포함된 item들이 JSONArray로 제공될 경우
                if (items.has("item")) {
                    Object itemObject = items.get("item");

                    if (itemObject instanceof JSONArray) {
                        JSONArray itemArray = (JSONArray) itemObject;
                        for (int i = 0; i < itemArray.length(); i++) {
                            JSONObject item = itemArray.getJSONObject(i);
                            HolidayResDto holidayDto = new HolidayResDto(item);
                            holidayList.add(holidayDto);
                        }
                    } else if (itemObject instanceof JSONObject) {
                        // 단일 객체 처리
                        JSONObject item = (JSONObject) itemObject;
                        HolidayResDto holidayDto = new HolidayResDto(item);
                        holidayList.add(holidayDto);
                    }
                }
            } else {
                log.info("공휴일이 없습니다.");
            }
        }

        return holidayList;
    }
}
