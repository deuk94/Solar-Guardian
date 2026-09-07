package com.kopo.solar.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * 기상청 단기예보/중기예보를 주기적으로 받아와 메모리에 캐싱.
 * kma.api-key 없거나 호출 실패하면 조용히 빈 목록 유지 (날씨 위젯만 안 뜨고 앱은 정상 기동)
 */
@Slf4j
@Service
public class WeatherService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int[] VILAGE_BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    private final RestClient restClient = RestClient.create();

    @Value("${kma.api-key}")
    private String apiKey;

    @Value("${kma.nx}")
    private int nx;

    @Value("${kma.ny}")
    private int ny;

    @Value("${kma.mid-land-reg-id}")
    private String midLandRegId;

    @Value("${kma.mid-ta-reg-id}")
    private String midTaRegId;

    private volatile List<HourlySlot> hourly = List.of();
    private volatile List<DailySlot> daily = List.of();

    public record HourlySlot(String label, Integer temp, String sky, Integer pop, String dayMarker,
                              Integer humidity, Integer windDeg, String windDir, Integer windSpeed, String pcp) {}

    public record DailySlot(LocalDate date, String label, Integer minTemp, Integer maxTemp,
                             Integer amPop, Integer pmPop, String amSky, String pmSky) {}

    // 캐싱된 단기예보 반환
    public List<HourlySlot> getHourly() {
        return hourly;
    }

    // 캐싱된 중기예보 반환
    public List<DailySlot> getDaily() {
        return daily;
    }

    // 앱 기동 시 캐시 최초 1회 채움
    @PostConstruct
    public void init() {
        refreshHourly();
        refreshDaily();
    }

    // 단기예보는 3시간마다 새 발표시각 생기니 그 주기로 갱신
    @Scheduled(fixedRate = 3 * 60 * 60 * 1000)
    public void refreshHourly() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        try {
            hourly = fetchHourly();
        } catch (Exception e) {
            log.warn("단기예보 조회 실패: {}", e.getMessage());
        }
    }

    // 중기예보는 하루 2번(06시/18시)만 갱신되니 그 주기로 충분
    @Scheduled(fixedRate = 12 * 60 * 60 * 1000)
    public void refreshDaily() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        try {
            daily = fetchDaily();
        } catch (Exception e) {
            log.warn("중기예보 조회 실패: {}", e.getMessage());
        }
    }

    // 최신 단기예보 조회해서 지금 시각 이후 24개 시간대로 정리
    private List<HourlySlot> fetchHourly() {
        LocalDateTime base = latestVilageBaseTime(LocalDateTime.now());
        String baseDate = base.format(DATE_FMT);
        String baseTime = String.format("%02d00", base.getHour());

        JsonNode root = get(vilageFcstUrl(baseDate, baseTime));

        JsonNode items = root.path("response").path("body").path("items").path("item");

        // fcstDate+fcstTime 별로 카테고리 값 모음
        Map<String, Map<String, String>> byTime = new LinkedHashMap<>();
        for (JsonNode item : items) {
            String key = item.path("fcstDate").asText() + item.path("fcstTime").asText();
            byTime.computeIfAbsent(key, k -> new LinkedHashMap<>())
                    .put(item.path("category").asText(), item.path("fcstValue").asText());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        List<HourlySlot> result = new ArrayList<>();
        LocalDate lastDate = today;
        for (var entry : byTime.entrySet()) {
            String key = entry.getKey();
            LocalDateTime slotTime = LocalDateTime.parse(key, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            if (slotTime.isBefore(now.withMinute(0).withSecond(0).withNano(0))) {
                continue;
            }
            Map<String, String> cat = entry.getValue();
            Integer temp = parseIntOrNull(cat.get("TMP"));
            String sky = mapSkyPty(cat.get("SKY"), cat.get("PTY"));
            if (!isDaytime(slotTime.getHour())) {
                if (sky.equals("CLEAR")) sky = "CLEAR_NIGHT";
                else if (sky.equals("PARTLY_CLOUDY")) sky = "PARTLY_CLOUDY_NIGHT";
            }
            Integer pop = parseIntOrNull(cat.get("POP"));
            Integer humidity = parseIntOrNull(cat.get("REH"));
            Integer windDeg = parseIntOrNull(cat.get("VEC"));
            String windDir = windDeg == null ? null : windDirName(windDeg);
            Integer windSpeed = parseIntOrNull(cat.get("WSD"));
            String pcp = parsePcp(cat.get("PCP"));

            LocalDate slotDate = slotTime.toLocalDate();
            String dayMarker = null;
            if (!slotDate.equals(lastDate)) {
                dayMarker = hourlyDayMarker((int) ChronoUnit.DAYS.between(today, slotDate));
                lastDate = slotDate;
            }

            result.add(new HourlySlot(String.format("%02d시", slotTime.getHour()), temp, sky, pop, dayMarker,
                    humidity, windDeg, windDir, windSpeed, pcp));
            if (result.size() >= 24) {
                break;
            }
        }
        return result;
    }

    // 단기예보는 3시간 단위로만 발표되니, 지금 시각 기준 가장 최근 발표시각 찾기
    private LocalDateTime latestVilageBaseTime(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(10);
        for (int i = VILAGE_BASE_HOURS.length - 1; i >= 0; i--) {
            LocalDateTime candidate = cutoff.toLocalDate().atTime(VILAGE_BASE_HOURS[i], 0);
            if (!candidate.isAfter(cutoff)) {
                return candidate;
            }
        }
        return cutoff.toLocalDate().minusDays(1).atTime(23, 0);
    }

    // SKY 코드 자체엔 낮/밤 구분 없음(맑음=1은 새벽이든 낮이든 동일) - 시간으로 직접 판단
    private boolean isDaytime(int hour) {
        return hour >= 6 && hour <= 19;
    }

    // 강수형태(PTY)·하늘상태(SKY) 코드를 화면 표시용 카테고리 문자열로 변환
    private String mapSkyPty(String skyCode, String ptyCode) {
        int pty = parseIntOrNull(ptyCode) == null ? 0 : parseIntOrNull(ptyCode);
        if (pty == 1) return "RAIN";
        if (pty == 4) return "SHOWER"; // 소나기 - 해 떠있는 채로 비
        if (pty == 2) return "RAIN"; // 비/눈
        if (pty == 3) return "SNOW";

        int sky = parseIntOrNull(skyCode) == null ? 1 : parseIntOrNull(skyCode);
        return switch (sky) {
            case 3 -> "PARTLY_CLOUDY";
            case 4 -> "CLOUDY";
            default -> "CLEAR";
        };
    }

    // 오늘~모레는 단기예보, 3~9일 후는 중기예보에서 가져와 10일치로 합침
    private List<DailySlot> fetchDaily() {
        List<DailySlot> result = new ArrayList<>();
        result.addAll(fetchTodayTomorrowFromVilage());
        result.addAll(fetchMidTerm());
        return result;
    }

    // 단기예보에서 오늘/내일/모레 3일치 최저·최고기온, 오전·오후 하늘상태·강수확률 추출
    private List<DailySlot> fetchTodayTomorrowFromVilage() {
        LocalDateTime base = latestVilageBaseTime(LocalDateTime.now());
        String baseDate = base.format(DATE_FMT);
        String baseTime = String.format("%02d00", base.getHour());

        JsonNode root = get(vilageFcstUrl(baseDate, baseTime));

        JsonNode items = root.path("response").path("body").path("items").path("item");

        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = List.of(today, today.plusDays(1), today.plusDays(2));
        Map<LocalDate, Integer> minByDate = new LinkedHashMap<>();
        Map<LocalDate, Integer> maxByDate = new LinkedHashMap<>();
        Map<LocalDate, String> amSkyByDate = new LinkedHashMap<>();
        Map<LocalDate, String> pmSkyByDate = new LinkedHashMap<>();
        Map<LocalDate, Integer> amPopByDate = new LinkedHashMap<>();
        Map<LocalDate, Integer> pmPopByDate = new LinkedHashMap<>();

        for (JsonNode item : items) {
            LocalDate date = LocalDate.parse(item.path("fcstDate").asText(), DATE_FMT);
            if (!targetDates.contains(date)) {
                continue;
            }
            String category = item.path("category").asText();
            String value = item.path("fcstValue").asText();
            int hour = Integer.parseInt(item.path("fcstTime").asText().substring(0, 2));

            switch (category) {
                case "TMN" -> minByDate.put(date, parseIntOrNull(value));
                case "TMX" -> maxByDate.put(date, parseIntOrNull(value));
                case "SKY" -> {
                    if (hour == 9) amSkyByDate.put(date, mapSkyPty(value, null));
                    if (hour == 15) pmSkyByDate.put(date, mapSkyPty(value, null));
                }
                case "POP" -> {
                    if (hour == 9) amPopByDate.put(date, parseIntOrNull(value));
                    if (hour == 15) pmPopByDate.put(date, parseIntOrNull(value));
                }
                default -> {}
            }
        }

        List<DailySlot> result = new ArrayList<>();
        for (int offset = 0; offset < targetDates.size(); offset++) {
            LocalDate date = targetDates.get(offset);
            result.add(new DailySlot(
                    date, dayLabel(date, offset),
                    minByDate.get(date), maxByDate.get(date),
                    amPopByDate.get(date), pmPopByDate.get(date),
                    amSkyByDate.get(date), pmSkyByDate.get(date)));
        }
        return result;
    }

    // 3~9일 후 육상예보(하늘상태·강수확률)와 기온예보 받아와 DailySlot으로 합침
    private List<DailySlot> fetchMidTerm() {
        LocalDateTime tmFc = latestMidBaseTime(LocalDateTime.now());
        String tmFcStr = tmFc.format(DATE_FMT) + String.format("%02d00", tmFc.getHour());

        JsonNode landRoot = get("https://apis.data.go.kr/1360000/MidFcstInfoService/getMidLandFcst"
                + "?serviceKey=" + apiKey
                + "&numOfRows=10&pageNo=1&dataType=JSON"
                + "&regId=" + midLandRegId
                + "&tmFc=" + tmFcStr);

        JsonNode taRoot = get("https://apis.data.go.kr/1360000/MidFcstInfoService/getMidTa"
                + "?serviceKey=" + apiKey
                + "&numOfRows=10&pageNo=1&dataType=JSON"
                + "&regId=" + midTaRegId
                + "&tmFc=" + tmFcStr);

        JsonNode land = firstItem(landRoot);
        JsonNode ta = firstItem(taRoot);

        // 오늘/내일/모레는 단기예보에서 이미 채우니, 중기예보는 3~9일 후까지만 붙여 10일치 맞춤
        List<DailySlot> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int d = 3; d <= 9; d++) {
            LocalDate date = today.plusDays(d);
            String amSky, pmSky;
            Integer amPop, pmPop;
            if (d <= 7) {
                amSky = mapMidText(text(land, "wf" + d + "Am"));
                pmSky = mapMidText(text(land, "wf" + d + "Pm"));
                amPop = intField(land, "rnSt" + d + "Am");
                pmPop = intField(land, "rnSt" + d + "Pm");
            } else {
                amSky = mapMidText(text(land, "wf" + d));
                pmSky = amSky;
                amPop = intField(land, "rnSt" + d);
                pmPop = amPop;
            }
            Integer minTemp = intField(ta, "taMin" + d);
            Integer maxTemp = intField(ta, "taMax" + d);
            result.add(new DailySlot(date, dayLabel(date, d), minTemp, maxTemp, amPop, pmPop, amSky, pmSky));
        }
        return result;
    }

    // 중기예보는 하루 06시/18시 두 번만 발표되니, 지금 시각 기준 가장 최근 발표시각 찾기
    private LocalDateTime latestMidBaseTime(LocalDateTime now) {
        LocalDateTime cutoff = now.minusMinutes(30);
        LocalDateTime todaySix = cutoff.toLocalDate().atTime(6, 0);
        LocalDateTime todayEighteen = cutoff.toLocalDate().atTime(18, 0);
        if (!todayEighteen.isAfter(cutoff)) return todayEighteen;
        if (!todaySix.isAfter(cutoff)) return todaySix;
        return cutoff.toLocalDate().minusDays(1).atTime(18, 0);
    }

    // 중기예보 날씨 문구("구름많음", "흐리고 비" 등)를 화면 표시용 카테고리로 변환
    private String mapMidText(String text) {
        if (text == null) return "CLEAR";
        if (text.contains("눈")) return "SNOW";
        if (text.contains("비") || text.contains("소나기")) return "RAIN";
        if (text.contains("흐림")) return "CLOUDY";
        if (text.contains("구름")) return "PARTLY_CLOUDY";
        return "CLEAR";
    }

    // VEC(풍향, 0~360도, 바람이 불어오는 방향)를 8방위 한글 이름으로 변환
    private String windDirName(int deg) {
        String[] names = {"북풍", "북동풍", "동풍", "남동풍", "남풍", "남서풍", "서풍", "북서풍"};
        int idx = (int) Math.round(((deg % 360) / 45.0)) % 8;
        if (idx < 0) idx += 8;
        return names[idx];
    }

    /*
     * PCP(1시간 강수량)는 "강수없음"/"1.0mm 미만"/"30.0~50.0mm"/"50.0mm 이상" 같은 범주형 텍스트로 옴
     * 화면엔 짧게 "0"/"~1"/"30~50"/"50+" 형태로 정리
     */
    private String parsePcp(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("-") || raw.contains("강수없음")) return "0";
        String cleaned = raw.replace("mm", "").trim();
        if (cleaned.contains("미만")) return "~" + trimDecimal(cleaned.replace("미만", "").trim());
        if (cleaned.contains("이상")) return trimDecimal(cleaned.replace("이상", "").trim()) + "+";
        if (cleaned.contains("~")) {
            String[] parts = cleaned.split("~");
            return trimDecimal(parts[0].trim()) + "~" + trimDecimal(parts[1].trim());
        }
        return trimDecimal(cleaned);
    }

    // 소수점 이하 0이면 정수로, 아니면 그대로
    private String trimDecimal(String s) {
        try {
            double d = Double.parseDouble(s);
            return d == Math.floor(d) ? String.valueOf((int) d) : String.valueOf(d);
        } catch (NumberFormatException e) {
            return s;
        }
    }

    // 시간당 그래프에서 날짜 넘어가는 지점에 "내일"/"모레"로 표시 (주간예보 요일 라벨과는 별개)
    private String hourlyDayMarker(int offset) {
        return switch (offset) {
            case 1 -> "내일";
            case 2 -> "모레";
            default -> offset + "일 후";
        };
    }

    // 오늘/내일은 "오늘"/"내일", 그 이후는 요일 한 글자
    private String dayLabel(LocalDate date, int offset) {
        if (offset == 0) return "오늘";
        if (offset == 1) return "내일";
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    /*
     * 서비스키가 이미 퍼센트인코딩된 값이라 UriComponentsBuilder 거치면 깨짐 (원인은 TROUBLESHOOTING.md)
     * 그래서 URL을 문자열로 직접 조립해 URI.create()로 넘김
     */
    private String vilageFcstUrl(String baseDate, String baseTime) {
        return "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
                + "?serviceKey=" + apiKey
                + "&numOfRows=1000&pageNo=1&dataType=JSON"
                + "&base_date=" + baseDate
                + "&base_time=" + baseTime
                + "&nx=" + nx
                + "&ny=" + ny;
    }

    // 기상청 API GET 호출 후 JSON 파싱
    private JsonNode get(String url) {
        return restClient.get().uri(URI.create(url)).retrieve().body(JsonNode.class);
    }

    /* ─── 파싱 유틸 ─── */

    // 중기예보 응답 items 배열에서 첫 항목만 꺼냄 (지역 하나만 조회하니 항상 1건)
    private JsonNode firstItem(JsonNode root) {
        JsonNode item = root.path("response").path("body").path("items").path("item");
        return item.isArray() && item.size() > 0 ? item.get(0) : item;
    }

    // 필드값 문자열로 꺼내기, 없으면 null
    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() ? null : v.asText();
    }

    // 필드값 정수로 꺼내기
    private Integer intField(JsonNode node, String field) {
        return parseIntOrNull(text(node, field));
    }

    // 문자열을 정수로 변환, 실패하거나 빈값/"-"/"null"이면 null
    private Integer parseIntOrNull(String v) {
        if (v == null || v.isBlank() || v.equals("-") || v.equals("null")) return null;
        try {
            return (int) Math.round(Double.parseDouble(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
