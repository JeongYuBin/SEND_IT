package com.sendit.notification;

import com.sendit.itinerary.Itinerary;
import com.sendit.itinerary.ItineraryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ItineraryReminderScheduler {
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private final ItineraryRepository itineraries;
    private final NotificationRepository notifications;
    private final Clock clock;

    @Autowired
    public ItineraryReminderScheduler(
            ItineraryRepository itineraries,
            NotificationRepository notifications
    ) {
        this(itineraries, notifications, Clock.system(KOREA));
    }

    ItineraryReminderScheduler(
            ItineraryRepository itineraries,
            NotificationRepository notifications,
            Clock clock
    ) {
        this.itineraries = itineraries;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.notification.itinerary-reminder-cron:0 0 * * * *}", zone = "Asia/Seoul")
    @Transactional
    public void createUpcomingReminders() {
        createUpcomingReminders(LocalDate.now(clock).plusDays(1));
    }

    void createUpcomingReminders(LocalDate startDate) {
        itineraries.findByStartDate(startDate).forEach(this::createIfAbsent);
    }

    private void createIfAbsent(Itinerary itinerary) {
        String uniqueKey = "itinerary-upcoming:" + itinerary.getId() + ":" + itinerary.getStartDate();
        if (notifications.existsByUniqueKey(uniqueKey)) return;
        String message = itinerary.getStartDate() + "에 시작하는 ‘" + itinerary.getTitle()
                + "’ 여행을 확인해 주세요.";
        notifications.save(new Notification(
                itinerary.getUser(), NotificationType.ITINERARY_UPCOMING,
                "내일 여행이 시작됩니다", message,
                "/itineraries/" + itinerary.getId(), uniqueKey));
    }
}
