package com.sendit.share;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MultiPlaceExtractor {
    private static final Pattern DESCRIPTION_LABEL = Pattern.compile("^\\s*(?:📍|\\d{1,2}\\uFE0F?\\u20E3|[①-⑳]|\\d{1,2}[.)]|(?:장소명|상호명?|가게명|매장명|숙소명)\\s*[:：-])\\s*(.+)$");
    public long descriptionPlaceCount(String description) {
        return description == null ? 0 : description.lines().filter(line -> DESCRIPTION_LABEL.matcher(line).find()).count();
    }
    private static final Pattern SLIDE = Pattern.compile("--- SLIDE \\d+ ---");
    private static final Pattern PREFIX = Pattern.compile(
            "^[\\s•·✓✔★☆▶▷#@|\\-–—]+|^\\d{1,2}[.)\\s-]+");
    private static final Pattern ADDRESS = Pattern.compile(
            ".*(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주).*(로|길|동|구|시|군)\\b.*");
    private static final List<String> BLOCKED = List.of(
            "총정리", "무한리필", "길거리간식", "맛집", "추천", "가격", "메뉴", "주소",
            "영업시간", "팔로우", "저장", "서울에서", "배부르게", "먹을 수", "instagram",
            "slide", "곳을", "모아", "간식배", "가성비", "데이트", "광고");
    private static final Pattern FOOD_NAME = Pattern.compile(
            "([가-힣A-Za-z0-9]{2,14})\\s*(부대찌개|나주곰탕|마라탕|칼국수|막국수|국밥|곱창|"
                    + "삼겹살|갈비|냉면|해장국|순대국|중국집|횟집|초밥|스시|카페|베이커리|자금성)");
    private static final Pattern SEOUL_DISTRICT = Pattern.compile(
            "(강남|강동|강북|강서|관악|광진|구로|금천|노원|도봉|동대문|동작|마포|서대문|"
                    + "서초|성동|성북|송파|양천|영등포|용산|은평|종로|중구|중랑)(?:구|맛집|역|동)?");

    private final KakaoPlaceSearchClient kakao;

    /** Captions often list several named places with an address on the following line. */
    public List<PageMetadata> extractDescription(String description, PageMetadata source) {
        if (description == null || description.isBlank()) return List.of();
        var parser = new SharedTextMetadataParser();
        var label = DESCRIPTION_LABEL;
        var lines = description.lines().limit(400).toList();
        Map<String, PageMetadata> found = new LinkedHashMap<>();
        java.util.Set<String> attempted = new java.util.HashSet<>();
        for (int i = 0; i < lines.size() && attempted.size() < 16; i++) {
            var match = label.matcher(lines.get(i));
            if (!match.find()) continue;
            String heading = match.group(1).trim();
            StringBuilder block = new StringBuilder("장소명: " + heading);
            for (int next = i + 1; next < Math.min(i + 5, lines.size()); next++) {
                if (label.matcher(lines.get(next)).find()) break;
                block.append('\n').append(lines.get(next));
            }
            PageMetadata parsed = parser.parse(block.toString());
            if (parsed.placeName() == null) continue;
            String hint = parsed.address() == null ? inferRegion(source) : parsed.address();
            if (!attempted.add(normalize(parsed.placeName()) + "|" + normalize(hint))) continue;
            var resolved = kakao.resolveCandidate(parsed.placeName(), hint);
            // Captions prefix business names with a neighbourhood or nearby landmark.
            // Retry without that prefix only when the same block provides an address.
            if (resolved.isEmpty() && parsed.address() != null && heading.contains(" ")) {
                resolved = kakao.resolveCandidate(heading.substring(heading.indexOf(' ') + 1).trim(), hint);
            }
            resolved.ifPresent(place -> found.putIfAbsent(
                    normalize(place.placeName()) + "|" + normalize(place.address()), place));
        }
        return new ArrayList<>(found.values());
    }

    public MultiPlaceExtractor(KakaoPlaceSearchClient kakao) {
        this.kakao = kakao;
    }

    public List<PageMetadata> extract(String ocrText, PageMetadata source) {
        if (ocrText == null || ocrText.isBlank()) return List.of();
        Map<String, PageMetadata> found = new LinkedHashMap<>();
        int attempts = 0;
        for (String raw : ocrText.lines().toList()) {
            String[] parts = raw.split("\\|", 2);
            String candidate = clean(parts[0]);
            String addressHint = parts.length > 1 ? parts[1].trim() : null;
            boolean structured = addressHint != null
                    && addressHint.matches(".*(로|길)\\s*\\d+.*");
            if (!isCandidate(candidate, structured)) continue;
            attempts++;
            resolve(candidate, addressHint, source, found);
            if (attempts >= 140) break;
        }
        return new ArrayList<>(found.values());
    }

    /** 자동 자막은 일반 문장이므로 음식 업종이 붙은 상호만 지역과 함께 검증한다. */
    public List<PageMetadata> extractCaption(String transcript, PageMetadata source) {
        if (transcript == null || transcript.isBlank()) return List.of();
        Map<String, PageMetadata> found = new LinkedHashMap<>();
        String region = inferRegion(source);
        int attempts = 0;
        for (String raw : transcript.lines().toList()) {
            var matcher = FOOD_NAME.matcher(raw.replaceAll("\\s+", " "));
            while (matcher.find() && attempts++ < 60) {
                String prefix = matcher.group(1);
                String suffix = matcher.group(2);
                if (isCaptionPrefix(prefix)) {
                    for (String candidate : captionVariants(prefix, suffix)) {
                        if (resolve(candidate, region, source, found)) break;
                    }
                }
            }
            // 자동 자막은 상호를 띄어 쓰기도 한다. 업종명 없이 고유 상호가 세 번 이상
            // 반복된 경우에만 후보로 만들고, 반드시 영상 지역의 카카오 결과와 대조한다.
            for (String candidate : repeatedNamedCandidates(raw)) {
                resolve(candidate, region, source, found);
            }
        }
        return new ArrayList<>(found.values());
    }

    private boolean resolve(String candidate, String addressHint, PageMetadata source,
                         Map<String, PageMetadata> found) {
        var resolved = kakao.resolveCandidate(candidate, addressHint)
                .or(() -> fallback(candidate, addressHint, source));
        resolved.ifPresent(place -> {
                    String key = normalize(place.placeName()) + "|" + normalize(place.address());
                    found.putIfAbsent(key, new PageMetadata(
                            source.title(), source.description(), null,
                            place.placeName(), place.category(), place.address(),
                            place.latitude(), place.longitude()));
                });
        return resolved.isPresent();
    }

    private List<String> captionVariants(String prefix, String suffix) {
        List<String> candidates = new ArrayList<>();
        candidates.add(prefix + suffix);
        candidates.add(prefix + " " + suffix);
        if (suffix.equals("곰탕")) candidates.add(prefix + "나주곰탕");
        return candidates.stream().distinct().toList();
    }

    private List<String> repeatedNamedCandidates(String raw) {
        String line = raw.replaceAll("[^0-9A-Za-z가-힣\\s]", " ")
                .replaceAll("\\s+", " ").trim();
        if (line.isBlank()) return List.of();
        String[] words = line.split(" ");
        List<String> candidates = new ArrayList<>();
        for (int size = 1; size <= 2; size++) {
            for (int start = 0; start + size * 3 <= words.length; start++) {
                String first = String.join(" ", java.util.Arrays.copyOfRange(words, start, start + size));
                String second = String.join(" ", java.util.Arrays.copyOfRange(words, start + size, start + size * 2));
                String third = String.join(" ", java.util.Arrays.copyOfRange(words, start + size * 2, start + size * 3));
                if (first.equals(second) && first.equals(third) && first.length() >= 3
                        && isCaptionPrefix(first) && isCandidate(first, false)) {
                    candidates.add(first);
                }
            }
        }
        return candidates;
    }

    private boolean isCaptionPrefix(String value) {
        String normalized = normalize(value);
        return normalized.length() >= 2 && !List.of(
                "안녕하세요", "배고파", "진하", "깊은", "그런", "이런", "그냥", "무슨",
                "메뉴", "한국인", "우리가", "오늘", "여기", "저기", "사장님", "몇번")
                .contains(normalized);
    }

    private String inferRegion(PageMetadata source) {
        String text = String.join(" ", source.title() == null ? "" : source.title(),
                source.description() == null ? "" : source.description());
        if (text.matches(".*(구디단|구로디지털단지).*")) return "서울 구로구";
        var matcher = SEOUL_DISTRICT.matcher(text);
        return matcher.find() ? "서울 " + matcher.group(1) + "구" : null;
    }

    private java.util.Optional<PageMetadata> fallback(
            String name, String addressHint, PageMetadata source) {
        if (addressHint == null || !addressHint.matches(".*(로|길)\\s*\\d+.*")) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new PageMetadata(
                source.title(), source.description(), null,
                name, "음식점", addressHint, null, null));
    }

    private String clean(String raw) {
        String value = SLIDE.matcher(raw.trim()).replaceAll("");
        value = PREFIX.matcher(value).replaceFirst("").trim();
        value = value.replaceAll("\\s{2,}", " ");
        value = value.replaceAll("\\s*[|/]\\s*.*$", "");
        value = value.replaceAll("\\s+\\d{1,3}(,\\d{3})*원.*$", "");
        return value.trim();
    }

    private boolean isCandidate(String value, boolean structured) {
        if (value.length() < 2 || value.length() > 35) return false;
        String lower = value.toLowerCase(Locale.KOREAN);
        if (!structured && BLOCKED.stream().anyMatch(lower::contains)) return false;
        if (ADDRESS.matcher(value).matches()) return false;
        if (value.matches("^[0-9.,:%~+\\-\\s]+$")) return false;
        long letters = value.chars().filter(Character::isLetter).count();
        long korean = value.chars().filter(ch -> ch >= '가' && ch <= '힣').count();
        if (korean > 0) return korean >= 3;
        return letters >= 5 && !value.equals(value.toUpperCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.KOREAN)
                .replaceAll("[^0-9a-z가-힣]", "");
    }
}
