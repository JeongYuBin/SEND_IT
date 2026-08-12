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
    private static final Pattern SLIDE = Pattern.compile("--- SLIDE \\d+ ---");
    private static final Pattern PREFIX = Pattern.compile(
            "^[\\s•·✓✔★☆▶▷#@|\\-–—]+|^\\d{1,2}[.)\\s-]+");
    private static final Pattern ADDRESS = Pattern.compile(
            ".*(서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주).*(로|길|동|구|시|군)\\b.*");
    private static final List<String> BLOCKED = List.of(
            "총정리", "무한리필", "길거리간식", "맛집", "추천", "가격", "메뉴", "주소",
            "영업시간", "팔로우", "저장", "서울에서", "배부르게", "먹을 수", "instagram",
            "slide", "곳을", "모아", "간식배", "가성비", "데이트", "광고");

    private final KakaoPlaceSearchClient kakao;

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
            java.util.Optional<PageMetadata> resolved = kakao.resolveCandidate(candidate, addressHint);
            resolved.or(() -> fallback(candidate, addressHint, source)).ifPresent(place -> {
                String key = normalize(place.placeName()) + "|" + normalize(place.address());
                found.putIfAbsent(key, new PageMetadata(
                        source.title(), source.description(), source.imageUrl(),
                        place.placeName(), place.category(), place.address(),
                        place.latitude(), place.longitude()));
            });
            if (attempts >= 140) break;
        }
        return new ArrayList<>(found.values());
    }

    private java.util.Optional<PageMetadata> fallback(
            String name, String addressHint, PageMetadata source) {
        if (addressHint == null || !addressHint.matches(".*(로|길)\\s*\\d+.*")) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new PageMetadata(
                source.title(), source.description(), source.imageUrl(),
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
