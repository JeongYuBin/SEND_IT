package com.sendit.itinerary;

import com.sendit.place.UserSavedPlace;
import com.sendit.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "itineraries")
public class Itinerary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "daily_start_time", nullable = false)
    private LocalTime dailyStartTime;
    @Column(name = "daily_end_time", nullable = false)
    private LocalTime dailyEndTime;
    @Enumerated(EnumType.STRING) @Column(name = "transport_type", nullable = false)
    private TransportType transportType;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ItineraryStatus status;
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ItineraryItem> items = new ArrayList<>();
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Itinerary() {}

    public Itinerary(User user, String title, LocalDate startDate, LocalDate endDate,
                     LocalTime dailyStartTime, LocalTime dailyEndTime, TransportType transportType) {
        this.user = user;
        this.title = title.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyStartTime = dailyStartTime;
        this.dailyEndTime = dailyEndTime;
        this.transportType = transportType;
        this.status = ItineraryStatus.DRAFT;
    }

    public void addPlace(UserSavedPlace savedPlace, int sequence) {
        items.add(new ItineraryItem(this, savedPlace, sequence, 60));
    }

    public void addPlace(UserSavedPlace savedPlace, int sequence, LocalDate visitDate) {
        ItineraryItem item = new ItineraryItem(this, savedPlace, sequence, 60);
        item.updateOrdering(visitDate, sequence);
        items.add(item);
    }

    public boolean removePlace(Long savedPlaceId) {
        return items.removeIf(item -> item.getSavedPlace().getId().equals(savedPlaceId));
    }

    public void markGenerated() {
        status = ItineraryStatus.GENERATED;
    }

    public void update(String title, LocalDate startDate, LocalDate endDate,
                       LocalTime dailyStartTime, LocalTime dailyEndTime,
                       TransportType transportType) {
        this.title = title.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyStartTime = dailyStartTime;
        this.dailyEndTime = dailyEndTime;
        this.transportType = transportType;
        this.status = ItineraryStatus.GENERATED;
        items.forEach(item -> item.clearPreferenceOutside(startDate, endDate));
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalTime getDailyStartTime() { return dailyStartTime; }
    public LocalTime getDailyEndTime() { return dailyEndTime; }
    public TransportType getTransportType() { return transportType; }
    public ItineraryStatus getStatus() { return status; }
    public List<ItineraryItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
}
