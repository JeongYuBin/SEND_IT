package com.sendit.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendit.itinerary.Itinerary;
import com.sendit.itinerary.ItineraryRepository;
import com.sendit.itinerary.TransportType;
import com.sendit.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ItineraryReminderSchedulerTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 8, 12);
    private final ItineraryRepository itineraries = Mockito.mock(ItineraryRepository.class);
    private final NotificationRepository notifications = Mockito.mock(NotificationRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-11T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ItineraryReminderScheduler scheduler =
            new ItineraryReminderScheduler(itineraries, notifications, clock);

    @Test
    void createsReminderForTomorrowItinerary() {
        Itinerary itinerary = itinerary();
        when(itineraries.findByStartDate(START_DATE)).thenReturn(List.of(itinerary));

        scheduler.createUpcomingReminders(START_DATE);

        verify(notifications).save(any(Notification.class));
    }

    @Test
    void skipsReminderWhenUniqueKeyAlreadyExists() {
        Itinerary itinerary = itinerary();
        when(itineraries.findByStartDate(START_DATE)).thenReturn(List.of(itinerary));
        when(notifications.existsByUniqueKey(any())).thenReturn(true);

        scheduler.createUpcomingReminders(START_DATE);

        verify(notifications, never()).save(any());
    }

    private Itinerary itinerary() {
        return new Itinerary(new User("user@example.com", "password", "여행자"),
                "속초 여행", START_DATE, START_DATE.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(18, 0), TransportType.CAR);
    }
}
