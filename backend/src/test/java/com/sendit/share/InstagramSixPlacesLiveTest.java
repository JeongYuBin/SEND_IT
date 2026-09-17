package com.sendit.share;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in public-page regression; no account writes or share creation. */
@EnabledIfEnvironmentVariable(named = "SENDIT_LIVE_REEL_TEST", matches = "true")
class InstagramSixPlacesLiveTest {
    @Test
    void resolvesSixBusinessesFromReportedPublicReel() throws Exception {
        String url = "https://www.instagram.com/reel/DW87bK4gY7Y/";
        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15)).build();
        var response = client.send(HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(25)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var mapper = new ObjectMapper();
        var metadata = new PageMetadataParser(mapper).parse(response.body(), url);
        var kakao = new KakaoPlaceSearchClient(mapper, System.getenv("KAKAO_REST_API_KEY"),
                "https://dapi.kakao.com/v2/local/search/keyword.json", 10, 15);
        var results = new MultiPlaceExtractor(kakao).extractDescription(metadata.description(), metadata);
        assertThat(results).extracting(PageMetadata::placeName)
                .containsExactly("개뿔", "문화식당", "창창", "새서울", "선셋무드", "그루바");
        assertThat(results).allMatch(new PlaceVerificationPolicy()::isVerified);
    }
}
