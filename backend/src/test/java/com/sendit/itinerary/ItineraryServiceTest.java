package com.sendit.itinerary;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.place.Place;
import com.sendit.place.UserSavedPlace;
import com.sendit.place.UserSavedPlaceRepository;
import com.sendit.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ItineraryServiceTest {
    private final ItineraryRepository itineraries = mock(ItineraryRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserSavedPlaceRepository savedPlaces = mock(UserSavedPlaceRepository.class);
    private final ItineraryRoutePlanner routePlanner = mock(ItineraryRoutePlanner.class);
    private final ItineraryService service = new ItineraryService(
            itineraries, users, savedPlaces, routePlanner);

    @Test
    void rejectsScheduleDateOutsideEventPeriod() {
        LocalDate eventEnd = LocalDate.of(2026, 8, 10);
        Itinerary itinerary = itineraryWithEvent(eventEnd);
        when(itineraries.findByIdAndUserEmail(1L, "user@example.com"))
                .thenReturn(Optional.of(itinerary));

        assertThatThrownBy(() -> service.updateItemSchedule(
                "user@example.com", 1L, 10L,
                new ItineraryDtos.UpdateItemScheduleRequest(
                        eventEnd.plusDays(1), LocalTime.of(10, 0), 60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("행사 종료일");
    }

    @Test
    void validatesEventPeriodBeforeReorderingAnyItem() {
        LocalDate eventEnd = LocalDate.of(2026, 8, 10);
        Itinerary itinerary = itineraryWithEvent(eventEnd);
        when(itineraries.findByIdAndUserEmail(1L, "user@example.com"))
                .thenReturn(Optional.of(itinerary));

        assertThatThrownBy(() -> service.reorder(
                "user@example.com", 1L,
                new ItineraryDtos.ReorderRequest(List.of(
                        new ItineraryDtos.ReorderItemRequest(
                                10L, eventEnd.plusDays(1), 1, TransportType.CAR)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("행사 종료일");
        verify(itineraries, never()).flush();
    }

    private Itinerary itineraryWithEvent(LocalDate eventEnd) {
        UserSavedPlace saved = mock(UserSavedPlace.class);
        Place place = mock(Place.class);
        when(saved.getId()).thenReturn(10L);
        when(saved.getPlace()).thenReturn(place);
        when(place.getEventEndDate()).thenReturn(eventEnd);
        Itinerary itinerary = new Itinerary(null, "여행",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 20),
                LocalTime.of(9, 0), LocalTime.of(20, 0), TransportType.CAR);
        itinerary.addPlace(saved, 1, LocalDate.of(2026, 8, 5));
        return itinerary;
    }
}
