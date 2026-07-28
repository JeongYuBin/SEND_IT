package com.sendit.itinerary;

import com.sendit.place.UserSavedPlace;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "itinerary_items")
public class ItineraryItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "saved_place_id")
    private UserSavedPlace savedPlace;
    @Column(nullable = false)
    private int sequence;
    @Column(name = "stay_minutes", nullable = false)
    private int stayMinutes;
    @Column(name = "preferred_visit_date")
    private LocalDate preferredVisitDate;
    @Column(name = "preferred_start_time")
    private LocalTime preferredStartTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type_from_previous")
    private TransportType transportTypeFromPrevious;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ItineraryItem() {}

    ItineraryItem(Itinerary itinerary, UserSavedPlace savedPlace, int sequence, int stayMinutes) {
        this.itinerary = itinerary;
        this.savedPlace = savedPlace;
        this.sequence = sequence;
        this.stayMinutes = stayMinutes;
    }

    public UserSavedPlace getSavedPlace() { return savedPlace; }
    public int getSequence() { return sequence; }
    public int getStayMinutes() { return stayMinutes; }
    public LocalDate getPreferredVisitDate() { return preferredVisitDate; }
    public LocalTime getPreferredStartTime() { return preferredStartTime; }
    public TransportType getTransportTypeFromPrevious() { return transportTypeFromPrevious; }

    public void updateSchedule(LocalDate visitDate, LocalTime startTime, int stayMinutes) {
        this.preferredVisitDate = visitDate;
        this.preferredStartTime = visitDate == null ? null : startTime;
        this.stayMinutes = stayMinutes;
    }

    public void updateOrdering(LocalDate visitDate, int sequence) {
        this.preferredVisitDate = visitDate;
        this.sequence = sequence;
    }

    public void updateTransportType(TransportType transportType) {
        this.transportTypeFromPrevious = transportType;
    }

    void clearPreferenceOutside(LocalDate startDate, LocalDate endDate) {
        if (preferredVisitDate != null
                && (preferredVisitDate.isBefore(startDate) || preferredVisitDate.isAfter(endDate))) {
            preferredVisitDate = null;
            preferredStartTime = null;
        }
    }
}
