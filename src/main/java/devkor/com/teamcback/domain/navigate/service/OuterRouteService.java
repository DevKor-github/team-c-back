package devkor.com.teamcback.domain.navigate.service;

import devkor.com.teamcback.domain.navigate.dto.OuterRouteRes;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OuterRouteService {
    public OuterRouteRes getOuterRoute(String startLong, String startLat, String endLong, String endLat)
        throws ParseException {

        //네이버 도보 길찾기 결과 문자열로 불러오기
        String url = String.format("https://map.naver.com/p/api/directions/walk?o=reco,wide,flat&l=%s,%s;%s,%s",startLong, startLat, endLong, endLat);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        String JsonInString = restTemplate.getForObject(url, String.class);

        JSONParser jsonParser = new JSONParser();
        Object object = jsonParser.parse(JsonInString);
        JSONObject jsonObject = (JSONObject) object;
        JSONArray arr = (JSONArray) jsonObject.get("routes");
        JSONObject mainBody = (JSONObject) arr.get(0);

        //JSON 파싱해서 소요시간 가져오기
        JSONObject summaryBody = (JSONObject) mainBody.get("summary");
        Long duration = (Long) summaryBody.get("duration");

        //JSON 파싱해서 경로 가져오기
        JSONArray routeBody = (JSONArray) mainBody.get("legs");
        JSONObject route = (JSONObject) routeBody.get(0);
        JSONArray steps = (JSONArray) route.get("steps");
        List<String[]> routeList = new ArrayList<>();
        int LastIndex = -1;
        for (int i = 0; i<steps.size(); i++){
            JSONObject pathInfo = (JSONObject) steps.get(i);
            if (pathInfo.get("path") != null){
                String unparsedPath = (String) pathInfo.get("path");
                String[] parsedPath = unparsedPath.split(" ");
                for (int j = 0; j<parsedPath.length; j++){
                    if ((LastIndex == -1) ||(!Objects.equals(routeList.get(LastIndex),
                        parsedPath[j]))) {
                        String[] splitPath = parsedPath[j].split(",");
                        String[] returnPath = {splitPath[1],splitPath[0]};
                        routeList.add(returnPath);
                        LastIndex++;
                    }
                }
            }
        }
        return new OuterRouteRes(String.valueOf(duration), routeList);
    }
}
