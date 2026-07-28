package com.sendit.map;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class KakaoTransitClientTest {

    private final KakaoTransitClient client =
            new KakaoTransitClient(new ObjectMapper(), "", 2, 2);

    @Test
    void parsesRecommendedTransitRouteAndSteps() throws Exception {
        String response = """
                {
                  "status": "OK",
                  "properties": {"landingURL": "https://map.kakao.com/route"},
                  "routes": [{
                    "properties": {
                      "type": "PUBLIC_TRANSIT",
                      "totalTime": 1621,
                      "totalDistance": 7050,
                      "transfers": 1,
                      "fare": {"value": 1500}
                    },
                    "steps": [{
                      "properties": {
                        "type": "SUBWAY",
                        "guidance": "1호선 승차",
                        "time": 601,
                        "distance": 4200,
                        "stops": [{"name": "서울역"}, {"name": "종각역"}],
                        "vehicles": [{"name": "1호선"}]
                      }
                    }]
                  }]
                }
                """;

        var route = client.parse(response).orElseThrow();

        assertThat(route.totalMinutes()).isEqualTo(28);
        assertThat(route.totalDistanceMeters()).isEqualTo(7050);
        assertThat(route.transfers()).isEqualTo(1);
        assertThat(route.fare()).isEqualTo(1500);
        assertThat(route.landingUrl()).isEqualTo("https://map.kakao.com/route");
        assertThat(route.steps()).singleElement().satisfies(step -> {
            assertThat(step.type()).isEqualTo("SUBWAY");
            assertThat(step.minutes()).isEqualTo(11);
            assertThat(step.startStop()).isEqualTo("서울역");
            assertThat(step.endStop()).isEqualTo("종각역");
            assertThat(step.vehicles()).containsExactly("1호선");
        });
    }
}
