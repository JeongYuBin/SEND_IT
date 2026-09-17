package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class MultiPlaceExtractorTest {
    private final KakaoPlaceSearchClient kakao = mock(KakaoPlaceSearchClient.class);
    private final MultiPlaceExtractor extractor = new MultiPlaceExtractor(kakao);

    @Test
    void separatesRestaurantNamesFromYouTubeCaptionAndRejectsConversation() {
        PageMetadata source = new PageMetadata("구디단 직장인 맛집", "", "thumb",
                null, null, null, null, null);
        when(kakao.resolveCandidate("하이탕마라탕", "서울 구로구"))
                .thenReturn(Optional.of(place("하이탕마라탕", "서울 구로구 디지털로 1")));
        when(kakao.resolveCandidate("무한자금성", "서울 구로구"))
                .thenReturn(Optional.of(place("무한자금성", "서울 구로구 디지털로 2")));

        var result = extractor.extractCaption("""
                안녕하세요 안녕하세요 안녕하세요 오늘 배고파요
                금성이라고 있어요 무한자금성
                어 하이탕 마라탕 하이탕 마라탕
                """, source);

        assertThat(result).extracting(PageMetadata::placeName)
                .containsExactly("무한자금성", "하이탕마라탕");
    }

    private PageMetadata place(String name, String address) {
        return new PageMetadata(null, null, null, name, "음식점", address, 37.0, 127.0);
    }

    @Test
    void findsMultipleCaptionPlacesWithTheirOwnAddressesAndDeduplicates() {
        PageMetadata source = new PageMetadata("제주 여행", null, null, null, null, null, null, null);
        when(kakao.resolveCandidate("돌담카페", "제주 제주시 애월로 12"))
                .thenReturn(Optional.of(place("돌담카페", "제주 제주시 애월로 12")));
        when(kakao.resolveCandidate("바다식탁", "제주 서귀포시 중문로 24"))
                .thenReturn(Optional.of(place("바다식탁", "제주 서귀포시 중문로 24")));
        var result = extractor.extractDescription("""
                📍돌담카페
                주소: 제주 제주시 애월로 12
                📍바다식탁
                주소: 제주 서귀포시 중문로 24
                📍돌담카페
                주소: 제주 제주시 애월로 12
                #제주맛집 #광고
                """, source);
        assertThat(result).extracting(PageMetadata::placeName).containsExactly("돌담카페", "바다식탁");
        org.mockito.Mockito.verify(kakao, org.mockito.Mockito.times(1))
                .resolveCandidate("돌담카페", "제주 제주시 애월로 12");
    }

    @Test
    void doesNotTreatUnresolvedCaptionTextAsAVerifiedPlace() {
        PageMetadata source = new PageMetadata(null, null, null, null, null, null, null, null);
        assertThat(extractor.extractDescription("📍비공개장소\n주소: 제주 제주시 애월로 12", source)).isEmpty();
    }
}
