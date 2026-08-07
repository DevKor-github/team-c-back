-- 고대로 App Store 공개 버전 기록과 기존 프런트 공지를 DB로 이관한다.
-- 과거 공지는 목록에만 남기기 위해 show_popup=false로 저장한다.
-- 공개 시각은 App Store 버전 기록의 UTC 시각을 Asia/Seoul로 변환한 값이다.
-- 1.1.5 행은 실제 1.1.5 바이너리가 스토어에 준비된 뒤 이 SQL을 적용해야 한다.

INSERT INTO tb_update_notice (
    title,
    description,
    published_at,
    app_version,
    show_popup,
    link_url,
    link_label
)
SELECT
    seed.title,
    seed.description,
    seed.published_at,
    seed.app_version,
    seed.show_popup,
    seed.link_url,
    seed.link_label
FROM (
    SELECT
        '고대로 1.0.1 출시' AS title,
        '고대로 iOS 정식 출시!' AS description,
        '2025-01-29 08:01:39' AS published_at,
        '1.0.1' AS app_version,
        FALSE AS show_popup,
        NULL AS link_url,
        NULL AS link_label
    UNION ALL SELECT
        '고대로 1.0.2 업데이트',
        '스토어 정보 변경',
        '2025-01-30 12:03:27',
        '1.0.2',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.0.3 업데이트',
        '건물 운영시간 추가',
        '2025-02-03 04:37:01',
        '1.0.3',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.0.5 업데이트',
        '※ 배리어프리 정보는 KUTV에서 제공해주셨습니다.',
        '2025-06-15 20:34:18',
        '1.0.5',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.0.8 업데이트',
        '고대로 앱이 새롭게 업데이트되었습니다! ※ 고대로에서 제작한 건물 실내지도 이미지를 캡처해 다른 곳에 사용할 경우 반드시 「고대로 앱」 출처를 밝혀주세요. 앞으로도 더 편리한 캠퍼스 생활을 위해 계속 개선해 나가겠습니다!!',
        '2025-09-18 17:33:18',
        '1.0.8',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.0.9 업데이트',
        'Fix: 연결 오류 버그 핫픽스',
        '2025-09-25 15:25:05',
        '1.0.9',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.1.1 업데이트',
        '라운지·강의실·공간정보 기능이 추가되었어요.',
        '2025-12-24 20:07:26',
        '1.1.1',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.1.3 업데이트',
        'Hotfix: Google 로그인 오류 수정',
        '2026-01-30 08:34:11',
        '1.1.3',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '개인정보 처리방침 제·개정 공지',
        '안녕하세요, 고대로입니다. 고대로를 이용해주시는 사용자 분들에게 진심으로 감사드립니다. 2026년 2월 28일자로 개인정보 처리방침이 일부 개정되었습니다. 개인정보 처리방침 관련 문의는 고대로 공식 이메일(kodaero.ku@gmail.com)로 연락 부탁드립니다. 고대로는 늘 사용자 분들의 안전한 개인정보 보호를 위해 힘쓰도록 하겠습니다. 감사합니다.',
        '2026-02-25 00:00:00',
        '1.1.4',
        FALSE,
        'https://lying-musician-34f.notion.site/75c795349e5e4eecbcc0b5d787e94b0c?pvs=4',
        '개인정보처리방침 원문 보기'
    UNION ALL SELECT
        '고대로 1.1.4 업데이트',
        '후기와 포인트, 실시간 위치, 건물·라운지 정보를 추가했어요.',
        '2026-03-01 13:26:09',
        '1.1.4',
        FALSE,
        NULL,
        NULL
    UNION ALL SELECT
        '고대로 1.1.5 업데이트',
        '포인트 상점과 공지·사용 의견 기능을 추가하고 편의시설 탐색을 다듬었어요.',
        CURRENT_TIMESTAMP,
        '1.1.5',
        TRUE,
        NULL,
        NULL
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_update_notice existing
    WHERE existing.app_version = seed.app_version
      AND existing.title = seed.title
);

-- 이전 초안 SQL이 이미 적용된 환경에서도 잘못 들어간 1.1.5 내용을 바로잡는다.
UPDATE tb_update_notice
SET description = '포인트 상점과 공지·사용 의견 기능을 추가하고 편의시설 탐색을 다듬었어요.',
    show_popup = TRUE
WHERE app_version = '1.1.5'
  AND title = '고대로 1.1.5 업데이트';

INSERT INTO tb_update_notice_feature (update_notice_id, display_order, feature)
SELECT
    notice.update_notice_id,
    seed.display_order,
    seed.feature
