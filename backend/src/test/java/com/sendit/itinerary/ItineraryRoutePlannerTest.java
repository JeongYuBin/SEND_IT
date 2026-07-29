package com.sendit.itinerary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sendit.map.KakaoDirectionsClient;
import com.sendit.map.KakaoTransitClient;
import com.sendit.place.Place;
import com.sendit.place.UserSavedPlace;
import com.sendit.tourism.TourApiClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItineraryRoutePlannerTest {

    private final KakaoTransitClient transitClient = mock(KakaoTransitClient.class);
    private final KakaoDirectionsClient directionsClient = mock(KakaoDirectionsClient.class);
    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final ItineraryRoutePlanner planner =
            new ItineraryRoutePlanner(transitClient, directionsClient, tourApiClient);

    @Test
    void ordersNearbyPlacesAndSplitsThemAcrossTravelDays() {
        ItineraryItem seoulStation = item(1, 37.5547, 126.9706);
        ItineraryItem distant = item(2, 37.5665, 126.9780);
        ItineraryItem nearby = item(3, 37.5563, 126.9723);
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getItems()).thenReturn(List.of(seoulStation, distant, nearby));
        when(itinerary.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getEndDate()).thenReturn(LocalDate.of(2026, 8, 2));
        when(itinerary.getDailyStartTime()).thenReturn(LocalTime.of(10, 0));
        when(itinerary.getDailyEndTime()).thenReturn(LocalTime.of(18, 0));
        when(itinerary.getTransportType()).thenReturn(TransportType.PUBLIC_TRANSIT);

        List<ItineraryRoutePlanner.DaySchedule> days = planner.plan(itinerary);

        assertThat(days).hasSize(2);
        assertThat(days.getFirst().stops()).hasSize(2);
        assertThat(days.getFirst().stops().getFirst().item()).isSameAs(seoulStation);
        assertThat(days.getFirst().stops().get(1).item()).isSameAs(nearby);
        assertThat(days.get(1).stops()).hasSize(1);
        assertThat(days.get(1).stops().getFirst().item()).isSameAs(distant);
        assertThat(days.getFirst().stops().get(1).travelMinutesFromPrevious()).isZero();
    }

    @Test
    void appliesFallbackTravelTimeWhenCoordinatesAreMissing() {
        ItineraryItem first = item(1, null, null);
        ItineraryItem second = item(2, null, null);
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getItems()).thenReturn(List.of(first, second));
        when(itinerary.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getEndDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getDailyStartTime()).thenReturn(LocalTime.of(9, 0));
        when(itinerary.getDailyEndTime()).thenReturn(LocalTime.of(18, 0));
        when(itinerary.getTransportType()).thenReturn(TransportType.CAR);

        var stops = planner.plan(itinerary).getFirst().stops();

        assertThat(stops.get(1).travelMinutesFromPrevious()).isEqualTo(20);
        assertThat(stops.get(1).distanceKmFromPrevious()).isNull();
        assertThat(stops.get(1).coordinateAvailable()).isFalse();
    }

    @Test
    void honorsPreferredVisitDateAndStartTime() {
        ItineraryItem fixed = item(1, 37.5, 127.0);
        when(fixed.getPreferredVisitDate()).thenReturn(LocalDate.of(2026, 8, 2));
        when(fixed.getPreferredStartTime()).thenReturn(LocalTime.of(14, 30));
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getItems()).thenReturn(List.of(fixed));
        when(itinerary.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getEndDate()).thenReturn(LocalDate.of(2026, 8, 2));
        when(itinerary.getDailyStartTime()).thenReturn(LocalTime.of(9, 0));
        when(itinerary.getDailyEndTime()).thenReturn(LocalTime.of(18, 0));
        when(itinerary.getTransportType()).thenReturn(TransportType.PUBLIC_TRANSIT);

        var days = planner.plan(itinerary);

        assertThat(days.getFirst().stops()).isEmpty();
        assertThat(days.get(1).stops().getFirst().arrivalTime()).isEqualTo(LocalTime.of(14, 30));
    }

    @Test
    void warnsWhenVisitStartsBeforeTourismOperatingHours() {
        ItineraryItem place = item(1, 37.5, 127.0);
        when(tourApiClient.operatingInfo(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.nullable(String.class)
        )).thenReturn(java.util.Optional.of(new TourApiClient.OperatingInfo(
                "10:00~18:00",
                "연중무휴",
                new TourApiClient.TimeRange(LocalTime.of(10, 0), LocalTime.of(18, 0))
        )));
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getItems()).thenReturn(List.of(place));
        when(itinerary.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getEndDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getDailyStartTime()).thenReturn(LocalTime.of(9, 0));
        when(itinerary.getDailyEndTime()).thenReturn(LocalTime.of(19, 0));
        when(itinerary.getTransportType()).thenReturn(TransportType.WALKING);

        var stop = planner.plan(itinerary).getFirst().stops().getFirst();

        assertThat(stop.operatingInfo()).isNotNull();
        assertThat(stop.visitWarning()).contains("운영 시작");
    }

    @Test
    void usesKakaoTransitRouteForPublicTransitSegments() {
        ItineraryItem first = item(1, 37.5547, 126.9706);
        ItineraryItem second = item(2, 37.5665, 126.9780);
        when(transitClient.route(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(java.util.Optional.of(new KakaoTransitClient.TransitRoute(
                "PUBLIC_TRANSIT", 32, 7_100, 1, 1_500,
                "https://map.kakao.com", List.of(), List.of()
        )));
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getItems()).thenReturn(List.of(first, second));
        when(itinerary.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getEndDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getDailyStartTime()).thenReturn(LocalTime.of(9, 0));
        when(itinerary.getDailyEndTime()).thenReturn(LocalTime.of(18, 0));
        when(itinerary.getTransportType()).thenReturn(TransportType.PUBLIC_TRANSIT);

        var secondStop = planner.plan(itinerary).getFirst().stops().get(1);

        assertThat(secondStop.travelMinutesFromPrevious()).isEqualTo(32);
        assertThat(secondStop.distanceKmFromPrevious()).isEqualTo(7.1);
        assertThat(secondStop.transitRoute()).isNotNull();
    }

    @Test
    void includesTransferFromPreviousDaysLastPlace() {
        ItineraryItem firstDay = item(1, 38.1906, 128.6020);
        ItineraryItem secondDay = item(2, 37.4803, 126.8898);
        when(firstDay.getPreferredVisitDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(secondDay.getPreferredVisitDate()).thenReturn(LocalDate.of(2026, 8, 2));
        when(directionsClient.route(
                org.mockito.ArgumentMatchers.eq(TransportType.CAR),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(java.util.Optional.of(
                new KakaoDirectionsClient.RouteEstimate(150, 205_000, List.of())));
        Itinerary itinerary = mock(Itinerary.class);
        when(itinerary.getItems()).thenReturn(List.of(firstDay, secondDay));
        when(itinerary.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(itinerary.getEndDate()).thenReturn(LocalDate.of(2026, 8, 2));
        when(itinerary.getDailyStartTime()).thenReturn(LocalTime.of(10, 0));
        when(itinerary.getDailyEndTime()).thenReturn(LocalTime.of(18, 0));
        when(itinerary.getTransportType()).thenReturn(TransportType.CAR);

        var secondDayStop = planner.plan(itinerary).get(1).stops().getFirst();

        assertThat(secondDayStop.crossDayTransfer()).isTrue();
        assertThat(secondDayStop.travelMinutesFromPrevious()).isEqualTo(150);
        assertThat(secondDayStop.distanceKmFromPrevious()).isEqualTo(205.0);
        assertThat(secondDayStop.arrivalTime()).isEqualTo(LocalTime.of(10, 0));
    }

    private ItineraryItem item(int sequence, Double latitude, Double longitude) {
        Place place = mock(Place.class);
        when(place.getName()).thenReturn("테스트 장소 " + sequence);
        when(place.getLatitude()).thenReturn(latitude);
        when(place.getLongitude()).thenReturn(longitude);
        UserSavedPlace savedPlace = mock(UserSavedPlace.class);
        when(savedPlace.getPlace()).thenReturn(place);
        ItineraryItem item = mock(ItineraryItem.class);
        when(item.getSequence()).thenReturn(sequence);
        when(item.getStayMinutes()).thenReturn(60);
        when(item.getSavedPlace()).thenReturn(savedPlace);
        return item;
    }
}
