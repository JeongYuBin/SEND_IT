package com.sendit.itinerary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sendit.place.Place;
import com.sendit.place.UserSavedPlace;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItineraryRoutePlannerTest {

    private final ItineraryRoutePlanner planner = new ItineraryRoutePlanner();

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
        assertThat(days.getFirst().stops().get(1).travelMinutesFromPrevious()).isGreaterThanOrEqualTo(5);
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

    private ItineraryItem item(int sequence, Double latitude, Double longitude) {
        Place place = mock(Place.class);
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