FROM (
    SELECT '1.0.5' AS app_version, '고대로 1.0.5 업데이트' AS title, 0 AS display_order,
           '정운오 IT교양관, 정경관 등 일부 건물 정보가 새로 추가되었어요.' AS feature
    UNION ALL SELECT '1.0.5', '고대로 1.0.5 업데이트', 1,
           '즐겨찾기 기능과 배리어프리 정보(경사로, 장애인 화장실)도 확인할 수 있어요!'
    UNION ALL SELECT '1.0.5', '고대로 1.0.5 업데이트', 2,
           '전체 UI도 더욱 보기 쉽고 편리하게 개선되었답니다.'

    UNION ALL SELECT '1.0.8', '고대로 1.0.8 업데이트', 0,
           '고연전 무료주점 위치와 메뉴를 확인할 수 있어요.'
    UNION ALL SELECT '1.0.8', '고대로 1.0.8 업데이트', 1,
           '주변 외부 음식점 정보도 함께 볼 수 있어요.'
    UNION ALL SELECT '1.0.8', '고대로 1.0.8 업데이트', 2,
           '리필로드 다회용컵과 야외 쓰레기통 편의시설이 추가되었어요.'
    UNION ALL SELECT '1.0.8', '고대로 1.0.8 업데이트', 3,
           '쓰레기통 위치를 쉽게 찾을 수 있어요.'

    UNION ALL SELECT '1.1.1', '고대로 1.1.1 업데이트', 0,
           '라운지의 실시간 혼잡도를 확인할 수 있어요.'
    UNION ALL SELECT '1.1.1', '고대로 1.1.1 업데이트', 1,
           '강의실 시간표가 추가되어 빈 강의실을 찾을 수 있어요.'
    UNION ALL SELECT '1.1.1', '고대로 1.1.1 업데이트', 2,
           '공간정보 탭에서 혼잡도와 학생 식당 메뉴를 한눈에 볼 수 있어요.'

    UNION ALL SELECT '1.1.3', '고대로 1.1.3 업데이트', 0,
           '라운지의 실시간 혼잡도를 확인할 수 있어요.'
    UNION ALL SELECT '1.1.3', '고대로 1.1.3 업데이트', 1,
           '강의실 시간표가 추가되어 빈 강의실을 찾을 수 있어요.'
    UNION ALL SELECT '1.1.3', '고대로 1.1.3 업데이트', 2,
           '공간정보 탭에서 혼잡도와 학생 식당 메뉴를 한눈에 볼 수 있어요.'
    UNION ALL SELECT '1.1.3', '고대로 1.1.3 업데이트', 3,
           'HotFix: 구글 로그인 오류 수정'

    UNION ALL SELECT '1.1.4', '개인정보 처리방침 제·개정 공지', 0,
           '개인정보 처리의 원칙: 제3자 정보 제공 업체에 네이버 주식회사가 추가되었습니다.'
    UNION ALL SELECT '1.1.4', '개인정보 처리방침 제·개정 공지', 1,
           '아동의 개인정보 보호: 이용 주체의 조건을 명확히 했습니다.'

    UNION ALL SELECT '1.1.4', '고대로 1.1.4 업데이트', 0,
           '다녀온 식당·카페의 후기를 남길 수 있어요.'
    UNION ALL SELECT '1.1.4', '고대로 1.1.4 업데이트', 1,
           '포인트를 모아 캐릭터를 성장시켜보세요.'
    UNION ALL SELECT '1.1.4', '고대로 1.1.4 업데이트', 2,
           '내 위치를 실시간으로 확인할 수 있어요.'
    UNION ALL SELECT '1.1.4', '고대로 1.1.4 업데이트', 3,
           '학생회관 건물 정보가 추가되었어요.'
    UNION ALL SELECT '1.1.4', '고대로 1.1.4 업데이트', 4,
           '혼잡도를 알 수 있는 라운지가 추가되었어요. (하나스퀘어 라운지, 과학도서관 알파라운지)'

    UNION ALL SELECT '1.1.5', '고대로 1.1.5 업데이트', 0,
           '포인트로 캐릭터를 구매하고 착용할 수 있는 포인트 상점을 추가했어요.'
    UNION ALL SELECT '1.1.5', '고대로 1.1.5 업데이트', 1,
           '서비스 개선을 위한 인앱 사용 설문과 추가 의견 보내기를 추가했어요.'
    UNION ALL SELECT '1.1.5', '고대로 1.1.5 업데이트', 2,
           '마이페이지에서 지난 공지를 모아보고, 중요한 공지만 한 번 팝업으로 확인할 수 있어요.'
    UNION ALL SELECT '1.1.5', '고대로 1.1.5 업데이트', 3,
           '정수기 옆에서 텀블러 세척기 태그를 바로 찾을 수 있도록 편의시설 순서를 개선했어요.'
    UNION ALL SELECT '1.1.5', '고대로 1.1.5 업데이트', 4,
           '오래된 앱에서는 최신 버전을 설치한 뒤 안전하게 이용하도록 업데이트 안내를 개선했어요.'
) seed
JOIN tb_update_notice notice
  ON notice.app_version = seed.app_version
 AND notice.title = seed.title
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_update_notice_feature existing_feature
    WHERE existing_feature.update_notice_id = notice.update_notice_id
      AND existing_feature.display_order = seed.display_order
);

-- 이전 1.1.5 초안의 같은 순번 feature가 남아 있어도 현재 릴리스 내용으로 교정한다.
UPDATE tb_update_notice_feature feature
SET feature.feature = CASE feature.display_order
    WHEN 0 THEN '포인트로 캐릭터를 구매하고 착용할 수 있는 포인트 상점을 추가했어요.'
    WHEN 1 THEN '서비스 개선을 위한 인앱 사용 설문과 추가 의견 보내기를 추가했어요.'
    WHEN 2 THEN '마이페이지에서 지난 공지를 모아보고, 중요한 공지만 한 번 팝업으로 확인할 수 있어요.'
    WHEN 3 THEN '정수기 옆에서 텀블러 세척기 태그를 바로 찾을 수 있도록 편의시설 순서를 개선했어요.'
    WHEN 4 THEN '오래된 앱에서는 최신 버전을 설치한 뒤 안전하게 이용하도록 업데이트 안내를 개선했어요.'
    ELSE feature.feature
END
WHERE feature.update_notice_id IN (
    SELECT notice.update_notice_id
    FROM tb_update_notice notice
    WHERE notice.app_version = '1.1.5'
      AND notice.title = '고대로 1.1.5 업데이트'
)
  AND feature.display_order BETWEEN 0 AND 4;
