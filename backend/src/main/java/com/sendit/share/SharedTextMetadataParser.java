package com.sendit.share;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SharedTextMetadataParser {
    private static final Pattern EXPLICIT_PLACE = Pattern.compile(
            "(?im)(?:장소명|상호명?|가게명)\\s*[:：]\\s*([^\\n#|]{2,80})");
    private static final Pattern LABELED_PLACE_INFO = Pattern.compile(
            "(?im)^\\s*\\[(?:식당|매장|가게|장소)정보]\\s*([^\\r\\n]+)");
    private static final Pattern KOREAN_ADDRESS = Pattern.compile(
            "(?:(?:서울|부산|대구|인천|광주|대전|울산)(?:특별시|광역시)?|"
                    + "세종(?:특별자치시)?|제주(?:특별자치도)?|"
                    + "(?:경기|강원|충북|충남|전북|전남|경북|경남)(?:특별자치도|도)?)"
                    + "\\s+(?:[가-힣]+시\\s+)?[가-힣]+(?:시|군|구)"
                    + "\\s+[가-힣0-9]+(?:읍|면|동|리|로|길)(?:\\s+\\d+(?:-\\d+)?)?");
    private static final Pattern HASHTAG = Pattern.compile("#([\\p{L}\\p{N}_]{2,50})");
    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACE_SUFFIX = Pattern.compile(
            ".*(?:횟집|식당|카페|커피|베이커리|빵집|분식|국수|냉면|갈비|치킨|"
                    + "펜션|호텔|리조트|호스텔|게스트하우스|캠핑장|시장|해변|해수욕장|"
                    + "공원|박물관|미술관|전시관|수목원|정원|전망대|성당|사찰|궁|역|집)$");
    private static final Set<String> GENERIC_HASHTAGS = Set.of(
            "맛집", "카페", "여행", "관광", "추천", "데이트", "핫플", "먹방",
            "국내여행", "여행스타그램", "먹스타그램", "카페스타그램", "일상", "릴스",
            "강원", "강원도", "서울", "부산", "제주", "제주도", "속초", "강릉");

    public PageMetadata parse(String sharedText) {
        if (sharedText == null || sharedText.isBlank()) return empty();
        String text = sharedText.trim();
        PlaceInfo labeled = labeledPlaceInfo(text);
        String placeName = labeled == null ? null : labeled.placeName();
        if (placeName == null) placeName = explicitPlace(text);
        if (placeName == null) placeName = hashtagPlace(text);
        String address = labeled == null ? address(text) : labeled.address();
        return new PageMetadata(
                firstReadableLine(text),
                text,
                null,
                placeName,
                category(placeName, text),
                address,
                null,
                null
        );
    }

    public PageMetadata merge(PageMetadata page, PageMetadata shared) {
        return new PageMetadata(
                first(page.title(), shared.title()),
                combine(page.description(), shared.description()),
                first(page.imageUrl(), shared.imageUrl()),
                first(page.placeName(), shared.placeName()),
                first(page.category(), shared.category()),
                first(page.address(), shared.address()),
                page.latitude() != null ? page.latitude() : shared.latitude(),
                page.longitude() != null ? page.longitude() : shared.longitude()
        );
    }

    public boolean hasContent(PageMetadata metadata) {
        return metadata.title() != null
                || metadata.description() != null
                || metadata.placeName() != null;
    }

    private String explicitPlace(String text) {
        Matcher matcher = EXPLICIT_PLACE.matcher(text);
        return matcher.find() ? cleanCandidate(matcher.group(1)) : null;
    }

    private PlaceInfo labeledPlaceInfo(String text) {
        Matcher matcher = LABELED_PLACE_INFO.matcher(text);
        if (!matcher.find()) return null;
        String line = URL.matcher(matcher.group(1)).replaceAll(" ")
                .replaceAll("\\s+", " ").trim();
        Matcher addressMatcher = KOREAN_ADDRESS.matcher(line);
        if (!addressMatcher.find()) return new PlaceInfo(cleanCandidate(line), null);
        String placeName = cleanCandidate(line.substring(0, addressMatcher.start()));
        return new PlaceInfo(placeName, cleanCandidate(addressMatcher.group()));
    }

    private String address(String text) {
        Matcher matcher = KOREAN_ADDRESS.matcher(text);
        return matcher.find() ? cleanCandidate(matcher.group()) : null;
    }

    private String hashtagPlace(String text) {
        Matcher matcher = HASHTAG.matcher(text);
        String fallback = null;
        while (matcher.find()) {
            String candidate = matcher.group(1).replace('_', ' ').trim();
            String normalized = candidate.toLowerCase(Locale.KOREAN).replace(" ", "");
            if (GENERIC_HASHTAGS.contains(normalized)
                    || normalized.endsWith("맛집")
                    || normalized.endsWith("여행")) continue;
            if (PLACE_SUFFIX.matcher(normalized).matches()) return candidate;
            if (fallback == null && candidate.length() >= 4 && candidate.length() <= 20) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private String firstReadableLine(String text) {
        for (String line : text.split("\\R")) {
            String cleaned = URL.matcher(line).replaceAll("")
                    .replaceAll("(?:^|\\s)#[\\p{L}\\p{N}_]+", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            if (!cleaned.isBlank()) return truncate(cleaned, 500);
        }
        return null;
    }

    private String category(String placeName, String text) {
        String value = ((placeName == null ? "" : placeName) + " " + text)
                .toLowerCase(Locale.KOREAN)
                .replaceAll("\\s+", " ");
        if (value.matches(".*(맛집|음식점|식당|횟집|국수|냉면|갈비|치킨|먹방).*")) {
            return "음식점";
        }
        if (value.matches(".*(카페|커피|베이커리|빵집).*")) return "카페";
        if (value.matches(".*(숙소|호텔|펜션|리조트|호스텔|게스트하우스).*")) return "숙박";
        return null;
    }

    private String cleanCandidate(String value) {
        String cleaned = URL.matcher(value).replaceAll("")
                .replaceAll("[.,;!?]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? null : truncate(cleaned, 200);
    }

    private String combine(String primary, String shared) {
        if (primary == null || primary.isBlank()) return shared;
        if (shared == null || shared.isBlank()
                || primary.contains(shared) || shared.contains(primary)) return primary;
        return primary + "\n\n공유된 게시물 문구\n" + shared;
    }

    private String first(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private PageMetadata empty() {
        return new PageMetadata(null, null, null, null, null, null, null, null);
    }

    private record PlaceInfo(String placeName, String address) {
    }
}
