package com.sendit.place;

import java.util.Comparator;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PlaceLocationResolver {
    private final KakaoPlaceDirectoryClient kakao;

    public PlaceLocationResolver(KakaoPlaceDirectoryClient kakao) {
        this.kakao = kakao;
    }

    public PlaceSearchDtos.Result resolve(String name, String address) {
        String normalizedName = normalize(name);
        String normalizedAddress = normalize(address);
        var response = kakao.search(name.trim() + " " + address.trim(), 1);
        var candidates = response.places();
        if (candidates.isEmpty()) {
            candidates = kakao.search(address.trim(), 1).places();
        }

        return candidates.stream()
                .filter(result -> result.latitude() != null && result.longitude() != null)
                .max(Comparator.comparingInt(result -> score(
                        normalizedName, normalizedAddress, result)))
                .filter(result -> score(normalizedName, normalizedAddress, result) >= 3)
                .orElseThrow(() -> new IllegalArgumentException(
                        "변경한 장소명과 주소에 일치하는 위치를 카카오맵에서 찾지 못했습니다."));
    }

    private int score(String name, String address, PlaceSearchDtos.Result result) {
        String candidateName = normalize(result.name());
        String candidateRoadAddress = normalize(result.roadAddress());
        String candidateAddress = normalize(result.address());
        int score = 0;
        if (!name.isEmpty() && name.equals(candidateName)) score += 5;
        else if (!name.isEmpty() && (name.contains(candidateName) || candidateName.contains(name))) score += 3;
        if (!address.isEmpty() && (address.equals(candidateRoadAddress)
                || address.equals(candidateAddress))) score += 7;
        else if (!address.isEmpty() && (containsEither(address, candidateRoadAddress)
                || containsEither(address, candidateAddress))) score += 3;
        return score;
    }

    private boolean containsEither(String left, String right) {
        return !right.isEmpty() && (left.contains(right) || right.contains(left));
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z가-힣]", "");
    }
}
