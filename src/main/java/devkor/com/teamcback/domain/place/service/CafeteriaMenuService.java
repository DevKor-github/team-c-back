package devkor.com.teamcback.domain.place.service;

import devkor.com.teamcback.domain.building.entity.Building;
import devkor.com.teamcback.domain.building.repository.BuildingRepository;
import devkor.com.teamcback.domain.place.dto.response.GetCafeteriaMenuListRes;
import devkor.com.teamcback.domain.place.entity.CafeteriaMenu;
import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.repository.CafeteriaMenuRepository;
import devkor.com.teamcback.domain.place.repository.PlaceRepository;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import devkor.com.teamcback.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CafeteriaMenuService {

    // 자연계 학생식당 메뉴 URL
    private static final String URL1 = "https://www.korea.ac.kr/ko/";
    private static final String URL2 = "/subview.do";
    // HTML 테이블을 선택하는 CSS 선택자
    private static final String TABLE_SELECTOR = ".table_1 table";
    // 식단 메뉴가 없을 때 문구
    private static final String NO_MENU_INFO = "등록된 식단내용이(가) 없습니다.";
    // 학생식당 식단 종류 순서 지정
    private static final List<String> customOrder = Arrays.asList("천원의아침", "천원의아침(테이크아웃)", "중식(한식반상)", "중식(일품반상)");

    private final CafeteriaMenuRepository cafeteriaMenuRepository;
    private final PlaceRepository placeRepository;

    /**
     * 학생식당 메뉴 조회
     */
    @Transactional(readOnly = true)
    public GetCafeteriaMenuListRes getCafeteriaMenu(Long placeId, LocalDate startDate, LocalDate endDate) {
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));

        String address = place.getBuilding() == null ? "" : place.getBuilding().getName() + " ";
        address += place.getFloor() < 0 ? "B" : "";
        address += (int)Math.abs(place.getFloor()) + "층";

        Map<LocalDate, Map<String, String>> menuMap = new HashMap<>();

        startDate
                .datesUntil(endDate)
                .forEach(date -> {
                    List<CafeteriaMenu> cafeteriaMenuList = cafeteriaMenuRepository.findByDateAndPlaceId(date, placeId);

                    Map<String, String> menuByKind = new HashMap<>();
                    for (CafeteriaMenu cafeteriaMenu : cafeteriaMenuList) {
                        menuByKind.put(cafeteriaMenu.getKind(), cafeteriaMenu.getMenu());
                    }

                    if(placeId == 9757) {
                        // 식단 종류에 따라 정렬하기
                        Comparator<String> customComparator = (key1, key2) -> {
                            int index1 = customOrder.indexOf(key1);
                            int index2 = customOrder.indexOf(key2);

                            // 맵에 정의되지 않은 키는 맨 뒤로 보냅니다 (큰 값 부여)
                            if (index1 == -1) index1 = customOrder.size();
                            if (index2 == -1) index2 = customOrder.size();

                            // 순서 비교: index1이 작으면 먼저 옵니다.
                            int comparison = Integer.compare(index1, index2);

                            // customOrder에 없는 키끼리의 순서는 원래 문자열 비교(알파벳순)를 따릅니다.
                            if (comparison == 0 && index1 >= customOrder.size()) {
                                return key1.compareTo(key2);
                            }
                            return comparison;
                        };

                        // 정렬된 맵 생성
                        Map<String, String> sortedMap = new TreeMap<>(customComparator);
                        sortedMap.putAll(menuByKind);

                        menuMap.put(date, sortedMap);
                    }
                    else {
                        menuMap.put(date, menuByKind);
                    }
                });

        GetCafeteriaMenuListRes res = new GetCafeteriaMenuListRes(placeId, place.getName(), address, place.getDetail(), place.getContact(), menuMap);

        return res;
    }

    /**
     * 웹 페이지에서 식단 정보를 스크래핑하고 리스트로 반환합니다.
     */
    @Transactional
    public void scrapeMenu(int page, Long placeId) {
        // 식당 설명 초기화
        Place place = placeRepository.findById(placeId).orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_PLACE));
        place.setDescription("");

        // 식당 설명(메뉴) 수정 여부
        boolean updated = false;

        // 문자열 형식에 맞는 포맷터(날짜 문자열 -> Date)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        Document doc;

        try {
            // 1. Jsoup을 사용하여 HTML 문서 가져오기 (requests + BeautifulSoup 역할)
            doc = Jsoup.connect(URL1 + page + URL2)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)") // User-Agent 설정
                    .timeout(5000) // 5초 타임아웃
                    .get();

            // 2. 지정된 선택자를 이용해 테이블 찾기
            Element mealTable = doc.selectFirst(TABLE_SELECTOR);

            if (mealTable == null) {
                System.err.println("오류: 지정된 선택자 '" + TABLE_SELECTOR + "'로 테이블을 찾을 수 없습니다.");
                return;
            }

            // 3. 테이블의 순수한 텍스트 내용 추출
            // Jsoup의 text()는 태그를 제거하고 공백을 정리합니다.
            String mealTableText = mealTable.text();

            // 4. 공백 정규화 및 정리
            String cleanedHtmlContent = mealTableText.replaceAll("[ \t]+", " ");
            cleanedHtmlContent = cleanedHtmlContent.trim();

            // 5. 데이터를 날짜-요일 패턴을 기준으로 분리하여 블록화
            // 패턴: (YYYY.MM.DD (요일))
            Pattern dateDayPattern = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})\\s*");
            Matcher dateDayMatcher = dateDayPattern.matcher(cleanedHtmlContent);

            List<String> blocks = new ArrayList<>();
            int lastEnd = 0;

            // 날짜(요일) 블록을 기준으로 텍스트를 나눕니다.
            while (dateDayMatcher.find()) {
                // 이전 블록 내용을 추가합니다.
                if (dateDayMatcher.start() > lastEnd) {
                    blocks.add(cleanedHtmlContent.substring(lastEnd, dateDayMatcher.start()).trim());
                }
                // 현재 날짜(요일) 블록을 추가합니다.
                blocks.add(dateDayMatcher.group().trim());
                lastEnd = dateDayMatcher.end();
            }
            // 마지막 블록의 나머지 내용을 추가합니다.
            if (lastEnd < cleanedHtmlContent.length()) {
                blocks.add(cleanedHtmlContent.substring(lastEnd).trim());
            }

            String currentDate = null;

            // 6. 분리된 블록을 순회하며 식단 데이터 추출
            // 패턴: (조식|중식|석식) (.+?) - 내용물은 하이픈(-) 기준으로 분리됩니다.
            Pattern menuItemsPattern = Pattern.compile(
                    "(조식|석식|식사|요리|파스타/스테이크 코스|천원의밥상|천원의아침\\(테이크아웃\\)|천원의아침|중식\\(한식반상\\)|중식\\(일품반상\\)|중식 A|중식 B|중식)\\s*(.*?)(?=\\s*(조식|석식|식사|요리|파스타/스테이크 코스|천원의밥상|천원의아침\\(테이크아웃\\)|천원의아침|중식\\(한식반상\\)|중식\\(일품반상\\)|중식 A|중식 B|중식)\\s*|$)",
                    Pattern.DOTALL
            );

            for (String block : blocks) {
                if (block.isEmpty()) {
                    continue;
                }

                // 현재 블록이 날짜 패턴을 포함하는 경우
                Matcher dateMatch = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})").matcher(block);
                if (dateMatch.find()) {
                    currentDate = dateMatch.group(1);
                    continue; // 메뉴가 포함된 다음 블록을 위해 건너뜁니다.
                }

                // 현재 블록이 메뉴 내용이면
                if (currentDate != null) {
                    LocalDate date = LocalDate.parse(currentDate, formatter);

                    Matcher menuMatcher = menuItemsPattern.matcher(block);

                    while (menuMatcher.find()) {
                        String kind = menuMatcher.group(1).trim();
                        String content = menuMatcher.group(2);

                        // &amp; 와 잔여 텍스트 정리
                        content = content.replace("&amp;", "&");
                        content = content.replaceAll("요일 식단구분 식단제목 식단내용 기타정보", ""); // 불필요한 헤더 제거
                        content = content.replaceAll("[-]+$", "").trim(); // 끝의 '-'와 공백 제거
                        content = content.replaceAll("\\s+", " ").trim(); // 여러 공백을 단일 공백으로 정리

                        if (!content.isEmpty()) {

                            // 애기능 메뉴 정리
                            if(placeId == 3103 || placeId == 2490) {
                                content = content.replaceAll("\\s*/\\s*", "/");
                                content = content.replaceAll("(\\s+)의(\\s+)", "의");
                                content = content.replaceAll("사이드메뉴: ", "사이드메뉴:");
                            }

                            // 안암학사 메뉴 정리
                            if(placeId == 3654) {
                                if(content.contains("또는")) {
                                    content = content.replaceAll(" ", "");
                                    content = content.replaceAll("또는", "/");
                                }
                                content = content.replaceAll("/", " ").trim();
                            }

                            // 학생회관 메뉴 정리
                            if(placeId == 9757) {
                                content = content.split("-제공되는 메뉴")[0].trim();
                            }

                            // 애기능 - 학생식당
                            if(placeId == 3103) {
                                if(!content.contains("[학생식당]")) content = NO_MENU_INFO;
                                else content = content.substring(content.lastIndexOf("[학생식당]") + "[학생식당]".length(), content.contains("[교직원식당]") && content.lastIndexOf("[교직원식당]") > content.lastIndexOf("[학생식당]")? content.lastIndexOf("[교직원식당]") : content.length()).trim();
                            }
                            // 애기능 - 교직원식당
                            else if(placeId == 2490) {
                                if(!content.contains("[교직원식당]")) content = NO_MENU_INFO;
                                else content = content.substring(content.lastIndexOf("[교직원식당]") + "[교직원식당]".length(), content.contains("[학생식당]") && content.lastIndexOf("[학생식당]") > content.lastIndexOf("[교직원식당]")? content.lastIndexOf("[학생식당]") : content.length()).trim();
                            }

                            CafeteriaMenu savedMenu = cafeteriaMenuRepository.findByDateAndKindAndPlaceId(date, kind, placeId);

                            // 메뉴가 존재하는 경우
                            if(!content.equals(NO_MENU_INFO)) {

                                // 메뉴가 추가된 경우
                                if (savedMenu == null) {
                                    // 학식 메뉴 저장
                                    cafeteriaMenuRepository.save(new CafeteriaMenu(date, kind, content, placeId));
                                }

                                // 메뉴가 변경된 경우
                                else if(!savedMenu.getMenu().equals(content)) {
                                    savedMenu.setMenu(content);
                                }

                            }
                        }
                    }
                    // 해당 날짜의 모든 메뉴를 추출했으므로 날짜를 초기화
                    currentDate = null;
                }
            }

            // 식당 설명 없으면 메뉴 정보 없다고 표시
            if(place.getDescription().isEmpty()) {
                place.setDescription(NO_MENU_INFO);
            }

        } catch (IOException e) {
            // 웹 접속 관련 오류 처리 (404, 네트워크 문제 등)
            System.err.println("웹 페이지 접속 오류 (IOException): " + e.getMessage());
        } catch (Exception e) {
            // 그 외 예상치 못한 오류 처리
            System.err.println("스크래핑 중 예상치 못한 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
