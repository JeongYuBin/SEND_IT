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
}
