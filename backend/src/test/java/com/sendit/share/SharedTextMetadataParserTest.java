package com.sendit.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SharedTextMetadataParserTest {
    private final SharedTextMetadataParser parser = new SharedTextMetadataParser();

    @Test
    void extractsRestaurantFromSharedHashtags() {
        PageMetadata result = parser.parse("""
                강릉 여행 중 찾은 현지 횟집
                https://www.instagram.com/reel/example/
                #강릉맛집 #건도리횟집 #여행스타그램
                """);

        assertThat(result.title()).isEqualTo("강릉 여행 중 찾은 현지 횟집");
        assertThat(result.placeName()).isEqualTo("건도리횟집");
        assertThat(result.category()).isEqualTo("음식점");
        assertThat(result.description()).contains("#건도리횟집");
    }

    @Test
    void prefersExplicitBusinessNameAndMergesPageMetadata() {
        PageMetadata shared = parser.parse("상호: 솔바람 카페\n#속초카페");
        PageMetadata page = new PageMetadata(
                "원본 게시물", null, "https://example.com/a.jpg",
                null, null, null, null, null);

        PageMetadata result = parser.merge(page, shared);

        assertThat(result.title()).isEqualTo("원본 게시물");
        assertThat(result.placeName()).isEqualTo("솔바람 카페");
        assertThat(result.category()).isEqualTo("카페");
        assertThat(result.imageUrl()).isEqualTo("https://example.com/a.jpg");
    }

    @Test
    void ignoresRegionalFoodTagAndExtractsInstagramBusinessTag() {
        PageMetadata result = parser.parse("""
                유행하는 양파쌈 레시피
                낙지본집 사장님도 인정한 양파쌈
                #양파쌈 #평택맛집 #낙지본집 #동삭동
                """);

        assertThat(result.placeName()).isEqualTo("낙지본집");
        assertThat(result.category()).isEqualTo("음식점");
    }

    @Test
    void extractsYouTubeRestaurantInfoBeforeGenericHashtag() {
        String description = """
                [식당정보] 스시화 서울 송파구 잠실동 207-16 https://naver.me/xf5AjmZ6
                제 채널에 식당 광고는 단 한 건도 없습니다.
                #잠실맛집 #가성비이자카야 #가성비맛집
                """;

        PageMetadata result = parser.parse("고등어 봉초밥이 9천원이요!?\n" + description);

        assertThat(result.placeName()).isEqualTo("스시화");
        assertThat(result.address()).isEqualTo("서울 송파구 잠실동 207-16");
        assertThat(result.category()).isEqualTo("음식점");
    }

    @Test
    void doesNotAppendSharedTextWhenItAlreadyContainsOriginalDescription() {
        PageMetadata page = new PageMetadata("영상", "원본 설명", null,
                null, null, null, null, null);
        PageMetadata shared = parser.parse("영상 제목\n원본 설명\n#잠실맛집");

        PageMetadata result = parser.merge(page, shared);

        assertThat(result.description()).isEqualTo("원본 설명");
    }

    @Test
    void extractsInstagramStyleExplicitPlaceAndRoadAddress() {
        PageMetadata result = parser.parse("""
                오늘 꼭 가볼 곳 저장해 두세요 ✨
                매장명: 바다정원
                위치: 강원특별자치도 강릉시 창해로 427
                #강릉맛집 #가성비카페
                """);

        assertThat(result.placeName()).isEqualTo("바다정원");
        assertThat(result.address()).isEqualTo("강원특별자치도 강릉시 창해로 427");
    }

    @Test
    void extractsTikTokStyleLodgingInfoBeforeHashtags() {
        PageMetadata result = parser.parse("""
                [숙소정보] 오션스테이 부산 해운대구 달맞이길 30
                #부산여행 #감성숙소 #오션뷰숙소
                """);

        assertThat(result.placeName()).isEqualTo("오션스테이");
        assertThat(result.address()).isEqualTo("부산 해운대구 달맞이길 30");
        assertThat(result.category()).isEqualTo("숙박");
    }

    @Test
    void extractsBlogStyleBusinessNameAndLotAddress() {
        PageMetadata result = parser.parse("""
                제주 여행에서 발견한 식당
                업체명 - 돌담식탁
                지번주소 - 제주특별자치도 제주시 애월읍 123-4
                #애월맛집 #가성비맛집
                """);

        assertThat(result.placeName()).isEqualTo("돌담식탁");
        assertThat(result.address()).isEqualTo("제주특별자치도 제주시 애월읍 123-4");
        assertThat(result.category()).isEqualTo("음식점");
    }

    @Test
    void doesNotUsePromotionalHashtagAsPlaceName() {
        PageMetadata result = parser.parse("""
                여기 분위기 정말 좋아요
                #잠실맛집 #가성비이자카야 #데이트추천 #내돈내산
                """);

        assertThat(result.placeName()).isNull();
    }
}
