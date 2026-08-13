package com.sendit.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceLocationResolverTest {
    private final KakaoPlaceDirectoryClient kakao = mock(KakaoPlaceDirectoryClient.class);
    private final PlaceLocationResolver resolver = new PlaceLocationResolver(kakao);

    @Test
    void selectsCandidateMatchingEditedAddress() {
        var wrong = result("노기", "전남 순천시 중앙로 1", 34.9, 127.4);
        var matched = result("노기", "전남 화순군 백아면 노기리", 35.1, 127.1);
        when(kakao.search("노기 전남 화순군 백아면 노기리", 1))
                .thenReturn(new PlaceSearchDtos.Response(List.of(wrong, matched), 1, true));

        assertThat(resolver.resolve("노기", "전남 화순군 백아면 노기리"))
                .isEqualTo(matched);
    }

    @Test
    void rejectsUnrelatedSearchResult() {
        var unrelated = result("다른 장소", "서울 중구 세종대로 1", 37.5, 126.9);
        when(kakao.search("노기 전남 화순군 백아면 노기리", 1))
                .thenReturn(new PlaceSearchDtos.Response(List.of(unrelated), 1, true));

        assertThatThrownBy(() -> resolver.resolve("노기", "전남 화순군 백아면 노기리"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private PlaceSearchDtos.Result result(String name, String roadAddress,
            double latitude, double longitude) {
        return new PlaceSearchDtos.Result("1", name, null, null, roadAddress,
                roadAddress, null, latitude, longitude, "https://place.map.kakao.com/1");
    }
}
