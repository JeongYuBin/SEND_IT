package com.sendit.place;

import java.util.Map;

public final class AddressNormalizer {
    private static final Map<String, String> REGION_NAMES = Map.ofEntries(
            Map.entry("서울", "서울특별시"), Map.entry("서울시", "서울특별시"),
            Map.entry("부산", "부산광역시"), Map.entry("부산시", "부산광역시"),
            Map.entry("대구", "대구광역시"), Map.entry("대구시", "대구광역시"),
            Map.entry("인천", "인천광역시"), Map.entry("인천시", "인천광역시"),
            Map.entry("광주", "광주광역시"), Map.entry("광주시", "광주광역시"),
            Map.entry("대전", "대전광역시"), Map.entry("대전시", "대전광역시"),
            Map.entry("울산", "울산광역시"), Map.entry("울산시", "울산광역시"),
            Map.entry("세종", "세종특별자치시"), Map.entry("세종시", "세종특별자치시"),
            Map.entry("경기", "경기도"),
            Map.entry("강원", "강원특별자치도"), Map.entry("강원도", "강원특별자치도"),
            Map.entry("충북", "충청북도"), Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"), Map.entry("전라북도", "전북특별자치도"),
            Map.entry("전남", "전라남도"), Map.entry("경북", "경상북도"),
            Map.entry("경남", "경상남도"), Map.entry("제주", "제주특별자치도"),
            Map.entry("제주도", "제주특별자치도")
    );

    private AddressNormalizer() {}

    public static String normalize(String address) {
        if (address == null) return null;
        String trimmed = address.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) return null;
        int separator = trimmed.indexOf(' ');
        String region = separator < 0 ? trimmed : trimmed.substring(0, separator);
        String normalizedRegion = REGION_NAMES.get(region);
        if (normalizedRegion == null) return trimmed;
        return separator < 0 ? normalizedRegion : normalizedRegion + trimmed.substring(separator);
    }
}
